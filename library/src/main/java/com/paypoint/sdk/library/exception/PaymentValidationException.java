/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.exception;

/**
 * Who:  Pete
 * When: 17/04/2015
 * What:
 */
public class PaymentValidationException extends Exception {

    public enum ErrorCode {
        CARD_EXPIRED,                   // card has expired
        CARD_EXPIRY_INVALID,            // incorrect length or non numeric
        CARD_PAN_INVALID,               // incorrect PAN length or non numeric
        CARD_PAN_INVALID_LUHN,          // invalid card PAN
        CARD_CV2_INVALID,               // incorrect CV2 length or non numeric
        TRANSACTION_INVALID_AMOUNT,     // no amount or negative amount specified
        TRANSACTION_INVALID_CURRENCY,   // no currency specified
        NETWORK_NO_CONNECTION,          // device has no network connection
        INVALID_CREDENTIALS,            // credentials missing (PayPoint token or installation id)
        INVALID_URL,                    // PayPoint server URL not passed in
        INVALID_REQUEST,                // empty PaymentRequest
        INVALID_TRANSACTION,            // empty Transaction
        INVALID_CARD                    // empty PaymentCard
    }

    private ErrorCode errorCode;

    public PaymentValidationException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
