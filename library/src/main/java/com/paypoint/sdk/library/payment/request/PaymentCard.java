/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment.request;

import com.google.gson.annotations.SerializedName;
import com.paypoint.sdk.library.exception.PaymentValidationException;
import com.paypoint.sdk.library.utils.Cv2Utils;
import com.paypoint.sdk.library.utils.ExpiryUtils;
import com.paypoint.sdk.library.utils.PanUtils;
import com.paypoint.sdk.library.utils.StringUtils;

import java.util.Date;

/**
 * Mandatory payment card details
 */
public class PaymentCard {

    @SerializedName("pan")
    private String pan;

    @SerializedName("cv2")
    private String cv2;

    @SerializedName("expiryDate")
    private String expiryDate;

    @SerializedName("cardHolderName")
    private String cardHolderName;

    /**
     * Set the card PAM
     * @param pan card PAN\number
     * @return
     */
    public PaymentCard setPan(String pan) {
        this.pan = StringUtils.deleteWhitespace(pan);
        return this;
    }

    /**
     * Set the card CV2
     * @param cv2 3-4 digits
     * @return
     */
    public PaymentCard setCv2(String cv2) {
        this.cv2 = StringUtils.deleteWhitespace(cv2);
        return this;
    }

    /**
     * Set the card expiry
     * @param expiryDate card expiry MMYY e.g. 0216 for February 2016
     * @return
     */
    public PaymentCard setExpiryDate(String expiryDate) {
        this.expiryDate = StringUtils.deleteWhitespace(expiryDate);
        return this;
    }

    /**
     * Set the cardholder
     * @param cardHolderName  name as written on the card
     * @return
     */
    public PaymentCard setCardHolderName(String cardHolderName) {
        this.cardHolderName = StringUtils.deleteWhitespace(cardHolderName);
        return this;
    }

    public String getPan() {
        return pan;
    }

    public String getCv2() {
        return cv2;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void validateData() throws PaymentValidationException {

        validatePan(pan);

        validateExpiry(expiryDate);

        validateCv2(cv2);
    }

    public static void validatePan(String pan) throws PaymentValidationException {
        pan = StringUtils.deleteWhitespace(pan);

        // check pan 15-19 digits + all numeric
        if (!PanUtils.isValidCardNumber(pan)) {
            throw new PaymentValidationException(PaymentValidationException.ErrorCode.CARD_PAN_INVALID);
        }

        // check luhn
        if (!PanUtils.checkLuhn(pan)) {
            throw new PaymentValidationException(PaymentValidationException.ErrorCode.CARD_PAN_INVALID_LUHN);
        }
    }

    public static void validateExpiry(String expiryDate) throws PaymentValidationException {
        ExpiryUtils expiryUtils = new ExpiryUtils();

        expiryDate = StringUtils.deleteWhitespace(expiryDate);

        if (!expiryUtils.isValid(expiryDate)) {
            throw new PaymentValidationException(PaymentValidationException.ErrorCode.CARD_EXPIRY_INVALID);
        }

        // check expiry
        if (expiryUtils.isCardExpired(expiryDate, new Date())) {
            throw new PaymentValidationException(PaymentValidationException.ErrorCode.CARD_EXPIRED);
        }
    }

    public static void validateCv2(String cv2) throws PaymentValidationException {
        cv2 = StringUtils.deleteWhitespace(cv2);

        // check ccv
        if (!Cv2Utils.isValidCv2Number(cv2)) {
            throw new PaymentValidationException(PaymentValidationException.ErrorCode.CARD_CV2_INVALID);
        }
    }
}
