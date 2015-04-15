package com.paypoint.sdk.library.payment.request;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.paypoint.sdk.library.exception.CardExpiredException;
import com.paypoint.sdk.library.exception.CardInvalidCv2Exception;
import com.paypoint.sdk.library.exception.CardInvalidExpiryException;
import com.paypoint.sdk.library.exception.CardInvalidLuhnException;
import com.paypoint.sdk.library.exception.CardInvalidPanException;
import com.paypoint.sdk.library.utils.Cv2Utils;
import com.paypoint.sdk.library.utils.ExpiryUtils;
import com.paypoint.sdk.library.utils.PanUtils;
import com.paypoint.sdk.library.utils.StringUtils;

import java.util.Date;

/**
 * Created by HendryP on 08/04/2015.
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

    public PaymentCard setPan(String pan) {
        this.pan = pan;
        return this;
    }

    public PaymentCard setCv2(String cv2) {
        this.cv2 = cv2;
        return this;
    }

    public PaymentCard setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
        return this;
    }

    public PaymentCard setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
        return this;
    }

    protected String getPan() {
        return StringUtils.deleteWhitespace(pan);
    }

    protected String getCv2() {
        return StringUtils.deleteWhitespace(cv2);
    }

    protected String getExpiryDate() {
        return StringUtils.deleteWhitespace(expiryDate);
    }

    protected String getCardHolderName() {
        return cardHolderName;
    }

    public void validateData() throws CardInvalidPanException, CardInvalidExpiryException, CardExpiredException,
            CardInvalidLuhnException, CardInvalidCv2Exception{

        ExpiryUtils expiryUtils = new ExpiryUtils();

        String pan = getPan();
        String expiry = getExpiryDate();

        // check pan 15-19 digits + all numeric
        if (!PanUtils.isValidCardNumber(pan)) {
            throw new CardInvalidPanException();
        }

        // check luhn
        if (!PanUtils.checkLuhn(pan)) {
            throw new CardInvalidLuhnException();
        }

        if (!expiryUtils.isValid(expiry)) {
            throw new CardInvalidExpiryException();
        }

        // check expiry
        if (expiryUtils.isCardExpired(expiry, new Date())) {
            throw new CardExpiredException();
        }

        // check ccv
        if (!Cv2Utils.isValidCv2Number(getCv2())) {
            throw new CardInvalidCv2Exception();
        }
    }
}
