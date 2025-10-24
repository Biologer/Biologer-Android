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

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.biologer.biologer.Localisation;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.gui.ActivityAnnouncement;
import org.biologer.biologer.gui.ActivityLanding;
import org.biologer.biologer.network.NotificationSyncWorker;
import org.biologer.biologer.network.RetrofitClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

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
        String type = data.get("type");
        //Log.d(TAG, "Raw FCM data: " + data);

        // Receive notifications
        if ("notification_created".equals(type)) {
            Log.d(TAG, "Received: new notification_created event → trigger incremental sync");
            showNotification(data);

            // Download new notification
            String timestamp = data.get("timestamp");
            Log.d(TAG, "Server returned timestamp: " + timestamp);
            if (timestamp == null || Long.parseLong(timestamp) == 0) { // If no data set to 0
                NotificationSyncWorker.enqueueNow(getApplicationContext(), 0);
            } else {
                long timestampMinus = Long.parseLong(timestamp);
                NotificationSyncWorker.enqueueNow(getApplicationContext(), Math.max(0L, timestampMinus - 1L));
            }
            return;
        }

        // Mark notification as read
        if ("notification_read".equals(type)) {
            Log.d(TAG, "Received: notification_read event → mark item as read locally");
            String notificationId = data.get("notification_id");
            if (notificationId != null) {
                // e.g. mark in your local DB so UI updates immediately
                //LocalNotificationStore.markAsRead(notificationId);
            }
            return;
        }

        // Receive announcements
        if ("announcement".equals(type)) {
            showAnnouncement(data);
        }
    }

    /**
     * Called whenever a new token is generated
     **/
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed FCM token: " + token);
        sendRegistrationToServer(token);
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

    private void showNotification(java.util.Map<String, String> data) {
        JSONObject translation = getJsonTranslation(data);
        if (translation == null) return;

        // Display notification
        String title = translation.optString("title", getString(R.string.notification));
        String body = translation.optString("message", getString(R.string.notification_text));

        Intent intent = new Intent(this, ActivityLanding.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (data.containsKey("field_observation_id")) {
            intent.putExtra("field_observation_id", data.get("field_observation_id"));
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
