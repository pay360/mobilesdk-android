/*
 * Copyright (c) 2015. PayPoint
 */

package com.pay360.sdk.library.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import com.pay360.sdk.library.BuildConfig;
import com.pay360.sdk.library.R;
import com.pay360.sdk.library.log.Logger;

import java.util.UUID;

/**
 * Manager for retrieving library configuration properties
 */
public class DeviceManager {

    private static final String SHARED_PREFERENCE_NAME = "ppSharedPreferences";
    private static final String PREFERENCE_INSTALLATION_ID = "PREFERENCE_INSTALLATION_ID";

    private static final String OS_ANDROID      = "ANDROID";
    private static final String FAMILY_ANDROID  = "Android";
    private static final String TYPE_SMARTPHONE = "SMARTPHONE";
    private static final String TYPE_TABLET     = "TABLET";

    private static final String PROPERTY_UNKNOWN = "unknown";

    private static final int    MAX_LENGTH_SDK_INSTALLATION_ID  = 36;
    private static final int    MAX_LENGTH_SDK_VERSION          = 32;
    private static final int    MAX_LENGTH_MERCHANT_APP_NAME    = 256;
    private static final int    MAX_LENGTH_MERCHANT_APP_VERSION = 32;
    private static final int    MAX_LENGTH_OS_FAMILY            = 32;
    private static final int    MAX_LENGTH_OS_NAME              = 32;
    private static final int    MAX_LENGTH_MODEL_FAMILY         = 32;
    private static final int    MAX_LENGTH_MODEL_NAME           = 256;
    private static final int    MAX_LENGTH_MANUFACTURER         = 32;
    private static final int    MAX_LENGTH_TYPE                 = 32;
    private static final int    MAX_LENGTH_SCREEN_RES           = 32;


    private Context context;

    public DeviceManager(Context context) {
        this.context = context;
    }

    public String getSdkVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public String getSdkInstallId() {
        String sdkInstallId = null;

        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCE_NAME,
                Context.MODE_PRIVATE);

        sdkInstallId = preferences.getString(PREFERENCE_INSTALLATION_ID, null);

        // set installation id if not already set
        if (sdkInstallId == null) {
            sdkInstallId = UUID.randomUUID().toString();
            preferences.edit().putString(PREFERENCE_INSTALLATION_ID, sdkInstallId).commit();
        }

        return sdkInstallId;
    }

    /**
     * Gets device OS family
     * @return
     */
    public String getOsFamily() {
        return checkPropertySet(OS_ANDROID, MAX_LENGTH_OS_FAMILY);
    }

    /**
     * Gets OS name
     * @return
     */
    public String getOsName() {
        return checkPropertySet(OS_ANDROID + " " + Build.VERSION.RELEASE,
                MAX_LENGTH_OS_NAME);
    }

    /**
     * Gets device model name
     * @return
     */
    public String getModelFamily() {
        return checkPropertySet(FAMILY_ANDROID, MAX_LENGTH_MODEL_FAMILY);
    }

    /**
     * Gets device model name
     * @return
     */
    public String getModelName() {
        return checkPropertySet(Build.MODEL, MAX_LENGTH_MODEL_NAME);
    }

    /**
     * Gets device manufacturer
     * @return
     */
    public String getManufacturer() {
        return checkPropertySet(Build.MANUFACTURER, MAX_LENGTH_MANUFACTURER);
    }

    /**
     * Gets device type, phone or tablet
     * @return
     */
    public String getType() {
        return context.getResources().getBoolean(R.bool.isTablet) ? TYPE_TABLET : TYPE_SMARTPHONE;
    }

    /**
     * Get screen res in pixels width x height
     * @return
     */
    public String getScreenRes() {
        String screenRes = "";

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        if (metrics != null) {
            screenRes = String.format("%dx%d", metrics.widthPixels, metrics.heightPixels);
        }

        return checkPropertySet(screenRes, MAX_LENGTH_SCREEN_RES);
    }

    /**
     * Get the screen density in dpi
     * @return
     */
    public int getScreenDpi() {
        int dpi = 0;

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        if (metrics != null) {
            dpi = (int)(metrics.density * 160f);
        }

        return dpi;
    }

    public String getMerchantAppName() {
        return checkPropertySet(context.getPackageName(), MAX_LENGTH_MERCHANT_APP_NAME);
    }

    public String getMerchantAppVersion() {
        String version = null;
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(Logger.TAG, "Failed to get merchant app version", e);
        }

        return checkPropertySet(version, MAX_LENGTH_MERCHANT_APP_VERSION);
    }

    private String checkPropertySet(String property, int maxLength) {

        // return "unknown" if poperty not set
        if (TextUtils.isEmpty(property)) {
            return PROPERTY_UNKNOWN;
        } else {
            // check max length
            if (property.length() > maxLength) {
                property = property.substring(0, maxLength);
            }
            return property;
        }
    }
}
