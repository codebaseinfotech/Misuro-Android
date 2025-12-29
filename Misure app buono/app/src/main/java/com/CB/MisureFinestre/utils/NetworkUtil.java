package com.CB.MisureFinestre.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

public class NetworkUtil {

    /**
     * Check if device has active internet connection
     */
    public static boolean isConnected(Context context) {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;

        // 🔹 Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            Network network = cm.getActiveNetwork();
            if (network == null) return false;

            NetworkCapabilities capabilities =
                    cm.getNetworkCapabilities(network);

            if (capabilities == null) return false;

            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
        }

        // 🔹 Android < 10
        else {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }
}

