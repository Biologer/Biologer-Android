package org.biologer.biologer.services;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import org.biologer.biologer.App;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.sql.UnreadNotificationsDb;
import org.biologer.biologer.sql.UnreadNotificationsDb_;

import java.util.List;
import java.util.Objects;

import io.objectbox.Box;
import io.objectbox.query.Query;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsHelper {
    private static final String TAG = "Biologer.NotyHelper";

    public static void deleteAllNotificationsLocally(Context context) {
        deleteAllNotificationsPhotos(context);
        App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).removeAll();
    }

    public static void deleteAllNotificationsPhotos(Context context) {
        // First delete photos from internal storage
        List<UnreadNotificationsDb> notifications = App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).getAll();
        for (int j = 0; j < notifications.size(); j++) {
            if (notifications.get(j).getThumbnail() != null && Objects.equals(FileManipulation.uriType(notifications.get(j).getThumbnail()), "file")) {
                UnreadNotificationsDb notification = notifications.get(j);
                FileManipulation.deleteInternalFileFromUri(context, Uri.parse(notification.getThumbnail()));
                FileManipulation.deleteInternalFileFromUri(context, Uri.parse(notification.getImage1()));
                if (notifications.get(j).getImage2() != null && Objects.equals(FileManipulation.uriType(notifications.get(j).getImage2()), "file")) {
                    FileManipulation.deleteInternalFileFromUri(context, Uri.parse(notification.getImage2()));
                    if (notifications.get(j).getImage3() != null && Objects.equals(FileManipulation.uriType(notifications.get(j).getImage3()), "file")) {
                        FileManipulation.deleteInternalFileFromUri(context, Uri.parse(notification.getImage3()));
                    }
                }
            }
        }
    }

    public static void setOnlineNotificationAsRead(String real_notification_id) {
        String[] notification = new String[1];
        notification[0] = real_notification_id;

        Call<ResponseBody> notificationRead = RetrofitClient
                .getService(SettingsManager.getDatabaseName())
                .setNotificationAsRead(notification);
        notificationRead.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Notification " + real_notification_id + " should be set to read now.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.d(TAG, "Setting notification as read failed!");
                t.printStackTrace();
            }
        });
    }

    public static void setAllOnlineNotificationsAsRead(Context context) {
        Call<ResponseBody> notificationRead = RetrofitClient
                .getService(SettingsManager.getDatabaseName())
                .setAllNotificationAsRead(true);

        notificationRead.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "All notifications should be set to read now.");
                    NotificationsHelper.deleteAllNotificationsLocally(context);
                    NotificationManagerCompat.from(context).cancelAll();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.d(TAG, "Setting notification as read failed!");
                t.printStackTrace();
            }
        });
    }

    public static void deleteNotificationFromObjectBox(long notification_id) {
        Box<UnreadNotificationsDb> unreadNotificationsDbBox = App.get().getBoxStore()
                .boxFor(UnreadNotificationsDb.class);
        Query<UnreadNotificationsDb> query = unreadNotificationsDbBox
                .query(UnreadNotificationsDb_.id.equal(notification_id))
                .build();
        query.remove();
        query.close();
        Log.d(TAG, "Notification " + notification_id + " removed from local database, " + App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).count() + " notifications remain.");
    }

    public static void deleteNotificationPhotos(Context context, long notification_id) {
        Box<UnreadNotificationsDb> unreadNotificationsDbBox = App.get().getBoxStore()
                .boxFor(UnreadNotificationsDb.class);
        Query<UnreadNotificationsDb> query = unreadNotificationsDbBox
                .query(UnreadNotificationsDb_.id.equal(notification_id))
                .build();
        UnreadNotificationsDb notification = query.findFirst();
        query.close();

        // Get the number of observations for the same field observation ID. It there are more
        // notifications for the same field observation, don’t delete the image.
        if (notification != null) {
            long observations;
            try (Query<UnreadNotificationsDb> queryFieldObservations = unreadNotificationsDbBox
                    .query(UnreadNotificationsDb_.fieldObservationId.equal(notification.getFieldObservationId()))
                    .build()) {
                observations = queryFieldObservations.count();
            }
            if (observations == 1) {
                Log.d(TAG, "Photos to be deleted: " + notification.getThumbnail() + "; "
                        + notification.getImage1() + "; " + notification.getImage2()
                        + "; " + notification.getImage3());
                if (notification.getThumbnail() != null && Objects.equals(FileManipulation.uriType(notification.getThumbnail()), "file")) {
                    Log.i(TAG, "Deleting photo 1 and its thumbnail from internal storage.");
                    FileManipulation.deleteInternalFileFromUri(context, Uri.parse(notification.getThumbnail()));
                    FileManipulation.deleteInternalFileFromUri(context, Uri.parse(notification.getImage1()));
                    if (notification.getImage2() != null && Objects.equals(FileManipulation.uriType(notification.getImage2()), "file")) {
                        Log.i(TAG, "Deleting photo 2 from internal storage.");
                        FileManipulation.deleteInternalFileFromUri(context, Uri.parse(notification.getImage2()));
                        if (notification.getImage3() != null && Objects.equals(FileManipulation.uriType(notification.getImage3()), "file")) {
                            Log.i(TAG, "Deleting photo 3 from internal storage.");
                            FileManipulation.deleteInternalFileFromUri(context, Uri.parse(notification.getImage3()));
                        }
                    }
                }
            } else {
                Log.i(TAG, "There are " + observations + " field observations with the same image. Not deleting the images...");
            }
        }
    }

}
