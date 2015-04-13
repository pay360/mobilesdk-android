package com.paypoint.sdk.library.payment.request;

import com.google.gson.annotations.SerializedName;

/**
 * Created by HendryP on 08/04/2015.
 */
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
