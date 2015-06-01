/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.network;

import com.paypoint.sdk.library.payment.request.MakePaymentRequest;
import com.paypoint.sdk.library.payment.request.ThreeDSResumeRequest;
import com.paypoint.sdk.library.payment.response.MakePaymentResponse;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Path;
import rx.Observable;

/**
 * PayPoint Retrofit REST service
 */
public interface PayPointService {

    @Headers("Content-Type: application/json; charset=utf-8")
    @POST("/acceptor/rest/mobile/transactions/{installationId}/payment")
    Observable<MakePaymentResponse> makePayment(@Body MakePaymentRequest request,
                                                @Header("Authorization") String token,
                                                @Header("AP-Operation-ID") String operationId,
                                                @Path("installationId") String installationId);

    @GET("/acceptor/rest/mobile/transactions/{installationId}/opref/{operationId}")
    Observable<MakePaymentResponse> paymentStatus(@Header("Authorization") String token,
                                                     @Path("installationId") String installationId,
                                                     @Path("operationId") String operationId);

    @Headers("Content-Type: application/json; charset=utf-8")
    @POST("/acceptor/rest/mobile/transactions/{installationId}/{transactionId}/resume")
    Observable<MakePaymentResponse> resume3DS(@Body ThreeDSResumeRequest request,
                                              @Header("Authorization") String token,
                                              @Header("AP-Operation-ID") String operationId,
                                              @Path("installationId") String installationId,
                                              @Path("transactionId") String transactionId);

}
