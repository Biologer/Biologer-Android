package org.biologer.biologer.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.biologer.biologer.App;
import org.biologer.biologer.helpers.Localisation;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.gui.ActivityAnnouncement;
import org.biologer.biologer.gui.ActivityNotification;
import org.biologer.biologer.workers.NotificationSyncWorker;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.helpers.NotificationsHelper;
import org.biologer.biologer.helpers.ObjectBoxHelper;
import org.biologer.biologer.sql.UnreadNotificationsDb;
import org.biologer.biologer.sql.UnreadNotificationsDb_;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import io.objectbox.Box;
import io.objectbox.query.Query;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BiologerFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "Biologer.FCM";

    /**
     * Called when a message arrives
     **/
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Map<String, String> data = remoteMessage.getData();
        Log.d(TAG, "Raw FCM data: " + data);

        if (data.containsKey("silent")) {
            Log.d(TAG, "Received silent background message: " + data);
            return;
        }

        if (data.containsKey("type")) {
            // Receive notifications
            if ("notification_created".equals(data.get("type"))) {
                Log.d(TAG, "Received: new notification_created event → trigger incremental sync");
                showNotification(data);
                downloadNewNotification(data);
                return;
            }

            // Mark notification as read
            if ("notification_read".equals(data.get("type"))) {
                Log.d(TAG, "Received: notification_read event → mark notification as online");
                String id = data.get("notification_id");
                if ("all".equals(id)) {
                    Log.d(TAG, "All notifications are now marked as read on the server side.");
                    NotificationsHelper.deleteAllNotificationsLocally(this);
                } else {
                    Log.d(TAG, "Notification " + id + " is now marked as read on the server side.");
                    NotificationsHelper.deletePhotosFromNotification(this, id);
                    NotificationsHelper.deleteNotificationFromObjectBox(id);
                }
                Intent intent = new Intent(NotificationSyncWorker.ACTION_NOTIFICATIONS_UPDATED);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }

            // Receive announcements
            if ("announcement".equals(data.get("type"))) {
                Log.d(TAG, "Received: announcement event → display notification");
                showAnnouncement(data);
                return;
            }

            return;
        }

        Log.d(TAG, "Received unhandled data message: " + data);
    }

    /**
     * Inspect if notification exist in ObjectBox and download it through Worker
     **/
    private void downloadNewNotification(Map<String, String> data) {
        String timestamp = data.get("timestamp");
        String notification_id = data.get("notification_id");
        Log.d(TAG, "Server returned timestamp: " + timestamp + " (Notification ID: " + notification_id + ").");

        if (notification_id != null) {
            Box<UnreadNotificationsDb> box = App.get().getBoxStore().boxFor(UnreadNotificationsDb.class);
            Query<UnreadNotificationsDb> query = box.query(
                            UnreadNotificationsDb_.realId.equal(notification_id))
                    .build();
            boolean notificationNotFound = query.find().isEmpty();
            query.close();

            if (notificationNotFound) {
                if (timestamp != null) {
                    NotificationSyncWorker.enqueueNow(getApplicationContext(), Long.parseLong(timestamp));
                } else {
                    Log.d(TAG, "Notification timestamp empty, redownloading all.");
                    NotificationSyncWorker.enqueueNow(getApplicationContext(), 0L);
                }
            } else {
                Log.d(TAG, "Notification already in ObjectBox or no timestamp");
            }
        }
    }

    /**
     * Called whenever a new token is generated
     **/
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed FCM token: " + token);
        sendRegistrationToServer(token);
        subscribeToTopics();
    }

    public static void getFirebaseToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.d(TAG, "Obtained FCM token: " + token);
                        sendRegistrationToServer(token);
                    } else {
                        Log.w(TAG, "Fetching FCM token failed", task.getException());
                    }
                });
    }

    /**
     * Make this STATIC helper method
     **/
    public static void sendRegistrationToServer(String token) {
        String databaseName = SettingsManager.getDatabaseName();
        if (databaseName == null || token == null) {
            Log.w(TAG, "Cannot send FCM token to server: missing database name or token");
            return;
        }

        String lastToken = SettingsManager.getLastFcmToken();
        if (token.equals(lastToken)) {
            Log.d(TAG, "Token unchanged; skipping server update");
            return;
        }

        Log.d(TAG, "Sending new FCM token to server...");
        Call<ResponseBody> call = RetrofitClient.getService(databaseName).updateFcmToken(token);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Token sent successfully to server");
                    SettingsManager.setLastFcmToken(token);
                } else {
                    Log.e(TAG, "Token send failed: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, "Error sending token: " + t.getMessage());
            }
        });
    }

    public static void subscribeToTopics() {
        FirebaseMessaging.getInstance().subscribeToTopic("announcements")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Biologer.FCM", "Subscribed to topic announcements.");
                    } else {
                        Log.e("Biologer.FCM", "Failed to subscribe to topic announcements " + task.getException());
                    }
                });

        String userTopic = "user_" + ObjectBoxHelper.getUserId();
        FirebaseMessaging.getInstance().subscribeToTopic(userTopic)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Biologer.FCM", "Subscribed to user topic: " + userTopic);
                    } else {
                        Log.e("Biologer.FCM", "Failed to subscribe to user topic: " + userTopic, task.getException());
                    }
                });
    }

    private void showAnnouncement(Map<String, String> data) {
        Log.d(TAG, "Received: new announcement event → display notification");
        JSONObject translation = getJsonTranslation(data);
        if (translation == null) return;

        // Display notification
        String title = translation.optString("title", getString(R.string.notification));
        String body = translation.optString("message", getString(R.string.notification_text));

        // Setup Intent to launch ActivityAnnouncement with the remote ID
        Intent intent = new Intent(this, ActivityAnnouncement.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (data.containsKey("announcement_id")) {
            intent.putExtra("announcement_id", data.get("announcement_id"));
        }

        int requestCode = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE);

        String channelId = "biologer_announcements";
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(channelId,
                            getString(R.string.channel_announcements),
                            NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(getString(R.string.channel_announcements_description));
            manager.createNotificationChannel(channel);
        }

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void showNotification(Map<String, String> data) {
        JSONObject translation = getJsonTranslation(data);
        if (translation == null) return;

        // Display notification
        String title = translation.optString("title", getString(R.string.notification));
        String body = translation.optString("message", getString(R.string.notification_text));

        Intent intent = new Intent(this, ActivityNotification.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (data.containsKey("notification_id")) {
            intent.putExtra("real_notification_id", data.get("notification_id"));
            intent.putExtra("from_recycler_view", false);
            Log.d(TAG, "Putting real notification ID to the bundle: " + data.get("notification_id"));
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        String channelId = "biologer_observations";
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Biologer Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }

    private JSONObject getJsonTranslation(Map<String, String> data ) {
        try {
            // Load all translations
            String json = data.get("translations");
            if (json == null) {
                Log.w(TAG, "Missing translations field in FCM data");
                return null;
            }
            JSONObject translations = new JSONObject(json);

            // Get translation for current locale
            JSONObject translation = translations.optJSONObject(Localisation.getLocaleScript());
            if (translation == null) translation = translations.optJSONObject("en");
            return translation;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse translations JSON", e);
            return null;
        }
    }

}
