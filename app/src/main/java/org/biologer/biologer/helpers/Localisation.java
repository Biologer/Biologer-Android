package org.biologer.biologer.helpers;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.StageDb_;

import java.util.List;
import java.util.Locale;

import io.objectbox.Box;
import io.objectbox.query.Query;

public class Localisation {

    private static final String TAG = "Biologer.Locale";

    private static Locale getCurrentLocale() {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = App.get().getResources().getConfiguration().getLocales().get(0);
            Log.d(TAG, "Current System locale is set to " + locale.getDisplayLanguage()
                    + " (" + locale.getLanguage() + "-" + locale.getScript()
                    + " - " + locale.getCountry() + ").");
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

    public static String getStageLocaleFromID(Context context, Long stageID) {
        if (stageID != null) {
            Box<StageDb> stageBox = App.get().getBoxStore().boxFor(StageDb.class);
            Query<StageDb> query = stageBox
                    .query(StageDb_.id.equal(stageID))
                    .build();
            List<StageDb> results = query.find();
            String stage_name = results.get(0).getName();
            query.close();

            return getStageLocale(context, stage_name);
        } else {
            return "";
        }
    }

    public static String getStageLocale(Context context, String stageName) {
        return switch (stageName) {
            case "egg" -> context.getString(R.string.stage_egg);
            case "larva" -> context.getString(R.string.stage_larva);
            case "pupa" -> context.getString(R.string.stage_pupa);
            case "adult" -> context.getString(R.string.stage_adult);
            case "juvenile" -> context.getString(R.string.stage_juvenile);
            default -> null;
        };
    }

    public static String getSexLocale(Context context, String sexName) {
        if (sexName == null) {
            return null;
        }

        return switch (sexName) {
            case "male" -> context.getString(R.string.male_text);
            case "female" -> context.getString(R.string.female_text);
            default -> null;
        };
    }
}
