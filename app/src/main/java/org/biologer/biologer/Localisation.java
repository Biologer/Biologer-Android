package org.biologer.biologer;

import android.os.Build;
import android.util.Log;

import java.util.Locale;

public class Localisation {

    private static final String TAG = "Biologer.Entry";

    private static Locale getCurrentLocale() {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = App.get().getResources().getConfiguration().getLocales().get(0);
            Log.d(TAG, "Current System locale is set to " + locale.getDisplayLanguage() + " (" + locale.getLanguage() + "-" + locale.getScript() + ").");
        } else {
            locale = App.get().getResources().getConfiguration().locale;
            Log.d(TAG, "Current System locale is set to " + locale.getLanguage());
        }
        return locale;
    }

    public static String getLocaleScript() {
        Locale locale = getCurrentLocale();
        // Workaround for Serbian Latin script
        if (locale.getLanguage().equals("sr") && locale.getScript().equals("Latn")) {
            return "sr-Latn";
        } else {
            return locale.getLanguage();
        }
    }
}
