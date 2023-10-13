package org.biologer.biologer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.ObservationTypesDb;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.TaxaTranslationDb;
import org.biologer.biologer.sql.TaxonDb;
import org.biologer.biologer.sql.TaxonGroupsDb;
import org.biologer.biologer.sql.TaxonGroupsTranslationData;
import org.biologer.biologer.sql.UnreadNotificationsDb;
import org.biologer.biologer.sql.UserDb;

/**
 * Created by brjovanovic on 12/24/2017.
 */

public class User {
    private static final String TAG = "Biologer.User";

    private static User user;

    private User() {
    }

    public static User getUser(){
        if(user==null){
            user = new User();
        }
        return user;
    }

    public boolean tokenPresent() {
        return SettingsManager.getAccessToken() != null;
    }

    public static void clearUserData(Context context) {
        Log.d(TAG, "Deleting all user settings.");
        deleteAllTables();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.get());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    private static void deleteAllTables() {
        Log.d(TAG, "Deleting all SQL tables.");
        App.get().deleteAllBoxes();
    }

    private static void resetPreferences(Context context) {
        Log.d(TAG, "Resetting preferences.");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("data_license", "0");
        editor.putString("image_license", "0");
        editor.putBoolean("firstrun", true);
        editor.apply();
    }

    static void resetSettings() {
        Log.d(TAG, "Resetting all settings.");
        SettingsManager.deleteAccessToken();
        SettingsManager.deleteRefreshToken();
        SettingsManager.setMailConfirmed(false);
        resetTaxaSettings();
    }

    public static void resetTaxaSettings() {
        Log.d(TAG, "Resetting taxa settings.");
        SettingsManager.setTaxaUpdatedAt("0");
        SettingsManager.setSkipTaxaDatabaseUpdate("0");
        SettingsManager.setTaxaLastPageFetched("1");
        SettingsManager.setObservationTypesUpdated("0");
    }

}
