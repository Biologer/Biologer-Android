package org.biologer.biologer.network;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import org.biologer.biologer.ObjectBox;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.JSON.UnreadNotification;
import org.biologer.biologer.network.JSON.UnreadNotificationsResponse;
import org.biologer.biologer.sql.UnreadNotificationsDb;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateUnreadNotifications extends Service {

    private static final String TAG = "Biologer.UpdateNotif";

    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Update notifications service started...");
        updateNotifications();
    }


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
                        // Check if the number of notifications in local SQL equals the number of notifications online.
                        // If not synchronise, other-ways assume that nothing changed since the last time.
                        if (size >= 1) {
                            int size_in_sql = (int) ObjectBox.get().boxFor(UnreadNotificationsDb.class).count();
                            if (size_in_sql != 15 || size != size_in_sql) {
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
                            }
                        } else {
                            Log.d(TAG, "No need to update notifications.");
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UnreadNotificationsResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Application could not get data from a server: " + t.getLocalizedMessage());
            }
        });

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
