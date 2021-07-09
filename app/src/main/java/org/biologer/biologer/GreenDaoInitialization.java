package org.biologer.biologer;

import android.content.Context;

import android.util.Log;

import org.biologer.biologer.sql.DaoMaster;
import org.greenrobot.greendao.database.Database;

import static org.biologer.biologer.sql.DaoMaster.dropAllTables;

public class GreenDaoInitialization extends DaoMaster.DevOpenHelper {

    GreenDaoInitialization(Context context, String name) {
        super(context, name);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        Log.i("Biologer.greenDAO", "Upgrading schema from version " + oldVersion + " to " + newVersion + " by dropping all tables");
        //User.resetSettings();
        // TODO Send data to server before updating the database
        SettingsManager.setSqlUpdated(true);
        User.resetTaxaSettings();
        dropAllTables(db, true);
        onCreate(db);
    }
}
