/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypoint.sdk.library.ThreeDSActivity;
import com.paypoint.sdk.library.exception.InvalidCredentialsException;
import com.paypoint.sdk.library.exception.PaymentValidationException;
import com.paypoint.sdk.library.log.Logger;
import com.paypoint.sdk.library.network.EndpointManager;
import com.paypoint.sdk.library.network.NetworkManager;
import com.paypoint.sdk.library.network.PayPointService;
import com.paypoint.sdk.library.network.SelfSignedSocketFactory;
import com.paypoint.sdk.library.payment.request.PaymentCard;
import com.paypoint.sdk.library.payment.request.PaymentMethod;
import com.paypoint.sdk.library.payment.request.MakePaymentRequest;
import com.paypoint.sdk.library.payment.request.ThreeDSResumeRequest;
import com.paypoint.sdk.library.payment.response.Response;
import com.paypoint.sdk.library.security.PayPointCredentials;
import com.squareup.okhttp.OkHttpClient;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.android.AndroidLog;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedByteArray;

/**
 * Who:  Pete
 * When: 20/04/2015
 * What: Manager for handling payment requests
 */
public class PaymentManager {

    private static final int HTTP_TIMEOUT_CONNECTION                = 20; // 20s
    private static final int HTTP_TIMEOUT_RESPONSE                  = 60; // 60s

    private static final int REASON_SUSPENDED_FOR_3D_SECURE         = 7;
    private static final int REASON_SUSPENDED_FOR_CLIENT_REDIRECT   = 8;

    public interface MakePaymentCallback {

        public void paymentSucceeded(PaymentSuccess success);

        public void paymentFailed(PaymentError error);
    }

    private Context context;
    private int responseTimeoutSeconds = HTTP_TIMEOUT_RESPONSE;
    private String url;
    private PayPointCredentials credentials;
    private PaymentManager.MakePaymentCallback callback;
    private boolean callbackLocked = false;
    private CallbackPending callbackPending;
    private boolean isCustomUrl;

    private static PaymentManager instance;

    private class CallbackPending {

        private boolean isError;
        private PaymentSuccess paymentSuccess;
        private PaymentError paymentError;
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
        this.context = context.getApplicationContext();

        // register to receive events from 3DS activity
        this.context.registerReceiver(new ThreeDSecureReceiver(),
                new IntentFilter(ThreeDSActivity.ACTION_COMPLETED));
    }

    private PayPointService getService(String serverUrl, int responseTimeoutSeconds)
        throws NoSuchAlgorithmException, KeyManagementException {

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS")
                .create();

        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setConnectTimeout(HTTP_TIMEOUT_CONNECTION, TimeUnit.SECONDS);
        okHttpClient.setReadTimeout(responseTimeoutSeconds, TimeUnit.SECONDS);

        // setting the executor is required for the Robolectric tests to run
        Executor executor = Executors.newSingleThreadExecutor();

        isCustomUrl = false;

        // by default Retrofit will throw an error if self signed certificate is used so allow
        // self signed certificate for custom URLs e.g. anything other than production
        if (EndpointManager.isCustomUrl(serverUrl)) {
            okHttpClient.setSslSocketFactory(new SelfSignedSocketFactory().build());
            isCustomUrl = true;
        }

        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(serverUrl)
                .setExecutors(executor, executor)
                .setConverter(new GsonConverter(gson))
//                .setLogLevel(RestAdapter.LogLevel.FULL)
//                .setLog(new AndroidLog(Logger.TAG))
                .setClient(new OkClient(okHttpClient))
                .build();

        return adapter.create(PayPointService.class);
    }

    public PaymentManager setResponseTimeout(int responseTimeoutSeconds) {
        this.responseTimeoutSeconds = responseTimeoutSeconds;
        return this;
    }

    public PaymentManager setUrl(String url) {
        this.url = url;
        return this;
    }

    public PaymentManager setCredentials(PayPointCredentials credentials) {
        this.credentials = credentials;
        return this;
    }

    public synchronized void registerPaymentCallback(PaymentManager.MakePaymentCallback callback) {
        this.callback = callback;
    }

    public synchronized void unregisterPaymentCallback() {
        this.callback = null;
    }

    public synchronized void lockCallback() {
        this.callbackLocked = true;
    }

    public synchronized void unlockCallback() {
        this.callbackLocked = false;

        // send back pending response
        if (callback != null) {
            if (callbackPending != null) {
                if (callbackPending.isError) {
                    callback.paymentFailed(callbackPending.paymentError);
                } else {
                    callback.paymentSucceeded(callbackPending.paymentSuccess);
                }
                callbackPending = null;
            }
        }
    }

    public void makePayment(final com.paypoint.sdk.library.payment.PaymentRequest request)
            throws PaymentValidationException {

        // ensure last payment is forgotten
        callbackPending = null;

        // check network
        if (!NetworkManager.hasConnection(context)) {
            throw new PaymentValidationException(PaymentValidationException.ErrorCode.NETWORK_NO_CONNECTION);
        }

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

        // call REST endpoint
        MakePaymentRequest jsonRequest = new MakePaymentRequest().setTransaction(request.getTransaction())
                .setPaymentMethod(new PaymentMethod().setCard(request.getCard())
                        .setBillingAddress(request.getAddress()));

        PayPointService service = null;

        try {
            service = getService(url,
                    responseTimeoutSeconds);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up payment service");
        }

        service.makePayment(jsonRequest, "Bearer " +
                    credentials.getToken(), credentials.getInstallationId(),
            new Callback<Response>() {
                @Override
                public void success(Response paymentResponse, retrofit.client.Response response) {
                    onPaymentSucceeded(paymentResponse, response);
                }

                @Override
                public void failure(RetrofitError error) {
                    onPaymentFailed(error);
                }
            });
    }

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
    }

    public void validateCardPan(String pan) throws PaymentValidationException {
        PaymentCard.validatePan(pan);
    }

    public void validateCardExpiry(String expiry) throws PaymentValidationException {
        PaymentCard.validateExpiry(expiry);
    }

    public void validateCardCv2(String cv2) throws PaymentValidationException {
        PaymentCard.validateCv2(cv2);
    }

    /**
     * Callback when payment succeeds
     * @param paymentResponse
     * @param response
     */
    private void onPaymentSucceeded(Response paymentResponse, retrofit.client.Response response) {

        if (paymentResponse != null &&
            (paymentResponse.isSuccessful() ||
            paymentResponse.isPending())) {

            // check if 3D secure redirect
            if (paymentResponse.getReasonCode() == REASON_SUSPENDED_FOR_3D_SECURE) {

                // ensure response contains valid 3DS credentials
                Response.threeDSecure threeDSecure = paymentResponse.getThreeDSecure();

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

                    context.startActivity(intent);
                }
            } else {
                // payment successful - build success object
                PaymentSuccess success = new PaymentSuccess();

                success.setAmount(paymentResponse.getAmount());
                success.setCurrency(paymentResponse.getCurrency());
                success.setTransactionId(paymentResponse.getTransactionId());
                success.setMerchantReference(paymentResponse.getMerchantRef());
                success.setLastFour(paymentResponse.getLastFourDigits());

                executeCallback(success);
            }
        } else {
            // payment failed
            PaymentError error = new PaymentError();
            error.setKind(PaymentError.Kind.PAYPOINT);
            error.getPayPointError().setReasonCode(paymentResponse.getReasonCode());
            error.getPayPointError().setReasonMessage(paymentResponse.getReasonMessage());

            executeCallback(error);
        }
    }

    /**
     * Callback when payment fails
     * @param retrofitError
     */
    private void onPaymentFailed(RetrofitError retrofitError) {

        PaymentError error = new PaymentError();

        error.setKind(PaymentError.Kind.NETWORK);

        if (retrofitError != null) {

            if (retrofitError.getResponse() != null) {
                error.getNetworkError().setHttpStatusCode(retrofitError.getResponse().getStatus());

                // attempt to parse JSON in the response
                Response paymentResponse = parseErrorResponse(retrofitError);

                if (paymentResponse != null) {
                    error.setKind(PaymentError.Kind.PAYPOINT);
                    error.getPayPointError().setReasonCode(paymentResponse.getReasonCode());
                    error.getPayPointError().setReasonMessage(paymentResponse.getReasonMessage());
                }
            }
        }
        executeCallback(error);
    }

    private synchronized void executeCallback(PaymentError error) {
        if (callbackLocked) {
            // store callback for when the callee re-registers the callback
            callbackPending = new CallbackPending();
            callbackPending.isError = true;
            callbackPending.paymentError = error;
        } else {
            if (callback != null) {
                callback.paymentFailed(error);
                callbackPending = null;
            }
        }
    }

    private synchronized void executeCallback(PaymentSuccess success) {
        if (callbackLocked) {
            // store callback for when the callee re-registers the callback
            callbackPending = new CallbackPending();
            callbackPending.isError = false;
            callbackPending.paymentSuccess = success;
        } else {
            if (callback != null) {
                callback.paymentSucceeded(success);
                callbackPending = null;
            }
        }
    }

    /**
     * Parse JSON from error response
     * @param retrofitError
     * @return
     */
    private Response parseErrorResponse(RetrofitError retrofitError) {

        Response response = null;

        try {

            if (retrofitError != null &&
                retrofitError.getResponse() != null &&
                retrofitError.getResponse().getBody() != null) {
                try {
                    String json = new String(((TypedByteArray) retrofitError.getResponse().getBody()).getBytes());
                    response = new Gson().fromJson(json, Response.class);
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
                String md = intent.getStringExtra(ThreeDSActivity.EXTRA_MD);

                // TODO need to check nd md same as md sent up - do this in ThreeDSActivity?

                ThreeDSResumeRequest jsonRequest = new ThreeDSResumeRequest(pares);

                PayPointService service = null;

                try {
                    service = getService(url,
                            responseTimeoutSeconds);
                } catch (Exception e) {
                    PaymentError error = new PaymentError();
                    error.setKind(PaymentError.Kind.PAYPOINT);
                    error.getPayPointError().setReasonCode(PaymentError.ReasonCode.UNKNOWN);

                    executeCallback(error);
                }

                service.resume3DS(jsonRequest, "Bearer " +
                                credentials.getToken(), credentials.getInstallationId(),
                                transactionId,
                        new Callback<Response>() {
                            @Override
                            public void success(Response paymentResponse, retrofit.client.Response response) {
                                onPaymentSucceeded(paymentResponse, response);
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                onPaymentFailed(error);
                            }
                        });
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
}
