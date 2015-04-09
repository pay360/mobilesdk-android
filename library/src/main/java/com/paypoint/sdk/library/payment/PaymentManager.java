package com.paypoint.sdk.library.payment;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypoint.sdk.library.R;
import com.paypoint.sdk.library.log.Logger;
import com.paypoint.sdk.library.network.NetworkManager;
import com.paypoint.sdk.library.network.PayPointService;
import com.paypoint.sdk.library.payment.request.BillingAddress;
import com.paypoint.sdk.library.payment.request.PaymentCard;
import com.paypoint.sdk.library.payment.request.PaymentMethod;
import com.paypoint.sdk.library.payment.request.PaymentRequest;
import com.paypoint.sdk.library.payment.request.Transaction;
import com.paypoint.sdk.library.payment.response.PaymentResponse;
import com.paypoint.sdk.library.security.PayPointCredentials;
import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.android.AndroidLog;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

/**
 * Created by HendryP on 08/04/2015.
 */
public class PaymentManager {

    private static final int HTTP_TIMEOUT_CONNECTION    = 20; // 20s
    private static final int HTTP_TIMEOUT_RESPONSE      = 60; // 20s

    public interface MakePaymentCallback {

        public void paymentSucceeded();

        public void paymentFailed();
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

    public void makePayment(Transaction transaction, PaymentCard card, BillingAddress address,
                            PayPointCredentials credentials, final MakePaymentCallback callback) {

        // check network
        if (!NetworkManager.hasConnection(context)) {
            // TODO throw exception
        }

        // validate request data
        validateData(transaction, card, address);

        // call REST endpoint
        PaymentRequest request = new PaymentRequest().setTransaction(transaction)
                .setPaymentMethod(new PaymentMethod().setCard(card).setBillingAddress(address));

        PayPointService service = getService(context.getString(R.string.paypoint_server_url),
                responseTimeoutSeconds);

        service.makePayment(request, context.getString(R.string.header_authorization_token,
                credentials.getToken()), credentials.getInstallationId(),
                new Callback<PaymentResponse>() {
                    @Override
                    public void success(PaymentResponse paymentResponse, Response response) {
                        onPaymentSucceeded(paymentResponse, response, callback);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                       onPaymentFailed(error, callback);
                    }
                });
    }

    private void validateData(Transaction transaction, PaymentCard card, BillingAddress address) {
        // check null transaction

        // check null card

        // check null address

        // check pan

        // check expiry

        // check luhn

        // check ccv

        // check amount present

        // check currency present
    }

    private void onPaymentSucceeded(PaymentResponse paymentResponse, Response response,
                                  MakePaymentCallback callback) {
        if (callback != null) {

            if (paymentResponse != null &&
                paymentResponse.isSuccessful()) {
                // payment successful
                callback.paymentSucceeded();
            } else {
                // payment failed
                callback.paymentFailed();
            }
        }
    }

    private void onPaymentFailed(RetrofitError error,
                                  MakePaymentCallback callback) {
        if (callback != null) {
            // TODO in error response return HTTP response code, if timeout, reason code, reason string etc
            callback.paymentFailed();
        }
    }
}
