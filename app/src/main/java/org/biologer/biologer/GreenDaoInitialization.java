package org.biologer.biologer;

import android.content.Context;

import androidx.preference.PreferenceManager;
import android.util.Log;

import org.biologer.biologer.model.DaoMaster;
import org.greenrobot.greendao.database.Database;

import static org.biologer.biologer.model.DaoMaster.dropAllTables;

public class GreenDaoInitialization extends DaoMaster.DevOpenHelper {

    public GreenDaoInitialization(Context context, String name) {
        super(context, name);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        Log.i("Biologer.greenDAO", "Upgrading schema from version " + oldVersion + " to " + newVersion + " by dropping all tables");
        SettingsManager.setTaxaDatabaseUpdated("0");
        SettingsManager.setTaxaLastPageFetched("1");
        SettingsManager.deleteToken();
        dropAllTables(db, true);
        onCreate(db);
    }
}
