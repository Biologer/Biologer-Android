package org.biologer.biologer.network;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.biologer.biologer.App;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.adapters.ArrayHelper;
import org.biologer.biologer.gui.LandingActivity;
import org.biologer.biologer.network.json.APIEntry;
import org.biologer.biologer.network.json.APIEntryPhotos;
import org.biologer.biologer.network.json.APIEntryResponse;
import org.biologer.biologer.network.json.UploadFileResponse;
import org.biologer.biologer.sql.EntryDb;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadRecords extends Service {

    private static final String TAG = "Biologer.UploadRecords";

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_CANCEL = "ACTION_CANCEL";
    static final public String TASK_COMPLETED = "org.biologer.biologer.UploadRecordsService.TASK_COMPLETED";

    boolean keep_going = true;

    ArrayList<EntryDb> entryList;
    int totalEntries = 0;
    int remainingEntries = 0;
    ArrayList<String> images_array = new ArrayList<>();
    List<APIEntryPhotos> photos = null;
    LocalBroadcastManager broadcaster;
    String[] filenames = new String[3]; // save image names to be deleted on success

    int n = 0;
    int m = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        broadcaster = LocalBroadcastManager.getInstance(this);
        Log.d(TAG, "Running onCreate()");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        super.onDestroy();
        sendResult("success", 0);
        Log.d(TAG, "Running onDestroy().");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_START:
                        // Do something...
                        Log.d(TAG, "Starting upload process…");
                        entryList = (ArrayList<EntryDb>) App.get().getBoxStore().boxFor(EntryDb.class).getAll();
                        totalEntries = entryList.size();
                        Log.d(TAG, "There are " + totalEntries + " entries to upload.");
                        notificationInitiate();
                        break;
                    case ACTION_CANCEL:
                        // Stop uploading!
                        cancelUpload(getString(R.string.notify_title_upload_canceled), getString(R.string.notify_desc_upload_canceled));
                        stopSelf();
                        break;
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void cancelUpload(String title, String description) {
        Log.d(TAG, "Canceling upload process…");
        keep_going = false;
        stopForegroundAndNotify(title, description);
    }

    // Stop the foreground service and update the notification
    private void stopForegroundAndNotify(String title, String description) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationUpdateText(title, description);
            stopForeground(STOP_FOREGROUND_DETACH);
        } else {
            stopForeground(true);
            notificationUpdateText(title, description);
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void notificationInitiate() {
        // Start the uploading and display notification
        Log.i(TAG, "Displaying notification for uploading service.");

        // Create initial notification to be set to Foreground
        Intent intent = new Intent(this, LandingActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "biologer_entries")
                .setSmallIcon(R.mipmap.ic_notification)
                .setContentTitle(getString(R.string.notify_title_entries))
                .setContentText(getString(R.string.notify_desc_entries))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false);

        Notification notification = mBuilder.build();

        // Delay starting the upload, trying to fix an issue on some phones.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "Starting foreground service after delay.");
            startForeground(1, notification);
            uploadStep1();
        }, 150);
    }

    // This checks if there are photos in the Entry Record.
    // If there are upload photos first.
    // If no photos/or after the photos are uploaded, upload the data.
    private void uploadStep1() {
        // n is a number of images from 1 to 3
        n = 0;
        ArrayList<String> listOfImages = new ArrayList<>();
        images_array.clear();

        // When all entries are uploaded
        if (entryList.isEmpty()) {
            Log.i(TAG, "All entries seems to be uploaded to the server!");
            App.get().getBoxStore().boxFor(EntryDb.class).removeAll();
            //App.get().getDaoSession().getEntryDao().deleteAll();
            // Stop the foreground service and update the notification
            stopForegroundAndNotify(getString(R.string.notify_title_entries_uploaded),
                    getString(R.string.notify_desc_entries_uploaded));
            stopSelf();
            return;
        }

        // Update upload status bar
        remainingEntries = totalEntries - entryList.size() + 1;
        String statusText =
                getString(R.string.notify_desc_uploading) + " " + remainingEntries + " " +
                        getString(R.string.notify_desc_uploading1) + " " + totalEntries + " " +
                        getString(R.string.notify_desc_uploading2);
        notificationUpdateProgress(totalEntries, remainingEntries, statusText);

        EntryDb entryDb = entryList.get(0);
        if (entryDb.getSlika1() != null) {
            n++;
            listOfImages.add(entryDb.getSlika1());
        }
        if (entryDb.getSlika2() != null) {
            n++;
            listOfImages.add(entryDb.getSlika2());
        }
        if (entryDb.getSlika3() != null) {
            n++;
            listOfImages.add(entryDb.getSlika3());
        }

       // If no photos upload the data
        if (n == 0) {
            uploadStep2();
        }

        // If there are photos send them first
        else {
            for (int i = 0; i < n; i++) {
                String image = listOfImages.get(i);
                int image_number = i+1;
                Log.d(TAG, "Uploading image " + image_number + ": " + image);
                filenames[i] = new File(image).getName();
                final File file = new File(getFilesDir(), filenames[i]);
                uploadPhoto(file);
            }
        }
    }

    // Checks weather to upload data on Birdloger or standard Biologer
    private void uploadStep2() {
        Log.d(TAG, "Upload data step 2 started...");
        APIEntry apiEntry = new APIEntry();
        photos = new ArrayList<>();
        // Create apiEntry object
        EntryDb entryDb = entryList.get(0);
        if (entryDb.getTaxonId() != 0) {
            apiEntry.setTaxonId((int) entryDb.getTaxonId());
        } else {
            apiEntry.setTaxonId(null);
        }
        apiEntry.setTaxonSuggestion(entryDb.getTaxonSuggestion());
        apiEntry.setYear(entryDb.getYear());
        apiEntry.setMonth(entryDb.getMonth());
        apiEntry.setDay(entryDb.getDay());
        apiEntry.setLatitude(entryDb.getLattitude());
        apiEntry.setLongitude(entryDb.getLongitude());
        if (entryDb.getAccuracy() == 0.0) {
            apiEntry.setAccuracy(null);
        } else {
            apiEntry.setAccuracy((int) entryDb.getAccuracy());
        }
        apiEntry.setLocation(entryDb.getLocation());
        apiEntry.setElevation((int) entryDb.getElevation());
        apiEntry.setNote(entryDb.getComment());
        apiEntry.setSex(entryDb.getSex());
        apiEntry.setNumber(entryDb.getNoSpecimens());
        apiEntry.setProject(entryDb.getProjectId());
        apiEntry.setLocation(entryDb.getLocation());
        apiEntry.setFoundOn(entryDb.getFoundOn());
        apiEntry.setStageId(entryDb.getStage());
        apiEntry.setAtlasCode(entryDb.getAtlasCode());
        apiEntry.setFoundDead(entryDb.getDeadOrAlive().equals("true") ? 0 : 1);
        apiEntry.setFoundDeadNote(entryDb.getCauseOfDeath());
        apiEntry.setDataLicense(entryDb.getDataLicence());
        apiEntry.setTime(entryDb.getTime());
        int[] observation_types = ArrayHelper.getArrayFromText(entryDb.getObservationTypeIds());
        // Handle situations when observation types are not downloaded from server
        if (observation_types == null) {
            Log.e(TAG, "Observation types are null!");
            int[] null_observation_types = new int[1];
            null_observation_types[0] = 1;
            apiEntry.setTypes(null_observation_types);
        } else {
            apiEntry.setTypes(observation_types);
        }
        for (int i = 0; i < n; i++) {
            APIEntryPhotos p = new APIEntryPhotos();
            p.setPath(images_array.get(i));
            p.setLicense(entryDb.getImageLicence());
            photos.add(p);
        }
        apiEntry.setPhotos(photos);
        apiEntry.setHabitat(entryDb.getHabitat());

        ObjectMapper mapper = new ObjectMapper();
        try {
            String s = mapper.writeValueAsString(apiEntry);
            Log.i(TAG, "Upload Entry " + s);
        } catch (JsonProcessingException e) {
            Log.e(TAG, "Error converting object to JSON: ", e);
        }

        Call<APIEntryResponse> call = RetrofitClient.getService(SettingsManager.getDatabaseName()).uploadEntry(apiEntry);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<APIEntryResponse> call, @NonNull Response<APIEntryResponse> response) {

                // Retry on server resource limit
                if (response.code() == 429) {
                    String retry_after = response.headers().get("Retry-After");
                    Log.e(TAG, "Server had too many requests from the app. Waiting " + retry_after + " seconds.");
                    if (retry_after != null) {
                        int wait = Integer.parseInt(retry_after) * 1000;

                        // Move the sleep and retry to a background thread
                        new Thread(() -> {
                            SystemClock.sleep(wait);
                            // Call the same method again on background thread after wait
                            if (keep_going) {
                                // run on main thread to keep Retrofit happy
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    uploadStep2();
                                });
                            }
                        }).start();
                        return;
                    }
                }

                if (response.isSuccessful()) {
                    // Wait... Don’t send too many requests to the server!
                    SystemClock.sleep(300);
                    // Delete uploaded entry
                    App.get().getBoxStore().boxFor(EntryDb.class).remove(entryDb);

                    // Inform the broadcaster on success
                    if (!entryList.isEmpty()) {
                        sendResult("id_uploaded", entryList.get(0).getId());
                        Log.d(TAG, "Entry ID " + entryList.get(0).getId() + " uploaded!");
                        entryList.remove(0);
                    }

                    // Delete image files from internal storage
                    for (String filename : filenames) {
                        if (filename != null) {
                            final File file = new File(getFilesDir(), filename);
                            boolean b = file.delete();
                            Log.d(TAG, "Deleting image " + filename + " returned: " + b);
                        }
                    }
                    filenames = new String[3];
                    // Reset the counter
                    m = 0;

                    // All done! Upload next entry :)
                    if (keep_going) {
                        uploadStep1();
                    } else {
                        Log.i(TAG, "Uploading has bean canceled by the user.");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<APIEntryResponse> call, @NonNull Throwable t) {
                Log.i(TAG, "Uploading of entry failed for some reason: " + Objects.requireNonNull(t.getLocalizedMessage()));
                cancelUpload(getResources().getString(R.string.failed), getResources().getString(R.string.upload_not_succesfull));
                sendResult("failed_entry", 0);
                stopSelf();
            }
        });
    }

    private void uploadPhoto(File image) {
        Log.i(TAG, "Opening image from the path: " + image.getAbsolutePath() + ".");

        RequestBody reqFile = RequestBody.create(image, MediaType.parse("image/*"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", image.getName(), reqFile);

        Call<UploadFileResponse> call = RetrofitClient.getService(SettingsManager.getDatabaseName()).uploadFile(body);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<UploadFileResponse> call, @NonNull Response<UploadFileResponse> response) {

                // Retry on server resources limit
                if (response.code() == 429) {
                    String retry_after = response.headers().get("Retry-After");
                    Log.e(TAG, "Server had too many requests from the app. Waiting " + retry_after + " seconds.");
                    if (retry_after != null) {
                        int wait = Integer.parseInt(retry_after) * 1000;

                        // Move the sleep and retry to a background thread
                        new Thread(() -> {
                            SystemClock.sleep(wait);
                            // Call the same method again on background thread after wait
                            if (keep_going) {
                                // run on main thread to keep Retrofit happy
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    uploadPhoto(image);
                                });
                            }
                        }).start();
                        return;
                    }
                }

                if (response.isSuccessful()) {
                    if (keep_going) {
                        UploadFileResponse responseFile = response.body();

                        if (responseFile != null) {
                            images_array.add(responseFile.getFile());
                            Log.d(TAG, "Uploaded file name: " + responseFile.getFile());
                            m++;
                            if (m == n) {
                                uploadStep2();
                            }
                        }
                    } else {
                        Log.i(TAG, "Uploading of images has bean canceled by the user.");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UploadFileResponse> call, @NonNull Throwable t) {
                if (t.getLocalizedMessage() != null) {
                    Log.e(TAG, "Upload of photo failed for some reason: " + t.getLocalizedMessage());
                    cancelUpload("Failed!", "Uploading of photo was not successful!");
                    sendResult("failed_photo", 0);
                    stopSelf();
                }
            }
        });
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void notificationUpdateProgress(int maxValue, int currentValue, String descriptionText) {
        // To do something if notification is taped, we must set up an intent
        Intent intent = new Intent(this, LandingActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }

        // Add Cancel button intent in notification.
        Intent cancelIntent = new Intent(this, UploadRecords.class);
        cancelIntent.setAction(ACTION_CANCEL);
        PendingIntent pendingCancelIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingCancelIntent = PendingIntent.getService(this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingCancelIntent = PendingIntent.getService(this, 0, cancelIntent, 0);
        }
        NotificationCompat.Action cancelAction = new NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.cancel), pendingCancelIntent);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "biologer_entries")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(getString(R.string.notify_title_entries))
                .setContentText(descriptionText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setProgress(maxValue, currentValue, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .addAction(cancelAction);

        Notification notification = mBuilder.build();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert mNotificationManager != null;
        mNotificationManager.notify(1, notification);
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void notificationUpdateText(String title, String description) {
        // To do something if notification is taped, we must set up an intent
        Intent intent = new Intent(this, LandingActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "biologer_taxa")
                .setSmallIcon(R.mipmap.ic_notification)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(false)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(false)
                .setAutoCancel(true);

        Notification notification = mBuilder.build();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert mNotificationManager != null;
        mNotificationManager.notify(1, notification);
    }

    public void sendResult(String message, long id) {
        Intent intent = new Intent(TASK_COMPLETED);
        intent.putExtra(TASK_COMPLETED, message);
        intent.putExtra("EntryID", id);
        broadcaster.sendBroadcast(intent);
    }
}
