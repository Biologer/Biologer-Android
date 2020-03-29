package org.biologer.biologer;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by brjovanovic on 12/24/2017.
 */

public class SettingsManager {

    private static final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.get());

    public enum KEY {
        token, FIRST_LAUNCH, DATABASE_NAME, GOOGLE_MAP_TYPE,
        TAXA_LAST_PAGE_FETCHED, TAXA_UPDATED_AT, SKIP_TAXA_UPDATE_FOR_THIS_TIMESTAMP,
        OBSERVATION_TYPES_UPDATED_AT
    }

    public static boolean isFirstLaunch()
    {
        return prefs.getBoolean(KEY.FIRST_LAUNCH.toString(), true);
    }

    public static void setFirstLaunch(boolean firstLaunch)
    {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY.FIRST_LAUNCH.toString(), firstLaunch);
        editor.apply();
    }

    public static void setDatabaseName(String databaseName) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SettingsManager.KEY.DATABASE_NAME.toString(), databaseName);
        editor.apply();
    }

    public static String getDatabaseName() {
        return prefs.getString(KEY.DATABASE_NAME.toString(),"https://biologer.org");
    }

    static void deleteToken(){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY.token.toString(), null);
        editor.apply();
    }

    public static void setToken(String token){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY.token.toString(), token);
        editor.apply();
    }

    public static String getToken(){
        return prefs.getString(KEY.token.toString(),null);
    }

    public static void setGoogleMapType(String google_map_type) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SettingsManager.KEY.GOOGLE_MAP_TYPE.toString(), google_map_type);
        editor.apply();
    }

    public static String getGoogleMapType() {
        return prefs.getString(KEY.GOOGLE_MAP_TYPE.toString(),"NORMAL");
    }

    static void setTaxaUpdatedAt(String taxaUpdatedAt) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY.TAXA_UPDATED_AT.toString(), taxaUpdatedAt);
        editor.apply();
    }

    public static String getTaxaUpdatedAt() {
        return prefs.getString(KEY.TAXA_UPDATED_AT.toString(),"0");
    }

    public static void setSkipTaxaDatabaseUpdate(String taxaDatabaseUpdate) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY.SKIP_TAXA_UPDATE_FOR_THIS_TIMESTAMP.toString(), taxaDatabaseUpdate);
        editor.apply();
    }

    public static String getSkipTaxaDatabaseUpdate() {
        return prefs.getString(KEY.SKIP_TAXA_UPDATE_FOR_THIS_TIMESTAMP.toString(),"0");
    }

    public static void setObservationTypesUpdated(String observation_types_updated) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY.OBSERVATION_TYPES_UPDATED_AT.toString(), observation_types_updated);
        editor.apply();
    }

    public static String getObservationTypesUpdated() {
        return prefs.getString(KEY.OBSERVATION_TYPES_UPDATED_AT.toString(),"0");
    }

    static void setTaxaLastPageFetched(String last_page) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY.TAXA_LAST_PAGE_FETCHED.toString(), last_page);
        editor.apply();
    }

    static String getTaxaLastPageFetched() {
        return prefs.getString(KEY.TAXA_LAST_PAGE_FETCHED.toString(),"1");
    }

}
