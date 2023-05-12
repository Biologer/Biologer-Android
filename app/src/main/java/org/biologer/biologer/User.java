package org.biologer.biologer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.biologer.biologer.sql.Entry;
import org.biologer.biologer.sql.ObservationTypesData;
import org.biologer.biologer.sql.Stage;
import org.biologer.biologer.sql.TaxaTranslationData;
import org.biologer.biologer.sql.TaxonData;
import org.biologer.biologer.sql.TaxonGroupsData;
import org.biologer.biologer.sql.TaxonGroupsTranslationData;
import org.biologer.biologer.sql.UserData;

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
        resetSettings();
        resetPreferences(context);
        deleteAllTables();
    }

    private static void deleteAllTables() {
        Log.d(TAG, "Deleting all SQL tables.");
        ObjectBox.get().boxFor(Entry.class).removeAll();
        ObjectBox.get().boxFor(TaxonGroupsData.class).removeAll();
        ObjectBox.get().boxFor(TaxonGroupsTranslationData.class).removeAll();
        deleteUserTables();
        deleteTaxaTables();
    }

    public static void deleteTaxaTables() {
        Log.d(TAG, "Deleting taxa SQL tables.");
        ObjectBox.get().boxFor(TaxonData.class).removeAll();
        ObjectBox.get().boxFor(TaxaTranslationData.class).removeAll();
        ObjectBox.get().boxFor(ObservationTypesData.class).removeAll();
        ObjectBox.get().boxFor(Stage.class).removeAll();
    }

    public static void deleteUserTables() {
        Log.d(TAG, "Deleting user SQL table.");
        ObjectBox.get().boxFor(UserData.class).removeAll();
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
