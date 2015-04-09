package com.paypoint.sdk.library.payment.response;

import com.google.gson.annotations.SerializedName;

/**
 * Who:  Pete
 * When: 09/04/2015
 * What:
 */
public class PaymentResponse {

    @SerializedName("paymentMethod")
    private PaymentMethod paymentMethod;

    @SerializedName("transaction")
    private Transaction transaction;

    @SerializedName("outcome")
    private Outcome outcome;

    public boolean isSuccessful() {
        return outcome != null && outcome.isSuccessful();
    }
}
