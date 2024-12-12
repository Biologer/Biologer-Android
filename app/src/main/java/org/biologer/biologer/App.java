package org.biologer.biologer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.multidex.MultiDexApplication;

import org.biologer.biologer.sql.MyObjectBox;

import io.objectbox.BoxStore;

/**
 * Created by brjovanovic on 12/24/2017.
 */

public class App extends MultiDexApplication {

    private BoxStore boxStore;
    private static App app;

    @Override
    public void onCreate() {
        super.onCreate();

        app = this;

        initializeBoxStore();

        // Create Notification channel in order to send notification to android API 26+
        createNotificationChannelEntries();
        createNotificationChannelUnreadNotifications();
        createNotificationChannelAnnouncements();

        // For initialisation of OpenBox database
        // ObjectBox.init(this);
    }

    public BoxStore getBoxStore() {
        /* From this method we can get always opened BoxStore */
        if (boxStore != null && boxStore.isClosed())
            initializeBoxStore();
        return boxStore;
    }

    private void initializeBoxStore() {
        boxStore = MyObjectBox.builder().androidContext(this).build();
    }

    public void deleteAllBoxes() {
        boxStore.close();
        boxStore.deleteAllFiles();
    }

    public void createNotificationChannelEntries() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channel_id = "biologer_entries";
            CharSequence name = getString(R.string.channel_entries_name);
            String description = getString(R.string.channel_entries_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channel_id, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void createNotificationChannelUnreadNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channel_id = "biologer_observations";
            CharSequence name = getString(R.string.channel_observation_name);
            String description = getString(R.string.channel_observation_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(channel_id, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void createNotificationChannelAnnouncements() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channel_id = "biologer_announcements";
            CharSequence name = getString(R.string.channel_announcements);
            String description = getString(R.string.channel_announcements_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channel_id, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static App get(){
        return app;
    }

}
