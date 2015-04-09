package com.paypoint.sdk.library.payment.request;

import com.google.gson.annotations.SerializedName;

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
}
