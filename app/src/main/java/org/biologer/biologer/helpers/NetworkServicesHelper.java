package org.biologer.biologer.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.biologer.biologer.network.InternetConnection;

public class NetworkServicesHelper {

    private static final String TAG = "Biologer.NetHelper";

    public static boolean shouldDownload(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String how_to_use_network = prefs.getString("auto_download", "wifi");

        String network_type = InternetConnection.networkType(context);

        if (network_type == null) {
            return true;
        }

        if ("all".equals(how_to_use_network) || ("wifi".equals(how_to_use_network) && "wifi".equals(network_type))) {
            return true;
        } else {
            Log.d(TAG, "Should ask user whether to download new taxonomic database (if there is one).");
            return false;
        }
    }
}
