package com.paypoint.sdk.library.network;

/**
 * Manager for getting endpoints for different PayPoint environments
 * Who:  Pete
 * When: 20/04/2015
 * What:
 */
public class EndpointManager {

    public enum Environment {
        STAGING(null),
        MITE(null),
        PRODUCTION(null),
        LIVE(null);

        String url;

        Environment(String url) {
            this.url = url;
        }
    }

    public static String getEndpointUrl(Environment environment) {
        return environment.url;
    }
}
