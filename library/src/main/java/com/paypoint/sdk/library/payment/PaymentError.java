/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment;

import com.paypoint.sdk.library.payment.request.CustomField;

import java.util.List;

/**
 * Error from payment request
 */
public class PaymentError {

    private List<CustomField> customFields;

    public enum Kind {
        PAYPOINT,
        NETWORK;
    }

    public enum ReasonCode {

        /**
         * Transaction cancelled by user
         */
        TRANSACTION_CANCELLED(-4),

        /**
         * Timeout waiting for 3D Secure
         */
        THREE_D_SECURE_TIMEOUT(-3),

        /**
         * Error processing 3D Secure
         */
        THREE_D_SECURE_ERROR(-2),

        /**
         * Unknown error
         */
        UNKNOWN(-1),

        /**
         * Operation successful as described
         */
        SUCCESS(0),

        /**
         * Request was not correctly formed
         */
        INVALID(1),

        /**
         * The presented API token was not valid, or the wrong type of authentication was used
         */
        AUTHENTICATION_FAILED(2),

        /**
         * Get a new token
         */
        CLIENT_TOKEN_EXPIRED(3),

        /**
         * The token was valid, but does not grant you access to use the specified feature
         */
        UNAUTHORISED_REQUEST(4),

        /**
         * The transaction was successfully submitted but failed to be processed correctly
         */
        TRANSACTION_FAILED_TO_PROCESS(5),

        /**
         * An internal server error occurred at PayPoint
         */
        SERVER_ERROR(6);

        int code;

        ReasonCode(int code) {
            this.code = code;
        }

        public static ReasonCode getReasonCode(int code) {

            for (ReasonCode reasonCode : ReasonCode.values()) {
                if (reasonCode.code == code) {
                    return reasonCode;
                }
            }

            return UNKNOWN;
        }
    }

    private Kind kind;

    private PayPointError payPointError = new PayPointError();

    private NetworkError networkError = new NetworkError();

    public Kind getKind() {
        return kind;
    }

    protected void setKind(Kind kind) {
        this.kind = kind;
    }

    public PayPointError getPayPointError() {
        return payPointError;
    }

    public NetworkError getNetworkError() {
        return networkError;
    }

    public List<CustomField> getCustomFields() {
        return customFields;
    }

    protected void setCustomFields(List<CustomField> customFields) {
        this.customFields = customFields;
    }

    public class PayPointError {

        private ReasonCode reasonCode;

        private String reasonMessage;

        public ReasonCode getReasonCode() {
            return reasonCode;
        }

        public void setReasonCode(int reasonCode) {
            this.reasonCode = ReasonCode.getReasonCode(reasonCode);
        }

        public void setReasonCode(ReasonCode reasonCode) {
            this.reasonCode = reasonCode;
        }

        public String getReasonMessage() {
            return reasonMessage;
        }

        public void setReasonMessage(String reasonMessage) {
            this.reasonMessage = reasonMessage;
        }
    }

    public class NetworkError {

        private int httpStatusCode;

        public int getHttpStatusCode() {
            return httpStatusCode;
        }

        public void setHttpStatusCode(int httpStatusCode) {
            this.httpStatusCode = httpStatusCode;
        }
    }
}
