/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.paypoint.sdk.library.log.Logger;
import com.paypoint.sdk.library.utils.PackageUtils;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

/**
 * Who:  Pete
 * When: 30/04/2015
 * What: Activity for handling 3D Secure interaction
 */
public class ThreeDSActivity extends ActionBarActivity {

    public static final String ACTION_COMPLETED                 = "com.paypoint.sdk.library.ACTION_COMPLETED";
    public static final String EXTRA_SUCCESS                    = "com.paypoint.sdk.library.EXTRA_SUCCESS";

    public static final String EXTRA_ACS_URL                    = "com.paypoint.sdk.library.EXTRA_ACS_URL";
    public static final String EXTRA_TERM_URL                   = "com.paypoint.sdk.library.EXTRA_TERM_URL";
    public static final String EXTRA_PAREQ                      = "com.paypoint.sdk.library.EXTRA_PAREQ";
    public static final String EXTRA_MD                         = "com.paypoint.sdk.library.EXTRA_MD";
    public static final String EXTRA_SESSION_TIMEOUT            = "com.paypoint.sdk.library.EXTRA_SESSION_TIMEOUT";
    public static final String EXTRA_PARES                      = "com.paypoint.sdk.library.EXTRA_PARES";
    public static final String EXTRA_TRANSACTION_ID             = "com.paypoint.sdk.library.EXTRA_TRANSACTION_ID";
    public static final String EXTRA_HAS_TIMED_OUT              = "com.paypoint.sdk.library.EXTRA_HAS_TIMED_OUT";
    public static final String EXTRA_CANCELLED                  = "com.paypoint.sdk.library.TRANSACTION_CANCELLED";
    public static final String EXTRA_ALLOW_SELF_SIGNED_CERTS    = "com.paypoint.sdk.library.EXTRA_ALLOW_SELF_SIGNED_CERTS";

    private static final String JAVASCRIPT_INTERFACE = "paypoint";

    private WebView webView;

    private String acsUrl;
    private String termUrl;
    private String pareq;
    private String md;
    private boolean allowSelfSignedCerts;
    private String transactionId;
    private Handler sessionTimerHandler;
    private SessionTimeoutTask sessionTimeoutTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_3ds);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.activity_3ds_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

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
//        webView.getSettings().setSupportMultipleWindows(true);

        webView.setWebViewClient(new CustomWebViewClient());
        webView.setWebChromeClient(new CustomWebChromeClient());

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

        sendBroadcast(intent);

        // clean up the timer if still running
        if (sessionTimerHandler != null) {
            sessionTimerHandler.removeCallbacks(sessionTimeoutTask);
        }

        // close the activity
        finish();
    }

    private class CustomWebChromeClient extends WebChromeClient {
//        @Override
//        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
//            // open target _blank links in WebView with external browser
//            WebView newWebView = new WebView(view.getContext());
//
//            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
//            transport.setWebView(newWebView);
//            resultMsg.sendToTarget();
//            return true;
//        }
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
                // TODO could hide the webview at this point + potentially show something underneath??
                webView.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            // TODO any way to stop this page showing but still being able to call loadUrl?
            // TODO doesn't work if call loadUrl from onPageStarted
            if (url.contains(termUrl)) {

                // TODO can we do this before the page renders?
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
     * JavaScript -> Android binding
     */
    public class WebAppInterface {

        @JavascriptInterface
        public void getData(String pares, String md) {
            on3DSSuccess(pares, md);
        }
    }
}
