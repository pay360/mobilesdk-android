/*
 * Copyright (c) 2016 Capita plc
 */

package com.pay360.sdk.library.payment;

import com.google.gson.annotations.SerializedName;

public class DeviceInfo {

    @SerializedName("sdkInstallId")
    private String sdkInstallId;

    @SerializedName("osFamily")
    private String osFamily;

    @SerializedName("osName")
    private String osName;

    @SerializedName("modelFamily")
    private String modelFamily;

    @SerializedName("modelName")
    private String modelName;

    @SerializedName("manufacturer")
    private String manufacturer;

    @SerializedName("type")
    private String type;

    @SerializedName("screenRes")
    private String screenRes;

    @SerializedName("screenDpi")
    private int screenDpi;


    public DeviceInfo setSdkInstallId(String sdkInstallId) {
        this.sdkInstallId = sdkInstallId;
        return this;
    }

    public DeviceInfo setOsFamily(String osFamily) {
        this.osFamily = osFamily;
        return this;
    }

    public DeviceInfo setOsName(String osName) {
        this.osName = osName;
        return this;
    }

    public DeviceInfo setModelName(String modelName) {
        this.modelName = modelName;
        return this;
    }

    public DeviceInfo setModelFamily(String modelFamily) {
        this.modelFamily = modelFamily;
        return this;
    }

    public DeviceInfo setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
        return this;
    }

    public DeviceInfo setType(String type) {
        this.type = type;
        return this;
    }

    public DeviceInfo setScreenRes(String screenRes) {
        this.screenRes = screenRes;
        return this;
    }

    public DeviceInfo setScreenDpi(int screenDpi) {
        this.screenDpi = screenDpi;
        return this;
    }
}
