package org.biologer.biologer.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;

public class InternetConnection {

    private static final String TAG = "Biologer.Internet";

    @SuppressWarnings("deprecation")
    public static boolean isConnected(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (networkCapabilities != null) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.d(TAG, "There is internet connection.");
                    return true;
                } else {
                    return false;
                }
            } else {
                Log.d(TAG, "There is no internet connection.");
                return false;
            }
        }

        // For older Android version
        else {
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null) {
                boolean connected = activeNetworkInfo.isConnected();
                if (connected) {
                    Log.d(TAG, "There is internet connection.");
                    return true;
                } else {
                    Log.d(TAG, "There is no internet connection.");
                    return false;
                }
            }
            else {
                Log.d(TAG, "There is no internet connection.");
                return false;
            }
        }
    }

    // Will return four strings: cellular, wifi, bluetooth or ethernet
    @SuppressWarnings("deprecation")
    public static String networkType(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (networkCapabilities != null) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.d(TAG, "You are connected to cellular network.");
                    return "cellular";
                }
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.d(TAG, "You are connected to wifi network.");
                    return "wifi";
                }
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                    Log.d(TAG, "You are connected to bluetooth network.");
                    return "bluetooth";
                }
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.d(TAG, "You are connected to ethernet network.");
                    return "ethernet";
                }
                else {
                    return null;
                }
            } else {
                Log.d(TAG, "There is no internet connection.");
                return null;
            }
        }

        // For older Android version
        else {
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null) {
                if (activeNetworkInfo.isConnected()) {
                    android.net.NetworkInfo networkInfo_cellular = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                    android.net.NetworkInfo networkInfo_wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    android.net.NetworkInfo networkInfo_bluetooth = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_BLUETOOTH);
                    android.net.NetworkInfo networkInfo_ethernet = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
                    if (networkInfo_cellular != null) {
                        if (networkInfo_cellular.isConnected()) {
                            Log.d(TAG, "You are connected to cellular network.");
                            return "cellular";
                        }
                    }
                    if (networkInfo_wifi != null) {
                        if (networkInfo_wifi.isConnected()) {
                            Log.d(TAG, "You are connected to wifi network.");
                            return "wifi";
                        }
                    }
                    if (networkInfo_bluetooth != null) {
                        if (networkInfo_bluetooth.isConnected()) {
                            Log.d(TAG, "You are connected to bluetooth network.");
                            return "bluetooth";
                        }
                    }
                    if (networkInfo_ethernet != null) {
                        if (networkInfo_ethernet.isConnected()) {
                            Log.d(TAG, "You are connected to ethernet network.");
                            return "ethernet";
                        }
                    }
                }
            }
            Log.d(TAG, "There is no internet connection.");
            return null;
        }
    }
}
