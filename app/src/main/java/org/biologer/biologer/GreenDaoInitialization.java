package org.biologer.biologer;

import android.content.Context;

import android.util.Log;

import org.biologer.biologer.model.greendao.DaoMaster;
import org.greenrobot.greendao.database.Database;

import static org.biologer.biologer.model.greendao.DaoMaster.dropAllTables;

public class GreenDaoInitialization extends DaoMaster.DevOpenHelper {

    GreenDaoInitialization(Context context, String name) {
        super(context, name);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        Log.i("Biologer.greenDAO", "Upgrading schema from version " + oldVersion + " to " + newVersion + " by dropping all tables");
        SettingsManager.setTaxaDatabaseUpdated("0");
        SettingsManager.setObservationTypesUpdated("0");
        SettingsManager.setTaxaLastPageFetched("1");
        SettingsManager.deleteToken();
        dropAllTables(db, true);
        onCreate(db);
    }
}
