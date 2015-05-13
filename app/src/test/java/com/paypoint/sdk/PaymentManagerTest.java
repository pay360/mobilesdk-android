package com.paypoint.sdk;

import com.paypoint.sdk.library.exception.PaymentValidationException;
import com.paypoint.sdk.library.payment.PaymentError;
import com.paypoint.sdk.library.payment.PaymentManager;
import com.paypoint.sdk.library.payment.PaymentRequest;
import com.paypoint.sdk.library.payment.PaymentSuccess;
import com.paypoint.sdk.library.payment.request.BillingAddress;
import com.paypoint.sdk.library.payment.request.PaymentCard;
import com.paypoint.sdk.library.payment.request.Transaction;
import com.paypoint.sdk.library.security.PayPointCredentials;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.*;
import static com.jayway.awaitility.Awaitility.await;

@RunWith(CustomRobolectricRunner.class)
@Config(emulateSdk = 18, reportSdk = 18)
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
    private PaymentSuccess responseSuccess;
    private PaymentError responseError;

    private String url = "http://localhost:5000";

    @Before
    public void setUp() {

        pm = PaymentManager.getInstance(Robolectric.application);

        Robolectric.getFakeHttpLayer().interceptHttpRequests(false);
        Robolectric.getFakeHttpLayer().interceptResponseContent(false);

        transaction = new Transaction()
                .setAmount(10)
                .setCurrency("GBP")
                .setMerchantReference(UUID.randomUUID().toString());

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
                .setAddress(address);

        // self signed cert
        //url = "https://192.168.6.143";

        pm.setUrl(url);
        pm.setCredentials(credentials);
        pm.registerPaymentCallback(this);
    }

    @Test
    public void testTokenValid() throws Exception {
        makePayment();

        Assert.assertTrue(success);

        Assert.assertNotNull(responseSuccess);
        Assert.assertTrue(responseSuccess.getAmount() > 0);
        Assert.assertNotNull(responseSuccess.getCurrency());
        Assert.assertNotNull(responseSuccess.getLastFour());
        Assert.assertNotNull(responseSuccess.getMerchantReference());
        Assert.assertNotNull(responseSuccess.getTransactionId());
    }

    @Test
    public void testNullAddress() throws Exception {

        request.setAddress(null);

        makePayment();

        Assert.assertTrue(success);
    }

    @Test
    public void testTokenInvalid() throws Exception {

        credentials.setToken("UNAUTHORIZED_TOKEN");

        makePayment();

        Assert.assertFalse(success);

        checkReasonCode(PaymentError.ReasonCode.AUTHENTICATION_FAILED);
    }

    @Test
    public void testTokenExpired() throws Exception {

        credentials.setToken("EXPIRED_TOKEN");

        makePayment();

        Assert.assertFalse(success);

        checkReasonCode(PaymentError.ReasonCode.CLIENT_TOKEN_EXPIRED);
    }

    @Test
    public void testInternalServerError() throws Exception {

        card.setPan("9900000000010407");

        makePayment();

        Assert.assertFalse(success);

        checkReasonCode(PaymentError.ReasonCode.SERVER_ERROR);
    }

    @Test
    public void testCardDeclined() throws Exception {

        card.setPan("9900 0000 0000 5282");

        makePayment();

        Assert.assertFalse(success);

        checkReasonCode(PaymentError.ReasonCode.TRANSACTION_FAILED_TO_PROCESS);
    }

    @Test
    public void testCardWaitFail() throws Exception {
        responseTimeout = 1;

        pm.setResponseTimeout(responseTimeout);

        // default is to wait 60s - this card returns after 2s
        card.setPan("9900000000000168");

        makePayment();

        Assert.assertFalse(success);
    }

    @Test
    public void testCardWaitSuccess() throws Exception {
        // default is to wait 60s - this card returns after 61s
        card.setPan("9900000000000168");

        responseTimeout = 4;

        pm.setResponseTimeout(responseTimeout);

        makePayment();

        Assert.assertTrue(success);
    }

    @Test
    public void testCardEmptyPan() throws Exception {
        card.setPan("");

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testCardNullPan() throws Exception {
        card.setPan(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CARD_PAN_INVALID);
        }
    }

    @Test
    public void testCardLongPan() throws Exception {
        card.setPan("99000000000051591123");

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CARD_PAN_INVALID);
        }
    }

    @Test
    public void testCardAlphPan() throws Exception {
        card.setPan("A900000000005159");

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CARD_PAN_INVALID);
        }
    }

    @Test
    public void testCardEmptyCV2() throws Exception {
        card.setCv2("");

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CARD_CV2_INVALID);
        }
    }

    @Test
    public void testCardNullCV2() throws Exception {
        card.setCv2(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CARD_CV2_INVALID);
        }
    }

    @Test
    public void testCardEmptyExpiry() throws Exception {
        card.setExpiryDate("");

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CARD_EXPIRY_INVALID);
        }
    }

    @Test
    public void testCardNullExpiry() throws Exception {
        card.setExpiryDate(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CARD_EXPIRY_INVALID);
        }
    }

    @Test
    public void testCardExpired() throws Exception {

        Calendar c = Calendar.getInstance();
        int month = c.get(Calendar.MONTH);
        int year = c.get(Calendar.YEAR);

        year -= 2000;

        // months are indexed from 0 so should give an expired month

        String expiry = String.format("%02d%02d", month, year);
        card.setExpiryDate(expiry);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CARD_EXPIRED);
        }
    }

    @Test
    public void testCardExpiryValid() throws Exception {

        Calendar c = Calendar.getInstance();
        int month = c.get(Calendar.MONTH);
        int year = c.get(Calendar.YEAR);

        year -= 2000;

        // months are indexed from 0 so should add one to get current month
        month += 1;

        String expiry = String.format("%02d%02d", month, year);
        card.setExpiryDate(expiry);

        makePayment();

        Assert.assertTrue(success);
    }

    @Test
    public void testCardExpiryAlpha() throws Exception {
        card.setExpiryDate("abcd");

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CARD_EXPIRY_INVALID);
        }
    }

    @Test
    public void testTransactionZeroAmount() throws Exception {
        transaction.setAmount(0);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.TRANSACTION_INVALID_AMOUNT);
        }
    }

    @Test
    public void testTransactionNegativeAmount() throws Exception {
        transaction.setAmount(0);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.TRANSACTION_INVALID_AMOUNT);
        }
    }

    @Test
    public void testCardEmptyCurrency() throws Exception {
        transaction.setCurrency("");

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.TRANSACTION_INVALID_CURRENCY);
        }
    }

    @Test
    public void testCardNullCurrency() throws Exception {
        transaction.setCurrency(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.TRANSACTION_INVALID_CURRENCY);
        }
    }

    @Test
    public void testNullTransaction() throws Exception {
        request.setTransaction(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.INVALID_TRANSACTION);
        }
    }

    @Test
    public void testNullCard() throws Exception {
        request.setCard(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.INVALID_CARD);
        }
    }

    @Test
    public void testNullRequest() throws Exception {

        request = null;

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.INVALID_REQUEST);
        }
    }

    @Test
    public void testNullUrl() throws Exception {

        pm.setUrl(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.INVALID_URL);
        }
    }

    @Test
    public void testNullPayPointCredentials() throws Exception {
        pm.setCredentials(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Test
    public void testEmptyTokenPayPointCredentials() throws Exception {
        credentials.setToken(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Test
    public void testEmptyInstallationIdPayPointCredentials() throws Exception {
        credentials.setInstallationId(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.INVALID_CREDENTIALS);
        }
    }

    private void makePayment() throws Exception {
        success = false;

        pm.makePayment(request);

        try {
            await().atMost(responseTimeout + 5, TimeUnit.SECONDS).until(responseReceived());
        } catch (Throwable e) {
            success = false;
        }
    }

    @Override
    public void paymentSucceeded(PaymentSuccess response) {
        responseReceived = true;
        success = true;
        responseSuccess = response;
    }

    @Override
    public void paymentFailed(PaymentError response) {
        responseReceived = true;
        success = false;
        responseError = response;
    }

    private Callable<Boolean> responseReceived() {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return responseReceived; // The condition that must be fulfilled
            }
        };
    }

    private void checkPaymentException(PaymentValidationException e, PaymentValidationException.ErrorCode expected) {
        if (e.getErrorCode() == expected) {
            Assert.assertTrue(true);
        } else {
            Assert.fail();
        }
    }

    private void checkNetwork(Integer expectedStatusCode) {
        Assert.assertEquals(PaymentError.Kind.NETWORK, responseError.getKind());
        Assert.assertNotNull(responseError);
        Assert.assertNotNull(responseError.getNetworkError());

        if (expectedStatusCode != null) {
            Assert.assertEquals(expectedStatusCode.intValue(), responseError.getNetworkError().getHttpStatusCode());
        }
    }

    private void checkReasonCode(PaymentError.ReasonCode expected) {
        Assert.assertEquals(PaymentError.Kind.PAYPOINT, responseError.getKind());
        Assert.assertNotNull(responseError);
        Assert.assertNotNull(responseError.getPayPointError());
        Assert.assertEquals(expected,
                responseError.getPayPointError().getReasonCode());
    }

}
