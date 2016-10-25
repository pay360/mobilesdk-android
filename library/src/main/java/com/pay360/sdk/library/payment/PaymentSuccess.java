/*
 * Copyright (c) 2016 Capita plc
 */

package com.pay360.sdk.library.payment;

import java.io.Serializable;
import java.util.List;

/**
 * Payment success data
 */
public class PaymentSuccess implements Serializable {

    private String transactionId;

    private String merchantReference;

    private String lastFourPan;

    private String maskedPan;

    private float amount;

    private String currency;

    private List<CustomField> customFields;

    protected void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    protected void setMerchantReference(String merchantReference) {
        this.merchantReference = merchantReference;
    }

    protected void setLastFourPan(String lastFourPan) {
        this.lastFourPan = lastFourPan;
    }

    protected void setMaskedPan(String maskedPan) {
        this.maskedPan = maskedPan;
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

    /**
     * Transaction Id
     * @return unique transaction identifier
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Merchant reference
     * @return sane unique reference specified in the request
     */
    public String getMerchantReference() {
        return merchantReference;
    }

    /**
     * Last four PAN
     * @return last four digits of the PAN
     */
    public String getLastFourPan() {
        return lastFourPan;
    }

    /**
     * Masked PAN
     * @return PAN with middle digits masked out for security
     */
    public String getMaskedPan() {
        return maskedPan;
    }

    /**
     * Transaction amount
     * @return the amount of the transaction
     */
    public float getAmount() {
        return amount;
    }

    /**
     * Transaction currency
     * @return the currency of the transaction
     */
    public String getCurrency() {
        return currency;
    }

    public List<CustomField> getCustomFields() {
        return customFields;
    }
}
