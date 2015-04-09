package com.paypoint.sdk.library.payment.response;

import com.google.gson.annotations.SerializedName;

/**
 * Created by HendryP on 08/04/2015.
 */
public class PaymentMethod {

    @SerializedName("card")
    private PaymentCard card;
}
