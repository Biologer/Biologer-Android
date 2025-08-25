package org.biologer.biologer.services;

import android.content.Context;
import android.util.Log;

import org.biologer.biologer.R;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateHelper {

    private static final String TAG = "DateHelper";

    public static Date getDateFromJSON(String date_string) {
        if (date_string == null || date_string.trim().isEmpty()) {
            Log.e(TAG, "getDateFromJSON() received null or empty string.");
            return null;
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());
        try {
            return dateFormat.parse(date_string);
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse JSON date string: " + date_string, e);
            return null;
        }
    }

    public static Date getDate(String date_string) {
        if (date_string == null || date_string.trim().isEmpty()) {
            Log.e(TAG, "getDate() received null or empty string.");
            return null;
        }

        DateFormat dateFormat = new SimpleDateFormat("d-M-yyyy", Locale.getDefault());
        try {
            return dateFormat.parse(date_string);
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse date string: " + date_string, e);
            return null;
        }
    }

    public static String getLocalizedDate(Date date, Context context) {
        DateFormat dateFormatLocalized = android.text.format.DateFormat.getLongDateFormat(context);
        if (date != null) {
            return dateFormatLocalized.format(date);
        } else {
            return context.getString(R.string.unknown_date);
        }
    }

    public static String getLocalizedTime(Date date, Context context) {
        DateFormat timeFormatLocalized = android.text.format.DateFormat.getTimeFormat(context);
        if (date != null) {
            return timeFormatLocalized.format(date);
        } else {
            return context.getString(R.string.unknown_date);
        }
    }

    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date currentTime = new Date();
        return sdf.format(currentTime);
    }

    public static String getCurrentYear() {
        Calendar calendar = Calendar.getInstance();
        return String.valueOf(calendar.get(Calendar.YEAR));
    }

    public static String getCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        return String.valueOf(month);
    }

    public static String getCurrentDay() {
        Calendar calendar = Calendar.getInstance();
        return String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
    }
}
