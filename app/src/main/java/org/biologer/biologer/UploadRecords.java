package org.biologer.biologer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.biologer.biologer.bus.DeleteEntryFromList;
import org.biologer.biologer.gui.SplashActivity;
import org.biologer.biologer.network.JSON.APIEntry;
import org.biologer.biologer.sql.Entry;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.JSON.UploadFileResponse;
import org.biologer.biologer.network.JSON.APIEntryResponse;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
    File image1, image2, image3;
    LocalBroadcastManager broadcaster;

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
                        try {
                            deleteCache();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
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
        stopForeground(true);
        notificationUpdateText(title, description);
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

        try {
            uploadStep1();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This checks if there are photos in the Entry Record.
    // If there are upload photos first.
    // If no photos/or after the photos are uploaded upload the data.
    private void uploadStep1() throws IOException {
        // n is a number of images from 1 to 3
        n = 0;
        ArrayList<String> listOfImages = new ArrayList<>();
        images_array.clear();

        // When all entries are uploaded
        if (entryList.size() == 0) {
            Log.i(TAG, "All entries seems to be uploaded to the server!");
            App.get().getDaoSession().getEntryDao().deleteAll();
            stopForeground(true);
            notificationUpdateText(getString(R.string.notify_title_entries_uploaded), getString(R.string.notify_desc_entries_uploaded));
            deleteCache();
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
        // If there are photos resize them and send them first
        else {

            for (int i = 0; i < n; i++) {
                String image = listOfImages.get(i);
                int image_number = i+1;
                Log.d(TAG, "Resizing image " + image_number + ": " + image);
                String tmp_image_path;
                Bitmap bitmap = resizeImage(image);

                if (bitmap == null) {
                    stopSelf();
                    Log.e(TAG, "Stopping the service, there is no image on the storage!");
                    sendResult("no image");
                } else {
                    if (i == 0) {
                        tmp_image_path = saveTmpImage(bitmap);
                        image1 = new File(tmp_image_path);
                        Log.d(TAG, "Uploading image 1 to a server.");
                        uploadPhoto(image1);
                    }
                    if (i == 1) {
                        tmp_image_path = saveTmpImage(bitmap);
                        image2 = new File(tmp_image_path);
                        Log.d(TAG, "Uploading image 2 to a server.");
                        uploadPhoto(image2);
                    }
                    if (i == 2) {
                        tmp_image_path = saveTmpImage(bitmap);
                        image3 = new File(tmp_image_path);
                        Log.d(TAG, "Uploading image 3 to a server.");
                        uploadPhoto(image3);
                    }
                }
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
        apiEntry.setFoundDead(entry.getDeadOrAlive().equals("true") ? 0 : 1);
        apiEntry.setFoundDeadNote(entry.getCauseOfDeath());
        apiEntry.setDataLicense(entry.getData_licence());
        apiEntry.setTime(entry.getTime());
        apiEntry.setTypes(getArrayFromText(entry.getObservation_type_ids()));
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
                if (response.isSuccessful()) {
                    if (keep_going) {
                        App.get().getDaoSession().getEntryDao().delete(entryList.get(0));
                        entryList.remove(0);
                        EventBus.getDefault().post(new DeleteEntryFromList());
                        m = 0;
                        try {
                            uploadStep1();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.i(TAG, "Uploading of entry did not work for some reason. No internet?");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<APIEntryResponse> call, @NonNull Throwable t) {
                Log.i(TAG, "Uploading of entry failed for some reason: " + Objects.requireNonNull(t.getLocalizedMessage()));
                cancelUpload("Failed!", "Uploading of entry was not successful!");
            }
        });
    }

    private int[] getArrayFromText(String observation_type_ids) {
        String[] strings = observation_type_ids.replace("[", "").replace("]", "").split(", ");
        int[] new_array = new int[strings.length];
        for (int i = 0; i < strings.length; i++) {
            new_array[i] = Integer.parseInt(strings[i]);
        }
        return new_array;
    }

    private void uploadPhoto(File image) {
        Log.i(TAG, "Opening image from the path: " + image.getAbsolutePath() + ".");

        RequestBody reqFile = RequestBody.create(image, MediaType.parse("image/*"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", image.getName(), reqFile);

        Call<UploadFileResponse> call = RetrofitClient.getService(SettingsManager.getDatabaseName()).uploadFile(body);

        call.enqueue(new Callback<UploadFileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UploadFileResponse> call, @NonNull Response<UploadFileResponse> response) {

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
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UploadFileResponse> call, @NonNull Throwable t) {
                if (t.getLocalizedMessage() != null) {
                    Log.e(TAG, "Upload of photo failed for some reason: " + t.getLocalizedMessage());
                    cancelUpload("Failed!", "Uploading of photo was not successful!");
                }
            }
        });
    }

    private String saveTmpImage(Bitmap resized_image) throws IOException {
        // Create unique file name
        String filename = UUID.randomUUID().toString() + ".jpg";
        File tmp_file = new File(getCacheDir(), filename);
        Log.d(TAG,"Temporary file for resized images will be: " + tmp_file.getAbsolutePath());

        try {
            OutputStream fos = new FileOutputStream(tmp_file);
            resized_image.compress(Bitmap.CompressFormat.JPEG, 70, fos);
            Objects.requireNonNull(fos).close();
            Log.i(TAG, "Temporary file saved.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i(TAG, "Temporary file NOT saved.");
        }
        return tmp_file.getAbsolutePath();
    }

    // This uses MediaStore to resize images, which is forced in Android Q
    private Bitmap resizeImage(String path_to_image) {
        Uri imageUri = Uri.parse(path_to_image);
        Bitmap input_image = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), imageUri);
                input_image = ImageDecoder.decodeBitmap(source);
            } else {
                input_image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (input_image == null) {
            Log.e(TAG, "It looks like input image does not exist!");
            return null;
        } else {
            return resizeBitmap(input_image, 1024);
        }
    }

    public Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        Log.i(TAG, "Resizing image to a maximum of " + maxSize + "px.");
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (height == width) {
            height = maxSize;
            width = maxSize;
        } if (height < width) {
            height = height * maxSize / width;
            width = maxSize;
        } else {
            width = width * maxSize /height;
            height = maxSize;
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, false);
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

    private void deleteCache () throws IOException {
        File cacheDir = getCacheDir();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Deleting cache.");
            Files.delete(cacheDir.toPath());
        } else {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                Log.d(TAG, "Deleting cache. There are " + files.length + " files in a cache directory.");
                for (File file : files)
                    if (file.delete()) {
                        Log.d(TAG, "Deleting cache file " + file.getName());
                    }
            }
        }
    }

    public void sendResult(String message) {
        Intent intent = new Intent(TASK_COMPLETED);
        if(message != null)
            intent.putExtra(TASK_COMPLETED, message);
        broadcaster.sendBroadcast(intent);
    }
}
