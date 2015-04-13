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
        UNKNOWN(-1),
        SUCCESS(0),
        INVALID(1),
        AUTHENTICATION_FAILED(2),
        CLIENT_TOKEN_EXPIRED(3),
        UNAUTHORISED_REQUEST(4),
        TRANSACTION_FAILED_TO_PROCESS(5),
        SERVER_ERROR(6),
        SUSPENDED_FOR_3D_SECURE(7),
        SUSPENDED_FOR_CLIENT_REDIRECT(8);

        int code;

        ReasonCode(int code) {
            this.code = code;
        }

        public static ReasonCode getReasonCode(int code) {

            for (ReasonCode reasonCode :ReasonCode.values()) {
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
