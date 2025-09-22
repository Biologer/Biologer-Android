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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.adapters.LandingFragmentItems;
import org.biologer.biologer.gui.ActivityLanding;
import org.biologer.biologer.network.json.APITimedCounts;
import org.biologer.biologer.network.json.APITimedCountsResponse;
import org.biologer.biologer.network.json.APIEntry;
import org.biologer.biologer.network.json.APIEntryPhotos;
import org.biologer.biologer.network.json.APIEntryResponse;
import org.biologer.biologer.network.json.UploadFileResponse;
import org.biologer.biologer.services.ObjectBoxHelper;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.TimedCountDb;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
    public static final String TASK_COMPLETED = "org.biologer.biologer.UploadRecordsService.TASK_COMPLETED";

    private static final int MAX_RETRIES = 3;

    private boolean keepGoing = true;
    private ArrayList<LandingFragmentItems> entries;
    private int totalEntries = 0;
    private int remainingEntries = 0;
    private LocalBroadcastManager broadcaster;
    private AtomicInteger remainingUploads;

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_START:
                        Log.d(TAG, "Starting upload process…");
                        entries = LandingFragmentItems.loadAllEntries(this);
                        totalEntries = entries.size();
                        remainingUploads = new AtomicInteger(entries.size());
                        Log.d(TAG, "There are " + totalEntries + " entries to upload.");
                        notificationInitiate();
                        break;
                    case ACTION_CANCEL:
                        cancelUpload(getString(R.string.notify_title_upload_canceled),
                                getString(R.string.notify_desc_upload_canceled));
                        stopSelf();
                        break;
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void cancelUpload(String title, String description) {
        Log.d(TAG, "Canceling upload process…");
        keepGoing = false;
        stopForegroundAndNotify(title, description);
    }

    @SuppressLint("deprecation")
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
        Intent intent = new Intent(this, ActivityLanding.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "biologer_entries")
                .setSmallIcon(R.mipmap.ic_notification)
                .setContentTitle(getString(R.string.notify_title_entries))
                .setContentText(getString(R.string.notify_desc_entries))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false);

        Notification notification = builder.build();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startForeground(1, notification);
            startUpload();
        }, 150);
    }

    private void startUpload() {
        for (LandingFragmentItems item : entries) {
            if (!keepGoing) {
                Log.i(TAG, "Uploading canceled by user.");
                break;
            }

            Integer timedCountId = item.getTimedCountId();
            incrementRemainingEntries();
            if (timedCountId != null) {
                uploadTimedCount(timedCountId, this::onUploadFinished, 0);
            } else {
                uploadObservation(item.getObservationId(), this::onUploadFinished);
            }
        }
    }

    private void incrementRemainingEntries() {
        remainingEntries++;
        String statusText = getString(R.string.notify_desc_uploading) + " " + remainingEntries +
                " " + getString(R.string.notify_desc_uploading1) + " " + totalEntries +
                " " + getString(R.string.notify_desc_uploading2);
        notificationUpdateProgress(totalEntries, remainingEntries, statusText);
    }

    private void onUploadFinished() {
        if (remainingUploads.decrementAndGet() == 0) {
            Log.i(TAG, "All entries have been uploaded!");
            sendResult("success", 0);
            stopForegroundAndNotify(getString(R.string.notify_title_entries_uploaded),
                    getString(R.string.notify_desc_entries_uploaded));
            stopSelf();
        }
    }

    private void uploadTimedCount(Integer id, Runnable onFinished, int retryCount) {
        TimedCountDb timedCount = ObjectBoxHelper.getTimedCountById(id);
        APITimedCounts tc = new APITimedCounts();
        tc.getFromTimedCountDatabase(timedCount);

        RetrofitClient.getService(SettingsManager.getDatabaseName())
                .uploadTimedCount(tc)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<APITimedCountsResponse> call,
                                           @NonNull Response<APITimedCountsResponse> response) {

                        if (response.code() == 429 && retryCount < MAX_RETRIES) {
                            String retryAfter = response.headers().get("Retry-After");
                            if (retryAfter != null) {
                                int wait = Integer.parseInt(retryAfter) * 1000;
                                new Thread(() -> {
                                    try { Thread.sleep(wait); } catch (InterruptedException ignored) {}
                                    if (keepGoing) new Handler(Looper.getMainLooper())
                                            .post(() -> uploadTimedCount(id, onFinished, retryCount + 1));
                                }).start();
                                return;
                            }
                        }

                        if (response.isSuccessful() && keepGoing) {
                            Log.i(TAG, "Timed Count uploaded ID " + id);
                            sendResult("timed_count_uploaded", id);

                            ArrayList<EntryDb> timedCountObservations = ObjectBoxHelper.getTimedCountObservations(id);
                            for (EntryDb observation : timedCountObservations) {
                                uploadObservation(observation.getId(), null);
                            }

                            if (onFinished != null) onFinished.run();
                        } else if (retryCount < MAX_RETRIES) {
                            Log.w(TAG, "Retrying timedCount ID " + id + " attempt " + (retryCount + 1));
                            uploadTimedCount(id, onFinished, retryCount + 1);
                        } else {
                            Log.e(TAG, "Timed Count upload failed after max retries!");
                            cancelUpload("Failed!", "Timed Count upload failed!");
                            if (onFinished != null) onFinished.run();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<APITimedCountsResponse> call, @NonNull Throwable t) {
                        if (retryCount < MAX_RETRIES) {
                            Log.w(TAG, "Retrying timedCount ID " + id + " after failure attempt " + (retryCount + 1));
                            uploadTimedCount(id, onFinished, retryCount + 1);
                        } else {
                            Log.e(TAG, "Timed Count upload failed after max retries: " + t.getMessage());
                            cancelUpload("Failed!", "Timed Count upload failed!");
                            if (onFinished != null) onFinished.run();
                        }
                    }
                });
    }

    private void uploadObservation(Long entryId, Runnable onFinished) {
        EntryDb entryDb = ObjectBoxHelper.getObservationById(entryId);
        if (entryDb == null) {
            Log.e(TAG, "Entry not found in DB: " + entryId);
            if (onFinished != null) onFinished.run();
            return;
        }

        ArrayList<String> imagesToUpload = new ArrayList<>();
        if (entryDb.getSlika1() != null) imagesToUpload.add(entryDb.getSlika1());
        if (entryDb.getSlika2() != null) imagesToUpload.add(entryDb.getSlika2());
        if (entryDb.getSlika3() != null) imagesToUpload.add(entryDb.getSlika3());

        if (imagesToUpload.isEmpty()) {
            uploadObservationStep2(entryDb, onFinished, new ArrayList<>(), 0);
        } else {
            uploadPhotos(entryDb, imagesToUpload, onFinished, 0);
        }
    }

    private void uploadPhotos(EntryDb entryDb, ArrayList<String> images, Runnable onFinished, int retryCount) {
        ArrayList<APIEntryPhotos> uploadedPhotos = new ArrayList<>();
        final int totalPhotos = images.size();
        final AtomicInteger photosLeft = new AtomicInteger(totalPhotos);

        for (String path : images) {
            File file = new File(getFilesDir(), new File(path).getName());
            RequestBody reqFile = RequestBody.create(file, MediaType.parse("image/*"));
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), reqFile);

            RetrofitClient.getService(SettingsManager.getDatabaseName())
                    .uploadFile(body)
                    .enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<UploadFileResponse> call, @NonNull Response<UploadFileResponse> response) {
                            if (response.isSuccessful() && keepGoing) {
                                UploadFileResponse resp = response.body();
                                if (resp != null) {
                                    APIEntryPhotos photo = new APIEntryPhotos();
                                    photo.setPath(resp.getFile());
                                    photo.setLicense(entryDb.getImageLicence());
                                    uploadedPhotos.add(photo);
                                }
                                if (photosLeft.decrementAndGet() == 0) {
                                    uploadObservationStep2(entryDb, onFinished, uploadedPhotos, retryCount);
                                }
                            } else if (retryCount < MAX_RETRIES) {
                                Log.w(TAG, "Retrying photo upload for entry " + entryDb.getId() + " attempt " + (retryCount + 1));
                                uploadPhotos(entryDb, images, onFinished, retryCount + 1);
                            } else {
                                Log.e(TAG, "Photo upload failed after max retries!");
                                cancelUpload("Failed!", "Photo upload failed!");
                                if (onFinished != null) onFinished.run();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<UploadFileResponse> call, @NonNull Throwable t) {
                            if (retryCount < MAX_RETRIES) {
                                Log.w(TAG, "Retrying photo upload for entry " + entryDb.getId() + " after failure attempt " + (retryCount + 1));
                                uploadPhotos(entryDb, images, onFinished, retryCount + 1);
                            } else {
                                Log.e(TAG, "Photo upload failed after max retries: " + t.getMessage());
                                cancelUpload("Failed!", "Photo upload failed!");
                                if (onFinished != null) onFinished.run();
                            }
                        }
                    });
        }
    }

    private void uploadObservationStep2(EntryDb entryDb, Runnable onFinished, List<APIEntryPhotos> photos, int retryCount) {
        APIEntry apiEntry = new APIEntry();
        apiEntry.getFromEntryDb(entryDb);
        apiEntry.setPhotos(photos);

        RetrofitClient.getService(SettingsManager.getDatabaseName())
                .uploadEntry(apiEntry)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<APIEntryResponse> call, @NonNull Response<APIEntryResponse> response) {
                        if (response.isSuccessful() && keepGoing) {
                            long id = entryDb.getId();
                            ObjectBoxHelper.removeObservationById(id);
                            Log.d(TAG, "Entry ID " + id + " uploaded!");
                            if (entryDb.getTimedCoundId() == null) sendResult("id_uploaded", id);
                            if (onFinished != null) onFinished.run();
                        } else if (retryCount < MAX_RETRIES) {
                            Log.w(TAG, "Retrying uploadObservationStep2 for entry " + entryDb.getId() +
                                    " attempt " + (retryCount + 1));
                            uploadObservationStep2(entryDb, onFinished, photos, retryCount + 1);
                        } else {
                            Log.e(TAG, "Entry upload failed after max retries!");
                            cancelUpload("Failed!", "Entry upload failed!");
                            if (onFinished != null) onFinished.run();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<APIEntryResponse> call, @NonNull Throwable t) {
                        if (retryCount < MAX_RETRIES) {
                            Log.w(TAG, "Retrying uploadObservationStep2 after failure for entry " + entryDb.getId() +
                                    " attempt " + (retryCount + 1));
                            uploadObservationStep2(entryDb, onFinished, photos, retryCount + 1);
                        } else {
                            Log.e(TAG, "Entry upload failed after max retries: " + t.getMessage());
                            cancelUpload("Failed!", "Entry upload failed!");
                            if (onFinished != null) onFinished.run();
                        }
                    }
                });
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void notificationUpdateProgress(int maxValue, int currentValue, String descriptionText) {
        Intent intent = new Intent(this, ActivityLanding.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "biologer_entries")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(getString(R.string.notify_title_entries))
                .setContentText(descriptionText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setProgress(maxValue, currentValue, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(1, builder.build());
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void notificationUpdateText(String title, String description) {
        Intent intent = new Intent(this, ActivityLanding.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "biologer_taxa")
                .setSmallIcon(R.mipmap.ic_notification)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(false)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(false)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(1, builder.build());
    }

    public void sendResult(String message, long id) {
        Intent intent = new Intent(TASK_COMPLETED);
        intent.putExtra(TASK_COMPLETED, message);
        intent.putExtra("EntryID", id);
        broadcaster.sendBroadcast(intent);
    }
}

