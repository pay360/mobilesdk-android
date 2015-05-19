/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment.request;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MakePaymentRequest {

    @SerializedName("sdkVersion")
    private String sdkVersion;

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
        this.sdkVersion = sdkVersion;
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
