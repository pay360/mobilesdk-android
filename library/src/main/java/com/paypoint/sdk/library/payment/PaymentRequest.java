package com.paypoint.sdk.library.payment;

import com.paypoint.sdk.library.payment.request.BillingAddress;
import com.paypoint.sdk.library.payment.request.PaymentCard;
import com.paypoint.sdk.library.payment.request.Transaction;
import com.paypoint.sdk.library.security.PayPointCredentials;

/**
 * Who:  Pete
 * When: 13/04/2015
 * What:
 */
public class PaymentRequest {

    private String url;

    private Transaction transaction;

    private PaymentCard card;

    private BillingAddress address;

    private PayPointCredentials credentials;

    private PaymentManager.MakePaymentCallback callback;

    public String getUrl() {
        return url;
    }

    public PaymentRequest setUrl(String url) {
        this.url = url;
        return this;
    }

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

    public PayPointCredentials getCredentials() {
        return credentials;
    }

    public PaymentRequest setCredentials(PayPointCredentials credentials) {
        this.credentials = credentials;
        return this;
    }

    public PaymentManager.MakePaymentCallback getCallback() {
        return callback;
    }

    public PaymentRequest setCallback(PaymentManager.MakePaymentCallback callback) {
        this.callback = callback;
        return this;
    }
}
