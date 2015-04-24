/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypoint.sdk.library.exception.InvalidCredentialsException;
import com.paypoint.sdk.library.exception.PaymentValidationException;
import com.paypoint.sdk.library.log.Logger;
import com.paypoint.sdk.library.network.NetworkManager;
import com.paypoint.sdk.library.network.PayPointService;
import com.paypoint.sdk.library.payment.request.PaymentCard;
import com.paypoint.sdk.library.payment.request.PaymentMethod;
import com.paypoint.sdk.library.payment.request.Request;
import com.paypoint.sdk.library.payment.response.Response;
import com.paypoint.sdk.library.security.PayPointCredentials;
import com.squareup.okhttp.OkHttpClient;

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

    private static final int HTTP_TIMEOUT_CONNECTION    = 20; // 20s
    private static final int HTTP_TIMEOUT_RESPONSE      = 60; // 60s

    public interface MakePaymentCallback {

        public void paymentSucceeded(PaymentSuccess success);

        public void paymentFailed(PaymentError error);
    }

    private Context context;
    private int responseTimeoutSeconds = HTTP_TIMEOUT_RESPONSE;
    private String url;
    private PayPointCredentials credentials;

    public PaymentManager(Context context) {
        this.context = context;
    }

    private PayPointService getService(String serverUrl, int responseTimeoutSeconds) {

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS")
//                .serializeNulls()
                .create();

        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setConnectTimeout(HTTP_TIMEOUT_CONNECTION, TimeUnit.SECONDS);
        okHttpClient.setReadTimeout(responseTimeoutSeconds, TimeUnit.SECONDS);

        // setting the executor is required for the Robolectric tests to run
        Executor executor = Executors.newSingleThreadExecutor();

        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(serverUrl)
                .setExecutors(executor, executor)
                .setConverter(new GsonConverter(gson))
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

    public void makePayment(final PaymentRequest request)
            throws PaymentValidationException {

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
        Request jsonRequest = new Request().setTransaction(request.getTransaction())
                .setPaymentMethod(new PaymentMethod().setCard(request.getCard())
                .setBillingAddress(request.getAddress()));

        PayPointService service = getService(url,
                responseTimeoutSeconds);

        service.makePayment(jsonRequest, "Bearer " +
                credentials.getToken(), credentials.getInstallationId(),
                new Callback<Response>() {
                    @Override
                    public void success(Response paymentResponse, retrofit.client.Response response) {
                        onPaymentSucceeded(paymentResponse, response, request.getCallback());
                    }

                    @Override
                    public void failure(RetrofitError error) {
                       onPaymentFailed(error, request.getCallback());
                    }
                });
    }

    public void validatePaymentDetails(PaymentRequest request)
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
     * @param callback
     */
    private void onPaymentSucceeded(Response paymentResponse, retrofit.client.Response response,
                                  MakePaymentCallback callback) {
        if (callback != null) {

            if (paymentResponse != null &&
                paymentResponse.isSuccessful()) {
                // payment successful - build success object
                PaymentSuccess success = new PaymentSuccess();

                success.setAmount(paymentResponse.getAmount());
                success.setCurrency(paymentResponse.getCurrency());
                success.setTransactionId(paymentResponse.getTransactionId());
                success.setMerchantReference(paymentResponse.getMerchantRef());
                success.setLastFour(paymentResponse.getLastFourDigits());

                callback.paymentSucceeded(success);
            } else {
                // payment failed
                PaymentError error = new PaymentError();
                error.setKind(PaymentError.Kind.PAYPOINT);
                error.getPayPointError().setReasonCode(paymentResponse.getReasonCode());
                error.getPayPointError().setReasonMessage(paymentResponse.getReasonMessage());

                callback.paymentFailed(error);
            }
        }
    }

    /**
     * Callback when payment fails
     * @param retrofitError
     * @param callback
     */
    private void onPaymentFailed(RetrofitError retrofitError,
                                 MakePaymentCallback callback) {
        if (callback != null) {

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
            callback.paymentFailed(error);
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
}
