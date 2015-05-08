/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;
import android.test.InstrumentationTestCase;

import com.paypoint.sdk.library.ThreeDSActivity;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;


/**
 * Who:  Pete
 * When: 08/05/2015
 * What:
 */
public class ThreeDSActivityTest extends ActivityInstrumentationTestCase2<ThreeDSActivity> {

    private ThreeDSActivity activity;

    public ThreeDSActivityTest() {
        super(ThreeDSActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());

        Intent intent = new Intent(getActivity(), ThreeDSActivity.class);
        intent.putExtra(ThreeDSActivity.EXTRA_ACS_URL, "http:\\www.bbc.co.uk");
//        intent.putExtra(ThreeDSActivity.EXTRA_TERM_URL, threeDSecure.getTermUrl());
//        intent.putExtra(ThreeDSActivity.EXTRA_PAREQ, threeDSecure.getPareq());
//        intent.putExtra(ThreeDSActivity.EXTRA_MD, threeDSecure.getMd());
//        intent.putExtra(ThreeDSActivity.EXTRA_TRANSACTION_ID, paymentResponse.getTransactionId());
//        intent.putExtra(ThreeDSActivity.EXTRA_SESSION_TIMEOUT, threeDSecure.getSessionTimeout());
//        intent.putExtra(ThreeDSActivity.EXTRA_ALLOW_SELF_SIGNED_CERTS, isCustomUrl);
        setActivityIntent(intent);
        activity = getActivity();

    }

    @Test
    public void test3DS() {
        Assert.assertTrue(true);
    }
}
