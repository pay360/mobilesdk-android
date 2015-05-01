/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Who:  Pete
 * When: 30/04/2015
 * What:
 */
public class ThreeDSActivity extends ActionBarActivity {

    public static final String EXTRA_ACS_URL = "com.paypoint.sdk.library.EXTRA_ACS_URL";
    public static final String EXTRA_TERM_URL = "com.paypoint.sdk.library.EXTRA_TERM_URL";
    public static final String EXTRA_TIMEOUT = "com.paypoint.sdk.library.EXTRA_TIMEOUT";

    public static final String ACTIVITY_3DS_COMPLETE = "com.paypoint.sdk.library.ACTIVITY_3DS_COMPLETE";

    // TODO this needs to be finalised
    private static final String JAVASCRIPT_CONTEXT = "paypoint";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_3ds);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.activity_3ds_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        WebView webView = (WebView)findViewById(R.id.webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.addJavascriptInterface(new WebAppInterface(), JAVASCRIPT_CONTEXT);

        webView.setWebViewClient(new CustomWebViewClient());


        // TODO set theme to show activity as a dialog - not sure we really need this though
        //<activity android:theme="@android:style/Theme.Dialog" />

        webView.loadUrl(getIntent().getStringExtra(EXTRA_ACS_URL));
    }

    private static class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // allow links to be loaded in the webview
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            // TODO check for term url

            // TODO if term url inject some javascript which kicks off setParRes
//            http://stackoverflow.com/questions/7544671/how-to-call-javascript-from-android
//            WebView.loadUrl("javascript: var result = window.YourJSLibrary.callSomeFunction();
//                    window.JavaCallback.returnResult(result)");
        }
    }

    /**
     * JavaScript -> Android binding
     */
    public class WebAppInterface {

        // TODO this needs to be finalised
        @JavascriptInterface
        public void setParRes(String toast) {

            // end of 3DS - broadcast event for PaymentManager to pick up
            sendBroadcast(new Intent(ACTIVITY_3DS_COMPLETE));
        }
    }
}
