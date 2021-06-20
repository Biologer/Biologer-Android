package org.biologer.biologer.adapters;

import android.content.Context;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.sql.StageDao;

public class StageLocalization {

    public static String getStageLocaleFromID(Context context, Long stageID) {
        if (stageID != null) {
            String stage_name = App.get().getDaoSession().getStageDao().queryBuilder()
                    .where(StageDao.Properties.StageId.eq(stageID)).list().get(0).getName();
            return StageLocalization.getStageLocale(context, stage_name);
        } else {
            return "";
        }
    }

    public static String getStageLocale(Context context, String stageName) {
        if (stageName.equals("egg")) {
            return context.getString(R.string.stage_egg);
        }
        if (stageName.equals("larva")) {
            return context.getString(R.string.stage_larva);
        }
        if (stageName.equals("pupa")) {
            return context.getString(R.string.stage_pupa);
        }
        if (stageName.equals("adult")) {
            return context.getString(R.string.stage_adult);
        }
        if (stageName.equals("juvenile")) {
            return context.getString(R.string.stage_juvenile);
        }
        else {
            return null;
        }
    }
}
