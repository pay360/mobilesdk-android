/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment.request;

import com.google.gson.annotations.SerializedName;

public class MakePaymentRequest {

    @SerializedName("transaction")
    private Transaction transaction;

    @SerializedName("financialServices")
    private FinancialServices financialServices;

    @SerializedName("customer")
    private CustomerDetails customer;

    @SerializedName("paymentMethod")
    private PaymentMethod paymentMethod;

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
}
