package com.paypoint.sdk;

import android.test.AndroidTestCase;

import com.paypoint.sdk.library.exception.CardInvalidPanException;
import com.paypoint.sdk.library.payment.PaymentError;
import com.paypoint.sdk.library.payment.PaymentManager;
import com.paypoint.sdk.library.payment.PaymentRequest;
import com.paypoint.sdk.library.payment.PaymentSuccess;
import com.paypoint.sdk.library.payment.request.BillingAddress;
import com.paypoint.sdk.library.payment.request.PaymentCard;
import com.paypoint.sdk.library.payment.request.Transaction;
import com.paypoint.sdk.library.security.PayPointCredentials;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.*;
import static com.jayway.awaitility.Awaitility.await;

/**
 * Who:  Pete
 * When: 09/04/2015
 * What:
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest="./app/src/main/AndroidManifest.xml", emulateSdk = 18, reportSdk = 18)
public class PaymentManagerTest implements PaymentManager.MakePaymentCallback {

    private static final int TIMEOUT_SIXTY_SECONDS = 60;

    private boolean responseReceived = false;
    private boolean success = false;

    private PaymentManager pm;
    private Transaction transaction;
    private PaymentCard card;
    private BillingAddress address;
    private PayPointCredentials credentials;
    private int responseTimeout;
    private PaymentRequest request;
    private String url = "http://127.0.0.1:5000/mobileapi/transaction";

    @Before
    public void setUp() {

        pm = new PaymentManager(Robolectric.application);

        Robolectric.getFakeHttpLayer().interceptHttpRequests(false);
        Robolectric.getFakeHttpLayer().interceptResponseContent(false);

        transaction = new Transaction().setAmount(10).setCurrency("GBP");

        card = new PaymentCard().setPan("9900000000005159").setCv2("123")
                .setExpiryDate("1115");

        address = new BillingAddress().setLine1("Flat1").setLine2("Cauldron House")
                .setLine3("A Street").setLine4("Twertonia").setCity("Bath").setRegion("Somerset")
                .setPostcode("BA1 234").setCountryCode("GBR");

        credentials = new PayPointCredentials().setInstallationId("1212312")
                .setToken("VALID_TOKEN");

        responseTimeout = 10;//TIMEOUT_SIXTY_SECONDS;

        request = new PaymentRequest();
        request.setTransaction(transaction)
                .setCard(card)
                .setAddress(address)
                .setCredentials(credentials)
                .setUrl(url)
                .setCallback(this);
    }

    @Test
    public void testTokenValid() throws Exception {
        makePayment();

        Assert.assertTrue(success);
    }

    public void testTokenInvalid() throws Exception {

        credentials.setToken("UNAUTHORIZED_TOKEN");

        makePayment();

        Assert.assertFalse(success);
    }

    public void testTokenExpired() throws Exception {

        credentials.setToken("EXPIRED_TOKEN");

        makePayment();

        Assert.assertFalse(success);
    }

    public void testCardDeclined() throws Exception {

        card.setPan("9900000000005282");

        makePayment();

        Assert.assertFalse(success);
    }

    public void testCardWaitFail() throws Exception {
        // default is to wait 60s - this card returns after 61s
        card.setPan("9900000000000168");

        makePayment();

        Assert.assertFalse(success);
    }

    public void testCardWaitSuccess() throws Exception {
        // default is to wait 60s - this card returns after 61s
        card.setPan("9900000000000168");

        responseTimeout = 65;

        pm.setResponseTimeout(responseTimeout);

        makePayment();

        Assert.assertTrue(success);
    }

    public void testCardEmptyPan() throws Exception {
        card.setPan("");

        try {
            makePayment();
            Assert.fail();
        } catch (CardInvalidPanException e) {
            Assert.assertTrue(true);
        }
    }

    public void testCardNullPan() throws Exception {
        card.setPan(null);

        try {
            makePayment();
            Assert.fail();
        } catch (CardInvalidPanException e) {
            Assert.assertTrue(true);
        }
    }

    public void testCardShortPan() throws Exception {
        card.setPan("12346786786786");

        try {
            makePayment();
            Assert.fail();
        } catch (CardInvalidPanException e) {
            Assert.assertTrue(true);
        }
    }

    public void testCardLongPan() throws Exception {
        card.setPan("99000000000051591123");

        try {
            makePayment();
            Assert.fail();
        } catch (CardInvalidPanException e) {
            Assert.assertTrue(true);
        }
    }

    public void testCardAlphPan() throws Exception {
        card.setPan("A900000000005159");

        try {
            makePayment();
            Assert.fail();
        } catch (CardInvalidPanException e) {
            Assert.assertTrue(true);
        }
    }

    // TODO add more tests for CCV etc

    private void makePayment() throws Exception {
        success = false;

        pm.makePayment(request);

        await().atMost(responseTimeout, TimeUnit.SECONDS).until(responseReceived());
    }

    @Override
    public void paymentSucceeded(PaymentSuccess response) {
        responseReceived = true;
        success = true;
    }

    @Override
    public void paymentFailed(PaymentError response) {
        responseReceived = true;
        success = false;
    }

    private Callable<Boolean> responseReceived() {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return responseReceived; // The condition that must be fulfilled
            }
        };
    }

}
