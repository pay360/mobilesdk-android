/*
 * Copyright (c) 2015. PayPoint
 */

package com.pay360.sdk.library.payment;

import java.util.List;

/**
 * Represents a payment/authorisation
 */
public class PaymentRequest {

    private Transaction transaction;

    private PaymentCard card;

    private BillingAddress address;

    private FinancialServices financialServices;

    private CustomerDetails customer;

    private List<CustomField> customFields;

    protected Transaction getTransaction() {
        return transaction;
    }

    /**
     * Set mandatory transaction details
     * @param transaction transation details
     * @return PaymentRequest for chaining
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
     * @param card card details
     * @return PaymentRequest for chaining
     */
    public PaymentRequest setCard(PaymentCard card) {
        this.card = card;
        return this;
    }

    protected BillingAddress getAddress() {
        return address;
    }

    /**
     * Set optional billing address
     * @param address billing address
     * @return PaymentRequest for chaining
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
     * @param financialServices financial services details
     * @return PaymentRequest for chaining
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
     * @param customer customer details
     * @return PaymentRequest for chaining
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
     * @param customFields custom fields
     * @return PaymentRequest for chaining
     */
    public PaymentRequest setCustomFields(List<CustomField> customFields) {
        this.customFields = customFields;
        return this;
    }
}
