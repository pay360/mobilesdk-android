/*
 * Copyright (c) 2016 Capita plc
 */

package com.pay360.sdk.library.payment;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MakePaymentResponse {

    private static final int REASON_CODE_UNKNOWN = -1;

    @SerializedName("paymentMethod")
    private PaymentMethod paymentMethod;

    @SerializedName("transaction")
    private Transaction transaction;

    @SerializedName("outcome")
    private Outcome outcome;

    @SerializedName("threeDSRedirect")
    private ThreeDSecure threeDSecure;

    @SerializedName("customFields")
    private CustomFieldsContainer customFieldsContainer;

    public boolean isSuccessful() {
        return outcome != null && outcome.isSuccessful();
    }

    public boolean isFailed() {
        return outcome != null && outcome.isFailed();
    }

    public boolean isPending() {
        return outcome != null && outcome.isPending();
    }

    public boolean isProcessing() {
        return outcome != null && outcome.isProcessing();
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
        String merchantRef = null;

        if (transaction != null) {
            merchantRef = transaction.getMerchantReference();
        }

        return merchantRef;
    }

    public String getLastFourDigits() {
        String lastFour = null;

        if (paymentMethod != null &&
            paymentMethod.getCard() != null) {
            lastFour = paymentMethod.getCard().getLastFour();
        }

        return lastFour;
    }

    public String getMaskedPan() {
        String maskedPan = null;

        if (paymentMethod != null &&
            paymentMethod.getCard() != null) {
            maskedPan = paymentMethod.getCard().getMaskedPan();
        }

        return maskedPan;
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

        if (outcome != null) {
            status = outcome.getStatus();
        }

        return status;
    }

    public List<CustomField> getCustomFields() {

        List<CustomField> customFields = null;

        if (customFieldsContainer != null) {
            customFields = customFieldsContainer.customFields;
        }

        return customFields;
    }

    public MakePaymentResponse.ThreeDSecure getThreeDSecure() {
        return threeDSecure;
    }

    private static class Transaction {

        @SerializedName("transactionId")
        private String transactionId;

        @SerializedName("merchantRef")
        private String merchantReference;

        @SerializedName("type")
        private String type;

        @SerializedName("amount")
        private float amount;

        @SerializedName("currency")
        private String currency;

        @SerializedName("transactionTime")
        private String transactionTime;

        public String getTransactionId() {
            return transactionId;
        }

        public String getMerchantReference() {
            return merchantReference;
        }

        public String getType() {
            return type;
        }

        public float getAmount() {
            return amount;
        }

        public String getCurrency() {
            return currency;
        }

        public String getTransactionTime() {
            return transactionTime;
        }
    }

    private class PaymentMethod {

        @SerializedName("card")
        private PaymentCard card;

        public PaymentCard getCard() {
            return card;
        }
    }

    private class PaymentCard {

        @SerializedName("cardUsageType")
        private String cardUsageType;

        @SerializedName("cardScheme")
        private String cardScheme;

        @SerializedName("lastFour")
        private String lastFour;

        @SerializedName("maskedPan")
        private String maskedPan;

        public String getCardUsageType() {
            return cardUsageType;
        }

        public String getCardScheme() {
            return cardScheme;
        }

        public String getLastFour() {
            return lastFour;
        }

        public String getMaskedPan() {
            return maskedPan;
        }
    }

    private class Outcome {

        private static final String RESPONSE_SUCCESS    = "SUCCESS";
        private static final String RESPONSE_PENDING    = "PENDING";
        private static final String RESPONSE_PROCESSING = "PROCESSING";
        private static final String RESPONSE_FAILED     = "FAILED";

        @SerializedName("status")
        private String status;

        @SerializedName("reasonCode")
        private int reasonCode;

        @SerializedName("reasonMessage")
        private String reasonMessage;

        public String getStatus() {
            return status;
        }

        public int getReasonCode() {
            return reasonCode;
        }

        public String getReasonMessage() {
            return reasonMessage;
        }

        public boolean isSuccessful() {
            return RESPONSE_SUCCESS.equalsIgnoreCase(status);
        }

        public boolean isPending() {
            return RESPONSE_PENDING.equalsIgnoreCase(status);
        }

        public boolean isProcessing() {
            return RESPONSE_PROCESSING.equalsIgnoreCase(status);
        }

        public boolean isFailed() {
            return RESPONSE_FAILED.equalsIgnoreCase(status);
        }
    }

    public class ThreeDSecure {

        @SerializedName("acsUrl")
        private String acsUrl;

        @SerializedName("pareq")
        private String pareq;

        @SerializedName("termUrl")
        private String termUrl;

        @SerializedName("md")
        private String md;

        @SerializedName("sessionTimeout")
        private long sessionTimeout;

        @SerializedName("passiveTimeout")
        private long passiveTimeout;

        public String getAcsUrl() {
            return acsUrl;
        }

        public String getPareq() {
            return pareq;
        }

        public String getTermUrl() {
            return termUrl;
        }

        public String getMd() {
            return md;
        }

        public long getSessionTimeout() {
            return sessionTimeout;
        }

        public long getPassiveTimeout() {
            return passiveTimeout;
        }

        public boolean validateData() {
            if (TextUtils.isEmpty(acsUrl)) {
                return false;
            }
            if (TextUtils.isEmpty(pareq)) {
                return false;
            }
            if (TextUtils.isEmpty(termUrl)) {
                return false;
            }
            if (TextUtils.isEmpty(md)) {
                return false;
            }

            return true;
        }
    }

    private static class CustomFieldsContainer {
        @SerializedName("fieldState")
        private List<CustomField> customFields;
    }
}
