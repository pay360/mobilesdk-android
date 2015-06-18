/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment;

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

    @SerializedName("recurring")
    private boolean recurring;

    /**
     * Transaction currency
     * @param currency e.g. "GDP"
     * @return Transaction for chaining
     */
    public Transaction setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    /**
     * Transaction amount
     * @param amount transaction amount
     * @return Transaction for chaining
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
     * @return Transaction for chaining
     */
    public Transaction setMerchantReference(String merchantReference) {
        this.merchantReference = merchantReference;
        return this;
    }

    /**
     * Submit an Authorisation instead of a Payment
     * @return Transaction for chaining
     */
    public Transaction setAuthorisation() {
        this.deferred = true;
        return this;
    }

    /**
     * Call if the first payment or authorisation of a continuous authority sequence.
     * Subsequent repeats can be initiated using the "Repeat a Payment" call.
     * Details can be found here https://developer.paypoint.com/payments/docs/#payments/repeat_a_payment
     */
    public void setRecurring() {
        this.recurring = true;
    }

    /**
     * Transaction amount
     * @return amount
     */
    protected float getAmount() {
        return amount;
    }

    /**
     * Transaction currency
     * @return currency
     */
    protected String getCurrency() {
        return currency;
    }

    protected void validateData() throws PaymentValidationException {
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
