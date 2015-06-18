/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.network;

/**
 * Manager for getting endpoints for different PayPoint environments
 */
public class EndpointManager {

    public enum Environment {

        /**
         * PayPoint Mite environment
         */
        MITE("https://api.mite.paypoint.net:2443"),

        /**
         * PayPoint Production environment
         */
        PRODUCTION("https://api.paypoint.net");

        String url;

        Environment(String url) {
            this.url = url;
        }
    }

    /**
     * Get the endpoint URL for given PayPoint environment
     * @param environment The PayPoint environment
     * @return Endpoint URL
     */
    public static String getEndpointUrl(Environment environment) {
        return environment.url;
    }

    /**
     * Checks if URL matches a PayPoint environment URL
     * @param url URL to test
     * @return true if PayPoint URL
     */
    public static boolean isPayPointUrl(String url) {
        for (Environment environment :Environment.values()) {
            // check if URL matches environment URL
            if (url.equals(environment.url)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if URL is a custom URL i.e. does not matches an PayPoint environment URL
     * @param url URL to test
     * @return true if custom URL
     */
    public static boolean isCustomUrl(String url) {
        return !isPayPointUrl(url);
    }
}
