/*
 * Copyright (c) 2015. PayPoint
 */

package com.pay360.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.test.InstrumentationRegistry;
import android.support.v4.content.LocalBroadcastManager;
import android.test.ActivityInstrumentationTestCase2;

import com.pay360.sdk.library.*;

import junit.framework.Assert;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;

public class ThreeDSActivityTest extends ActivityInstrumentationTestCase2<ThreeDSActivity> {

    private LocalBroadcastManager lbm;
    private boolean responseReceived;
    private boolean success;
    private String pares;

    public ThreeDSActivityTest() {
        super(ThreeDSActivity.class);
    }

    public void before(String pareq, long sessionTimeout) throws Exception {

        injectInstrumentation(InstrumentationRegistry.getInstrumentation());

        // 10.0.2.2 is local loopback to host machine - http://developer.android.com/tools/devices/emulator.html#networkaddresses
        String host = "http://10.0.2.2:5000";

        // this sets up a fast redirect where the ACS page returns the term URL after a few
        // seconds without any user input
        Intent intent = new Intent(getInstrumentation().getContext(), ThreeDSActivity.class);
        intent.putExtra(ThreeDSActivity.EXTRA_ACS_URL, host + "/acs/auth");
        intent.putExtra(ThreeDSActivity.EXTRA_TERM_URL, host + "/landingpage/");
        intent.putExtra(ThreeDSActivity.EXTRA_PAREQ, pareq);
        intent.putExtra(ThreeDSActivity.EXTRA_MD, "7897162297"); intent.putExtra(ThreeDSActivity.EXTRA_TRANSACTION_ID, "7897162297");
        intent.putExtra(ThreeDSActivity.EXTRA_SESSION_TIMEOUT, sessionTimeout);
        intent.putExtra(ThreeDSActivity.EXTRA_ALLOW_SELF_SIGNED_CERTS, true);
        setActivityIntent(intent);

        lbm = LocalBroadcastManager.getInstance(getActivity());
    }

    /**
     * Test 3DS fast redirect
     */
    @Test
    public void test3DFastRedirect() throws Exception {

        before("VALID_PAREQ_FAST_REDIR", 3600000);

        // register to receive event from 3DS activity
        lbm.registerReceiver(new ThreeDSecureReceiver(),
                new IntentFilter(ThreeDSActivity.ACTION_COMPLETED));

        // wait for 15 seconds for thread to become unblocked
        try {
            await().atMost(15, TimeUnit.SECONDS).until(responseReceived());
        } catch (Throwable e) {
            // swallow
        }
        Assert.assertTrue(success);
        Assert.assertNotNull(pares);
    }

    /**
     * Test 3DS page showing on enter PIN, should timeout
     */
    @Test
    public void test3DSEnterPin() throws Exception {

        before("eJxVUttuwjAMfd9XVHxAk/RCAZmgQtHGAxuiPGyPURqNSvRC2q6wr59TWhhRIvnYybF9HFhcspP1o3SVFvl8xGw6WvAXOBy1UlGsZKMVh62qKvGtrDSZj+iwmONQ5lIWeOMRh124V2cOPRFHHtsBMkBk0PIo8pqDkOfl5p1PGfUoBdJDyJTeRNwLAsdx/QDIDUMuMsXdKLYGAmu1X0cf+/DTitdAujDIoslrfeUTZwxkANDoEz/WdTkjpG1bO+sJ0G3LIgNi4kAehe0aY1XId0kTvo3C9n4Om+s2Wv/icd+jrzkQcwMSUSuOGnjUZROLTme4/QmQzg8iM4Vw1mmFfd4glCZL+Bz77wNUXKtcXvnrcofdDAjUpSxyhTdQ1rsNiaokj0VWnpR10CKvhKxRcCzCBIA8mlq9Ge1ljaretMcB3gbpM3dqBtHFTJ4U5WM+ZV0iA4CY16SfMem/A1pP3+QPIfS8/w==", 3600000);

        // register to receive event from 3DS activity
        lbm.registerReceiver(new ThreeDSecureReceiver(),
                new IntentFilter(ThreeDSActivity.ACTION_COMPLETED));

        // wait for 15 seconds for thread to become unblocked
        try {
            await().atMost(15, TimeUnit.SECONDS).until(responseReceived());
        } catch (Throwable e) {
            // should timeout
            Assert.assertTrue(true);
        }
        Assert.assertFalse(responseReceived);
    }

    /**
     * Test 3DS showing on enter PIN but with short session timeout so expecting to come back within 10s window
     */
    @Test
    public void test3DSSessionTimeout() throws Exception {

        before("eJxVUttuwjAMfd9XVHxAk/RCAZmgQtHGAxuiPGyPURqNSvRC2q6wr59TWhhRIvnYybF9HFhcspP1o3SVFvl8xGw6WvAXOBy1UlGsZKMVh62qKvGtrDSZj+iwmONQ5lIWeOMRh124V2cOPRFHHtsBMkBk0PIo8pqDkOfl5p1PGfUoBdJDyJTeRNwLAsdx/QDIDUMuMsXdKLYGAmu1X0cf+/DTitdAujDIoslrfeUTZwxkANDoEz/WdTkjpG1bO+sJ0G3LIgNi4kAehe0aY1XId0kTvo3C9n4Om+s2Wv/icd+jrzkQcwMSUSuOGnjUZROLTme4/QmQzg8iM4Vw1mmFfd4glCZL+Bz77wNUXKtcXvnrcofdDAjUpSxyhTdQ1rsNiaokj0VWnpR10CKvhKxRcCzCBIA8mlq9Ge1ljaretMcB3gbpM3dqBtHFTJ4U5WM+ZV0iA4CY16SfMem/A1pP3+QPIfS8/w==", 5000);

        // register to receive event from 3DS activity
        lbm.registerReceiver(new ThreeDSecureReceiver(),
                new IntentFilter(ThreeDSActivity.ACTION_COMPLETED));

        // wait for 10 seconds for thread to become unblocked
        try {
            await().atMost(10, TimeUnit.SECONDS).until(responseReceived());
        } catch (Throwable e) {
            // swallow
        }
        Assert.assertTrue(responseReceived);
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
            pares = intent.getStringExtra(ThreeDSActivity.EXTRA_PARES);
            responseReceived = true;
        }
    }
}
