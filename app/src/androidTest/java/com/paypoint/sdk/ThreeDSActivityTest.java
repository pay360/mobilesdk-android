/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.test.ActivityInstrumentationTestCase2;
import android.test.InstrumentationTestCase;

import com.paypoint.sdk.library.ThreeDSActivity;

import junit.framework.Assert;

import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.*;
import static com.jayway.awaitility.Awaitility.await;

public class ThreeDSActivityTest extends ActivityInstrumentationTestCase2<ThreeDSActivity> {

    private ThreeDSActivity activity;
    private boolean responseReceived;
    private boolean success;

    public ThreeDSActivityTest() {
        super(ThreeDSActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());

        // this sets up a fast redirect where the ACS page returns the term URL after a few
        // seconds without any user input
        Intent intent = new Intent(getInstrumentation().getContext(), ThreeDSActivity.class);
        intent.putExtra(ThreeDSActivity.EXTRA_ACS_URL, "http://192.168.3.242:5000/acs/auth");
        intent.putExtra(ThreeDSActivity.EXTRA_TERM_URL, "http://192.168.3.242:5000/landingpage/");
        intent.putExtra(ThreeDSActivity.EXTRA_PAREQ, "VALID_PAREQ_FAST_REDIR");
        intent.putExtra(ThreeDSActivity.EXTRA_MD, "7897162297");
        intent.putExtra(ThreeDSActivity.EXTRA_TRANSACTION_ID, "7897162297");
        intent.putExtra(ThreeDSActivity.EXTRA_SESSION_TIMEOUT, 3600000);
        intent.putExtra(ThreeDSActivity.EXTRA_ALLOW_SELF_SIGNED_CERTS, true);
        setActivityIntent(intent);

        activity = getActivity();
    }

    @Test
    public void test3DSSuccess() {

        // register to receive event from 3DS activity
        activity.registerReceiver(new ThreeDSecureReceiver(),
                new IntentFilter(ThreeDSActivity.ACTION_COMPLETED));

        // wait for 10 seconds for thread to become unblocked
        try {
            await().atMost(10, TimeUnit.SECONDS).until(responseReceived());
        } catch (Throwable e) {
            // swallow
        }
        Assert.assertTrue(success);
    }

    @Test
    public void test3DSTimeout() {

        // register to receive event from 3DS activity
        activity.registerReceiver(new ThreeDSecureReceiver(),
                new IntentFilter(ThreeDSActivity.ACTION_COMPLETED));

        // wait for 1 second for thread to come unblocked
        try {
            await().atMost(1, TimeUnit.SECONDS).until(responseReceived());
        } catch (Throwable e) {
            // swallow
        }
        Assert.assertFalse(success);
    }

    private Callable<Boolean> responseReceived() {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return responseReceived; // The condition that must be fulfilled
            }
        };
    }

    private class ThreeDSecureReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            success = intent.getBooleanExtra(ThreeDSActivity.EXTRA_SUCCESS, false);
            responseReceived = true;
        }
    }
}
