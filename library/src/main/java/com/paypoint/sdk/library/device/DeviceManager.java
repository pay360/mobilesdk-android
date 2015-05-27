/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import com.paypoint.sdk.library.BuildConfig;
import com.paypoint.sdk.library.R;
import com.paypoint.sdk.library.log.Logger;

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

    private static final String PROPERY_UNKOWN  = "unknown";

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
        return checkPropertySet(OS_ANDROID);
    }

    /**
     * Gets OS name
     * @return
     */
    public String getOsName() {
        return checkPropertySet(OS_ANDROID + " " + Build.VERSION.RELEASE);
    }

    /**
     * Gets device model name
     * @return
     */
    public String getModelFamily() {
        return checkPropertySet(FAMILY_ANDROID);
    }

    /**
     * Gets device model name
     * @return
     */
    public String getModelName() {
        return checkPropertySet(Build.MODEL);
    }

    /**
     * Gets device manufacturer
     * @return
     */
    public String getManufacturer() {
        return checkPropertySet(Build.MANUFACTURER);
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

        return checkPropertySet(screenRes);
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
        return checkPropertySet(context.getPackageName());
    }

    public String getMerchantAppVersion() {
        String version = null;
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(Logger.TAG, "Failed to get merchant app version", e);
        }

        return checkPropertySet(version);
    }

    private String checkPropertySet(String property) {

        // return "unknown" if poperty not set
        if (TextUtils.isEmpty(property)) {
            return PROPERY_UNKOWN;
        } else {
            return property;
        }
    }
}
