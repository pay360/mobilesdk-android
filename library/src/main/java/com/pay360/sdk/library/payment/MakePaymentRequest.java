/*
 * Copyright (c) 2016 Capita plc
 */

package com.pay360.sdk.library.payment;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MakePaymentRequest {

    @SerializedName("sdkVersion")
    private String sdkVersion;

    @SerializedName("merchantAppName")
    private String merchantAppName;

    @SerializedName("merchantAppVersion")
    private String merchantAppVersion;

    @SerializedName("deviceInfo")
    private DeviceInfo deviceInfo;

    @SerializedName("transaction")
    private Transaction transaction;

    @SerializedName("financialServices")
    private FinancialServices financialServices;

    @SerializedName("customer")
    private CustomerDetails customer;

    @SerializedName("paymentMethod")
    private PaymentMethod paymentMethod;

    @SerializedName("customFields")
    private CustomFieldsContainer customFieldsContainer;

    public MakePaymentRequest setSdkVersion(String sdkVersion) {
        this.sdkVersion = "pp_android_sdk:" + sdkVersion;
        return this;
    }

    public MakePaymentRequest setMerchantAppName(String merchantAppName) {
        this.merchantAppName = merchantAppName;
        return this;
    }

    public MakePaymentRequest setMerchantAppVersion(String merchantAppVersion) {
        this.merchantAppVersion = merchantAppVersion;
        return this;
    }

    public MakePaymentRequest setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
        return this;
    }

    public MakePaymentRequest setTransaction(Transaction transaction) {
        this.transaction = transaction;
        return this;
    }

    public MakePaymentRequest setFinancialServices(FinancialServices financialServices) {
        this.financialServices = financialServices;
        return this;
    }

    public MakePaymentRequest setCustomer(CustomerDetails customer) {
        this.customer = customer;
        return this;
    }

    public MakePaymentRequest setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public MakePaymentRequest setCustomFields(List<CustomField> customFields) {
        if (customFields != null &&
            !customFields.isEmpty()) {
            this.customFieldsContainer = new CustomFieldsContainer();
            this.customFieldsContainer.customFields = customFields;
        }
        return this;
    }

    private static class CustomFieldsContainer {
        @SerializedName("fieldState")
        private List<CustomField> customFields;
    }
}
