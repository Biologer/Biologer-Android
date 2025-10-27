package org.biologer.biologer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import androidx.multidex.MultiDexApplication;

import org.biologer.biologer.firebase.BiologerFirebaseMessagingService;
import org.biologer.biologer.sql.MyObjectBox;

import io.objectbox.BoxStore;
import io.objectbox.android.Admin;

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

        // Initialize settings trying to fix an error with missing database name
        SettingsManager.init(getApplicationContext());

        // Create Notification channel in order to send notification to android API 26+
        createNotificationChannelEntries();
        createNotificationChannelUnreadNotifications();
        createNotificationChannelAnnouncements();
        createNotificationChannelLocationTracker();

        // Initialize FCM token and subscribe
        initializeFCM();
    }

    private void initializeFCM() {
        BiologerFirebaseMessagingService.getFirebaseToken();
        BiologerFirebaseMessagingService.subscribeToTopics();
    }

    public BoxStore getBoxStore() {
        /* From this method we can get always opened BoxStore */
        if (boxStore != null && boxStore.isClosed())
            initializeBoxStore();
        return boxStore;
    }

    private void initializeBoxStore() {
        boxStore = MyObjectBox.builder().androidContext(this).build();

        // Debug versions should include ObjectBox Admin so that I can browse the database
        if (BuildConfig.DEBUG) {
            boolean started = new Admin(boxStore).start(this);
            Log.i("Biologer", "Started: " + started);
        }
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
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public void createNotificationChannelLocationTracker() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channel_id = "biologer_location";
            CharSequence name = getString(R.string.channel_location);
            String description = getString(R.string.channel_location_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channel_id, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
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
            int importance = NotificationManager.IMPORTANCE_HIGH;
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
