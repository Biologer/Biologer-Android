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

import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.gui.ActivityAnnouncement;
import org.biologer.biologer.gui.ActivityLanding;
import org.biologer.biologer.network.RetrofitClient;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BiologerFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "Biologer.FCM";

    /** Called when a message arrives **/
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Map<String, String> data = remoteMessage.getData();
        String type = data.get("type");

        // Receive announcements
        if ("announcement".equals(type)) {
            showAnnouncement(remoteMessage);
            return;
        }

        // Receive notifications
        if (remoteMessage.getNotification() != null) {
            showNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    remoteMessage.getData()
            );
        } else if (!remoteMessage.getData().isEmpty()) {
            showNotification("Biologer", remoteMessage.getData().get("body"), remoteMessage.getData());
        }
    }

    /** Called whenever a new token is generated **/
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed FCM token: " + token);
        sendRegistrationToServer(token);
    }

    /** Make this STATIC helper method **/
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

    private void showAnnouncement(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();

        String title;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            title = remoteMessage.getNotification() != null
                    ? remoteMessage.getNotification().getTitle()
                    : data.getOrDefault("title", getString(R.string.channel_announcements));
        } else {
            if (remoteMessage.getNotification() != null && remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            } else {
                title = data.containsKey("title") ? data.get("title") : getString(R.string.channel_announcements);
            }
        }
        String body;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            body = remoteMessage.getNotification() != null
                    ? remoteMessage.getNotification().getBody()
                    : data.getOrDefault("body", getString(R.string.new_announcement_available));
        } else {
            if (remoteMessage.getNotification() != null && remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            } else {
                body = data.containsKey("body") ? data.get("body") : getString(R.string.new_announcement_available);
            }
        }

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

    private void showNotification(String title, String messageBody, java.util.Map<String, String> data) {
        Intent intent = new Intent(this, ActivityLanding.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (data != null && data.containsKey("field_observation_id")) {
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
                .setContentText(messageBody)
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
}
