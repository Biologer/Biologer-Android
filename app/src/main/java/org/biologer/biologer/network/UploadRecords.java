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
    private static final long OBSERVATION_DELAY_MS = 500;
    private static final long PHOTO_DELAY_MS = 1200;
    private static final long TIMED_COUNT_CHILD_DELAY_MS = 500;
    private boolean keepGoing = true;
    private ArrayList<LandingFragmentItems> entries;
    private int totalEntries = 0;
    private int remainingEntries = 0;
    private LocalBroadcastManager broadcaster;
    private AtomicInteger remainingUploads;
    private final AtomicInteger activeTimedCountUploads = new AtomicInteger(0);
    private volatile boolean cancelRequested = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

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

        if (activeTimedCountUploads.get() > 0) {
            cancelRequested = true;
            Log.i(TAG, "Cancel requested but timed-count child uploads are in progress — deferring.");
            return;
        }

        keepGoing = false;
        stopForegroundAndNotify(title, description);
    }

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
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

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

        handler.postDelayed(() -> {
            startForeground(1, notification);
            startUpload();
        }, 150);
    }

    private void startUpload() {
        AtomicInteger index = new AtomicInteger(0);

        Runnable uploader = new Runnable() {
            @Override
            public void run() {
                if (!keepGoing || index.get() >= entries.size()) return;

                LandingFragmentItems item = entries.get(index.getAndIncrement());
                incrementRemainingEntries();

                Integer timedCountId = item.getTimedCountId();
                if (timedCountId != null) {
                    uploadTimedCount(timedCountId, () -> {
                        onUploadFinished();
                        handler.postDelayed(this, TIMED_COUNT_CHILD_DELAY_MS);
                    });
                } else {
                    uploadObservation(item.getObservationId(), () -> {
                        onUploadFinished();
                        handler.postDelayed(this, OBSERVATION_DELAY_MS);
                    });
                }
            }
        };

        handler.post(uploader);
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
            Log.i(TAG, "All entries uploaded!");
            sendResult("success", 0);
            stopForegroundAndNotify(getString(R.string.notify_title_entries_uploaded),
                    getString(R.string.notify_desc_entries_uploaded));
            stopSelf();
        }
    }

    /** ---------------- TIMED COUNTS ---------------- */
    private void uploadTimedCount(Integer id, Runnable onFinished) {
        TimedCountDb timedCount = ObjectBoxHelper.getTimedCountById(id);
        if (timedCount == null) {
            onFinished.run();
            return;
        }

        APITimedCounts tc = new APITimedCounts();
        tc.getFromTimedCountDatabase(timedCount);

        RetrofitClient.getService(SettingsManager.getDatabaseName())
                .uploadTimedCount(tc)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<APITimedCountsResponse> call,
                                           @NonNull Response<APITimedCountsResponse> response) {

                        if (!keepGoing) return;

                        if (handle429Retry(response, () -> uploadTimedCount(id, onFinished))) return;

                        if (response.isSuccessful() && response.body() != null) {
                            long serverId = response.body().getData().getId();
                            Log.i(TAG, "Timed Count uploaded: local " + id + " → server " + serverId);
                            sendResult("timed_count_uploaded", id);

                            ArrayList<EntryDb> children = ObjectBoxHelper.getTimedCountObservations(id);
                            if (children.isEmpty()) {
                                ObjectBoxHelper.removeTimedCountById(id);
                                onFinished.run();
                                return;
                            }

                            activeTimedCountUploads.incrementAndGet();
                            AtomicInteger left = new AtomicInteger(children.size());

                            for (EntryDb obs : children) {
                                obs.setTimedCoundId((int) serverId);
                                ObjectBoxHelper.setObservation(obs);
                                remainingUploads.incrementAndGet();
                                incrementRemainingEntries();

                                handler.postDelayed(() -> uploadObservation(obs.getId(), () -> {
                                    onUploadFinished();
                                    if (left.decrementAndGet() == 0) {
                                        ObjectBoxHelper.removeTimedCountById(id);
                                        activeTimedCountUploads.decrementAndGet();
                                        onFinished.run();
                                        if (cancelRequested) {
                                            cancelRequested = false;
                                            cancelUpload(getString(R.string.notify_title_upload_canceled),
                                                    getString(R.string.notify_desc_upload_canceled));
                                        }
                                    }
                                }), TIMED_COUNT_CHILD_DELAY_MS);
                            }
                        } else {
                            Log.e(TAG, "Timed Count upload failed: HTTP " + response.code());
                            cancelUpload(getString(R.string.upload_failed),
                                    getString(R.string.timed_count_upload_response_invalid));
                            onFinished.run();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<APITimedCountsResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "Timed Count upload failed: " + t.getMessage());
                        cancelUpload(getString(R.string.upload_failed),
                                getString(R.string.timed_count_upload_response_invalid) + ": " + t.getMessage());
                        onFinished.run();
                    }
                });
    }

    /** ---------------- SINGLE OBSERVATIONS ---------------- */
    private void uploadObservation(Long entryId, Runnable onFinished) {
        EntryDb entryDb = ObjectBoxHelper.getObservationById(entryId);
        if (entryDb == null) {
            onFinished.run();
            return;
        }

        ArrayList<String> images = new ArrayList<>();
        if (entryDb.getSlika1() != null) images.add(entryDb.getSlika1());
        if (entryDb.getSlika2() != null) images.add(entryDb.getSlika2());
        if (entryDb.getSlika3() != null) images.add(entryDb.getSlika3());

        if (images.isEmpty()) {
            uploadObservationStep2(entryDb, onFinished, new ArrayList<>());
        } else {
            uploadPhotos(entryDb, images, onFinished);
        }
    }

    private void uploadPhotos(EntryDb entryDb, ArrayList<String> images, Runnable onFinished) {
        ArrayList<APIEntryPhotos> uploaded = new ArrayList<>();
        AtomicInteger left = new AtomicInteger(images.size());

        for (String path : images) {
            handler.postDelayed(() ->
                            uploadSinglePhoto(entryDb, path, uploaded, left, onFinished),
                    PHOTO_DELAY_MS * left.get());
        }
    }

    private void uploadSinglePhoto(EntryDb entryDb, String path,
                                   ArrayList<APIEntryPhotos> uploadedPhotos,
                                   AtomicInteger photosLeft, Runnable onFinished) {

        File file = new File(getFilesDir(), new File(path).getName());
        RequestBody reqFile = RequestBody.create(file, MediaType.parse("image/*"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), reqFile);

        RetrofitClient.getService(SettingsManager.getDatabaseName())
                .uploadFile(body)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<UploadFileResponse> call,
                                           @NonNull Response<UploadFileResponse> response) {

                        if (!keepGoing) return;

                        if (handle429Retry(response, () -> uploadSinglePhoto(entryDb, path, uploadedPhotos, photosLeft, onFinished))) return;

                        if (response.isSuccessful() && response.body() != null) {
                            APIEntryPhotos photo = new APIEntryPhotos();
                            photo.setPath(response.body().getFile());
                            photo.setLicense(entryDb.getImageLicence());
                            uploadedPhotos.add(photo);
                        } else {
                            Log.e(TAG, "Photo upload failed: HTTP " + response.code());
                            cancelUpload(getString(R.string.upload_failed),
                                    getString(R.string.photo_upload_response_invalid));
                        }

                        if (photosLeft.decrementAndGet() == 0) {
                            uploadObservationStep2(entryDb, onFinished, uploadedPhotos);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<UploadFileResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "Photo upload failed: " + t.getMessage());
                        cancelUpload(getString(R.string.upload_failed),
                                getString(R.string.photo_upload_response_invalid) + ": " + t.getMessage());
                        if (photosLeft.decrementAndGet() == 0) {
                            uploadObservationStep2(entryDb, onFinished, uploadedPhotos);
                        }
                    }
                });
    }

    private void uploadObservationStep2(EntryDb entryDb, Runnable onFinished, List<APIEntryPhotos> photos) {
        APIEntry apiEntry = new APIEntry();
        apiEntry.getFromEntryDb(entryDb);
        apiEntry.setPhotos(photos);

        RetrofitClient.getService(SettingsManager.getDatabaseName())
                .uploadEntry(apiEntry)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<APIEntryResponse> call,
                                           @NonNull Response<APIEntryResponse> response) {

                        if (!keepGoing) return;

                        if (handle429Retry(response, () -> uploadObservationStep2(entryDb, onFinished, photos))) return;

                        if (response.isSuccessful()) {
                            ObjectBoxHelper.removeObservationById(entryDb.getId());
                            Log.d(TAG, "Entry ID " + entryDb.getId() + " uploaded!");
                            if (entryDb.getTimedCoundId() == null) {
                                sendResult("id_uploaded", entryDb.getId());
                            }
                            onFinished.run();
                        } else {
                            Log.e(TAG, "Entry upload failed: HTTP " + response.code());
                            cancelUpload(getString(R.string.upload_failed),
                                    getString(R.string.observation_upload_response_invalid));
                            onFinished.run();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<APIEntryResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "Entry upload failed: " + t.getMessage());
                        cancelUpload(getString(R.string.upload_failed),
                                getString(R.string.observation_upload_response_invalid) + ": " + t.getMessage());
                        onFinished.run();
                    }
                });
    }

    /** Handle HTTP 429 Too Many Requests **/
    private boolean handle429Retry(Response<?> response, Runnable retryAction) {
        if (response.code() == 429) {
            String retryAfter = response.headers().get("Retry-After");
            if (retryAfter != null) {
                int wait = Integer.parseInt(retryAfter) * 1000;
                Log.w(TAG, "Server throttled request. Retrying in " + wait + " ms");
                handler.postDelayed(retryAction, wait);
                return true;
            }
        }
        return false;
    }

    /** ---------------- NOTIFICATIONS ---------------- */
    @SuppressLint("UnspecifiedImmutableFlag")
    private void notificationUpdateProgress(int maxValue, int currentValue, String descriptionText) {
        Intent intent = new Intent(this, ActivityLanding.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

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
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "biologer_entries")
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
