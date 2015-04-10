package com.paypoint.sdk.library.payment.response;

import com.google.gson.annotations.SerializedName;

/**
 * Who:  Pete
 * When: 09/04/2015
 * What:
 */
public class PaymentResponse {

    private static final int REASON_CODE_UNKNOWN = -1;

    @SerializedName("paymentMethod")
    private PaymentMethod paymentMethod;

    @SerializedName("transaction")
    private Transaction transaction;

    @SerializedName("outcome")
    private Outcome outcome;

    public boolean isSuccessful() {
        return outcome != null && outcome.isSuccessful();
    }

    public float getAmount() {
        float amount = 0;

        if (transaction != null) {
            amount = transaction.getAmount();
        }

        return amount;
    }

    public String getCurrency() {
        String currency = null;

        if (transaction != null) {
            currency = transaction.getCurrency();
        }

        return currency;
    }

    public String getTransactionId() {
        String transactionId = null;

        if (transaction != null) {
            transactionId = transaction.getTransactionId();
        }

        return transactionId;
    }

    public String getMerchantRef() {
        String merchanttRef = null;

        if (transaction != null) {
            merchanttRef = transaction.getMerchantReference();
        }

        return merchanttRef;
    }

    public String getLastFourDigits() {
        String lastFour = null;

        if (paymentMethod != null &&
            paymentMethod.getCard() != null) {
            lastFour = paymentMethod.getCard().getLastFour();
        }

        return lastFour;
    }

    public int getReasonCode() {
        int reasonCode = REASON_CODE_UNKNOWN;

        if (outcome != null) {
            reasonCode = outcome.getReasonCode();
        }

        return reasonCode;
    }

    public String getReasonMessage() {
        String reasonMessage = null;

        if (outcome != null) {
            reasonMessage = outcome.getReasonMessage();
        }

        return reasonMessage;
    }

    public String getStatus() {
        String status = null;

        // TODO check status or reasonCode?
        if (outcome != null) {
            status = outcome.getStatus();
        }

        return status;
    }
}
