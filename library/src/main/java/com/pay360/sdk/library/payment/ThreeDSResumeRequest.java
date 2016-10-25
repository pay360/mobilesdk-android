/*
 * Copyright (c) 2016 Capita plc
 */

package com.pay360.sdk.library.payment;

import com.google.gson.annotations.SerializedName;

public class ThreeDSResumeRequest {

    public ThreeDSResumeRequest(String pares) {
        response = new threeDSResponse(pares);
    }

    @SerializedName("threeDSecureResponse")
    private threeDSResponse response;

    private static class threeDSResponse {
        @SerializedName("pares")
        private String pares;

        public threeDSResponse(String pares) {
            this.pares = pares;
        }
    }
}
