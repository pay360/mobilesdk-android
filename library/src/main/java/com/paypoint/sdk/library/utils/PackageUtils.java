/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.List;

public class PackageUtils {

    /** Return true if intent can be resolved
     * @param context
     * @param intent
     * @return
     */
    public static boolean isIntentAvailable(Context context, Intent intent) {

        try {
            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> list =
                    packageManager.queryIntentActivities(intent,
                            PackageManager.MATCH_DEFAULT_ONLY);
            return list.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
