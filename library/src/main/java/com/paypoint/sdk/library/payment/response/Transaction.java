package com.paypoint.sdk.library.payment.response;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Created by HendryP on 08/04/2015.
 */
public class Transaction {

    @SerializedName("transactionId")
    private String transactionId;

    @SerializedName("merchantRef")
    private String merchantReference;

    @SerializedName("type")
    private String type;

    @SerializedName("amount")
    private float amount;

    @SerializedName("currency")
    private String currency;

    @SerializedName("transactionTime")
    private String transactionTime;
}
