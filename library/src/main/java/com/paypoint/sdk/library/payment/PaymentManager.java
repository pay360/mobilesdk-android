package com.paypoint.sdk.library.payment;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypoint.sdk.library.exception.CardExpiredException;
import com.paypoint.sdk.library.exception.CardInvalidCv2Exception;
import com.paypoint.sdk.library.exception.CardInvalidExpiryException;
import com.paypoint.sdk.library.exception.CardInvalidLuhnException;
import com.paypoint.sdk.library.exception.CardInvalidPanException;
import com.paypoint.sdk.library.exception.NoNetworkException;
import com.paypoint.sdk.library.exception.TransactionInvalidAmountException;
import com.paypoint.sdk.library.exception.TransactionInvalidCurrencyException;
import com.paypoint.sdk.library.log.Logger;
import com.paypoint.sdk.library.network.NetworkManager;
import com.paypoint.sdk.library.network.PayPointService;
import com.paypoint.sdk.library.payment.request.BillingAddress;
import com.paypoint.sdk.library.payment.request.PaymentCard;
import com.paypoint.sdk.library.payment.request.PaymentMethod;
import com.paypoint.sdk.library.payment.request.Request;
import com.paypoint.sdk.library.payment.request.Transaction;
import com.paypoint.sdk.library.payment.response.Response;
import com.paypoint.sdk.library.security.PayPointCredentials;
import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.android.AndroidLog;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedByteArray;


/**
 * Created by HendryP on 08/04/2015.
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

    public PaymentManager(Context context) {
        this.context = context;
    }

    private PayPointService getService(String serverUrl, int responseTimeoutSeconds) {

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS")
                .serializeNulls()
                .create();

        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setConnectTimeout(HTTP_TIMEOUT_CONNECTION, TimeUnit.SECONDS);
        okHttpClient.setReadTimeout(responseTimeoutSeconds, TimeUnit.SECONDS);

        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(serverUrl)
                .setConverter(new GsonConverter(gson))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setLog(new AndroidLog(Logger.TAG))
                .setClient(new OkClient(okHttpClient))
                .build();

        return adapter.create(PayPointService.class);
    }

    public void setResponseTimeout(int responseTimeoutSeconds) {
        this.responseTimeoutSeconds = responseTimeoutSeconds;
    }

    public void makePayment(final PaymentRequest request)
            throws NoNetworkException, CardExpiredException, CardInvalidExpiryException,
            CardInvalidPanException, CardInvalidLuhnException, CardInvalidCv2Exception,
            TransactionInvalidAmountException, TransactionInvalidCurrencyException {

        // check network
        if (!NetworkManager.hasConnection(context)) {
            throw new NoNetworkException();
        }

        // validate request data
        validateData(request);

        // call REST endpoint
        Request jsonRequest = new Request().setTransaction(request.getTransaction())
                .setPaymentMethod(new PaymentMethod().setCard(request.getCard())
                .setBillingAddress(request.getAddress()));

        PayPointService service = getService(request.getUrl(),
                responseTimeoutSeconds);

        service.makePayment(jsonRequest, "Bearer " +
                request.getCredentials().getToken(), request.getCredentials().getInstallationId(),
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

    private void validateData(PaymentRequest request)
            throws CardExpiredException, CardInvalidExpiryException, CardInvalidPanException, CardInvalidLuhnException,
            CardInvalidCv2Exception, TransactionInvalidAmountException,
            TransactionInvalidCurrencyException {

        if (request == null) {
            throw new IllegalArgumentException("Request is a required field");
        }

        // check null transaction
        if (TextUtils.isEmpty(request.getUrl())) {
            throw new IllegalArgumentException("URL is a required field");
        }

        // check null transaction
        if (request.getCredentials() == null) {
            throw new IllegalArgumentException("Credentials is a required field");
        }

        // check null transaction
        if (request.getTransaction() == null) {
            throw new IllegalArgumentException("Transaction is a required field");
        }

        // check null card
        if (request.getCard() == null) {
            throw new IllegalArgumentException("Card is a required field");
        }

        // validate transaction data
        request.getTransaction().validateData();

        // validate card data
        request.getCard().validateData();
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
