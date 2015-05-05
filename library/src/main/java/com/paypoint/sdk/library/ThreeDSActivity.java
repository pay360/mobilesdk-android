/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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

    public static final String ACTION_COMPLETED         = "com.paypoint.sdk.library.ACTION_COMPLETED";
    public static final String EXTRA_SUCCESS            = "com.paypoint.sdk.library.EXTRA_SUCCESS";

    public static final String EXTRA_ACS_URL            = "com.paypoint.sdk.library.EXTRA_ACS_URL";
    public static final String EXTRA_TERM_URL           = "com.paypoint.sdk.library.EXTRA_TERM_URL";
    public static final String EXTRA_PAREQ              = "com.paypoint.sdk.library.EXTRA_PAREQ";
    public static final String EXTRA_MD                 = "com.paypoint.sdk.library.EXTRA_MD";
    public static final String EXTRA_TIMEOUT            = "com.paypoint.sdk.library.EXTRA_TIMEOUT";
    public static final String EXTRA_PARES              = "com.paypoint.sdk.library.EXTRA_PARES";
    public static final String EXTRA_TRANSACTION_ID     = "com.paypoint.sdk.library.EXTRA_TRANSACTION_ID";

    // TODO this needs to be finalised
    private static final String JAVASCRIPT_INTERFACE = "paypoint";

    private WebView webView;

    private String acsUrl;
    private String termUrl;
    private String pareq;
    private String md;
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

        webView = (WebView)findViewById(R.id.webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.addJavascriptInterface(new WebAppInterface(), JAVASCRIPT_INTERFACE);

        webView.setWebViewClient(new CustomWebViewClient());
        webView.setWebChromeClient(new CustomWebChromeClient());

        loadAcsPage(acsUrl,
                pareq,
                md,
                termUrl);

        long sessionTimeout = getIntent().getLongExtra(EXTRA_TIMEOUT, 0);

        if (sessionTimeout > 0) {

            // start a timer to wait for the term url
            sessionTimerHandler = new Handler();

            sessionTimeoutTask = new SessionTimeoutTask();

            // callback when session timeout expired. Session timeout is in ms
            sessionTimerHandler.postDelayed(sessionTimeoutTask, sessionTimeout / 1000);
        }
    }

    private class SessionTimeoutTask implements Runnable {
        @Override
        public void run() {
            // callback with success=false
            on3DSFinished(null, false);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        on3DSFinished(null, false);
    }

    /**
     * Load the ACS page
     * @param acsUrl
     * @param pareq
     * @param md
     * @param termUrl
     */
    private void loadAcsPage(String acsUrl, String pareq, String md, String termUrl) {

        List<NameValuePair> params = new LinkedList<>();
        params.add(new BasicNameValuePair("PaReq", pareq));
        params.add(new BasicNameValuePair("MD", md));
        params.add(new BasicNameValuePair("TermUrl", termUrl));
        ByteArrayOutputStream encodedParams = new ByteArrayOutputStream();

        try {
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(params, "UTF-8");
            form.writeTo(encodedParams);
            webView.postUrl(acsUrl, encodedParams.toByteArray());
        } catch (IOException e) {
            on3DSFinished(null, false);
        }
    }

    private void on3DSFinished(String pares, boolean success) {
        // end of 3DS - broadcast event for PaymentManager to pick up
        Intent intent = new Intent(ACTION_COMPLETED);
        intent.putExtra(EXTRA_PARES, pares);
        intent.putExtra(EXTRA_TRANSACTION_ID, transactionId);
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
        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            // open target _blank links in WebView with external browser
            WebView.HitTestResult result = view.getHitTestResult();
            String data = result.getExtra();

            if (!TextUtils.isEmpty(data)) {
                Context context = view.getContext();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
                context.startActivity(browserIntent);
            }
            return false;
        }
    }

    private class CustomWebViewClient extends WebViewClient {

        // TODO - do we need this anymore?
//        @Override
//        public boolean shouldOverrideUrlLoading(WebView view, String url) {
//            // allow links to be loaded in the webview
//            view.loadUrl(url);
//            return true;
//        }


//        @Override
//        public void onPageStarted(WebView view, String url, Bitmap favicon) {
//
//            if (url.contains(termUrl)) {
//
//                // TODO run some JS to call function to return pares and md
//                webView.loadUrl("javascript:get3DSData();");
//
//            } else {
//                super.onPageStarted(view, url, favicon);
//            }
//        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (url.contains(termUrl)) {

                super.onPageFinished(view, url);
                // TODO run some JS to call function to return pares and md
                webView.loadUrl("javascript:get3DSData();");

            } else {
                super.onPageFinished(view, url);
            }
        }

        // TODO think we can do everything in onPageStarted? May need to wait until onPageFinished
        // to invoke JS on the page though??
//        @Override
//        public void onPageFinished(WebView view, String url) {
//            super.onPageFinished(view, url);
//
//            // TODO check for term url
//
//            // TODO if term url inject some javascript which kicks off setParRes
////            http://stackoverflow.com/questions/7544671/how-to-call-javascript-from-android
////            WebView.loadUrl("javascript: var result = window.YourJSLibrary.callSomeFunction();
////                    window.JavaCallback.returnResult(result)");
//        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            view.stopLoading();
            on3DSFinished(null, false);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            view.stopLoading();
            on3DSFinished(null, false);
        }
    }

    /**
     * JavaScript -> Android binding
     */
    public class WebAppInterface {

        // TODO this needs to be finalised
        @JavascriptInterface
        public void getData(String pares, String md) {
            on3DSFinished(pares, true);

        }
    }
}
