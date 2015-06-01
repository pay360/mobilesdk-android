/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypoint.sdk.library.ThreeDSActivity;
import com.paypoint.sdk.library.device.DeviceManager;
import com.paypoint.sdk.library.exception.InvalidCredentialsException;
import com.paypoint.sdk.library.exception.PaymentInProgressException;
import com.paypoint.sdk.library.exception.PaymentValidationException;
import com.paypoint.sdk.library.network.EndpointManager;
import com.paypoint.sdk.library.network.NetworkManager;
import com.paypoint.sdk.library.network.PayPointService;
import com.paypoint.sdk.library.network.SelfSignedSocketFactory;
import com.paypoint.sdk.library.payment.request.CustomField;
import com.paypoint.sdk.library.payment.request.DeviceInfo;
import com.paypoint.sdk.library.payment.request.PaymentCard;
import com.paypoint.sdk.library.payment.request.PaymentMethod;
import com.paypoint.sdk.library.payment.request.MakePaymentRequest;
import com.paypoint.sdk.library.payment.request.ThreeDSResumeRequest;
import com.paypoint.sdk.library.payment.response.MakePaymentResponse;
import com.paypoint.sdk.library.security.PayPointCredentials;
import com.paypoint.sdk.library.utils.Timer;
import com.squareup.okhttp.OkHttpClient;

import java.lang.ref.WeakReference;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedByteArray;
import rx.Observer;
import rx.subscriptions.CompositeSubscription;

/**
 * Handles payments
 *
 * <p>Call {@link #makePayment(PaymentRequest)} to initiate payment
 */
public class PaymentManager {

    private static final int HTTP_TIMEOUT_CONNECTION                = 10; // 10s

    private static final int TIMEOUT_RESPONSE_PAYMENT          = 30; // 30s
    private static final int TIMEOUT_RESPONSE_RESUME           = 30; // 30s
    private static final int TIMEOUT_RESPONSE_STATUS           = 5; // 5s

    private static final int DEFAULT_SESSION_TIMEOUT           = 60; // 60s


    private static final int REASON_SUSPENDED_FOR_3D_SECURE         = 7;
    private static final int REASON_SUSPENDED_FOR_CLIENT_REDIRECT   = 8;

    public interface MakePaymentCallback {

        public void paymentSucceeded(PaymentSuccess success);

        public void paymentFailed(PaymentError error);
    }

    private Context context;
    private int sessionTimeoutSeconds = DEFAULT_SESSION_TIMEOUT;
    private String url;
    private PayPointCredentials credentials;

    // use a WeakReference to ensure calling activity can be GC'ed
    private WeakReference<PaymentManager.MakePaymentCallback> callback;
    private boolean callbackLocked = false;
    private CallbackPending callbackPending;
    private boolean isCustomUrl;
    private DeviceManager deviceManager;
    private DeviceInfo deviceInfo;
    private String transactionId;
    private String operationId;
    private PayPointService service;
    private MakePaymentRequest makePaymentRequest;
    private ThreeDSResumeRequest threeDSResumeRequest;
    private Timer sessionTimer;
    private State state = State.STATE_IDLE;
    private CompositeSubscription subscriptions = new CompositeSubscription();

    // flag to toggle reliable delivery e.g. when making payment internally call GET status
    // until session timeout - don't require same functionality if app calls GET status directly
    private boolean reliableDeliveryEnabled = false;

    private static PaymentManager instance;

    private class CallbackPending {

        private boolean isError;
        private PaymentSuccess paymentSuccess;
        private PaymentError paymentError;
    }

    private enum State {
        STATE_IDLE,
        STATE_PAYMENT_WAITING_NETWORK,
        STATE_PAYMENT_WAITING_RESPONSE,
        STATE_RESUME_WAITING_NETWORK,
        STATE_RESUME_WAITING_RESPONSE,
        STATE_STATUS_WAITING_NETWORK,
        STATE_STATUS_WAITING_RESPONSE,
        STATE_SUSPENDED_FOR_3DS
    }

    private enum Event {
        EVENT_RESPONSE_NOT_RECEIVED,
        EVENT_NETWORK_CONNECTED,
        EVENT_SESSION_TIMEOUT;
    }

    // Requires a singleton to maintain state between screen orientation changes
    public synchronized static PaymentManager getInstance(Context context) {
        if (instance == null) {
            instance = new PaymentManager(context);
        }
        return instance;
    }

    private PaymentManager() {
        // private as a singleton
    }

    private PaymentManager(Context context) {
        // use the application context - DO NOT HOLD ONTO AN ACTIVITY CONTEXT as PaymentManager
        // is a singleton an may\will outlast the activity leading to memory leaks
        this.context = context.getApplicationContext();

        // no need to unregister receivers as PaymentManager is a singleton as thus has the
        // same lifetime as the app hence won't not unregistering won't cause any leaks

        // register to receive events from 3DS activity
        this.context.registerReceiver(new ThreeDSecureReceiver(),
                new IntentFilter(ThreeDSActivity.ACTION_COMPLETED));

        // register to receive network connectivity events
        this.context.registerReceiver(new NetworkConnectivityReceiver(),
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        deviceManager = new DeviceManager(this.context);
        deviceInfo = new DeviceInfo()
                .setSdkInstallId(deviceManager.getSdkInstallId())
                .setOsFamily(deviceManager.getOsFamily())
                .setOsName(deviceManager.getOsName())
                .setManufacturer(deviceManager.getManufacturer())
                .setModelFamily(deviceManager.getModelFamily())
                .setModelName(deviceManager.getModelName())
                .setType(deviceManager.getType())
                .setScreenRes(deviceManager.getScreenRes())
                .setScreenDpi(deviceManager.getScreenDpi());
    }

    private PayPointService createService(String serverUrl)
        throws NoSuchAlgorithmException, KeyManagementException {

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS")
                .create();

        OkHttpClient httpClient = new OkHttpClient();
        httpClient.setConnectTimeout(HTTP_TIMEOUT_CONNECTION, TimeUnit.SECONDS);
        httpClient.setReadTimeout(DEFAULT_SESSION_TIMEOUT, TimeUnit.SECONDS);

        // setting the executor is required for the Robolectric tests to run
        Executor executor = Executors.newSingleThreadExecutor();

        isCustomUrl = false;

        // by default Retrofit will throw an error if self signed certificate is used so allow
        // self signed certificate for custom URLs e.g. anything other than production
        if (EndpointManager.isCustomUrl(serverUrl)) {
            httpClient.setSslSocketFactory(new SelfSignedSocketFactory().build());
            isCustomUrl = true;
        }

        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(serverUrl)
                .setExecutors(executor, executor)
                .setConverter(new GsonConverter(gson))
//                .setLogLevel(RestAdapter.LogLevel.FULL)
//                .setLog(new AndroidLog(Logger.TAG))
                .setClient(new OkClient(httpClient))
                .build();

        return adapter.create(PayPointService.class);
    }

    /**
     * Timeout waiting for response from PayPoint server
     * Defaults to 60s.
     * DO NOT ALTER WITHOUT GOOD REASON
     * @param sessionTimeoutSeconds
     * @return
     */
    public PaymentManager setSessionTimeout(int sessionTimeoutSeconds) {
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
        return this;
    }

    /**
     * URL of the PayPoint server
     * @param url the base url of the PayPoint server. The URL can be obtained from
     * {@link com.paypoint.sdk.library.network.EndpointManager#getEndpointUrl(com.paypoint.sdk.library.network.EndpointManager.Environment)}
     */
    public PaymentManager setUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * Set PayPoint authentication credentials - retrieve these from a call to YOUR server
     * @param credentials the credentials required to make a payment
     * @return
     */
    public PaymentManager setCredentials(PayPointCredentials credentials) {
        this.credentials = credentials;
        return this;
    }

    /**
     * Register the payment callback. Call this prior to {@link #makePayment(PaymentRequest)}
     * @param callback
     */
    public void registerPaymentCallback(PaymentManager.MakePaymentCallback callback) {
        this.callback = new WeakReference<MakePaymentCallback>(callback);
    }

    /**
     * Unregister the payment callback. Call this prior to exiting your payment activity/fragment
     */
    public void unregisterPaymentCallback() {
        this.callback = null;
    }

    /**
     * Locks the callback mechanism during screen orientation change. Call this BEFORE {@link #unregisterPaymentCallback()}
     * in onPause() activity/fragment lifecycle e.g.
     *
     * <p>
     * <pre>
     *protected void onPause() {
     *  super.onPause();
     *
     *  paymentManager.lockCallback();
     *  paymentManager.unregisterPaymentCallback();
     *}
     * </pre>
     *
     */
    public void lockCallback() {
        this.callbackLocked = true;
    }

    /**
     * Unkocks the callback mechanism during screen orientation change. Call this AFTER {@link #registerPaymentCallback(com.paypoint.sdk.library.payment.PaymentManager.MakePaymentCallback)}
     * in onResume() activity/fragment lifecycle e.g.
     *
     * <p>
     * <pre>
     *protected void onResume() {
     *  super.onResume();
     *
     *  paymentManager.registerPaymentCallback(this);
     *  paymentManager.unlockCallback();
     *}
     * </pre>
     */
    public void unlockCallback() {
        this.callbackLocked = false;

        // send back pending response
        if (callback != null &&
            callback.get() != null) {
            if (callbackPending != null) {
                if (callbackPending.isError) {
                    callback.get().paymentFailed(callbackPending.paymentError);
                } else {
                    callback.get().paymentSucceeded(callbackPending.paymentSuccess);
                }
                callbackPending = null;
            }
        }
    }

    /**
     * Asynchronously make the payment using the values specified in the request
     *
     * <p>You must call these functions first:
     * <p>{@link #registerPaymentCallback(com.paypoint.sdk.library.payment.PaymentManager.MakePaymentCallback)}
     * <p>{@link #setUrl(String)}
     * <p>{@link #setCredentials(com.paypoint.sdk.library.security.PayPointCredentials)}
     *
     * @param request payment details
     * @throws PaymentValidationException incorrect payment details in the request.
     * Use {@link com.paypoint.sdk.library.exception.PaymentValidationException#getErrorCode()} to determine error
     * @throws com.paypoint.sdk.library.exception.PaymentInProgressException payment is still in progress
     * @return unique identifier
     */
    public String makePayment(final PaymentRequest request)
            throws PaymentValidationException, PaymentInProgressException {

        // fail fast if session still in progress
        if (state != State.STATE_IDLE) {
            throw new PaymentInProgressException();
        }

        // ensure last payment is forgotten
        callbackPending = null;

        // validate request data
        validatePaymentDetails(request);

        // check null transaction
        if (TextUtils.isEmpty(url)) {
            throw new PaymentValidationException(PaymentValidationException.ErrorCode.INVALID_URL);
        }

        // check null transaction
        if (credentials == null) {
            throw new PaymentValidationException(PaymentValidationException.ErrorCode.INVALID_CREDENTIALS);
        }

        // validate credentials
        try {
            credentials.validateData();
        } catch (InvalidCredentialsException e) {
            throw new PaymentValidationException(PaymentValidationException.ErrorCode.INVALID_CREDENTIALS);
        }

        try {
            service = createService(url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up payment service");
        }

        // call REST endpoint
        makePaymentRequest = new MakePaymentRequest()
                .setSdkVersion(deviceManager.getSdkVersion())
                .setMerchantAppName(deviceManager.getMerchantAppName())
                .setMerchantAppVersion(deviceManager.getMerchantAppVersion())
                .setDeviceInfo(deviceInfo)
                .setTransaction(request.getTransaction())
                .setPaymentMethod(new PaymentMethod().setCard(request.getCard())
                .setBillingAddress(request.getAddress()))
                .setFinancialServices(request.getFinancialServices())
                .setCustomer(request.getCustomer())
                .setCustomFields(request.getCustomFields());

        // create a unique identifier for the payment operation which can be used by the app to query
        // the status
        operationId = UUID.randomUUID().toString();

        // retry on failure
        reliableDeliveryEnabled = true;

        onSessionStart();

        setState(State.STATE_PAYMENT_WAITING_NETWORK);

        // wait for network connection - this returns straightaway
        waitForNetworkConnection();

        return operationId;
    }

    public void getPaymentStatus(String operationId) {

        // TODO check credentials

        // clear existing payment operation - this will use the same callback
        onSessionEnd();

        // start timer
        onSessionStart();

        this.operationId = operationId;

        reliableDeliveryEnabled = false;

        setState(State.STATE_STATUS_WAITING_NETWORK);
    }

    private void setState(State state) {
        this.state = state;
    }

    /*
    * State machine - no need to any synchronisation as all callbacks on the UI thread
    */
    private void onEvent(Event event) {

        if (state != null &&
                event != null) {

            switch (event) {
                case EVENT_NETWORK_CONNECTED:
                    onEventNetworkConnected(state);
                    break;

                case EVENT_RESPONSE_NOT_RECEIVED:
                    // TODO wait for 1s to avoid overloading server?
                    onEventResponseNotReceived(state);
                    break;

                case EVENT_SESSION_TIMEOUT:
                    onEventSessionTimeout(state);
                    break;
            }
        }
    }

    private void onEventResponseNotReceived(State state) {
        // only request status if waiting payment or resume response
        if (state == State.STATE_PAYMENT_WAITING_RESPONSE ||
                state == State.STATE_RESUME_WAITING_RESPONSE ||
                state == State.STATE_STATUS_WAITING_RESPONSE) {

            subscriptions.add(service.paymentStatus("Bearer " + credentials.getToken(),
                    credentials.getInstallationId(), operationId)
                    .timeout(TIMEOUT_RESPONSE_STATUS, TimeUnit.SECONDS)
                    .subscribe(new ResponseObserver()));

            setState(State.STATE_STATUS_WAITING_RESPONSE);
        }
    }

    private void onEventNetworkConnected(State state) {

        // network connected - send request
        if (state == State.STATE_PAYMENT_WAITING_NETWORK) {
            subscriptions.add(service.makePayment(makePaymentRequest, "Bearer " + credentials.getToken(),
                    operationId,
                    credentials.getInstallationId())
                    .timeout(TIMEOUT_RESPONSE_PAYMENT, TimeUnit.SECONDS)
                    .subscribe(new ResponseObserver()));

            setState(State.STATE_PAYMENT_WAITING_RESPONSE);
        } else if (state == State.STATE_RESUME_WAITING_NETWORK) {
            subscriptions.add(service.resume3DS(threeDSResumeRequest, "Bearer " + credentials.getToken(),
                    operationId, credentials.getInstallationId(), transactionId)
                    .timeout(TIMEOUT_RESPONSE_RESUME, TimeUnit.SECONDS)
                    .subscribe(new ResponseObserver()));

            setState(State.STATE_RESUME_WAITING_RESPONSE);
        } else if (state == State.STATE_STATUS_WAITING_NETWORK) {
            subscriptions.add(service.paymentStatus("Bearer " + credentials.getToken(),
                    credentials.getInstallationId(), operationId)
                    .timeout(TIMEOUT_RESPONSE_STATUS, TimeUnit.SECONDS)
                    .subscribe(new ResponseObserver()));
        }
    }

    private void onEventSessionTimeout(State state) {

        if (state != State.STATE_IDLE) {
            PaymentError error = new PaymentError();
            error.setKind(PaymentError.Kind.NETWORK);

            executeCallback(error);
        }
    }

    private void onSessionStart() {
        subscriptions = new CompositeSubscription();

        // reset state
        setState(State.STATE_IDLE);

        // cancel any existing timers
        if (sessionTimer != null) {
            sessionTimer.cancel();
        }

        // start a new timer
        sessionTimer = new Timer(new SessionTimeoutHandler(), sessionTimeoutSeconds * 1000, false);
        sessionTimer.start();
    }

    private void onSessionEnd() {
        // unsubscribe so that any pending REST callbacks are ignored - once unsubscribed the composite
        // subscription is unusable so need to recreate it
        if (subscriptions != null) {
            subscriptions.unsubscribe();
        }

        // reset state
        setState(State.STATE_IDLE);

        if (sessionTimer != null) {
            sessionTimer.cancel();
        }
    }


    /**
     * Validates the payment request.
     * Call this prior to going online to get PayPoint credentials for the call to {@link #makePayment(PaymentRequest)}
     * to detect any errors in the payment form
     * @param request
     * @throws PaymentValidationException
     */
    public void validatePaymentDetails(com.paypoint.sdk.library.payment.PaymentRequest request)
            throws PaymentValidationException {

        if (request == null) {
            throw new PaymentValidationException(PaymentValidationException.ErrorCode.INVALID_REQUEST);
        }

        // check null transaction
        if (request.getTransaction() == null) {
            throw new PaymentValidationException(PaymentValidationException.ErrorCode.INVALID_TRANSACTION);
        }

        // check null card
        if (request.getCard() == null) {
            throw new PaymentValidationException(PaymentValidationException.ErrorCode.INVALID_CARD);
        }

        // validate transaction data
        request.getTransaction().validateData();

        // validate card data
        request.getCard().validateData();

        // validate custom fields if set
        if (request.getCustomFields() != null) {
            for (CustomField customField : request.getCustomFields()) {
                customField.validateData();
            }
        }
    }

    /**
     * Validates PAN/card number. Useful for inline form validation
     * @param pan
     * @throws PaymentValidationException
     */
    public void validateCardPan(String pan) throws PaymentValidationException {
        PaymentCard.validatePan(pan);
    }

    /**
     * Validates card expiry. Useful for inline form validation
     * @param expiry
     * @throws PaymentValidationException
     */
    public void validateCardExpiry(String expiry) throws PaymentValidationException {
        PaymentCard.validateExpiry(expiry);
    }

    /**
     * Validates cv2. Useful for inline form validation
     * @param cv2
     * @throws PaymentValidationException
     */
    public void validateCardCv2(String cv2) throws PaymentValidationException {
        PaymentCard.validateCv2(cv2);
    }

    private class ResponseObserver implements Observer<MakePaymentResponse> {

        /**
         * Callback when REST call succeeds i.e. HTTP 200
         * @param paymentResponse
         */
        @Override
        public void onNext(MakePaymentResponse paymentResponse) {
            if (paymentResponse != null &&
               (paymentResponse.isSuccessful() ||
                paymentResponse.isPending() ||
                paymentResponse.isProcessing())) {

                // check if 3D secure redirect
                if (paymentResponse.getReasonCode() == REASON_SUSPENDED_FOR_3D_SECURE) {

                    // ensure response contains valid 3DS credentials
                    MakePaymentResponse.threeDSecure threeDSecure = paymentResponse.getThreeDSecure();

                    if (threeDSecure == null ||
                       !threeDSecure.validateData()) {
                        PaymentError error = new PaymentError();
                        error.setKind(PaymentError.Kind.PAYPOINT);
                        error.getPayPointError().setReasonCode(PaymentError.ReasonCode.SERVER_ERROR);
                        error.getPayPointError().setReasonMessage("Missing 3D Secure credentials");

                        executeCallback(error);
                    } else {
                        // show 3D secure in separate activity
                        Intent intent = new Intent(context, ThreeDSActivity.class);
                        intent.putExtra(ThreeDSActivity.EXTRA_ACS_URL, threeDSecure.getAcsUrl());
                        intent.putExtra(ThreeDSActivity.EXTRA_TERM_URL, threeDSecure.getTermUrl());
                        intent.putExtra(ThreeDSActivity.EXTRA_PAREQ, threeDSecure.getPareq());
                        intent.putExtra(ThreeDSActivity.EXTRA_MD, threeDSecure.getMd());
                        intent.putExtra(ThreeDSActivity.EXTRA_TRANSACTION_ID, paymentResponse.getTransactionId());
                        intent.putExtra(ThreeDSActivity.EXTRA_SESSION_TIMEOUT, threeDSecure.getSessionTimeout());
                        intent.putExtra(ThreeDSActivity.EXTRA_ALLOW_SELF_SIGNED_CERTS, isCustomUrl);
                        intent.putExtra(ThreeDSActivity.EXTRA_REDIRECT_TIMEOUT, threeDSecure.getRedirectTimeout());

                        // required as starting the activity from an application context
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        // cancel the session timer
                        // TODO is it possible that could get stuck in this state causing makePayment to fails as transaction not in idle state?
                        setState(state.STATE_SUSPENDED_FOR_3DS);

                        sessionTimer.cancel();

                        context.startActivity(intent);
                    }
                } else {

                    if (paymentResponse.isProcessing()) {
                        // payment is in flight

                        // TODO do we do this for GET status calls or just reliableDeliveryEnabled = true
                        onEvent(Event.EVENT_RESPONSE_NOT_RECEIVED);
                    } else {
                        // payment successful - build success object
                        PaymentSuccess success = new PaymentSuccess();

                        success.setAmount(paymentResponse.getAmount());
                        success.setCurrency(paymentResponse.getCurrency());
                        success.setTransactionId(paymentResponse.getTransactionId());
                        success.setMerchantReference(paymentResponse.getMerchantRef());
                        success.setLastFour(paymentResponse.getLastFourDigits());
                        success.setCustomFields(paymentResponse.getCustomFields());

                        executeCallback(success);
                    }
                }
            } else {
                // payment failed
                PaymentError error = new PaymentError();
                error.setKind(PaymentError.Kind.PAYPOINT);
                error.getPayPointError().setReasonCode(paymentResponse.getReasonCode());
                error.getPayPointError().setReasonMessage(paymentResponse.getReasonMessage());
                error.setCustomFields(paymentResponse.getCustomFields());

                executeCallback(error);
            }
        }

        /**
         * Callback when REST call fails i.e. no connection or HTTP != 200
         * @param e
         */
        @Override
        public void onError(Throwable e) {

            PaymentError error = new PaymentError();
            error.setKind(PaymentError.Kind.NETWORK);

            if (e instanceof RetrofitError) {

                RetrofitError retrofitError = (RetrofitError)e;

                // Kind.NETWORK - An IOException occurred while communicating to the server.
                if (retrofitError.getKind() == RetrofitError.Kind.NETWORK) {

                    // attempt to get status of transaction
                    if (reliableDeliveryEnabled) {
                        onEvent(Event.EVENT_RESPONSE_NOT_RECEIVED);
                    } else {
                        // if not enabled callback to app
                        executeCallback(error);
                    }
                } else {

                    error.setKind(PaymentError.Kind.PAYPOINT);


                    if (retrofitError.getResponse() != null) {
                        int httpStatus = retrofitError.getResponse().getStatus();

                        // TODO do we need to check if 404 from getStatus command - if so then payment not received - does
                        if (state == State.STATE_STATUS_WAITING_RESPONSE &&
                            httpStatus == 404) {
                            // TODO what to do here??
                        } else {

                            error.getNetworkError().setHttpStatusCode(httpStatus);

                            // attempt to parse JSON in the response
                            MakePaymentResponse paymentResponse = parseErrorResponse(retrofitError);

                            if (paymentResponse != null) {

                                error.getPayPointError().setReasonCode(paymentResponse.getReasonCode());
                                error.getPayPointError().setReasonMessage(paymentResponse.getReasonMessage());
                                error.setCustomFields(paymentResponse.getCustomFields());
                            }
                        }
                    }
                    executeCallback(error);
                }

            } else {
                // something went wrong but not a Retrofit error
                if (reliableDeliveryEnabled) {
                    onEvent(Event.EVENT_RESPONSE_NOT_RECEIVED);
                } else {
                    // if not enabled callback to app
                    executeCallback(error);
                }
            }
        }

        @Override
        public void onCompleted() {

        }
    }

    private void executeCallback(PaymentError error) {

        onSessionEnd();

        // TODO stop session timer
        if (callbackLocked) {
            // store callback for when the callee re-registers the callback
            callbackPending = new CallbackPending();
            callbackPending.isError = true;
            callbackPending.paymentError = error;
        } else {
            if (callback != null &&
                callback.get() != null) {
                callbackPending = null;
                callback.get().paymentFailed(error);
            }
        }
    }

    private void executeCallback(PaymentSuccess success) {

        onSessionEnd();

        // TODO stop session timer
        if (callbackLocked) {
            // store callback for when the callee re-registers the callback
            callbackPending = new CallbackPending();
            callbackPending.isError = false;
            callbackPending.paymentSuccess = success;
        } else {
            if (callback != null &&
                callback.get() != null) {
                callbackPending = null;
                callback.get().paymentSucceeded(success);
            }
        }
    }

    /**
     * Parse JSON from error response
     * @param retrofitError
     * @return
     */
    private MakePaymentResponse parseErrorResponse(RetrofitError retrofitError) {

        MakePaymentResponse response = null;

        try {

            if (retrofitError != null &&
                retrofitError.getResponse() != null &&
                retrofitError.getResponse().getBody() != null) {
                try {
                    String json = new String(((TypedByteArray) retrofitError.getResponse().getBody()).getBytes());
                    response = new Gson().fromJson(json, MakePaymentResponse.class);
                } catch (Exception e) {
                    // if JSON is invalid swallow exception - SDK will return
                }
            }
        } catch (Exception e) {
            // if JSON is invalid swallow exception - SDK will return
        }

        return response;
    }

    /**
     * Receiver for broadcast events from ThreeDSActivity
     */
    private class ThreeDSecureReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getBooleanExtra(ThreeDSActivity.EXTRA_SUCCESS, false)) {
                // 3DS successful - post to resume endpoint
                transactionId = intent.getStringExtra(ThreeDSActivity.EXTRA_TRANSACTION_ID);
                String pares = intent.getStringExtra(ThreeDSActivity.EXTRA_PARES);

                threeDSResumeRequest = new ThreeDSResumeRequest(pares);

                try {
                    setState(state.STATE_RESUME_WAITING_NETWORK);

                    // wait for network connection - this returns straightaway
                    waitForNetworkConnection();

                } catch (Exception e) {
                    PaymentError error = new PaymentError();
                    error.setKind(PaymentError.Kind.PAYPOINT);
                    error.getPayPointError().setReasonCode(PaymentError.ReasonCode.UNKNOWN);

                    executeCallback(error);
                }
            } else {
                // 3DS failure
                PaymentError error = new PaymentError();
                error.setKind(PaymentError.Kind.PAYPOINT);

                boolean cancelled = intent.getBooleanExtra(ThreeDSActivity.EXTRA_CANCELLED, false);
                boolean timeout = intent.getBooleanExtra(ThreeDSActivity.EXTRA_HAS_TIMED_OUT, false);

                if (cancelled) {
                    error.getPayPointError().setReasonMessage("Transaction cancelled");
                    error.getPayPointError().setReasonCode(PaymentError.ReasonCode.TRANSACTION_CANCELLED);
                } else if (timeout) {
                    error.getPayPointError().setReasonMessage("3D Secure timed out");
                    error.getPayPointError().setReasonCode(PaymentError.ReasonCode.THREE_D_SECURE_TIMEOUT);
                } else {
                    error.getPayPointError().setReasonMessage("3D Secure failed");
                    error.getPayPointError().setReasonCode(PaymentError.ReasonCode.THREE_D_SECURE_ERROR);
                }

                executeCallback(error);
            }
        }
    }

    private void waitForNetworkConnection() {
        // return straightaway if network already connected
        if (NetworkManager.hasConnection(context)) {
            onEvent(Event.EVENT_NETWORK_CONNECTED);
        }

        // otherwise wait for the NetworkConnectivityReceiver to be fired
    }

    /**
     * Callback for session timer expiration
     */
    private class SessionTimeoutHandler implements Runnable {
        @Override
        public void run() {
            onEvent(Event.EVENT_SESSION_TIMEOUT);
        }
    }

    /**
     * Receiver for network connectivity events
     */
    private class NetworkConnectivityReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (NetworkManager.hasConnection(context)) {
                onEvent(Event.EVENT_NETWORK_CONNECTED);
            }
        }
    }
}
