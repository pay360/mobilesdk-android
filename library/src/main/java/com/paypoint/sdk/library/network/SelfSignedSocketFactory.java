/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.network;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Socket Factory for accepting self signed SSL certificates
 */
public class SelfSignedSocketFactory {

    public SSLSocketFactory build() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] myTrustManagerArray = new TrustManager[]{new TrustEveryoneManager()};

        SSLContext sc = SSLContext.getInstance("SSL");

        sc.init(null, myTrustManagerArray, new java.security.SecureRandom());

        return sc.getSocketFactory();
    }

    /**
     * Trust manager which accepts all certificates
     */
    class TrustEveryoneManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] arg0, String arg1){}
        public void checkServerTrusted(X509Certificate[] arg0, String arg1){}
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
