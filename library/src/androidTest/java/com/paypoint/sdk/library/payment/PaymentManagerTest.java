package com.paypoint.sdk.library.payment;

import android.test.AndroidTestCase;

import com.paypoint.sdk.library.payment.request.BillingAddress;
import com.paypoint.sdk.library.payment.request.PaymentCard;
import com.paypoint.sdk.library.payment.request.Transaction;
import com.paypoint.sdk.library.security.PayPointCredentials;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.*;

/**
 * Who:  Pete
 * When: 09/04/2015
 * What:
 */
public class PaymentManagerTest extends AndroidTestCase {

    private static final int TIMEOUT_SIXTY_SECONDS = 60;

    private boolean responseReceived = false;

    public void testPaymentAuthorized() {

        PaymentManager pm = new PaymentManager(getContext());

        Transaction transaction = new Transaction().setAmount(10).setCurrency("GBP");

        PaymentCard card = new PaymentCard().setPan("9900000000005159").setCv2("123")
                .setExpiryDate("1115");

        BillingAddress address = new BillingAddress().setLine1("Flat1").setLine2("Cauldron House")
                .setLine3("A Street").setLine4("Twertonia").setCity("Bath").setRegion("Somerset")
                .setPostcode("BA1 234").setCountryCode("GBR");

        PayPointCredentials credentials = new PayPointCredentials().setInstallationId("1212312")
                .setToken("VALID_TOKEN");

        pm.makePayment(transaction, card, address, credentials, TIMEOUT_SIXTY_SECONDS, new PaymentManager.MakePaymentCallback() {
            @Override
            public void paymentSucceeded() {
                responseReceived = true;
            }

            @Override
            public void paymentFailed() {
                responseReceived = true;
            }
        });

        await().atMost(TIMEOUT_SIXTY_SECONDS, TimeUnit.SECONDS).until(responseReceived());
    }

    private Callable<Boolean> responseReceived() {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return responseReceived; // The condition that must be fulfilled
            }
        };
    }

}
