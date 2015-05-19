/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment;

import com.paypoint.sdk.library.payment.request.BillingAddress;
import com.paypoint.sdk.library.payment.request.CustomField;
import com.paypoint.sdk.library.payment.request.CustomerDetails;
import com.paypoint.sdk.library.payment.request.FinancialServices;
import com.paypoint.sdk.library.payment.request.PaymentCard;
import com.paypoint.sdk.library.payment.request.Transaction;
import com.paypoint.sdk.library.security.PayPointCredentials;

import java.util.List;

/**
 * Models a payment request
 */
public class PaymentRequest {

    private Transaction transaction;

    private PaymentCard card;

    private BillingAddress address;

    private FinancialServices financialServices;

    private CustomerDetails customer;

    private List<CustomField> customFields;

    public Transaction getTransaction() {
        return transaction;
    }

    public PaymentRequest setTransaction(Transaction transaction) {
        this.transaction = transaction;
        return this;
    }

    public PaymentCard getCard() {
        return card;
    }

    public PaymentRequest setCard(PaymentCard card) {
        this.card = card;
        return this;
    }

    public BillingAddress getAddress() {
        return address;
    }

    public PaymentRequest setAddress(BillingAddress address) {
        this.address = address;
        return this;
    }

    public FinancialServices getFinancialServices() {
        return financialServices;
    }

    public PaymentRequest setFinancialServices(FinancialServices financialServices) {
        this.financialServices = financialServices;
        return this;
    }

    public CustomerDetails getCustomer() {
        return customer;
    }

    public PaymentRequest setCustomer(CustomerDetails customer) {
        this.customer = customer;
        return this;
    }

    public List<CustomField> getCustomFields() {
        return customFields;
    }

    public PaymentRequest setCustomFields(List<CustomField> customFields) {
        this.customFields = customFields;
        return this;
    }
}
