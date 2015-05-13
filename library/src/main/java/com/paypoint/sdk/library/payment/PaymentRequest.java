/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment;

import com.paypoint.sdk.library.payment.request.BillingAddress;
import com.paypoint.sdk.library.payment.request.PaymentCard;
import com.paypoint.sdk.library.payment.request.Transaction;
import com.paypoint.sdk.library.security.PayPointCredentials;

/**
 * Models a payment request
 */
public class PaymentRequest {

    private Transaction transaction;

    private PaymentCard card;

    private BillingAddress address;

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
}
