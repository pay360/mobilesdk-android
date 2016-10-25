/*
 * Copyright (c) 2015. Pay360
 */

package com.pay360.sdk.library.exception;

/**
 * Payment validation error
 */
public class PaymentValidationException extends Exception {

    public enum ErrorCode {

        /**
         * Card has expired
         */
        CARD_EXPIRED,

        /**
         * Incorrect expiry date length or non numeric
         */
        CARD_EXPIRY_INVALID,

        /**
         * Incorrect PAN length or non numeric
         */
        CARD_PAN_INVALID,

        /**
         * Invalid card PAN
         */
        CARD_PAN_INVALID_LUHN,

        /**
         * Incorrect CV2 length or non numeric
         */
        CARD_CV2_INVALID,

        /**
         * No amount or negative amount specified
         */
        TRANSACTION_INVALID_AMOUNT,

        /**
         * No currency specified
         */
        TRANSACTION_INVALID_CURRENCY,

        /**
         * Device has no network connection
         */
        NETWORK_NO_CONNECTION,

        /**
         * Credentials missing (token or installation id)
         */
        INVALID_CREDENTIALS,

        /**
         * Server URL not passed in
         */
        INVALID_URL,

        /**
         * Empty PaymentRequest
         */
        INVALID_REQUEST,

        /**
         * Empty Transaction
         */
        INVALID_TRANSACTION,

        /**
         * Empty PaymentCard
         */
        INVALID_CARD,

        /**
         * Custom field exceeds maximum length
         */
        CUSTOM_FIELD_LENGTH_EXCEEDED
    }

    private ErrorCode errorCode;

    public PaymentValidationException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
