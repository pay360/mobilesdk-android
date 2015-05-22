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
 * Represents a payment
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

    /**
     * Set mandatory transaction details
     * @param transaction
     */
    public PaymentRequest setTransaction(Transaction transaction) {
        this.transaction = transaction;
        return this;
    }

    protected PaymentCard getCard() {
        return card;
    }

    /**
     * Set mandatory card details
     * @param card
     */
    public PaymentRequest setCard(PaymentCard card) {
        this.card = card;
        return this;
    }

    protected BillingAddress getAddress() {
        return address;
    }

    /**
     * Set mandatory optional billing address
     * @param address
     */
    public PaymentRequest setAddress(BillingAddress address) {
        this.address = address;
        return this;
    }

    protected FinancialServices getFinancialServices() {
        return financialServices;
    }

    /**
     * Set optional financial services
     * @param financialServices
     */
    public PaymentRequest setFinancialServices(FinancialServices financialServices) {
        this.financialServices = financialServices;
        return this;
    }

    protected CustomerDetails getCustomer() {
        return customer;
    }

    /**
     * Set optional customer details
     * @param customer
     */
    public PaymentRequest setCustomer(CustomerDetails customer) {
        this.customer = customer;
        return this;
    }

    protected List<CustomField> getCustomFields() {
        return customFields;
    }

    /**
     * Set optional custom fields
     * @param customFields
     */
    public PaymentRequest setCustomFields(List<CustomField> customFields) {
        this.customFields = customFields;
        return this;
    }
}
