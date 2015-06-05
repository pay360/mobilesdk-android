/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.paypoint.sdk.library.utils.PackageUtils;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Activity for handling 3D Secure interaction
 */
public class ThreeDSActivity extends ActionBarActivity {

    public static final String ACTION_COMPLETED                 = "com.paypoint.sdk.library.ACTION_COMPLETED";
    public static final String EXTRA_SUCCESS                    = "com.paypoint.sdk.library.EXTRA_SUCCESS";

    public static final String EXTRA_ACS_URL                    = "com.paypoint.sdk.library.EXTRA_ACS_URL";
    public static final String EXTRA_TERM_URL                   = "com.paypoint.sdk.library.EXTRA_TERM_URL";
    public static final String EXTRA_PAREQ                      = "com.paypoint.sdk.library.EXTRA_PAREQ";
    public static final String EXTRA_MD                         = "com.paypoint.sdk.library.EXTRA_MD";
    public static final String EXTRA_SESSION_TIMEOUT            = "com.paypoint.sdk.library.EXTRA_SESSION_TIMEOUT";
    public static final String EXTRA_REDIRECT_TIMEOUT           = "com.paypoint.sdk.library.EXTRA_REDIRECT_TIMEOUT";
    public static final String EXTRA_PARES                      = "com.paypoint.sdk.library.EXTRA_PARES";
    public static final String EXTRA_TRANSACTION_ID             = "com.paypoint.sdk.library.EXTRA_TRANSACTION_ID";
    public static final String EXTRA_HAS_TIMED_OUT              = "com.paypoint.sdk.library.EXTRA_HAS_TIMED_OUT";
    public static final String EXTRA_CANCELLED                  = "com.paypoint.sdk.library.TRANSACTION_CANCELLED";
    public static final String EXTRA_ALLOW_SELF_SIGNED_CERTS    = "com.paypoint.sdk.library.EXTRA_ALLOW_SELF_SIGNED_CERTS";

    private static final String JAVASCRIPT_INTERFACE = "paypoint";

    private static final long TIMEOUT_JAVASCRIPT = 5000;

    private WebView webView;

    private String acsUrl;
    private String termUrl;
    private String pareq;
    private String md;
    private boolean allowSelfSignedCerts;
    private String transactionId;
    private Handler sessionTimerHandler;
    private SessionTimeoutTask sessionTimeoutTask;
    private Handler redirectTimerHandler;
    private RedirectTimeoutTask redirectTimeoutTask;
    private Handler javascriptTimerHandler;
    private JavascriptTimeoutTask javascriptTimeoutTask;
    private boolean finished;
    private ViewGroup rootContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_3ds);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.pp_activity_3ds_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        rootContainer = (ViewGroup)findViewById(R.id.rootContainer);

        acsUrl = getIntent().getStringExtra(EXTRA_ACS_URL);
        termUrl = getIntent().getStringExtra(EXTRA_TERM_URL);
        pareq = getIntent().getStringExtra(EXTRA_PAREQ);
        md = getIntent().getStringExtra(EXTRA_MD);
        transactionId = getIntent().getStringExtra(EXTRA_TRANSACTION_ID);
        allowSelfSignedCerts = getIntent().getBooleanExtra(EXTRA_ALLOW_SELF_SIGNED_CERTS, false);

        webView = (WebView)findViewById(R.id.webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.addJavascriptInterface(new WebAppInterface(), JAVASCRIPT_INTERFACE);

        webView.setWebViewClient(new CustomWebViewClient());

        // load up the initial page
        loadAcsPage(acsUrl, pareq, md, termUrl);
    }

    private class SessionTimeoutTask implements Runnable {
        @Override
        public void run() {
            // callback with success=false
            on3DSTimedOut();
        }
    }

    private class RedirectTimeoutTask implements Runnable {
        @Override
        public void run() {
            // show root container
            rootContainer.setVisibility(View.VISIBLE);
        }
    }

    private class JavascriptTimeoutTask implements Runnable {
        @Override
        public void run() {
           on3DSFailure();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        on3DSCancelled();
    }

    /**
     * Load the ACS page
     * @param acsUrl
     * @param pareq
     * @param md
     * @param termUrl
     */
    private void loadAcsPage(String acsUrl, String pareq, String md, String termUrl) {

        // create POST request
        List<NameValuePair> params = new LinkedList<>();
        params.add(new BasicNameValuePair("PaReq", pareq));
        params.add(new BasicNameValuePair("MD", md));
        params.add(new BasicNameValuePair("TermUrl", termUrl));
        ByteArrayOutputStream encodedParams = new ByteArrayOutputStream();

        try {
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(params, "UTF-8");
            form.writeTo(encodedParams);
            webView.postUrl(acsUrl, encodedParams.toByteArray());

            // start session timer
            long sessionTimeout = getIntent().getLongExtra(EXTRA_SESSION_TIMEOUT, 0);

            if (sessionTimeout > 0) {

                // start a timer to wait for the term url
                sessionTimerHandler = new Handler();

                sessionTimeoutTask = new SessionTimeoutTask();

                // callback when session timeout expired. Session timeout is in ms
                sessionTimerHandler.postDelayed(sessionTimeoutTask, sessionTimeout);
            }

            // start redirect timer - only make this activity visible if showing acs page
            // after given number of seconds
            long directTimeout = getIntent().getLongExtra(EXTRA_REDIRECT_TIMEOUT, 0);

            if (directTimeout > 0) {
                // start a timer to wait for the term url
                redirectTimerHandler = new Handler();

                redirectTimeoutTask = new RedirectTimeoutTask();

                redirectTimerHandler.postDelayed(redirectTimeoutTask, directTimeout);
            } else {
                // no redirect, show webview
                rootContainer.setVisibility(View.VISIBLE);
            }
        } catch (IOException e) {
            on3DSFailure();
        }
    }

    private void on3DSSuccess(String pares, String md) {
        // ensure pares has been successfully captured
        if (TextUtils.isEmpty(pares)) {
            on3DSFailure();
        } else {
            on3DSFinished(pares, md, true, false, false);
        }
    }

    private void on3DSTimedOut() {
        on3DSFinished(null, null, false, true, false);
    }

    private void on3DSCancelled() {
        on3DSFinished(null, null, false, false, true);
    }

    private void on3DSFailure() {
        on3DSFinished(null, null, false, false, false);
    }

    private void on3DSFinished(String pares, String md, boolean success, boolean timeout, boolean cancelled) {

        // need a boolean flag here just in case page runs JS function on a timer after this
        // activity which fires after this activity has been finished. This flag avoids
        // multiple callbacks

        if (!finished) {
            // end of 3DS - broadcast event for PaymentManager to pick up
            Intent intent = new Intent(ACTION_COMPLETED);

            if (success) {
                intent.putExtra(EXTRA_PARES, pares);
                intent.putExtra(EXTRA_MD, md);
                intent.putExtra(EXTRA_TRANSACTION_ID, transactionId);
            }

            intent.putExtra(EXTRA_HAS_TIMED_OUT, timeout);
            intent.putExtra(EXTRA_CANCELLED, cancelled);
            intent.putExtra(EXTRA_SUCCESS, success);

            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);

            lbm.sendBroadcast(intent);

            // clean up the timers if still running
            if (sessionTimerHandler != null) {
                sessionTimerHandler.removeCallbacks(sessionTimeoutTask);
            }

            if (redirectTimerHandler != null) {
                redirectTimerHandler.removeCallbacks(redirectTimeoutTask);
            }

            if (javascriptTimerHandler != null) {
                javascriptTimerHandler.removeCallbacks(javascriptTimeoutTask);
            }

            // close the activity
            finish();
        }

        finished = true;
    }

    private class CustomWebViewClient extends WebViewClient {

        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

            // check intent can be resolved
            if (PackageUtils.isIntentAvailable(ThreeDSActivity.this, browserIntent)) {
                startActivity(browserIntent);
            }
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            if (url.contains(termUrl)) {
                rootContainer.setVisibility(View.INVISIBLE);

                // cancel the redirect timer
                if (redirectTimerHandler != null) {
                    redirectTimerHandler.removeCallbacks(redirectTimeoutTask);
                }
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (url.contains(termUrl)) {
                // start a timer to check Javascript evaluated
                javascriptTimerHandler = new Handler();

                javascriptTimeoutTask = new JavascriptTimeoutTask();

                javascriptTimerHandler.postDelayed(javascriptTimeoutTask, TIMEOUT_JAVASCRIPT);

                // call JS to get back pares - get3DSData calls back into WebAppInterface.getData()
                webView.loadUrl("javascript:get3DSData();");
            } else {
                super.onPageFinished(view, url);
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            view.stopLoading();
            on3DSFailure();
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            // check if accept self signed SSL certificates
            if (allowSelfSignedCerts) {
                handler.proceed();
            } else {
                // don't allow - close activity and return error to calling app
                view.stopLoading();
                on3DSFailure();
            }
        }
    }

    /**
     * JavaScript to Android binding
     */
    public class WebAppInterface {

        @JavascriptInterface
        public void getData(String pares, String md) {

            // stop timer
            if (javascriptTimerHandler != null) {
                javascriptTimerHandler.removeCallbacks(javascriptTimeoutTask);
            }

            on3DSSuccess(pares, md);
        }
    }
}
