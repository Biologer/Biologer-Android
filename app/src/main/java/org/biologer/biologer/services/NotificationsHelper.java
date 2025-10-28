package org.biologer.biologer.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import org.biologer.biologer.App;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.json.FieldObservationData;
import org.biologer.biologer.network.json.FieldObservationDataPhotos;
import org.biologer.biologer.network.json.FieldObservationResponse;
import org.biologer.biologer.sql.UnreadNotificationsDb;
import org.biologer.biologer.sql.UnreadNotificationsDb_;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
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
        notificationRead.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Notification " + real_notification_id + " should be set to read now.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.d(TAG, "Setting notification as read failed: " + t.getMessage());
            }
        });
    }

    public static void setAllOnlineNotificationsAsRead(Context context) {
        Call<ResponseBody> notificationRead = RetrofitClient
                .getService(SettingsManager.getDatabaseName())
                .setAllNotificationAsRead(true);

        notificationRead.enqueue(new Callback<>() {
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
                Log.d(TAG, "Setting notification as read failed:" + t.getMessage());
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

    public static void deleteNotificationFromObjectBox(String notification_id) {
        Box<UnreadNotificationsDb> unreadNotificationsDbBox = App.get().getBoxStore()
                .boxFor(UnreadNotificationsDb.class);
        Query<UnreadNotificationsDb> query = unreadNotificationsDbBox
                .query(UnreadNotificationsDb_.realId.equal(notification_id))
                .build();
        query.remove();
        query.close();
        Log.d(TAG, "Notification " + notification_id + " removed from local database, " + App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).count() + " notifications remain.");
    }

    public static void deletePhotosFromNotification(Context context, long notificationId) {
        Box<UnreadNotificationsDb> unreadNotificationsDbBox = App.get().getBoxStore()
                .boxFor(UnreadNotificationsDb.class);
        Query<UnreadNotificationsDb> query = unreadNotificationsDbBox
                .query(UnreadNotificationsDb_.id.equal(notificationId))
                .build();
        UnreadNotificationsDb notification = query.findFirst();
        query.close();
        deletePhotos(context, notification);
    }

    public static void deletePhotosFromNotification(Context context, String realNotificationId) {
        Box<UnreadNotificationsDb> unreadNotificationsDbBox = App.get().getBoxStore()
                .boxFor(UnreadNotificationsDb.class);
        Query<UnreadNotificationsDb> query = unreadNotificationsDbBox
                .query(UnreadNotificationsDb_.realId.equal(realNotificationId))
                .build();
        UnreadNotificationsDb notification = query.findFirst();
        query.close();
        deletePhotos(context, notification);
    }

    private static void deletePhotos(Context context, UnreadNotificationsDb notification) {
        // Get the number of observations for the same field observation ID. It there are more
        // notifications for the same field observation, don’t delete the image.
        if (notification != null) {
            long observations;
            Box<UnreadNotificationsDb> unreadNotificationsDbBox = App.get().getBoxStore()
                    .boxFor(UnreadNotificationsDb.class);
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

    // You'll need the Context (to save the bitmap) and the Callback
    public static void fetchFieldObservationAndPhotos(
            Context context,
            UnreadNotificationsDb notification,
            NotificationFetchCallback callback) {

        // Check if data already exists, if so, skip fetching
        if (notification.getThumbnail() != null) {
            Log.i(TAG, "Thumbnail already exists, skipping fetch.");
            // We still need to call the callback so the caller can update the UI
            callback.onNotificationUpdated(notification);
            return;
        }

        // 1. Fetch the Field Observation Data
        Call<FieldObservationResponse> fieldObservationCall = RetrofitClient
                .getService(SettingsManager.getDatabaseName())
                .getFieldObservation(String.valueOf(notification.getFieldObservationId()));

        fieldObservationCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<FieldObservationResponse> call, @NonNull Response<FieldObservationResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null && response.body().getData().length > 0) {

                    FieldObservationData data = response.body().getData()[0];
                    List<FieldObservationDataPhotos> photos = data.getPhotos();

                    // --- STEP 2a: Update Notification Metadata (Date, Location, Taxon, Project) ---
                    updateNotificationMetadata(notification, data, context);

                    // --- STEP 2b: Process Photos (Image 1, 2, 3 URLs) ---

                    if (photos != null && !photos.isEmpty()) {
                        String url1 = photos.get(0).getUrl();
                        notification.setImage1(url1);
                        notification.setImage2(photos.size() > 1 ? photos.get(1).getUrl() : "No photo");
                        notification.setImage3(photos.size() > 2 ? photos.get(2).getUrl() : "No photo");

                        // Save these URLs to ObjectBox immediately
                        App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).put(notification);

                        // --- STEP 2c: Download Thumbnail (Only the first image) ---
                        downloadAndSavePhoto(context, notification, url1, callback);

                    } else {
                        // Case: No photos found
                        Log.i(TAG, "This observation does not have a photo.");
                        notification.setThumbnail("No photo");
                        notification.setImage1("No photo");
                        notification.setImage2("No photo");
                        notification.setImage3("No photo");
                        App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).put(notification);
                        callback.onNotificationUpdated(notification);
                    }

                } else if (response.code() == 429 || response.code() == 508) {
                    long delay = response.code() == 429 ? getRetryDelay(response) : 5000;

                    Log.d(TAG, "Server resource limitation reached or loop detected, retrying in " + delay + "ms.");

                    // Schedule a retry
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(() -> fetchFieldObservationAndPhotos(context, notification, callback), delay);

                    // Inform the caller (e.g., to show a temporary loading state)
                    callback.onRetryScheduled(notification, delay);

                } else {
                    Log.e(TAG, "Field observation fetch unsuccessful. Code: " + response.code() + ", Message: " + response.message());
                    // Inform the caller of the failure
                    callback.onFailure(notification, new IOException("API call failed: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<FieldObservationResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Network failure during field observation fetch: " + t.getLocalizedMessage(), t);
                callback.onFailure(notification, t);
            }
        });
    }

    private static void updateNotificationMetadata(UnreadNotificationsDb notification, FieldObservationData data, Context context) {
        String date = data.getDay() + "-" + data.getMonth() + "-" + data.getYear();
        String location = data.getLocation();

        if (location == null || location.isEmpty()) {
            DecimalFormat f = new DecimalFormat("##.0000");
            location = f.format(data.getLongitude()) + "° E; " + f.format(data.getLatitude()) + "° N";
        }

        // Update all fields that were previously saved in the large loop
        notification.setDate(date);
        notification.setLocation(location);
        notification.setProject(data.getProject());
        notification.setFinalTaxonName(data.getTaxonSuggestion());

        App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).put(notification);
    }

    private static void downloadAndSavePhoto(
            Context context,
            UnreadNotificationsDb notification,
            String url,
            NotificationFetchCallback callback) {

        Call<ResponseBody> photoResponse = RetrofitClient.getService(SettingsManager.getDatabaseName()).getPhoto(url);
        photoResponse.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) {
                            InputStream inputStream = responseBody.byteStream();
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                            // 1. Save Original Image 1
                            Uri uri = PhotoUtils.saveBitmap(context, bitmap);
                            notification.setImage1(uri != null ? uri.toString() : null);

                            // 2. Create and Save Thumbnail
                            Bitmap thumbnailBitmap = createSquareThumbnail(bitmap);
                            Uri th_uri = PhotoUtils.saveBitmap(context, thumbnailBitmap);
                            notification.setThumbnail(th_uri != null ? th_uri.toString() : null);

                            // 3. Save Final Notification State and notify caller
                            App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).put(notification);
                            callback.onNotificationUpdated(notification);

                        } else {
                            callback.onFailure(notification, new IOException("Server returned null image response."));
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error saving bitmap.", e);
                        callback.onFailure(notification, e);
                    }
                } else {
                    callback.onFailure(notification, new IOException("Photo download unsuccessful. Code: " + response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                callback.onFailure(notification, t);
            }
        });
    }

    // Helper method to extract the retry delay from the header
    private static long getRetryDelay(Response<?> response) {
        String retryAfter = response.headers().get("retry-after");
        if (retryAfter != null) {
            try {
                return Long.parseLong(retryAfter) * 1000;
            } catch (NumberFormatException e) {
                // Ignore and use a default
            }
        }
        return 60000; // Default to 60 seconds if header is missing or malformed
    }

    // Helper method to crop a square thumbnail
    private static Bitmap createSquareThumbnail(Bitmap original) {
        int w = original.getWidth();
        int h = original.getHeight();

        // Cropping logic from your original code: center a square of size (min(w,h) - 200)
        int size = Math.min(w, h) - 200;
        if (size <= 0) {
            // Fallback: if the image is too small, just return a scaled version
            return Bitmap.createScaledBitmap(original, 100, 100, true);
        }

        int x = (w - size) / 2;
        int y = (h - size) / 2;

        // The original code uses a fixed margin of 100 on one side and calculates the crop size
        // The logic is slightly complex, let's simplify based on the goal: a centered square thumbnail.

        // Simpler, correct centered square crop:
        int smallest = Math.min(w, h);
        int offset_x = (w - smallest) / 2;
        int offset_y = (h - smallest) / 2;

        // This creates a square from the center of the image, then you can scale it down if needed.
        Bitmap square = Bitmap.createBitmap(original, offset_x, offset_y, smallest, smallest);

        // Since you want a thumbnail, scale it down (e.g., to 100x100 for efficiency)
        return Bitmap.createScaledBitmap(square, 100, 100, true);
    }

}
