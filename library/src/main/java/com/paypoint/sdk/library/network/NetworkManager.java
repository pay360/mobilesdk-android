package com.paypoint.sdk.library.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by HendryP on 09/04/2015.
 */
public class NetworkManager {

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
