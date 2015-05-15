/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment.request;

import com.google.gson.annotations.SerializedName;

public class MakePaymentRequest {

    @SerializedName("deviceInfo")
    private DeviceInfo deviceInfo;

    @SerializedName("transaction")
    private Transaction transaction;

    @SerializedName("paymentMethod")
    private PaymentMethod paymentMethod;

    public MakePaymentRequest setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
        return this;
    }

    public MakePaymentRequest setTransaction(Transaction transaction) {
        this.transaction = transaction;
        return this;
    }

    public MakePaymentRequest setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }
}
