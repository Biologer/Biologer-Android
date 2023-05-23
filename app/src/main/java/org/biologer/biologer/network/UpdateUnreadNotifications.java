package org.biologer.biologer.network;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.biologer.biologer.ObjectBox;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.gui.NotificationView;
import org.biologer.biologer.network.JSON.UnreadNotification;
import org.biologer.biologer.network.JSON.UnreadNotificationsResponse;
import org.biologer.biologer.sql.UnreadNotificationsDb;

import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateUnreadNotifications extends Service {

    private static final String TAG = "Biologer.NotificationU";
    public static String GROUP_NOTIFICATIONS = "biologer.UnreadNotifications";
    int SUMMARY_ID = 0;

    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Update notifications service started...");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        boolean should_download = intent.getBooleanExtra("download", true);
        //long notification_id = intent.getLongExtra("notification_id", 0);
        if (should_download) {
            Log.d(TAG, "Notifications will be downloaded and displayed.");
            updateNotifications();
        } else {
            Log.d(TAG, "Notification view will be displayed only.");
            //if (notification_id != 0) {
            //    displayUnreadNotification(notification_id);
            //} else {
                displayUnreadNotifications();
            //}
        }
        return flags;

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // This will download 15 notifications from the veb and call displayUnreadNotifications()
    // to displays all of them in notification area
    private void updateNotifications() {

        // Get new notifications from the API
        Call<UnreadNotificationsResponse> unreadNotificationsResponseCall = RetrofitClient.getService(SettingsManager.getDatabaseName()).getUnreadNotifications();
        unreadNotificationsResponseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<UnreadNotificationsResponse> call, @NonNull Response<UnreadNotificationsResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        int size = response.body().getMeta().getTotal();
                        Log.d(TAG, "Number of unread notifications: " + size);
                        if (size >= 15) {
                            size = 15;
                        }
                        // Check if there is any notification
                        if (size >= 1) {
                            // Check if the number of notifications in local SQL equals the number of notifications online.
                            // If not synchronise, other-ways assume that nothing changed since the last time.
                            int size_in_sql = (int) ObjectBox.get().boxFor(UnreadNotificationsDb.class).count();
                            Log.d(TAG, "There are " + size_in_sql + " notifications stored locally.");
                            if (size_in_sql != 15) {
                                if (size != size_in_sql) {
                                    Log.d(TAG, "Updating notifications.");
                                    // Clean the SQL database
                                    ObjectBox.get().boxFor(UnreadNotificationsDb.class).removeAll();
                                    UnreadNotificationsDb[] notificationForSQL = new UnreadNotificationsDb[size];
                                    for (int i = 0; i < size; i++) {
                                        UnreadNotification unreadNotification = response.body().getData().get(i);
                                        notificationForSQL[i] = new UnreadNotificationsDb(
                                                0, unreadNotification.getId(),
                                                unreadNotification.getType(),
                                                unreadNotification.getNotifiable_type(),
                                                unreadNotification.getData().getField_observation_id(),
                                                unreadNotification.getData().getCauser_name(),
                                                unreadNotification.getData().getCurator_name(),
                                                unreadNotification.getData().getTaxon_name(),
                                                unreadNotification.getUpdated_at());
                                    }
                                    ObjectBox.get().boxFor(UnreadNotificationsDb.class).put(notificationForSQL);
                                    Log.d(TAG, "Notifications saved");

                                } else {
                                    Log.d(TAG, "Number of notifications online equals the ones stored locally.");
                                }
                            } else {
                                Log.d(TAG, "15 notifications are already stored locally.");
                            }
                        } else {
                            Log.d(TAG, "No need to update notifications.");
                        }
                    }

                    displayUnreadNotifications();

                }
            }

            @Override
            public void onFailure(@NonNull Call<UnreadNotificationsResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Application could not get data from a server: " + t.getLocalizedMessage());
            }
        });

    }

    // This will display all the notifications found in local ObjectBox database
    @SuppressLint("MissingPermission")
    public void displayUnreadNotifications() {
        Log.d(TAG, "Displaying UnreadNotifications for the observations.");
        List<UnreadNotificationsDb> unreadNotificationsDbs = ObjectBox.get().boxFor(UnreadNotificationsDb.class).getAll();
        Collections.reverse(unreadNotificationsDbs); // Reverse elements in a list to display notification in correct order

        if (!unreadNotificationsDbs.isEmpty()) {

            // Display no more than 15 notifications!
            for (int i = 0; i < unreadNotificationsDbs.size(); i++) {

                long notification_id = unreadNotificationsDbs.get(i).getId();

                String author = getAuthor(unreadNotificationsDbs.get(i));
                String action = getAction(unreadNotificationsDbs.get(i).getType());

                Log.d(TAG, "Notification ID for Android system is " + notification_id);

                Bundle bundle = new Bundle();
                bundle.putInt("id", (int) notification_id);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "biologer_observations")
                        .setSmallIcon(R.mipmap.ic_notification)
                        .setContentTitle(getString(R.string.observation_changed))
                        .setContentText(author + " " + action + " " + unreadNotificationsDbs.get(i).getTaxonName() + ".")
                        .setContentIntent(getPendingIntent(bundle, (int) notification_id))
                        .setGroup(GROUP_NOTIFICATIONS)
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true);
                NotificationManagerCompat.from(this).notify((int) notification_id, builder.build());
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "biologer_observations")
                    .setSmallIcon(R.mipmap.ic_notification)
                    .setContentTitle(getString(R.string.observation_changed))
                    .setContentText(getString(R.string.there_are_at_least) + " " + unreadNotificationsDbs.size() + " " + getString(R.string.changes_to_your_field_observations))
                    .setStyle(new NotificationCompat.InboxStyle()
                            .setSummaryText(getString(R.string.your_field_observations_were_changed))
                            .setBigContentTitle(getString(R.string.biologer)))
                    .setGroup(GROUP_NOTIFICATIONS)
                    .setAutoCancel(true)
                    .setGroupSummary(true);
            NotificationManagerCompat.from(this).notify(SUMMARY_ID, builder.build());

        } else {
            NotificationManagerCompat.from(this).cancelAll();
        }
    }

    private String getAction(String type) {
        String action;
        switch (type) {
            case "App\\Notifications\\FieldObservationApproved":
                action = getString(R.string.approved_observation);
                break;
            case "App\\Notifications\\FieldObservationEdited":
                action = getString(R.string.changed_observation);
                break;
            case "App\\Notifications\\FieldObservationMarkedUnidentifiable":
                action = getString(R.string.marked_as_unidentifiable);
                break;
            default:
                action = getString(R.string.did_something_with_observation);
                break;
        }
        return action;
    }

    private String getAuthor(UnreadNotificationsDb unreadNotificationsDb) {
        String author;
        if (unreadNotificationsDb.getCuratorName() != null) {
            author = unreadNotificationsDb.getCuratorName();
        } else {
            author = unreadNotificationsDb.getCauserName();
        }
        return author;
    }

    private PendingIntent getPendingIntent(Bundle bundle, int id) {
        Intent notificationIntent = new Intent(this, NotificationView.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notificationIntent.putExtras(bundle);
        return PendingIntent.getActivity(this, id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
