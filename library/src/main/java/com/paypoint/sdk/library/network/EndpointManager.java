package com.paypoint.sdk.library.network;

/**
 * Manager for getting endpoints for different PayPoint environments
 * Who:  Pete
 * When: 20/04/2015
 * What:
 */
public class EndpointManager {

    public enum Environment {
        STAGING(null),      // TODO populate URL when known
        MITE(null),         // TODO populate URL when known
        PRODUCTION(null),   // TODO populate URL when known
        LIVE(null);         // TODO populate URL when known

        String url;

        Environment(String url) {
            this.url = url;
        }
    }

    public static String getEndpointUrl(Environment environment) {
        return environment.url;
    }
}
