package org.biologer.biologer.adapters;

import android.content.Context;

import org.biologer.biologer.R;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateHelper {

    public static Date getDateFromJSON(String date_string) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());
        Date date;
        try {
            date = dateFormat.parse(date_string);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return date;
    }

    public static Date getDate(String date_string) {
        DateFormat dateFormat = new SimpleDateFormat("d-M-yyyy", Locale.getDefault());
        Date date;
        try {
            date = dateFormat.parse(date_string);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return date;
    }

    public static String getLocalizedDate(Date date, Context context) {
        DateFormat dateFormatLocalized = android.text.format.DateFormat.getLongDateFormat(context);
        String date_string;
        if (date != null) {
            date_string = dateFormatLocalized.format(date);
        } else {
            date_string = context.getString(R.string.unknown_date);
        }
        return date_string;
    }

    public static String getLocalizedTime(Date date, Context context) {
        DateFormat timeFormatLocalized = android.text.format.DateFormat.getTimeFormat(context);
        String time_string;
        if (date != null) {
            time_string = timeFormatLocalized.format(date);
        } else {
            time_string = context.getString(R.string.unknown_date);
        }
        return time_string;
    }

}
