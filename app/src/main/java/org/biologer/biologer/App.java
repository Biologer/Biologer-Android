package org.biologer.biologer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.multidex.MultiDexApplication;

import org.biologer.biologer.model.DaoMaster;
import org.biologer.biologer.model.DaoSession;
import org.greenrobot.greendao.database.Database;

/**
 * Created by brjovanovic on 12/24/2017.
 */

public class App extends MultiDexApplication {

    private static final String TAG = "Biologer.App";

    private static App app;
    private DaoSession daoSession;

    @Override
    public void onCreate() {
        super.onCreate();

        app = this;

        // Create Notification channel in order to send notification to android API 26+
        createNotificationChannel();

        // For initialisation of GreenDAO database
        GreenDaoInitialization helper = new GreenDaoInitialization(this, "notes-db");
        Database db = helper.getWritableDb();
        daoSession = new DaoMaster(db).newSession();
    }

    public void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channel_id = "biologer_taxa";
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channel_id, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static App get(){
        return app;
    }

    public DaoSession getDaoSession() {return daoSession;}
}
