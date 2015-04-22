/*
 * Copyright (c) 2015. PayPoint
 */

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
 * Who:  Pete
 * When: 20/04/2015
 * What: PayPoint Retrofit REST service
 */
public interface PayPointService {

    @Headers("Content-Type: application/json; charset=utf-8")
    @POST("/transactions/{installationId}/payment")
    void makePayment(@Body Request request,
                    @Header("Authorization") String token,
                    @Path("installationId") String installationId,
                    Callback<Response> callback);
}
