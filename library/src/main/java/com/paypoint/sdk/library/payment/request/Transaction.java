package com.paypoint.sdk.library.payment.request;

import com.google.gson.annotations.SerializedName;

/**
 * Created by HendryP on 08/04/2015.
 */
public class Transaction {

    @SerializedName("currency")
    private String currency;

    @SerializedName("amount")
    private float amount;

    @SerializedName("description")
    private String description;

    @SerializedName("merchantRef")
    private String merchantReference;

    @SerializedName("deferred")
    private boolean deferred;

    public Transaction setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public Transaction setAmount(float amount) {
        this.amount = amount;
        return this;
    }

    public Transaction setDescription(String description) {
        this.description = description;
        return this;
    }

    public Transaction setMerchantReference(String merchantReference) {
        this.merchantReference = merchantReference;
        return this;
    }

    public Transaction setDeferred(boolean deferred) {
        this.deferred = deferred;
        return this;
    }
}
