package com.paypoint.sdk;

import android.test.AndroidTestCase;

import com.paypoint.sdk.library.exception.InvalidCredentialsException;
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

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;

/**
 * Who:  Pete
 * When: 09/04/2015
 * What:
 */
public class PaymentManagerTest extends AndroidTestCase implements PaymentManager.MakePaymentCallback {

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
//    private String url = "http://10.0.3.2:5000/mobileapi"; For Genymotion
    private String url = "http://192.168.3.138:5000/mobileapi";
    private PaymentSuccess responseSuccess;
    private PaymentError responseError;

    @Override
    public void setUp() {

        credentials = new PayPointCredentials().setInstallationId("1212312")
                .setToken("VALID_TOKEN");

        pm = new PaymentManager(getContext())
                .setUrl(url)
                .setCredentials(credentials);

        transaction = new Transaction()
                .setAmount(10)
                .setCurrency("GBP")
                .setMerchantReference(UUID.randomUUID().toString());

        card = new PaymentCard().setPan("9900000000005159").setCv2("123")
                .setExpiryDate("1115");

        address = new BillingAddress().setLine1("Flat1").setLine2("Cauldron House")
                .setLine3("A Street").setLine4("Twertonia").setCity("Bath").setRegion("Somerset")
                .setPostcode("BA1 234").setCountryCode("GBR");

        responseTimeout = TIMEOUT_SIXTY_SECONDS;

        request = new PaymentRequest();
        request.setTransaction(transaction)
                .setCard(card)
                .setAddress(address)
                .setCallback(this);
    }

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

    public void testNullAddress() throws Exception {

        request.setAddress(null);

        makePayment();

        Assert.assertFalse(success);
    }

    public void testTokenInvalid() throws Exception {

        credentials.setToken("UNAUTHORIZED_TOKEN");

        makePayment();

        Assert.assertFalse(success);

        checkReasonCode(PaymentError.ReasonCode.AUTHENTICATION_FAILED);
    }

    public void testTokenExpired() throws Exception {

        credentials.setToken("EXPIRED_TOKEN");

        makePayment();

        Assert.assertFalse(success);

        checkReasonCode(PaymentError.ReasonCode.CLIENT_TOKEN_EXPIRED);
    }

    public void testInternalServerError() throws Exception {

        card.setPan("9900000000010407");

        makePayment();

        Assert.assertFalse(success);

        checkReasonCode(PaymentError.ReasonCode.SERVER_ERROR);
    }

    public void testCardDeclined() throws Exception {

        card.setPan("9900 0000 0000 5282");

        makePayment();

        Assert.assertFalse(success);

        checkReasonCode(PaymentError.ReasonCode.TRANSACTION_FAILED_TO_PROCESS);
    }

    public void testCardWaitFail() throws Exception {
        responseTimeout = 1;

        pm.setResponseTimeout(responseTimeout);

        // default is to wait 60s - this card returns after 2s
        card.setPan("9900000000000168");

        makePayment();

        Assert.assertFalse(success);
    }

    public void testCardWaitSuccess() throws Exception {
        // default is to wait 60s - this card returns after 61s
        card.setPan("9900000000000168");

        responseTimeout = 4;

        pm.setResponseTimeout(responseTimeout);

        makePayment();

        Assert.assertTrue(success);
    }

    public void testCardEmptyPan() throws Exception {
        card.setPan("");

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            Assert.assertTrue(true);
        }
    }

    public void testCardNullPan() throws Exception {
        card.setPan(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CARD_PAN_INVALID);
        }
    }

    public void testCardLongPan() throws Exception {
        card.setPan("99000000000051591123");

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CARD_PAN_INVALID);
        }
    }

    public void testCardAlphPan() throws Exception {
        card.setPan("A900000000005159");

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CARD_PAN_INVALID);
        }
    }

    public void testCardEmptyCV2() throws Exception {
        card.setCv2("");

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CARD_CV2_INVALID);
        }
    }

    public void testCardNullCV2() throws Exception {
        card.setCv2(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CARD_CV2_INVALID);
        }
    }

    public void testCardEmptyExpiry() throws Exception {
        card.setExpiryDate("");

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CARD_EXPIRY_INVALID);
        }
    }

    public void testCardNullExpiry() throws Exception {
        card.setExpiryDate(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CARD_EXPIRY_INVALID);
        }
    }

    public void testCardExpired() throws Exception {
        card.setExpiryDate("0315");

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CARD_EXPIRED);
        }
    }

    public void testTransactionZeroAmount() throws Exception {
        transaction.setAmount(0);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.TRANSACTION_INVALID_AMOUNT);
        }
    }

    public void testTransactionNegativeAmount() throws Exception {
        transaction.setAmount(0);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.TRANSACTION_INVALID_AMOUNT);
        }
    }

    public void testCardEmptyCurrency() throws Exception {
        transaction.setCurrency("");

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.TRANSACTION_INVALID_CURRENCY);
        }
    }

    public void testCardNullCurrency() throws Exception {
        transaction.setCurrency(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.TRANSACTION_INVALID_CURRENCY);
        }
    }

    public void testNullTransaction() throws Exception {
        request.setTransaction(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.INVALID_TRANSACTION);
        }
    }

    public void testNullCard() throws Exception {
        request.setCard(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.INVALID_CARD);
        }
    }

    public void testNullRequest() throws Exception {

        request = null;

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.INVALID_REQUEST);
        }
    }

    public void testNullUrl() throws Exception {

        pm.setUrl(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.INVALID_URL);
        }
    }

    public void testNullPayPointCredentials() throws Exception {
        pm.setCredentials(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.INVALID_CREDENTIALS);
        }
    }

    public void testEmptyTokenPayPointCredentials() throws Exception {
        credentials.setToken(null);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.INVALID_CREDENTIALS);
        }
    }

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
