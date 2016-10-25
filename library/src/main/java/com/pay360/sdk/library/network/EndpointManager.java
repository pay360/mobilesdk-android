/*
 * Copyright (c) 2016 Capita plc
 */

package com.pay360.sdk.library.network;

/**
 * Manager for getting endpoints for different Pay360 environments
 */
public class EndpointManager {

    public enum Environment {

        /**
         * Mite environment
         */
        MITE("https://api.mite.paypoint.net:2443"),

        /**
         * Production environment
         */
        PRODUCTION("https://api.paypoint.net");

        String url;

        Environment(String url) {
            this.url = url;
        }
    }

    /**
     * Get the endpoint URL for given Pay360 environment
     * @param environment The Pay360 environment
     * @return Endpoint URL
     */
    public static String getEndpointUrl(Environment environment) {
        return environment.url;
    }

    /**
     * Checks if URL matches a Pay360 environment URL
     * @param url URL to test
     * @return true if Pay360 URL
     */
    public static boolean isPay360Url(String url) {
        for (Environment environment :Environment.values()) {
            // check if URL matches environment URL
            if (url.equals(environment.url)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if URL is a custom URL i.e. does not match an Pay360 environment URL
     * @param url URL to test
     * @return true if custom URL
     */
    public static boolean isCustomUrl(String url) {
        return !isPay360Url(url);
    }
}
