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

    public enum ReasonCode {

        /**
         * Network issue occurred during payment processing
         */
        NETWORK_ERROR_DURING_PROCESSING(-7, true),

        /**
         * Failed to communicate with payment server
         */
        NETWORK_NO_CONNECTION(-6, false),

        /**
         * Transaction not returned in given timeout period
         */
        TRANSACTION_TIMED_OUT(-5, true),

        /**
         * Transaction cancelled by user
         */
        TRANSACTION_CANCELLED_BY_USER(-4, false),

        /**
         * An unexpected error
         */
        UNEXPECTED(-1, true),

        /**
         * Request was not correctly formed
         */
        INVALID(1, false),

        /**
         * The presented API token was not valid, or the wrong type of authentication was used
         */
        AUTHENTICATION_FAILED(2, false),

        /**
         * Get a new token
         */
        CLIENT_TOKEN_EXPIRED(3, false),

        /**
         * The token was valid, but does not grant you access to use the specified feature
         */
        UNAUTHORISED_REQUEST(4, false),

        /**
         * The transaction was declined by the server
         */
        TRANSACTION_DECLINED(5, false),

        /**
         * An internal server error occurred at PayPoint
         */
        SERVER_ERROR(6, true),

        /**
         * Transaction not found on the server, payment not taken
         */
        TRANSACTION_NOT_FOUND(10, false);

        int code;
        boolean checkStatus;

        ReasonCode(int code, boolean checkStatus) {
            this.code = code;
            this.checkStatus = checkStatus;
        }

        /**
         * Whether the transaction completed e.g. success\fail or is in in unknown state
         * If true then get the status of the current transaction is unknown in which
         * case the app should call getTransactionStatus to get the status of the transaction
         * @return true if transaction in unknown state
         */
        public boolean shouldCheckStatus() {
            return checkStatus;
        }

        public static ReasonCode getReasonCode(int code) {

            for (ReasonCode reasonCode : ReasonCode.values()) {
                if (reasonCode.code == code) {
                    return reasonCode;
                }
            }

            return UNEXPECTED;
        }
    }

    public List<CustomField> getCustomFields() {
        return customFields;
    }

    protected void setCustomFields(List<CustomField> customFields) {
        this.customFields = customFields;
    }

    private ReasonCode reasonCode = ReasonCode.UNEXPECTED;

    private String reasonMessage;

    public ReasonCode getReasonCode() {
        return reasonCode;
    }

    /**
     * The reason for failure
     * @param reasonCode enumerated error code
     */
    public void setReasonCode(int reasonCode) {
        this.reasonCode = ReasonCode.getReasonCode(reasonCode);
    }

    public void setReasonCode(ReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }

    /**
     * Returns details about what went wrong - this should not be displayed to the user
     * please formulate your own messages
     * @return verbose error details
     */
    public String getReasonMessage() {
        return reasonMessage;
    }

    public void setReasonMessage(String reasonMessage) {
        this.reasonMessage = reasonMessage;
    }
}
