/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment;

import com.paypoint.sdk.library.payment.request.CustomField;

import java.io.Serializable;
import java.util.List;

/**
 * Payment success data
 */
public class PaymentSuccess implements Serializable {

    private String transactionId;

    private String merchantReference;

    private String lastFour;

    private float amount;

    private String currency;

    private List<CustomField> customFields;

    protected void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    protected void setMerchantReference(String merchantReference) {
        this.merchantReference = merchantReference;
    }

    protected void setLastFour(String lastFour) {
        this.lastFour = lastFour;
    }

    protected void setAmount(float amount) {
        this.amount = amount;
    }

    protected void setCurrency(String currency) {
        this.currency = currency;
    }

    protected void setCustomFields(List<CustomField> customFields) {
        this.customFields = customFields;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getMerchantReference() {
        return merchantReference;
    }

    public String getLastFour() {
        return lastFour;
    }

    public float getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public List<CustomField> getCustomFields() {
        return customFields;
    }
}
