package org.biologer.biologer.network;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.gui.AnnouncementsActivity;
import org.biologer.biologer.network.JSON.AnnouncementsData;
import org.biologer.biologer.network.JSON.AnnouncementsResponse;
import org.biologer.biologer.sql.AnnouncementTranslationsDb;
import org.biologer.biologer.sql.AnnouncementsDb;
import org.biologer.biologer.sql.AnnouncementsDb_;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.Query;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateAnnouncements extends Service {

    private static final String TAG = "Biologer.UpdateAnnounce";
    static final public String TASK_COMPLETED = "org.biologer.biologer.UpdateAnnounceService.TASK_COMPLETED";
    LocalBroadcastManager broadcastManager;
    boolean notify;

    @Override
    public void onCreate() {
        super.onCreate();
        broadcastManager = LocalBroadcastManager.getInstance(this);
        Log.i(TAG, "UpdateAnnouncements service started.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            notify = intent.getBooleanExtra("show_notification", true);
        }
        Log.d(TAG, "Should display announcement upon download? " + notify);
        getAnnouncements();
        return super.onStartCommand(intent, flags, startId);
    }

    private void getAnnouncements() {
        String database_url = SettingsManager.getDatabaseName();

        if (database_url != null) {
            Call<AnnouncementsResponse> announcements = RetrofitClient.getService(database_url).getAnnouncements();
            announcements.enqueue(new Callback<>() {
                @SuppressLint("UnspecifiedImmutableFlag")
                @Override
                public void onResponse(@NonNull Call<AnnouncementsResponse> call, @NonNull Response<AnnouncementsResponse> response) {
                    if (response.isSuccessful()) {
                        AnnouncementsData[] announcementsData;
                        if (response.body() != null) {
                            announcementsData = response.body().getData();
                            int number_of_announcements = announcementsData.length;
                            AnnouncementsDb[] announcementsDbs = new AnnouncementsDb[number_of_announcements];
                            for (int i = 0; i < number_of_announcements; i++) {
                                announcementsDbs[i] = new AnnouncementsDb(
                                        announcementsData[i].getId(),
                                        announcementsData[i].getCreatorName(),
                                        announcementsData[i].isPrivate(),
                                        announcementsData[i].getCreatedAt(),
                                        announcementsData[i].getUpdatedAt(),
                                        announcementsData[i].isRead(),
                                        announcementsData[i].getTitle(),
                                        announcementsData[i].getMessage());

                                int number_of_translations = announcementsData[i].getTranslations().length;
                                List<AnnouncementTranslationsDb> announcementTranslations = new ArrayList<>();
                                for (int j = 0; j < number_of_translations; j++) {
                                    if (announcementsData[i].getTranslations()[j].getAnnouncementId() != null) {
                                        announcementTranslations.add(new AnnouncementTranslationsDb(
                                                announcementsData[i].getTranslations()[j].getId(),
                                                announcementsData[i].getId(),
                                                announcementsData[i].getTranslations()[j].getLocale(),
                                                announcementsData[i].getTranslations()[j].getTitle(),
                                                announcementsData[i].getTranslations()[j].getMessage()
                                        ));
                                    }
                                }
                                App.get().getBoxStore().boxFor(AnnouncementTranslationsDb.class).put(announcementTranslations);

                            }
                            App.get().getBoxStore().boxFor(AnnouncementsDb.class).put(announcementsDbs);
                            Log.d(TAG, "There are " + number_of_announcements + " announcements and " +
                                    App.get().getBoxStore().boxFor(AnnouncementTranslationsDb.class).count() +
                                    " announcement translations.");

                            if (notify) {
                                displayNotification();
                            }

                            sendResult("success");

                        }
                    }
                }

                @SuppressLint("UnspecifiedImmutableFlag")
                private void displayNotification() {
                    Box<AnnouncementsDb> announcementsDbBox = App.get().getBoxStore().boxFor(AnnouncementsDb.class);
                    Query<AnnouncementsDb> announcementsDbQuery = announcementsDbBox
                            .query(AnnouncementsDb_.isRead.equal(false)).build();
                    if (announcementsDbQuery.count() >= 1) {
                        AnnouncementsDb announcement = announcementsDbQuery.findFirst();
                        if (announcement != null) {
                            Intent intent = new Intent(getApplicationContext(), AnnouncementsActivity.class);
                            PendingIntent pendingIntent;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);
                            } else {
                                pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
                            }

                            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "biologer_announcements")
                                    .setSmallIcon(R.mipmap.ic_notification)
                                    .setContentTitle(announcement.getTitle())
                                    .setContentText(announcement.getMessage())
                                    .setContentIntent(pendingIntent)
                                    .setOnlyAlertOnce(true)
                                    .setAutoCancel(true);
                            Notification notification = mBuilder.build();
                            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            assert mNotificationManager != null;
                            mNotificationManager.notify(1, notification);
                        }

                    }
                    announcementsDbQuery.close();
                }

                @Override
                public void onFailure(@NonNull Call<AnnouncementsResponse> call, @NonNull Throwable t) {
                    Log.d(TAG, "Could not get announcements: " + t.getLocalizedMessage());
                }
            });
        }
    }

    public void sendResult(String message) {
        Intent intent = new Intent(TASK_COMPLETED);
        intent.putExtra(TASK_COMPLETED, message);
        broadcastManager.sendBroadcast(intent);
    }
}
