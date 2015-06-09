package com.paypoint.sdk;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.paypoint.sdk.library.ThreeDSActivity;
import com.paypoint.sdk.library.exception.InvalidCredentialsException;
import com.paypoint.sdk.library.exception.PaymentValidationException;
import com.paypoint.sdk.library.exception.TransactionInProgressException;
import com.paypoint.sdk.library.exception.TransactionSuspendedFor3DSException;
import com.paypoint.sdk.library.payment.PaymentError;
import com.paypoint.sdk.library.payment.PaymentManager;
import com.paypoint.sdk.library.payment.PaymentRequest;
import com.paypoint.sdk.library.payment.PaymentSuccess;
import com.paypoint.sdk.library.payment.request.BillingAddress;
import com.paypoint.sdk.library.payment.request.CustomField;
import com.paypoint.sdk.library.payment.request.CustomerDetails;
import com.paypoint.sdk.library.payment.request.FinancialServices;
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
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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
    private PayPointCredentials credentials;
    private int responseTimeout;
    private PaymentRequest request;
    private PaymentSuccess responseSuccess;
    private PaymentError responseError;
    private String operationId;
    private long timestamp;

    private String url = "http://localhost:5000";

    @Before
    public void setUp() throws Exception {

        // reset singleton for each test - required by test which call sendBroadcast
        // invoke test method by reflection as don't want to declare resetInstance as public
        Method m = PaymentManager.class.getDeclaredMethod("TEST_resetInstance", null);
        m.setAccessible(true); //if security settings allow this
        m.invoke(null, (Object[])null);

        pm = PaymentManager.getInstance(Robolectric.application);

        Robolectric.getFakeHttpLayer().interceptHttpRequests(false);
        Robolectric.getFakeHttpLayer().interceptResponseContent(false);

        transaction = new Transaction()
                .setAmount(10)
                .setCurrency("GBP")
                .setMerchantReference(UUID.randomUUID().toString());

        card = new PaymentCard().setPan("9900000000005159").setCv2("123")
                .setExpiryDate("1115");

        credentials = new PayPointCredentials().setInstallationId("1212312")
                .setToken("VALID_TOKEN");

        responseTimeout = 10;//TIMEOUT_SIXTY_SECONDS;

        request = new PaymentRequest();
        request.setTransaction(transaction)
                .setCard(card);

        operationId = null;

        pm.setUrl(url);
        pm.setCredentials(credentials);
        pm.registerPaymentCallback(this);

        timestamp = System.currentTimeMillis();
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

        checkReasonCode(PaymentError.ReasonCode.TRANSACTION_DECLINED);
    }

    @Test
    public void testCardWaitFail() throws Exception {

        responseTimeout = 1;

        pm.setSessionTimeout(responseTimeout);

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

        pm.setSessionTimeout(responseTimeout);

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
        } catch (InvalidCredentialsException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testNullPayPointCredentials() throws Exception {
        pm.setCredentials(null);

        try {
            makePayment();
            Assert.fail();
        } catch (InvalidCredentialsException e) {
           Assert.assertTrue(true);
        }
    }

    @Test
    public void testEmptyTokenPayPointCredentials() throws Exception {
        credentials.setToken(null);

        try {
            makePayment();
            Assert.fail();
        } catch (InvalidCredentialsException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testEmptyInstallationIdPayPointCredentials() throws Exception {
        credentials.setInstallationId(null);

        try {
            makePayment();
            Assert.fail();
        } catch (InvalidCredentialsException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testDeferredPayment() throws Exception {
        transaction.setDeferred(true);

        makePayment();

        Assert.assertTrue(success);
    }

    @Test
    public void testBillingAddress() throws Exception {

        BillingAddress address = new BillingAddress().setLine1("Flat1").setLine2("Cauldron House")
                .setLine3("A Street").setLine4("Twertonia").setCity("Bath").setRegion("Somerset")
                .setPostcode("BA1 234").setCountryCode("GBR");

        request.setAddress(address);

        makePayment();

        Assert.assertTrue(success);
    }

    @Test
    public void testFinancialServices() throws Exception {

        FinancialServices financialServices = new FinancialServices()
                .setDateOfBirth("19870818")
                .setSurname("Smith")
                .setAccountNumber("123ABC")
                .setPostCode("BS20");

        request.setFinancialServices(financialServices);

        makePayment();

        Assert.assertTrue(success);
    }

    @Test
    public void testCustomerDetails() throws Exception {

        CustomerDetails customerDetails = new CustomerDetails()
                .setEmail("test@paypoint.com")
                .setDateOfBirth("1900-01-01")
                .setTelephone("01225 123456");

        request.setCustomer(customerDetails);

        makePayment();

        Assert.assertTrue(success);
    }

    @Test
    public void testCustomFields() throws Exception {

        List<CustomField> customFields = new ArrayList<CustomField>();

        customFields.add(new CustomField()
                .setName("Name 1")
                .setValue("Value 1")
                .setTransient(true));

        customFields.add(new CustomField()
                .setName("Name 2")
                .setTransient(true));

        customFields.add(new CustomField()
                .setName("Name 3")
                .setTransient(false));

        customFields.add(new CustomField()
                .setName("Name 4"));

        request.setCustomFields(customFields);

        makePayment();

        Assert.assertTrue(success);

        // custom fields are echoed back in the response
        Assert.assertNotNull(responseSuccess);
        Assert.assertNotNull(responseSuccess.getCustomFields());
        Assert.assertEquals(customFields, responseSuccess.getCustomFields());
    }

    @Test
    public void testCustomFieldsDecline() throws Exception {

        card.setPan("9900 0000 0000 5282");

        List<CustomField> customFields = new ArrayList<CustomField>();

        customFields.add(new CustomField()
                .setName("Name 1")
                .setValue("Value 1")
                .setTransient(true));

        customFields.add(new CustomField()
                .setName("Name 2")
                .setTransient(true));

        customFields.add(new CustomField()
                .setName("Name 3")
                .setTransient(false));

        customFields.add(new CustomField()
                .setName("Name 4"));

        request.setCustomFields(customFields);

        makePayment();

        Assert.assertFalse(success);

        // custom fields are echoed back in the response
        Assert.assertNotNull(responseError);
        Assert.assertNotNull(responseError.getCustomFields());
        Assert.assertEquals(customFields, responseError.getCustomFields());
    }

    @Test
    public void testCustomFieldsLengthExceeded() throws Exception {
        List<CustomField> customFields = new ArrayList<CustomField>();

        // create custom field with length > 255
        StringBuilder value = new StringBuilder();

        for (int i = 0; i < 256; i++) {
            value.append('1');
        }

        customFields.add(new CustomField()
                .setName("Name")
                .setValue(value.toString())
                .setTransient(true));

        request.setCustomFields(customFields);

        try {
            makePayment();
            Assert.fail();
        } catch (PaymentValidationException e) {
            checkPaymentException(e, PaymentValidationException.ErrorCode.CUSTOM_FIELD_LENGTH_EXCEEDED);
        }
    }

    @Test
    public void testReliableDeliverySuccess() throws Exception {

        responseTimeout = 60;

        pm.setSessionTimeout(responseTimeout);

        // "9900000000000267" will Simulate a network failure on the initial payment
        // after the payment was received by PayPoint (i.e. payment will be processed)
        card.setPan("9900000000000267");

        makePayment();

        Assert.assertTrue(success);
    }

    @Test
    public void testReliableDeliveryFail() throws Exception {

        responseTimeout = 60;

        pm.setSessionTimeout(responseTimeout);

        // "9900000000000366" will Simulate a network failure on the initial payment
        // before the payment was received by PayPoint (payment will not have been processed)
        card.setPan("9900000000000366");

        makePayment();

        Assert.assertFalse(success);
    }

    @Test
    public void testReliableDeliveryDelaySuccess() throws Exception {

        responseTimeout = 15;

        pm.setSessionTimeout(responseTimeout);

        // "9900000000000275" will simluate a network failure on a transaction which takes 5 seconds to process
        card.setPan("9900000000000275");

        makePayment();

        Assert.assertTrue(success);
    }

    @Test
    public void testReliableDeliveryDelayFail() throws Exception {

        responseTimeout = 15;

        pm.setSessionTimeout(responseTimeout);

        // "9900000000000176" will simulate a network failure on a transaction which takes 5 seconds to process and then declines
        card.setPan("9900000000000176");

        makePayment();

        Assert.assertFalse(success);
        Assert.assertNotNull(responseError);
    }

    @Test
    public void testGetStatusSuccess() throws Exception {

        // first make successful payment
        makePayment();

        Assert.assertTrue(success);

        // now check status of payment - should also be successful
        getPaymentStatus();

        Assert.assertTrue(success);
    }

    @Test
    public void testGetStatusFailure() throws Exception {

        // first make failed payment
        card.setPan("9900 0000 0000 5282");

        makePayment();

        Assert.assertFalse(success);

        // now check status of payment - should also return failed
        getPaymentStatus();

        Assert.assertFalse(success);
    }

    @Test
    public void testGetStatusDuringPayment() throws Exception {
        responseTimeout = 60;

        pm.setSessionTimeout(responseTimeout);

        success = false;
        responseReceived = false;

        operationId = pm.makePayment(request);

        // try getting the status at this point - should throw exception
        try {
            pm.getTransactionStatus(operationId);
            Assert.fail();
        } catch (TransactionInProgressException e) {
            // expected
        }

        // no wait for transaction to finish
        try {
            await().atMost(responseTimeout + 5, TimeUnit.SECONDS).until(responseReceived());
        } catch (Throwable e) {
            success = false;
        }

        Assert.assertTrue(success);
    }

    @Test
    public void testReliableDelivery3DSSuccess() throws Exception {

        responseTimeout = 10;

        pm.setSessionTimeout(responseTimeout);

        // "9900000000020604" will simulate a network failure on a 3DS resume which takes 5 seconds to process
        card.setPan("9900000000020604");

        makePayment();

        // not expecting callback as suspended for 3DS
        Assert.assertFalse(success);

        // now check get payment status
        try {
            pm.getTransactionStatus(operationId);
            Assert.fail();
        } catch (TransactionSuspendedFor3DSException e) {
            // expected
        }

        // broadcast 3DS event to kick manager to continue with resume
        Intent intent = new Intent(ThreeDSActivity.ACTION_COMPLETED);

        intent.putExtra(ThreeDSActivity.EXTRA_PARES, "VALID_PARES_FAIL_AFTER_RESUME_DELAY");
        intent.putExtra(ThreeDSActivity.EXTRA_SUCCESS, true);

        // now wait for successful payment response
        success = false;
        responseReceived = false;

        // this should now kick of the resume which will fail for 5s then succeed
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(Robolectric.application);
        lbm.sendBroadcast(intent);

        try {
            await().atMost(10, TimeUnit.SECONDS).until(responseReceived());
        } catch (Throwable e) {
            success = false;
        }

        Assert.assertTrue(true);
    }

    @Test
    public void testReliableDelivery3DSFail() throws Exception {

        responseTimeout = 10;

        pm.setSessionTimeout(responseTimeout);

        // "9900000000020703" will simulate a network failure on a 3DS resume which takes 5 seconds to process and then declines
        card.setPan("9900000000020703");

        makePayment();

        // no expecting callback as suspended for 3DS
        Assert.assertFalse(success);

        // now check get payment status
        try {
            pm.getTransactionStatus(operationId);
            Assert.fail();
        } catch (TransactionSuspendedFor3DSException e) {
            // expected
        }

        // broadcast 3DS event to kick manager to continue with resume
        Intent intent = new Intent(ThreeDSActivity.ACTION_COMPLETED);

        intent.putExtra(ThreeDSActivity.EXTRA_PARES, "VALID_PARES_FAIL_AFTER_RESUME_DELAY_DECLINE");
        intent.putExtra(ThreeDSActivity.EXTRA_SUCCESS, true);

        // now wait for successful payment response
        success = false;
        responseReceived = false;

        // this should now kick of the resume which will fail for 5s then decline
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(Robolectric.application);
        lbm.sendBroadcast(intent);

        try {
            await().atMost(10, TimeUnit.SECONDS).until(responseReceived());
        } catch (Throwable e) {
            success = false;
        }

        Assert.assertFalse(success);

        checkReasonCode(PaymentError.ReasonCode.TRANSACTION_DECLINED);
    }

    @Test
    public void testReliableDelivery3DSResponseFail() throws Exception {

        responseTimeout = 10;

        pm.setSessionTimeout(responseTimeout);

        // "9900000000020505" will simulate a network failure on the request to a 3DS resume (payment will not proceed);
        card.setPan("9900000000020505");

        makePayment();

        // no expecting callback as suspended for 3DS
        Assert.assertFalse(success);

        // broadcast 3DS event to kick manager to continue with resume
        Intent intent = new Intent(ThreeDSActivity.ACTION_COMPLETED);

        intent.putExtra(ThreeDSActivity.EXTRA_PARES, "VALID_PARES_FAIL_BEFORE_RESUME");
        intent.putExtra(ThreeDSActivity.EXTRA_SUCCESS, true);

        // now wait for successful payment response
        success = false;
        responseReceived = false;

        // this should now kick of the resume which will fail for 5s then decline
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(Robolectric.application);
        lbm.sendBroadcast(intent);

        try {
            await().atMost(10, TimeUnit.SECONDS).until(responseReceived());
        } catch (Throwable e) {
            success = false;
        }

        Assert.assertFalse(success);

        checkReasonCode(PaymentError.ReasonCode.NETWORK_ERROR_DURING_PROCESSING);
    }

    @Test
    public void testReliableDelivery3DSResponseSuccess() throws Exception {

        responseTimeout = 10;

        pm.setSessionTimeout(responseTimeout);

        // "9900000000020406" will simulate a network failure on the response to a 3DS resume (payment will proceed)
        card.setPan("9900000000020406");

        makePayment();

        // no expecting callback as suspended for 3DS
        Assert.assertFalse(success);

        // broadcast 3DS event to kick manager to continue with resume
        Intent intent = new Intent(ThreeDSActivity.ACTION_COMPLETED);

        intent.putExtra(ThreeDSActivity.EXTRA_PARES, "VALID_PARES_FAIL_AFTER_RESUME");
        intent.putExtra(ThreeDSActivity.EXTRA_SUCCESS, true);

        // now wait for successful payment response
        success = false;
        responseReceived = false;

        // this should now kick of the resume which will fail for 5s then decline
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(Robolectric.application);
        lbm.sendBroadcast(intent);

        try {
            await().atMost(10, TimeUnit.SECONDS).until(responseReceived());
        } catch (Throwable e) {
            success = false;
        }

        Assert.assertTrue(success);
    }

    @Test
    public void testSessionTimeout() throws Exception {

        responseTimeout = 2;

        pm.setSessionTimeout(responseTimeout);

        // "9900000000000275" will simluate a network failure on a transaction which takes 5 seconds to process
        card.setPan("9900000000000275");

        makePayment();

        Assert.assertFalse(success);

        checkReasonCode(PaymentError.ReasonCode.TRANSACTION_TIMED_OUT);
    }

    private void makePayment() throws Exception {
        success = false;
        responseReceived = false;

        operationId = pm.makePayment(request);

        try {
            await().atMost(responseTimeout + 5, TimeUnit.SECONDS).until(responseReceived());
        } catch (Throwable e) {
            success = false;
        }
    }

    private void getPaymentStatus() throws Exception {
        success = false;
        responseReceived = false;

        pm.getTransactionStatus(operationId);

        try {
            await().atMost(responseTimeout + 5, TimeUnit.SECONDS).until(responseReceived());
        } catch (Throwable e) {
            success = false;
        }
    }

    @Override
    public void paymentSucceeded(PaymentSuccess response) {
        success = true;
        responseSuccess = response;
        responseReceived = true;
    }

    @Override
    public void paymentFailed(PaymentError response) {
        success = false;
        responseError = response;
        responseReceived = true;
    }

    private Callable<Boolean> responseReceived() {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                // this is required to allow timer\handlers to run
                Robolectric.getUiThreadScheduler().advanceBy(System.currentTimeMillis() - timestamp);

                timestamp = System.currentTimeMillis();
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

    private void checkReasonCode(PaymentError.ReasonCode expected) {
        Assert.assertNotNull(responseError);
        Assert.assertEquals(expected,
                responseError.getReasonCode());
    }

}
