package org.biologer.biologer.services;

import android.content.Context;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.sql.StageDb;
import org.biologer.biologer.sql.StageDb_;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.Query;

public class StageAndSexLocalization {

    public static String getStageLocaleFromID(Context context, Long stageID) {
        if (stageID != null) {
            Box<StageDb> stageBox = App.get().getBoxStore().boxFor(StageDb.class);
            Query<StageDb> query = stageBox
                    .query(StageDb_.id.equal(stageID))
                    .build();
            List<StageDb> results = query.find();
            String stage_name = results.get(0).getName();
            query.close();

            return StageAndSexLocalization.getStageLocale(context, stage_name);
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
            return "";
        }

        return switch (sexName) {
            case "male" -> context.getString(R.string.male_text);
            case "female" -> context.getString(R.string.female_text);
            default -> "";
        };
    }

}
