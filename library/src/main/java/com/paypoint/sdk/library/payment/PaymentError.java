/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment;

/**
 * Who:  Pete
 * When: 09/04/2015
 * What:
 */
public class PaymentError {

    public enum Kind {
        PAYPOINT,
        NETWORK;
    }

    public enum ReasonCode {
        TRANSACTION_CANCELLED(-4),          // Transaction cancelled by user
        THREE_D_SECURE_TIMEOUT(-3),         // Timeout waiting for 3D Secure
        THREE_D_SECURE_ERROR(-2),           // Error occurred processing 3D Secure
        UNKNOWN(-1),
        SUCCESS(0),                         // Operation successful as described
        INVALID(1),                         // Request was not correctly formed
        AUTHENTICATION_FAILED(2),           // The presented API token was not valid, or the wrong type of authentication was used
        CLIENT_TOKEN_EXPIRED(3),            // Get a new token
        UNAUTHORISED_REQUEST(4),            // The token was valid, but does not grant you access to use the specified feature
        TRANSACTION_FAILED_TO_PROCESS(5),   // The transaction was successfully submitted but failed to be processed correctly.
        SERVER_ERROR(6);                    // An internal server error occurred at paypoint

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

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public PayPointError getPayPointError() {
        return payPointError;
    }

    public NetworkError getNetworkError() {
        return networkError;
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
