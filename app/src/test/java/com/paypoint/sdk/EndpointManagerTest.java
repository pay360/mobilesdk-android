/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk;

import com.paypoint.sdk.library.network.EndpointManager;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;

/**
 * Who:  Pete
 * When: 28/04/2015
 * What:
 */
public class EndpointManagerTest extends TestCase {

    @Test
    public void testMiteUrl()  {
        Assert.assertEquals("https://api.mite.paypoint.net:2443",
                EndpointManager.getEndpointUrl(EndpointManager.Environment.MITE));
    }

    @Test
    public void testProductionUrl()  {
        Assert.assertEquals("https://api.paypoint.net",
                EndpointManager.getEndpointUrl(EndpointManager.Environment.PRODUCTION));
    }
}
