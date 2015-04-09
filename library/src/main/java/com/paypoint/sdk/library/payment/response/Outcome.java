package com.paypoint.sdk.library.payment.response;

import com.google.gson.annotations.SerializedName;

/**
 * Who:  Pete
 * When: 09/04/2015
 * What:
 */
public class Outcome {

    private static final String RESPONSE_SUCCESS = "SUCCESS";

    @SerializedName("status")
    private String status;

    @SerializedName("reasonCode")
    private int reasonCode;

    @SerializedName("reasonMessage")
    private String reasonMessage;

    public boolean isSuccessful() {
        return RESPONSE_SUCCESS.equalsIgnoreCase(status);
    }
}
