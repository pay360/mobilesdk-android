/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment;

import com.google.gson.annotations.SerializedName;

public class PaymentMethod {

    @SerializedName("card")
    private PaymentCard card;

    @SerializedName("billingAddress")
    private BillingAddress billingAddress;

    public PaymentMethod setCard(PaymentCard card) {
        this.card = card;
        return this;
    }

    public PaymentMethod setBillingAddress(BillingAddress billingAddress) {
        this.billingAddress = billingAddress;
        return this;
    }
}
