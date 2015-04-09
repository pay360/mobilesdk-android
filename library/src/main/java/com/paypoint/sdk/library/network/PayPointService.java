package com.paypoint.sdk.library.network;

import android.support.v7.internal.view.menu.MenuPresenter;

import com.paypoint.sdk.library.payment.request.PaymentRequest;
import com.paypoint.sdk.library.payment.response.PaymentResponse;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by HendryP on 09/04/2015.
 */
public interface PayPointService {

    @Headers("Content-Type: application/json; charset=utf-8")
    @POST("/{installationId}/payment")
    void makePayment(@Body PaymentRequest request,
                    @Header("Authorization") String token,
                    @Path("installationId") String installationId,
                    Callback<PaymentResponse> callback);
}
