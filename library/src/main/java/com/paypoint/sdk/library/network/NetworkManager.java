/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Manager for handling network related functions
 */
public class NetworkManager {

    /**
     * Check if device has connectivity
     * @param context
     * @return
     */
    public static boolean hasConnection(Context context) {
        boolean isConnected = false;

        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isConnected = activeNetwork != null &&
                    activeNetwork.isConnected();
        }

        return isConnected;
    }
}
