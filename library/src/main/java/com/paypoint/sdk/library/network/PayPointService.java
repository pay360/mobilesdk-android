package com.paypoint.sdk.library.network;

import com.paypoint.sdk.library.payment.request.Request;
import com.paypoint.sdk.library.payment.response.Response;

import retrofit.Callback;
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
    @POST("/transactions/{installationId}/payment")
    void makePayment(@Body Request request,
                    @Header("Authorization") String token,
                    @Path("installationId") String installationId,
                    Callback<Response> callback);
}
