package org.biologer.biologer.network;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
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
import org.biologer.biologer.bus.DeleteEntryFromList;
import org.biologer.biologer.network.JSON.APIEntry;
import org.biologer.biologer.sql.Entry;
import org.biologer.biologer.network.JSON.UploadFileResponse;
import org.biologer.biologer.network.JSON.APIEntryResponse;
import org.greenrobot.eventbus.EventBus;

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

    ArrayList<Entry> entryList;
    int totalEntries = 0;
    int remainingEntries = 0;
    ArrayList<String> images_array = new ArrayList<>();
    List<APIEntry.Photo> photos = null;
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
        sendResult("Uploading task is completed, shutting down service.");
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
                        entryList = (ArrayList<Entry>) App.get().getDaoSession().getEntryDao().loadAll();
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

    private void notificationInitiate() {
        // Start the uploading and display notification
        Log.i(TAG, "Displaying notification for uploading service.");

        // Create initial notification to be set to Foreground
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

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
        startForeground(1, notification);

        uploadStep1();
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
        if (entryList.size() == 0) {
            Log.i(TAG, "All entries seems to be uploaded to the server!");
            App.get().getDaoSession().getEntryDao().deleteAll();
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

        Entry entry = entryList.get(0);
        if (entry.getSlika1() != null) {
            n++;
            listOfImages.add(entry.getSlika1());
        }
        if (entry.getSlika2() != null) {
            n++;
            listOfImages.add(entry.getSlika2());
        }
        if (entry.getSlika3() != null) {
            n++;
            listOfImages.add(entry.getSlika3());
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

    // Upload the entries, one by one Record
    private void uploadStep2() {
        Log.d(TAG, "Upload data step 2 started...");
        APIEntry apiEntry = new APIEntry();
        photos = new ArrayList<>();
        // Create apiEntry object
        Entry entry = entryList.get(0);
        apiEntry.setTaxonId(entry.getTaxonId() != null ? entry.getTaxonId().intValue() : null);
        apiEntry.setTaxonSuggestion(entry.getTaxonSuggestion());
        apiEntry.setYear(entry.getYear());
        apiEntry.setMonth(entry.getMonth());
        apiEntry.setDay(entry.getDay());
        apiEntry.setLatitude(entry.getLattitude());
        apiEntry.setLongitude(entry.getLongitude());
        if (entry.getAccuracy() == 0.0) {
            apiEntry.setAccuracy(null);
        } else {
            apiEntry.setAccuracy((int) entry.getAccuracy());
        }
        apiEntry.setLocation(entry.getLocation());
        apiEntry.setElevation((int) entry.getElevation());
        apiEntry.setNote(entry.getComment());
        apiEntry.setSex(entry.getSex());
        apiEntry.setNumber(entry.getNoSpecimens());
        apiEntry.setProject(entry.getProjectId());
        apiEntry.setFoundOn(entry.getFoundOn());
        apiEntry.setStageId(entry.getStage());
        apiEntry.setAtlasCode(entry.getAtlasCode());
        apiEntry.setFoundDead(entry.getDeadOrAlive().equals("true") ? 0 : 1);
        apiEntry.setFoundDeadNote(entry.getCauseOfDeath());
        apiEntry.setDataLicense(entry.getData_licence());
        apiEntry.setTime(entry.getTime());
        int[] observation_types = ArrayHelper.getArrayFromText(entry.getObservation_type_ids());
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
            APIEntry.Photo p = new APIEntry.Photo();
            p.setPath(images_array.get(i));
            p.setLicense(entry.getImage_licence());
            photos.add(p);
        }
        apiEntry.setPhotos(photos);
        apiEntry.setHabitat(entry.getHabitat());

        ObjectMapper mapper = new ObjectMapper();
        try {
            String s = mapper.writeValueAsString(apiEntry);
            Log.i(TAG, "Upload Entry " + s);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        Call<APIEntryResponse> call = RetrofitClient.getService(SettingsManager.getDatabaseName()).uploadEntry(apiEntry);
        call.enqueue(new Callback<APIEntryResponse>() {
            @Override
            public void onResponse(@NonNull Call<APIEntryResponse> call, @NonNull Response<APIEntryResponse> response) {

                // Retry if server deny your access
                if (response.code() == 429) {
                    String retry_after = response.headers().get("Retry-After");
                    Log.e(TAG, "Server had too many requests from the app. Waiting " + retry_after + " seconds.");
                    if (retry_after != null) {
                        int wait = Integer.parseInt(retry_after) * 1000;
                        SystemClock.sleep(wait);
                        uploadStep2();
                    }
                }

                if (response.isSuccessful()) {
                    if (keep_going) {
                        // Wait... Don’t send too many requests to the server!
                        SystemClock.sleep(300);
                        // Delete uploaded entry
                        App.get().getDaoSession().getEntryDao().delete(entry);
                        entryList.remove(0);
                        EventBus.getDefault().post(new DeleteEntryFromList());
                        // Delete image files from internal storage
                        for (String filename: filenames) {
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
                stopSelf();
            }
        });
    }

    private void uploadPhoto(File image) {
        Log.i(TAG, "Opening image from the path: " + image.getAbsolutePath() + ".");

        RequestBody reqFile = RequestBody.create(image, MediaType.parse("image/*"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", image.getName(), reqFile);

        Call<UploadFileResponse> call = RetrofitClient.getService(SettingsManager.getDatabaseName()).uploadFile(body);

        call.enqueue(new Callback<UploadFileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UploadFileResponse> call, @NonNull Response<UploadFileResponse> response) {

                if (response.code() == 429) {
                    String retry_after = response.headers().get("Retry-After");
                    Log.e(TAG, "Server had too many requests from the app. Waiting " + retry_after + " seconds.");
                    if (retry_after != null) {
                        int wait = Integer.parseInt(retry_after) * 1000;
                        SystemClock.sleep(wait);
                        uploadPhoto(image);
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
                    stopSelf();
                }
            }
        });
    }

    private void notificationUpdateProgress(int maxValue, int currentValue, String descriptionText) {
        // To do something if notification is taped, we must set up an intent
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Add Cancel button intent in notification.
        Intent cancelIntent = new Intent(this, UploadRecords.class);
        cancelIntent.setAction(ACTION_CANCEL);
        PendingIntent pendingCancelIntent = PendingIntent.getService(this, 0, cancelIntent, 0);
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

    private void notificationUpdateText(String title, String description) {
        // To do something if notification is taped, we must set up an intent
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

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

    public void sendResult(String message) {
        Intent intent = new Intent(TASK_COMPLETED);
        if(message != null)
            intent.putExtra(TASK_COMPLETED, message);
        broadcaster.sendBroadcast(intent);
    }
}
