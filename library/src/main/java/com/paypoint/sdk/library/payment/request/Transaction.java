/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment.request;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.paypoint.sdk.library.exception.PaymentValidationException;

/**
 * Mandatory payment transaction details
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

    /**
     * Transaction currency
     * @param currency e.g. "GDP"
     * @return
     */
    public Transaction setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    /**
     * Transaction amount
     * @param amount
     * @return
     */
    public Transaction setAmount(float amount) {
        this.amount = amount;
        return this;
    }

    public Transaction setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Merchant reference
     * @param merchantReference generate your own unique identifier for tracking
     * @return
     */
    public Transaction setMerchantReference(String merchantReference) {
        this.merchantReference = merchantReference;
        return this;
    }

    /**
     * Deferred transaction
     * @param deferred to to true for authorization
     * @return
     */
    public Transaction setDeferred(boolean deferred) {
        this.deferred = deferred;
        return this;
    }

    public float getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void validateData() throws PaymentValidationException {
        // check amount present
        if (getAmount() <= 0) {
            throw new PaymentValidationException(PaymentValidationException.ErrorCode.TRANSACTION_INVALID_AMOUNT);
        }

        // check currency present
        // TODO need to do any sanity check on the value?
        if (TextUtils.isEmpty(getCurrency())) {
            throw new PaymentValidationException(PaymentValidationException.ErrorCode.TRANSACTION_INVALID_CURRENCY);
        }
    }
}
