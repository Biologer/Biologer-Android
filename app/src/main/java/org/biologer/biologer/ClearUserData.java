package org.biologer.biologer;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class ClearUserData {

    public static void deleteAll(Context context) {
        resetSettings();
        resetPreferences(context);
        deleteDaoTables();
    }

    private static void deleteDaoTables() {
        App.get().getDaoSession().getEntryDao().deleteAll();
        App.get().getDaoSession().getObservationTypesDataDao().deleteAll();
        App.get().getDaoSession().getStageDao().deleteAll();
        App.get().getDaoSession().getTaxonDataDao().deleteAll();
        App.get().getDaoSession().getUserDataDao().deleteAll();
    }

    private static void resetPreferences(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("data_license", "0");
        editor.putString("image_license", "0");
        editor.apply();
    }

    static void resetSettings() {
        SettingsManager.deleteToken();
        SettingsManager.setTaxaUpdatedAt("0");
        SettingsManager.setSkipTaxaDatabaseUpdate("0");
        SettingsManager.setTaxaLastPageFetched("1");
        SettingsManager.setObservationTypesUpdated("0");
    }
}
