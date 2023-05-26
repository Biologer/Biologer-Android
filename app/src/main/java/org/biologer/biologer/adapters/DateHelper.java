package org.biologer.biologer.adapters;

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

}
