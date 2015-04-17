package com.paypoint.sdk.library.exception;

/**
 * Who:  Pete
 * When: 17/04/2015
 * What:
 */
public class PaymentException extends Exception {

    public enum ErrorCode {
        CARD_EXPIRED,
        CARD_EXPIRY_INVALID,
        CARD_PAN_INVALID,
        CARD_PAN_INVALID_LUHN,
        CARD_CV2_INVALID,
        TRANSACTION_INVALID_AMOUNT,
        TRANSACTION_INVALID_CURRENCY,
        NETWORK_NO_CONNECTION,
        CREDENTIALS_INVALID,
    }

    private ErrorCode errorCode;

    public PaymentException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
