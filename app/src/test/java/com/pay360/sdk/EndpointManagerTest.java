/*
 * Copyright (c) 2016 Capita plc
 */

package com.pay360.sdk;

import com.pay360.sdk.library.network.EndpointManager;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;

public class EndpointManagerTest extends TestCase {

    @Test
    public void testMiteUrl()  {
        Assert.assertEquals("mobileapi.mite.pay360.com",
                EndpointManager.getEndpointUrl(EndpointManager.Environment.MITE));
    }

    @Test
    public void testProductionUrl()  {
        Assert.assertEquals("mobileapi.pay360.com",
                EndpointManager.getEndpointUrl(EndpointManager.Environment.PRODUCTION));
    }
}
