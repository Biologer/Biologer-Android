package org.biologer.biologer.network;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.gui.NotificationsActivity;
import org.biologer.biologer.network.json.UnreadNotification;
import org.biologer.biologer.network.json.UnreadNotificationsResponse;
import org.biologer.biologer.sql.UnreadNotificationsDb;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateUnreadNotifications extends Service {

    private static final String TAG = "Biologer.NotificationU";
    int NOTIFICATION_ID = 0;

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
            displayUnreadNotifications((int) App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).count());
        }
        return flags;

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // This will download notifications from the veb and call displayUnreadNotifications()
    // to notify user
    private void updateNotifications() {

        // Get new notifications from the API
        Call<UnreadNotificationsResponse> unreadNotificationsResponseCall = RetrofitClient.getService(SettingsManager.getDatabaseName()).getUnreadNotifications(1);
        unreadNotificationsResponseCall.enqueue(new Callback<UnreadNotificationsResponse>() {
            @Override
            public void onResponse(@NonNull Call<UnreadNotificationsResponse> call, @NonNull Response<UnreadNotificationsResponse> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        int size = response.body().getMeta().getTotal();
                        int pages = response.body().getMeta().getLastPage();
                        Log.d(TAG, "Number of unread notifications: " + size);
                        // Check if there is any notification
                        if (size >= 1) {
                            // Check if the number of notifications in local SQL equals the number of notifications online.
                            // If not synchronise, other-ways assume that nothing changed since the last time.
                            int size_in_sql = (int) App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).count();
                            Log.d(TAG, "There are " + size_in_sql + " notifications stored locally.");
                            if (size != size_in_sql) {
                                // Clean the SQL database
                                App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).removeAll();
                                for (int p = 0; p < pages; p++) {
                                    Log.d(TAG, "Updating notifications, page " + p + ".");
                                    if (p == 0) {
                                        saveNotificationsSQL(response.body());
                                    } else {
                                        getAndSaveNotificationsSQL(p + 1);
                                    }
                                }
                            } else {
                                Log.d(TAG, "Number of notifications online equals the ones stored locally.");
                            }
                            displayUnreadNotifications(size);
                        } else {
                            Log.d(TAG, "No unread notifications online, Hurray!");
                        }
                    }
                }
            }

            private void getAndSaveNotificationsSQL(int page) {
                Call<UnreadNotificationsResponse> unreadNotificationsResponseCall = RetrofitClient.getService(SettingsManager.getDatabaseName()).getUnreadNotifications(page);
                unreadNotificationsResponseCall.enqueue(new Callback<UnreadNotificationsResponse>() {

                    @Override
                    public void onResponse(@NonNull Call<UnreadNotificationsResponse> call, @NonNull Response<UnreadNotificationsResponse> response) {
                        if (response.isSuccessful()) {
                            if (response.body() != null) {
                                saveNotificationsSQL(response.body());
                            }
                        } else if (response.code() == 429) {
                            String retryAfter = response.headers().get("retry-after");
                            long sec = Long.parseLong(Objects.requireNonNull(retryAfter, "Header did not return number of seconds."));
                            Log.d(TAG, "Server resource limitation reached, retry after " + sec + " seconds.");
                            // Add handler to delay fetching
                            Handler handler = new Handler();
                            Runnable runnable = () -> getAndSaveNotificationsSQL(page);
                            handler.postDelayed(runnable, sec * 1000);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<UnreadNotificationsResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "Application could not get data from a server: " + t.getLocalizedMessage());
                    }
                });
            }

            private void saveNotificationsSQL(UnreadNotificationsResponse response) {
                int size = response.getData().size();
                UnreadNotificationsDb[] notificationForSQL = new UnreadNotificationsDb[size];
                for (int i = 0; i < size; i++) {
                    UnreadNotification unreadNotification = response.getData().get(i);
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
                App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).put(notificationForSQL);
                Log.d(TAG, "Notifications saved");
            }

            @Override
            public void onFailure(@NonNull Call<UnreadNotificationsResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Application could not get data from a server: " + t.getLocalizedMessage());
            }
        });

    }

    // This will display all the notifications found in local ObjectBox database
    @SuppressLint("MissingPermission")
    public void displayUnreadNotifications(int size) {
        Log.d(TAG, "Displaying Android notification for unread online notifications.");
        String text;
        if (size == 1) {
            text = size + " " + getString(R.string.new_notification_text);
        } else {
            text = size + " " + getString(R.string.new_notifications_text);
        }
        Intent notificationIntent = new Intent(this, NotificationsActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "biologer_observations")
                .setSmallIcon(R.mipmap.ic_notification)
                .setContentTitle(getString(R.string.observation_changed))
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true);
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
    }

}
