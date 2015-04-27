/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.network;

/**
 * Manager for getting endpoints for different PayPoint environments
 * Who:  Pete
 * When: 20/04/2015
 * What:
 */
public class EndpointManager {

    public enum Environment {
        MITE(null),         // TODO populate URL when known
        PRODUCTION(null);   // TODO populate URL when known

        String url;

        Environment(String url) {
            this.url = url;
        }
    }

    public static String getEndpointUrl(Environment environment) {
        return environment.url;
    }

    /**
     * Checks if URL is a custom URL i.e. does not matches an PayPoint environment URL
     * @param url
     * @return
     */
    public static boolean isCustomUrl(String url) {
        for (Environment environment :Environment.values()) {
            // check if URL matches environment URL
            if (url.equals(environment.url)) {
                return false;
            }
        }

        return true;
    }
}
