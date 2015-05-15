/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.device;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Manager for retrieving library configuration properties
 */
public class DeviceManager {

    private static final String SHARED_PREFERENCE_NAME = "ppSharedPreferences";
    private static final String PREFERENCE_INSTALLATION_ID = "PREFERENCE_INSTALLATION_ID";

    private Context context;

    public DeviceManager(Context context) {
        this.context = context;
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
}
