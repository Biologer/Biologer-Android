package org.biologer.biologer;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

/**
 * Created by brjovanovic on 12/24/2017.
 */

public class SettingsManager {

    private static final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.get());

    public enum KEY {
        ACCESS_TOKEN, REFRESH_TOKEN, TOKEN_EXPIRE_TIMESTAMP, MAIL_CONFIRMED, ENTRY_OPEN, DATABASE_NAME, GOOGLE_MAP_TYPE,
        TAXA_LAST_PAGE_FETCHED, TAXA_UPDATED_AT, SKIP_TAXA_UPDATE_FOR_THIS_TIMESTAMP,
        OBSERVATION_TYPES_UPDATED_AT, SQL_UPDATED, FIRST_RUN, PREVIOUS_LOCATION_LONG, PREVIOUS_LOCATION_LAT
    }

    public static void deleteAccessToken() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY.ACCESS_TOKEN.toString(), null);
        editor.apply();
    }

    public static void setAccessToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY.ACCESS_TOKEN.toString(), token);
        editor.apply();
    }

    public static String getAccessToken() {
        return sharedPreferences.getString(KEY.ACCESS_TOKEN.toString(),null);
    }

    public static void deleteRefreshToken() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY.REFRESH_TOKEN.toString(), null);
        editor.apply();
    }

    public static void setRefreshToken(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY.REFRESH_TOKEN.toString(), token);
        editor.apply();
    }

    public static void setTokenExpire(String timestamp) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY.TOKEN_EXPIRE_TIMESTAMP.toString(), timestamp);
        editor.apply();
    }

    public static String getTokenExpire() {
        return sharedPreferences.getString(KEY.TOKEN_EXPIRE_TIMESTAMP.toString(),null);
    }

    public static String getRefreshToken() {
        return sharedPreferences.getString(KEY.REFRESH_TOKEN.toString(),null);
    }

    public static boolean isMailConfirmed() {
        return sharedPreferences.getBoolean(KEY.MAIL_CONFIRMED.toString(), false);
    }

    public static void setMailConfirmed(boolean mailConfirmed) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY.MAIL_CONFIRMED.toString(), mailConfirmed);
        editor.apply();
    }

    public static boolean getEntryOpen() {
        return sharedPreferences.getBoolean(KEY.ENTRY_OPEN.toString(), false);
    }

    public static void setEntryOpen(boolean entryOpen) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY.ENTRY_OPEN.toString(), entryOpen);
        editor.apply();
    }

    public static void setDatabaseName(String databaseName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SettingsManager.KEY.DATABASE_NAME.toString(), databaseName);
        editor.apply();
    }

    public static String getDatabaseName() {
        return sharedPreferences.getString(KEY.DATABASE_NAME.toString(),null);
    }

    public static void setGoogleMapType(String google_map_type) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SettingsManager.KEY.GOOGLE_MAP_TYPE.toString(), google_map_type);
        editor.apply();
    }

    public static String getGoogleMapType() {
        return sharedPreferences.getString(KEY.GOOGLE_MAP_TYPE.toString(),"NORMAL");
    }

    public static void setTaxaUpdatedAt(String taxaUpdatedAt) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY.TAXA_UPDATED_AT.toString(), taxaUpdatedAt);
        editor.apply();
    }

    public static String getTaxaUpdatedAt() {
        return sharedPreferences.getString(KEY.TAXA_UPDATED_AT.toString(),"0");
    }

    public static void setSkipTaxaDatabaseUpdate(String taxaDatabaseUpdate) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY.SKIP_TAXA_UPDATE_FOR_THIS_TIMESTAMP.toString(), taxaDatabaseUpdate);
        editor.apply();
    }

    public static String getSkipTaxaDatabaseUpdate() {
        return sharedPreferences.getString(KEY.SKIP_TAXA_UPDATE_FOR_THIS_TIMESTAMP.toString(),"0");
    }

    public static void setObservationTypesUpdated(String observation_types_updated) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY.OBSERVATION_TYPES_UPDATED_AT.toString(), observation_types_updated);
        editor.apply();
    }

    public static String getObservationTypesUpdated() {
        return sharedPreferences.getString(KEY.OBSERVATION_TYPES_UPDATED_AT.toString(),"0");
    }

    public static void setTaxaLastPageFetched(String last_page) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY.TAXA_LAST_PAGE_FETCHED.toString(), last_page);
        editor.apply();
    }

    public static String getTaxaLastPageFetched() {
        return sharedPreferences.getString(KEY.TAXA_LAST_PAGE_FETCHED.toString(),"1");
    }

    public static void setSqlUpdated(boolean sqlUpdated) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY.SQL_UPDATED.toString(), sqlUpdated);
        editor.apply();
    }

    public static Boolean isSqlUpdated() {
        return sharedPreferences.getBoolean(KEY.SQL_UPDATED.toString(),false);
    }
    public static void setFirstRun(boolean firstRun) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY.FIRST_RUN.toString(), firstRun);
        editor.apply();
    }

    public static Boolean isFirstRun() {
        return sharedPreferences.getBoolean(KEY.FIRST_RUN.toString(), true);
    }

    public static void setPreviousLocationLong(String previousLocationLong) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY.PREVIOUS_LOCATION_LONG.toString(), previousLocationLong);
        editor.apply();
    }

    public static String getPreviousLocationLong() {
        return sharedPreferences.getString(KEY.PREVIOUS_LOCATION_LONG.toString(),null);
    }

    public static void setPreviousLocationLat(String previousLocationLong) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY.PREVIOUS_LOCATION_LAT.toString(), previousLocationLong);
        editor.apply();
    }

    public static String getPreviousLocationLat() {
        return sharedPreferences.getString(KEY.PREVIOUS_LOCATION_LAT.toString(),null);
    }

}
