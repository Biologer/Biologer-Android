package org.biologer.biologer;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by brjovanovic on 12/24/2017.
 */

public class SettingsManager {

    private static final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.get());

    public enum KEY {
        ACCESS_TOKEN, REFRESH_TOKEN, TOKEN_EXPIRE_TIMESTAMP, MAIL_CONFIRMED, DATABASE_NAME, GOOGLE_MAP_TYPE,
        TAXA_LAST_PAGE_FETCHED, TAXA_UPDATED_AT, SKIP_TAXA_UPDATE_FOR_THIS_TIMESTAMP,
        OBSERVATION_TYPES_UPDATED_AT, SQL_UPDATED
    }

    public static void deleteAccessToken() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY.ACCESS_TOKEN.toString(), null);
        editor.apply();
    }

    public static void setAccessToken(String token) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY.ACCESS_TOKEN.toString(), token);
        editor.apply();
    }

    public static String getAccessToken() {
        return prefs.getString(KEY.ACCESS_TOKEN.toString(),null);
    }

    public static void deleteRefreshToken() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY.REFRESH_TOKEN.toString(), null);
        editor.apply();
    }

    public static void setRefreshToken(String token) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY.REFRESH_TOKEN.toString(), token);
        editor.apply();
    }

    public static void setTokenExpire(String timestamp) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY.TOKEN_EXPIRE_TIMESTAMP.toString(), timestamp);
        editor.apply();
    }

    public static String getTokenExpire() {
        return prefs.getString(KEY.TOKEN_EXPIRE_TIMESTAMP.toString(),null);
    }

    public static String getRefreshToken() {
        return prefs.getString(KEY.REFRESH_TOKEN.toString(),null);
    }

    public static boolean isMailConfirmed() {
        return prefs.getBoolean(KEY.MAIL_CONFIRMED.toString(), false);
    }

    public static void setMailConfirmed(boolean mailConfirmed) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY.MAIL_CONFIRMED.toString(), mailConfirmed);
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

    public static void setSqlUpdated(boolean sqlUpdated) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY.SQL_UPDATED.toString(), sqlUpdated);
        editor.apply();
    }

    public static Boolean isSqlUpdated() {
        return prefs.getBoolean(KEY.SQL_UPDATED.toString(),false);
    }

}
