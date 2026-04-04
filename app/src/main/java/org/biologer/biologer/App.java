package org.biologer.biologer;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.google.firebase.FirebaseApp;

import org.biologer.biologer.firebase.BiologerFirebaseMessagingService;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.MyObjectBox;
import org.biologer.biologer.sql.PhotoDb;
import org.biologer.biologer.sql.TimedCountDb;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.android.Admin;

/**
 * Created by brjovanovic on 12/24/2017.
 */

public class App extends Application {

    private BoxStore boxStore;
    private static App app;

    @Override
    public void onCreate() {
        super.onCreate();

        app = this;

        initializeBoxStore();

        migrateOldObjectBoxes();

        // Initialize Firebase logging
        FirebaseApp.initializeApp(this);

        // Initialize settings trying to fix an error with missing database name
        SettingsManager.init(getApplicationContext());

        // Create Notification channel in order to send notification to android API 26+
        createNotificationChannelUnreadNotifications();
        createNotificationChannelAnnouncements();
        createNotificationChannelLocationTracker();

        // Initialize FCM token and subscribe only if user is logged in
        if (SettingsManager.getAccessToken() != null) {
            initializeFCM();
        }
    }

    private void initializeFCM() {
        Log.d("Biologer", "Syncing FCM service token and subscribing to the channels.");
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

    public void migrateOldObjectBoxes() {
        Box<EntryDb> entryBox = App.get().getBoxStore().boxFor(EntryDb.class);
        List<EntryDb> allEntries = entryBox.getAll();

        for (EntryDb entry : allEntries) {
            if (entry.photos.isEmpty()) {
                boolean migrated = false;

                if (entry.getSlika1() != null && !entry.getSlika1().isEmpty()) {
                    PhotoDb p = new PhotoDb();
                    p.setLocalPath(entry.getSlika1());
                    p.setServerPath(null);
                    p.setServerId(0);
                    entry.photos.add(p);
                    migrated = true;
                }

                if (entry.getSlika2() != null && !entry.getSlika2().isEmpty()) {
                    PhotoDb p = new PhotoDb();
                    p.setLocalPath(entry.getSlika2());
                    p.setServerPath(null);
                    p.setServerId(0);
                    entry.photos.add(p);
                    migrated = true;
                }

                if (entry.getSlika3() != null && !entry.getSlika3().isEmpty()) {
                    PhotoDb p = new PhotoDb();
                    p.setLocalPath(entry.getSlika3());
                    p.setServerPath(null);
                    p.setServerId(0);
                    entry.photos.add(p);
                    migrated = true;
                }

                if (migrated) {
                    entryBox.put(entry);
                    Log.d("Migration", "Photos for Entry ID: " + entry.getId() + " migrated.");
                }
            }
        }

        // --- Start of TimedCountDb Migration ---
        Box<TimedCountDb> timedCountBox = App.get().getBoxStore().boxFor(TimedCountDb.class);
        List<TimedCountDb> allTimedCounts = timedCountBox.getAll();

        for (TimedCountDb tc : allTimedCounts) {
            boolean migrated = false;

            // Migrate Day
            if (tc.getNewDay() == null && tc.getDay() != null && !tc.getDay().isEmpty()) {
                try {
                    tc.setNewDay(Integer.parseInt(tc.getDay()));
                    migrated = true;
                } catch (NumberFormatException e) {
                    Log.e("Migration", "Could not parse day: " + tc.getDay());
                }
            }

            // Migrate Month
            if (tc.getNewMonth() == null && tc.getMonth() != null && !tc.getMonth().isEmpty()) {
                try {
                    tc.setNewMonth(Integer.parseInt(tc.getMonth()));
                    migrated = true;
                } catch (NumberFormatException e) {
                    Log.e("Migration", "Could not parse month: " + tc.getMonth());
                }
            }

            // Migrate Year
            if (tc.getNewYear() == null && tc.getYear() != null && !tc.getYear().isEmpty()) {
                try {
                    tc.setNewYear(Integer.parseInt(tc.getYear()));
                    migrated = true;
                } catch (NumberFormatException e) {
                    Log.e("Migration", "Could not parse year: " + tc.getYear());
                }
            }

            // Migrate Taxon Group (String to Long)
            if (tc.getNewTaxonGroup() == null && tc.getTaxonGroup() != null && !tc.getTaxonGroup().isEmpty()) {
                try {
                    tc.setNewTaxonGroup(Long.parseLong(tc.getTaxonGroup()));
                    migrated = true;
                } catch (NumberFormatException e) {
                    Log.e("Migration", "Could not parse taxonGroup: " + tc.getTaxonGroup());
                }
            }

            if (migrated) {
                tc.setDay(null);
                tc.setMonth(null);
                tc.setYear(null);
                tc.setTaxonGroup(null);

                timedCountBox.put(tc);
                Log.d("Migration", "TimedCount ID: " + tc.getId() + " fields migrated to Integer/Long.");
            }
        }
    }

    public static App get(){
        return app;
    }

}
