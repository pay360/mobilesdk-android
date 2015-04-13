package com.paypoint.sdk.library.payment.request;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.paypoint.sdk.library.exception.CardExpiredException;
import com.paypoint.sdk.library.exception.CardInvalidCv2Exception;
import com.paypoint.sdk.library.exception.CardInvalidLuhnException;
import com.paypoint.sdk.library.exception.CardInvalidPanException;

import net.paypoint.cardutilities.Cv2;
import net.paypoint.cardutilities.ExpiryUtils;
import net.paypoint.cardutilities.Pan;
import net.paypoint.cardutilities.PanUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * Created by HendryP on 08/04/2015.
 */
public class PaymentCard {

    private static final int PAN_LENGTH_MIN = 15;
    private static final int PAN_LENGTH_MAX = 19;

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

    public String getPan() {
        return StringUtils.deleteWhitespace(pan);
    }

    public String getCv2() {
        return StringUtils.deleteWhitespace(cv2);
    }

    public String getExpiryDate() {
        return StringUtils.deleteWhitespace(expiryDate);
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void validateData() throws CardInvalidPanException, CardExpiredException,
            CardInvalidLuhnException, CardInvalidCv2Exception{

        ExpiryUtils expiryUtils = new ExpiryUtils();

        String pan = getPan();

        if (TextUtils.isEmpty(pan)) {
            throw new CardInvalidPanException();
        }

        // check pan 15-19 digits + all numeric
        if (!Pan.isValidCardNumber(pan)) {
            throw new CardInvalidPanException();
        }

        if (pan.length() < PAN_LENGTH_MIN ||
            pan.length() > PAN_LENGTH_MAX) {
            throw new CardInvalidPanException();
        }

        // check expiry
        if (expiryUtils.isCardExpired(getExpiryDate(), new Date())) {
            throw new CardExpiredException();
        }

        // check luhn
        if (!PanUtils.checkLuhn(getPan())) {
            throw new CardInvalidLuhnException();
        }

        // check ccv
        if (!Cv2.isValidCv2Number(getCv2())) {
            throw new CardInvalidCv2Exception();
        }
    }
}
