/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypoint.sdk.library.ThreeDSActivity;
import com.paypoint.sdk.library.device.DeviceManager;
import com.paypoint.sdk.library.exception.InvalidCredentialsException;
import com.paypoint.sdk.library.exception.PaymentValidationException;
import com.paypoint.sdk.library.exception.TransactionInProgressException;
import com.paypoint.sdk.library.exception.TransactionSuspendedFor3DSException;
import com.paypoint.sdk.library.network.EndpointManager;
import com.paypoint.sdk.library.network.NetworkManager;
import com.paypoint.sdk.library.network.PayPointService;
import com.paypoint.sdk.library.network.SelfSignedSocketFactory;
import com.paypoint.sdk.library.payment.MakePaymentResponse;
import com.paypoint.sdk.library.security.PayPointCredentials;
import com.paypoint.sdk.library.utils.Timer;
import com.squareup.okhttp.OkHttpClient;

import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedByteArray;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Handles payments
 *
 * <p>Call {@link #makePayment(PaymentRequest)} to initiate payment
 */
public class PaymentManager {

    private static final int HTTP_TIMEOUT_CONNECTION                = 10; // 10s

    private static final int TIMEOUT_RESPONSE_PAYMENT               = 30; // 30s
    private static final int TIMEOUT_RESPONSE_RESUME                = 30; // 30s
    private static final int TIMEOUT_RESPONSE_STATUS                = 5; // 5s

    private static final int DEFAULT_SESSION_TIMEOUT                = 60; // 60s

    private static final int REASON_SUSPENDED_FOR_3D_SECURE         = 7;
    private static final int REASON_SUSPENDED_FOR_CLIENT_REDIRECT   = 8;

    private static final long DEFAULT_STATUS_BACKOFF                = 5000L;

    // backoff in ms for retrying status command e.g. first retry = 1000ms, 2nd = 2000ms, 3rd = 2000ms
    // 4th = 5000ms, thereafter 5000ms
    private static final Long[] STATUS_RETRY_BACKOFF = new Long[] {1000L, 2000L, 2000L, 5000L};

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
    private String serviceServerUrl;
    private MakePaymentRequest makePaymentRequest;
    private ThreeDSResumeRequest threeDSResumeRequest;
    private Timer sessionTimer;
    private Timer retryDelayTimer;
    private State state = State.STATE_IDLE;
    private CompositeSubscription subscriptions = new CompositeSubscription();
    private int retryCount;

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
        EVENT_GET_STATUS,
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

    private static void TEST_resetInstance() {
        instance = null;
    }

    private PaymentManager(Context context) {
        // use the application context - DO NOT HOLD ONTO AN ACTIVITY CONTEXT as PaymentManager
        // is a singleton an may\will outlast the activity leading to memory leaks
        this.context = context.getApplicationContext();

        // no need to unregister receivers as PaymentManager is a singleton as thus has the
        // same lifetime as the app hence won't not unregistering won't cause any leaks

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this.context);

        // register to receive events from 3DS activity - use LocalBroadcastManager here as
        // ThreeDSActivity.ACTION_COMPLETED is internal to the SDK
        lbm.registerReceiver(new ThreeDSecureReceiver(),
                new IntentFilter(ThreeDSActivity.ACTION_COMPLETED));

        // register to receive network connectivity events - use this.context here as
        // ConnectivityManager.CONNECTIVITY_ACTION is global
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

        retryDelayTimer = new Timer(new RetryDelayTimeoutHandler(), DEFAULT_STATUS_BACKOFF, false);
    }

    private PayPointService createService(String serverUrl)
        throws NoSuchAlgorithmException, KeyManagementException {

        // only create service once or if serverUrl changes
        if (service == null ||
            !serviceServerUrl.equals(serverUrl)) {

            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS")
                    .create();

            OkHttpClient httpClient = new OkHttpClient();
            httpClient.setConnectTimeout(HTTP_TIMEOUT_CONNECTION, TimeUnit.SECONDS);
            httpClient.setReadTimeout(DEFAULT_SESSION_TIMEOUT, TimeUnit.SECONDS);

            // this is required to disable OkHttp silently retrying requests following connection
            // issues - we want to have complete control over this so disable this functionality
            httpClient.setRetryOnConnectionFailure(false);

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

            service = adapter.create(PayPointService.class);

            serviceServerUrl = serverUrl;
        }

        return service;
    }

    /**
     * Timeout waiting for a callback when making a payment. SDK guarantees to make callback within
     * this period. Defaults to 60s.
     * DO NOT ALTER WITHOUT GOOD REASON
     * @param sessionTimeoutSeconds timeout for making a payment in second
     * @return PaymentManager for chaining
     */
    public PaymentManager setSessionTimeout(int sessionTimeoutSeconds) {
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
        return this;
    }

    /**
     * URL of the PayPoint server
     * @param url the base url of the PayPoint server. The URL can be obtained from
     * {@link com.paypoint.sdk.library.network.EndpointManager#getEndpointUrl(com.paypoint.sdk.library.network.EndpointManager.Environment)}
     * @return PaymentManager for chaining
     */
    public PaymentManager setUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * Set PayPoint authentication credentials - retrieve these from a call to YOUR server
     * @param credentials the credentials required to make a payment
     * @return PaymentManager for chaining
     */
    public PaymentManager setCredentials(PayPointCredentials credentials) {
        this.credentials = credentials;
        return this;
    }

    /**
     * Register the payment callback. Call this prior to {@link #makePayment(PaymentRequest)}
     * @param callback callback when payment completed (success or fail)
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
     * Unlocks the callback mechanism during screen orientation change. Call this AFTER {@link #registerPaymentCallback(com.paypoint.sdk.library.payment.PaymentManager.MakePaymentCallback)}
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
     * @throws InvalidCredentialsException missing token, installation id or server url
     * @throws TransactionInProgressException a transaction is in flight, please wait for the callback
     * @return unique identifier - use this to query transaction status in {@link #getTransactionStatus(String)}
     */
    public String makePayment(final PaymentRequest request)
            throws PaymentValidationException, InvalidCredentialsException, TransactionInProgressException {

        // fail fast if session still in progress
        if (state != State.STATE_IDLE) {
           throw new TransactionInProgressException();
        }

        // ensure last payment is forgotten
        callbackPending = null;

        // validate request data
        validatePaymentDetails(request);

        createService();

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

        onSessionStart();

        setState(State.STATE_PAYMENT_WAITING_NETWORK);

        // wait for network connection - this returns straightaway
        waitForNetworkConnection();

        return operationId;
    }

    /**
     * Get the status of a transaction
     * @param operationId - identifier returned from {@link #makePayment(PaymentRequest)}
     * @throws InvalidCredentialsException missing token, installation id or server url
     * @throws TransactionInProgressException if a transaction is currently in progress - try again
     * when the SDK has executed the callback
     * @throws TransactionSuspendedFor3DSException the SDK is waiting for the user to complete 3D
     * Secure authentication - tey again when the SDK has executed the callback
     */
    public void getTransactionStatus(String operationId) throws InvalidCredentialsException,
            TransactionInProgressException, TransactionSuspendedFor3DSException {

        switch (state) {
            case STATE_IDLE:

                this.operationId = operationId;

                // call server endpoint to get transaction status
                createService();

                onSessionStart();

                setState(State.STATE_STATUS_WAITING_NETWORK);

                // wait for network connection - this returns straightaway
                waitForNetworkConnection();
                break;
            case STATE_SUSPENDED_FOR_3DS:
               throw new TransactionSuspendedFor3DSException();
            default:
               throw new TransactionInProgressException();
        }
    }

    private void createService() throws InvalidCredentialsException {
        // check null url
        if (TextUtils.isEmpty(url)) {
            throw new InvalidCredentialsException();
        }

        // check null credentials
        if (credentials == null) {
            throw new InvalidCredentialsException();
        }

        // validate credentials
        credentials.validateData();

        try {
            createService(url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up payment service");
        }
    }

    private void setState(State state) {
        this.state = state;
    }

    /*
    * State machine - no need to for synchronisation as all callbacks on the UI thread
    */
    private void onEvent(Event event) {

        if (state != null &&
                event != null) {

            switch (event) {
                case EVENT_NETWORK_CONNECTED:
                    onEventNetworkConnected(state);
                    break;

                case EVENT_RESPONSE_NOT_RECEIVED:
                    onEventResponseNotReceived(state);
                    break;

                case EVENT_GET_STATUS:
                    onEventRetryStatus(state);
                    break;

                case EVENT_SESSION_TIMEOUT:
                    onEventSessionTimeout(state);
                    break;
            }
        }
    }

    /**
     * Timeout or socket error
     * @param state
     */
    private void onEventResponseNotReceived(State state) {
        // only request status if waiting payment or resume response
        if (state == State.STATE_PAYMENT_WAITING_RESPONSE ||
            state == State.STATE_RESUME_WAITING_RESPONSE ||
            state == State.STATE_STATUS_WAITING_RESPONSE) {

            // back off for period dependant on retry attempts
            retryDelayTimer.start(getStatusBackoff(retryCount++));
        }
    }

    /**
     * Callback for retry timer expiration
     */
    private class RetryDelayTimeoutHandler implements Runnable {
        @Override
        public void run() {
            onEvent(Event.EVENT_GET_STATUS);
        }
    }

    /**
     * Returns backoff period to wait before retrying get status
     * @param retry
     * @return
     */
    private long getStatusBackoff(int retry) {

        // get backoff period before sending to status endpoint, period depends on retry attempts
        long backoff = DEFAULT_STATUS_BACKOFF;

        if (retry < STATUS_RETRY_BACKOFF.length) {
            backoff = STATUS_RETRY_BACKOFF[retry];
        }

        return backoff;
    }

    private void onEventRetryStatus(State state) {
        // ensure still in correct state + session hasn't timed out
        if (state == State.STATE_PAYMENT_WAITING_RESPONSE ||
            state == State.STATE_RESUME_WAITING_RESPONSE ||
            state == State.STATE_STATUS_WAITING_RESPONSE) {

            subscriptions.add(service.paymentStatus("Bearer " + credentials.getToken(),
                    credentials.getInstallationId(), operationId)
                    .timeout(TIMEOUT_RESPONSE_STATUS, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new ResponseObserver()));

            setState(State.STATE_STATUS_WAITING_RESPONSE);
        }
    }

    /**
     * Network connection made
     * @param state
     */
    private void onEventNetworkConnected(State state) {

        // network connected - send request
        if (state == State.STATE_PAYMENT_WAITING_NETWORK) {
            subscriptions.add(service.makePayment(makePaymentRequest, "Bearer " + credentials.getToken(),
                    operationId, credentials.getInstallationId())
                    .timeout(TIMEOUT_RESPONSE_PAYMENT, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new ResponseObserver()));

            setState(State.STATE_PAYMENT_WAITING_RESPONSE);
        } else if (state == State.STATE_RESUME_WAITING_NETWORK) {
            subscriptions.add(service.resume3DS(threeDSResumeRequest, "Bearer " + credentials.getToken(),
                    operationId, credentials.getInstallationId(), transactionId)
                    .timeout(TIMEOUT_RESPONSE_RESUME, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new ResponseObserver()));

            setState(State.STATE_RESUME_WAITING_RESPONSE);
        } else if (state == State.STATE_STATUS_WAITING_NETWORK) {
            subscriptions.add(service.paymentStatus("Bearer " + credentials.getToken(),
                    credentials.getInstallationId(), operationId)
                    .timeout(TIMEOUT_RESPONSE_STATUS, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new ResponseObserver()));
        }
    }

    /**
     * Session timeout
     * @param state
     */
    private void onEventSessionTimeout(State state) {

        if (state != State.STATE_IDLE) {
            PaymentError error = new PaymentError();
            error.setReasonCode(PaymentError.ReasonCode.TRANSACTION_TIMED_OUT);
            executeCallback(error);
        }
    }

    private void onSessionStart() {
        subscriptions = new CompositeSubscription();

        // reset state
        setState(State.STATE_IDLE);

        // reset retry count
        retryCount = 0;

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

        // cancel timers
        if (sessionTimer != null) {
            sessionTimer.cancel();
        }

        if (retryDelayTimer != null) {
            retryDelayTimer.cancel();
        }
    }

    /**
     * Validates the payment request.
     * Call this prior to going online to get PayPoint credentials for the call to {@link #makePayment(PaymentRequest)}
     * to detect any errors in the payment form
     * @param request payment request
     * @throws PaymentValidationException error validating payment request
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
     * @param pan card PAN
     * @throws PaymentValidationException error validating card PAN
     */
    public void validateCardPan(String pan) throws PaymentValidationException {
        PaymentCard.validatePan(pan);
    }

    /**
     * Validates card expiry. Useful for inline form validation
     * @param expiry card expiry date
     * @throws PaymentValidationException error validating card expiry date
     */
    public void validateCardExpiry(String expiry) throws PaymentValidationException {
        PaymentCard.validateExpiry(expiry);
    }

    /**
     * Validates cv2. Useful for inline form validation
     * @param cv2 card CV2
     * @throws PaymentValidationException error validating card CV2
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
               !paymentResponse.isFailed()) {

                // check if 3D secure redirect
                if (paymentResponse.getReasonCode() == REASON_SUSPENDED_FOR_3D_SECURE) {
                    on3DSecure(paymentResponse);
                } else {
                    if (paymentResponse.isProcessing()) {
                        onPaymentProcessing(paymentResponse);
                    } else {
                        onPaymentSuccess(paymentResponse);
                    }
                }
            } else {
                onPaymentFailed(paymentResponse);
            }
        }

        /**
         * Callback when REST call fails i.e. no connection or HTTP != 200
         * @param e
         */
        @Override
        public void onError(Throwable e) {

            PaymentError error = new PaymentError();

            if (e instanceof RetrofitError) {

                RetrofitError retrofitError = (RetrofitError)e;

                switch (retrofitError.getKind()) {
                    // An IOException occurred while communicating to the server.
                    case NETWORK:
                        // if failed to connect when making payment or resume then return straight away,
                        // no point in attempting to read state of payment as we know the request
                        // never made it to the server
                        if ((state == State.STATE_PAYMENT_WAITING_RESPONSE ||
                             state == State.STATE_RESUME_WAITING_RESPONSE) &&
                            (retrofitError.getCause() instanceof ConnectException ||
                             retrofitError.getCause() instanceof UnknownHostException)) {

                            error.setReasonCode(PaymentError.ReasonCode.NETWORK_NO_CONNECTION);
                            executeCallback(error);
                        } else {
                            // attempt to get status of transaction
                            onEvent(Event.EVENT_RESPONSE_NOT_RECEIVED);
                        }
                        break;

                    // A non-200 HTTP status code was received from the server
                    case HTTP:

                        if (retrofitError.getResponse() != null) {

                            // attempt to parse JSON in the response
                            MakePaymentResponse paymentResponse = parseErrorResponse(retrofitError);

                            if (paymentResponse != null) {

                                error.setReasonCode(paymentResponse.getReasonCode());
                                error.setReasonMessage(paymentResponse.getReasonMessage());
                                error.setCustomFields(paymentResponse.getCustomFields());
                            }
                        }
                        executeCallback(error);

                        break;

                    // An exception was thrown while (de)serializing a body
                    case CONVERSION:
                        // An internal error occurred while attempting to execute a request
                    case UNEXPECTED:
                    default:
                        executeCallback(error);
                        break;
                }
            } else {
                // something other than a Retrofit exception - not expecting this but need to handle
                onEvent(Event.EVENT_RESPONSE_NOT_RECEIVED);
            }
        }

        @Override
        public void onCompleted() {
            // nothing to do
        }

        private void onPaymentProcessing(MakePaymentResponse paymentResponse) {
            // payment is in flight, retry status to wait until success\failure
            onEvent(Event.EVENT_RESPONSE_NOT_RECEIVED);
        }

        private void onPaymentSuccess(MakePaymentResponse paymentResponse) {
            // payment successful - build success object
            PaymentSuccess success = new PaymentSuccess();

            success.setAmount(paymentResponse.getAmount());
            success.setCurrency(paymentResponse.getCurrency());
            success.setTransactionId(paymentResponse.getTransactionId());
            success.setMerchantReference(paymentResponse.getMerchantRef());
            success.setLastFourPan(paymentResponse.getLastFourDigits());
            success.setMaskedPan(paymentResponse.getMaskedPan());
            success.setCustomFields(paymentResponse.getCustomFields());

            executeCallback(success);
        }

        private void onPaymentFailed(MakePaymentResponse paymentResponse) {
            // payment failed
            PaymentError error = new PaymentError();

            if (paymentResponse != null) {
                error.setReasonCode(paymentResponse.getReasonCode());
                error.setReasonMessage(paymentResponse.getReasonMessage());
                error.setCustomFields(paymentResponse.getCustomFields());
            }

            executeCallback(error);
        }

        private void on3DSecure(MakePaymentResponse paymentResponse) {
            // ensure response contains valid 3DS credentials
            MakePaymentResponse.ThreeDSecure threeDSecure = paymentResponse.getThreeDSecure();

            if (threeDSecure == null ||
                !threeDSecure.validateData()) {
                PaymentError error = new PaymentError();
                error.setReasonCode(PaymentError.ReasonCode.SERVER_ERROR);
                error.setReasonMessage("Missing 3D Secure credentials");

                executeCallback(error);
            } else {

                // if this is in response to a get status then pass back error
                if (state == State.STATE_STATUS_WAITING_RESPONSE) {
                    PaymentError error = new PaymentError();
                    error.setReasonCode(PaymentError.ReasonCode.NETWORK_ERROR_DURING_PROCESSING);

                    executeCallback(error);
                } else {
                    // otherwise continue with 3DS
                    transactionId = paymentResponse.getTransactionId();

                    // show 3D secure in separate activity
                    Intent intent = new Intent(context, ThreeDSActivity.class);
                    intent.putExtra(ThreeDSActivity.EXTRA_ACS_URL, threeDSecure.getAcsUrl());
                    intent.putExtra(ThreeDSActivity.EXTRA_TERM_URL, threeDSecure.getTermUrl());
                    intent.putExtra(ThreeDSActivity.EXTRA_PAREQ, threeDSecure.getPareq());
                    intent.putExtra(ThreeDSActivity.EXTRA_MD, threeDSecure.getMd());
                    intent.putExtra(ThreeDSActivity.EXTRA_TRANSACTION_ID, paymentResponse.getTransactionId());
                    intent.putExtra(ThreeDSActivity.EXTRA_SESSION_TIMEOUT, threeDSecure.getSessionTimeout());
                    intent.putExtra(ThreeDSActivity.EXTRA_ALLOW_SELF_SIGNED_CERTS, isCustomUrl);
                    intent.putExtra(ThreeDSActivity.EXTRA_REDIRECT_TIMEOUT, threeDSecure.getPassiveTimeout());

                    // required as starting the activity from an application context
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    // cancel the session timer
                    setState(state.STATE_SUSPENDED_FOR_3DS);

                    sessionTimer.cancel();

                    context.startActivity(intent);
                }
            }
        }
    }

    private void executeCallback(PaymentError error) {
        executeCallback(error, true);
    }

    private void executeCallback(PaymentSuccess success) {
        executeCallback(success, true);
    }

    private void executeCallback(PaymentError error,  boolean endSession) {

        if (endSession) {
            onSessionEnd();
        }

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

    private void executeCallback(PaymentSuccess success, boolean endSession) {

        if (endSession) {
            onSessionEnd();
        }

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
                String transactionId = intent.getStringExtra(ThreeDSActivity.EXTRA_TRANSACTION_ID);
                String pares = intent.getStringExtra(ThreeDSActivity.EXTRA_PARES);

                threeDSResumeRequest = new ThreeDSResumeRequest(pares);

                // restart the session timer
                sessionTimer.reset();

                setState(state.STATE_RESUME_WAITING_NETWORK);

                // wait for network connection - this returns straightaway
                waitForNetworkConnection();
            } else {
                // 3DS failure
                PaymentError error = new PaymentError();

                boolean cancelled = intent.getBooleanExtra(ThreeDSActivity.EXTRA_CANCELLED, false);
                boolean timeout = intent.getBooleanExtra(ThreeDSActivity.EXTRA_HAS_TIMED_OUT, false);

                if (cancelled) {
                    error.setReasonCode(PaymentError.ReasonCode.TRANSACTION_CANCELLED_BY_USER);
                } else if (timeout) {
                    error.setReasonCode(PaymentError.ReasonCode.TRANSACTION_TIMED_OUT);
                } else {
                    error.setReasonCode(PaymentError.ReasonCode.NETWORK_NO_CONNECTION);
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
