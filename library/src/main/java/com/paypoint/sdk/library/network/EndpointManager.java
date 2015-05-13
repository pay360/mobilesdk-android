/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.network;

/**
 * Manager for getting endpoints for different PayPoint environments
 */
public class EndpointManager {

    public enum Environment {
        MITE("https://api.mite.paypoint.net:2443"),
        PRODUCTION("https://api.paypoint.net");

        String url;

        Environment(String url) {
            this.url = url;
        }
    }

    public static String getEndpointUrl(Environment environment) {
        return environment.url;
    }

    /**
     * Checks if URL matches a PayPoint environment URL
     * @param url
     * @return
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
     * @param url
     * @return
     */
    public static boolean isCustomUrl(String url) {
        return !isPayPointUrl(url);
    }
}
