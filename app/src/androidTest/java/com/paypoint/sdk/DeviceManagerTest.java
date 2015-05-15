/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk;

/*
 * Copyright (c) 2015. PayPoint
 */

import android.app.Application;
import android.test.ApplicationTestCase;

import com.paypoint.sdk.library.device.DeviceManager;

import junit.framework.Assert;


import com.paypoint.sdk.library.device.DeviceManager;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

public class DeviceManagerTest extends ApplicationTestCase<Application> {

    public DeviceManagerTest() {
        super(Application.class);
    }

    private DeviceManager deviceManager;

    @Before
    public void setUp() {
        deviceManager = new DeviceManager(getContext());
    }

    @Test
    public void testSdkVersion() {
        // test sdk installation id same if called multiple times
        String sdkVersion = deviceManager.getSdkVersion();
        Assert.assertNotNull(sdkVersion);
    }

    @Test
    public void testSdkInstallationId() {
        // test sdk installation id same if called multiple times
        String sdkInstallId1 = deviceManager.getSdkInstallId();
        String sdkInstallId2 = deviceManager.getSdkInstallId();
        Assert.assertEquals(sdkInstallId1, sdkInstallId2);
    }

    @Test
    public void testGetOsFamily() {
        String osFamily = deviceManager.getOsFamily();
        Assert.assertEquals("ANDROID", osFamily);
    }

    @Test
    public void testGetOsName() {
        String osName = deviceManager.getOsName();
        Assert.assertNotNull(osName);
    }

    @Test
    public void testGetModelFamily() {
        String modelFamily = deviceManager.getModelFamily();
        Assert.assertEquals("Android", modelFamily);
    }

    @Test
    public void testGetModelName() {
        String modelName = deviceManager.getModelName();
        Assert.assertNotNull(modelName);
    }

    @Test
    public void testGetManufacturer() {
        String manufacturer = deviceManager.getManufacturer();
        Assert.assertNotNull(manufacturer);
    }

    @Test
    public void testGetType() {
        String type = deviceManager.getType();
        Assert.assertTrue(type.equals("SMARTPHONE") || type.equals("TABLET"));
    }

    @Test
    public void testGetScreenRes() {
        String screenRes = deviceManager.getScreenRes();
        Assert.assertNotNull(screenRes);
    }

    @Test
    public void testGetScreenDpi() {
        int screenDpi = deviceManager.getScreenDpi();
        Assert.assertTrue(screenDpi > 0);
    }
}
