package org.biologer.biologer.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.biologer.biologer.App;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.helpers.ObjectBoxHelper;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.json.FieldObservationData;
import org.biologer.biologer.network.json.FieldObservationDataActivity;
import org.biologer.biologer.network.json.FieldObservationDataPhotos;
import org.biologer.biologer.network.json.TimedCountData;
import org.biologer.biologer.network.json.TimedCountResponse;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.ObservationActivityDb;
import org.biologer.biologer.sql.PhotoDb;
import org.biologer.biologer.sql.PhotoDb_;
import org.biologer.biologer.sql.TimedCountDb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.objectbox.Box;
import io.objectbox.query.Query;
import retrofit2.Response;

public class TimedCountsDownloadWorker extends Worker {
    private static final String TAG = "Biologer.TimedCountsWorker";
    boolean firstSync = false;
    Set<Long> addedTimedCountIds = new HashSet<>();

    public TimedCountsDownloadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        boolean isSyncOnScroll = getInputData().getBoolean("isSyncOnScroll", false);
        long beforeId = getInputData().getLong("beforeId", -1);
        long afterId = getInputData().getLong("afterId", -1);
        long updatedAt = getInputData().getLong("updatedAt", -1);
        int page = getInputData().getInt("page", 1);
        long currentSyncTimestamp = getInputData().getLong("currentSyncTimestamp", -1);

        Long serverBeforeId = (beforeId != -1) ? beforeId : null;
        Long serverAfterId = (afterId != -1) ? afterId : null;
        Long serverUpdatedAt = (updatedAt != -1) ? updatedAt : null;

        String database = SettingsManager.getDatabaseName();
        if (database == null) return Result.failure();

        // For the first sync we set the serverUpdatedAt and serverAfterId to null
        if (updatedAt == 0 && !isSyncOnScroll) {
            Log.d(TAG, "First sync of timed counts initiated.");
            serverUpdatedAt = null;
            serverAfterId = null;
            firstSync = true;
        }

        if (currentSyncTimestamp == -1 && (firstSync || afterId != -1 || updatedAt != -1)) {
            currentSyncTimestamp = System.currentTimeMillis() / 1000;
        }

        try {
            Response<TimedCountResponse> response = RetrofitClient.getService(database)
                    .getMyTimedCounts(
                            page,
                            25,
                            serverUpdatedAt,
                            "id",
                            "desc",
                            serverAfterId,
                            serverBeforeId
                    ).execute();

            if (response.isSuccessful() && response.body() != null) {
                List<TimedCountData> data = Arrays.asList(response.body().getData());
                if (!data.isEmpty()) {
                    saveToLocalDatabase(data);
                }

                // Resume download if there are new data on the server compared to local data
                // In other cases we will download data when the user scrolls down the recycler view
                Log.d(TAG, "META: " + response.body().getMeta());
                boolean hasNextPage = response.body().getMeta().getCurrentPage() < response.body().getMeta().getLastPage();
                if (hasNextPage && (serverUpdatedAt != null || serverAfterId != null)) {

                    Data nextInput = new Data.Builder()
                            .putBoolean("isSyncOnScroll", false)
                            .putLong("updatedAt", updatedAt)
                            .putLong("currentSyncTimestamp", currentSyncTimestamp)
                            .putLong("afterId", afterId)
                            .putInt("page", page + 1)
                            .build();

                    OneTimeWorkRequest timedCountRequest = new OneTimeWorkRequest.Builder(TimedCountsDownloadWorker.class)
                            .setInputData(nextInput)
                            .addTag("TIMED_COUNTS_DOWNLOAD_UPDATED_AT")
                            .build();

                    WorkManager.getInstance(getApplicationContext())
                            .enqueueUniqueWork("timedcounts_sync_chain",
                                    ExistingWorkPolicy.APPEND, timedCountRequest);
                } else {
                    if (!isSyncOnScroll && currentSyncTimestamp != -1) {
                        SettingsManager.setObservationsUpdatedAt(currentSyncTimestamp);
                        Log.d(TAG, "Observations sync completed. Updating timestamp to: " + currentSyncTimestamp);
                    }
                }

                // Prepare data to return
                long[] idsArray = new long[addedTimedCountIds.size()];
                int i = 0;
                for (Long id : addedTimedCountIds) {
                    idsArray[i++] = id;
                }

                Data output = new Data.Builder()
                        .putLongArray("updatedTimedCountIds", idsArray)
                        .build();

                return Result.success(output);
            } else {
                return Result.retry();
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error during sync", e);
            return Result.retry();
        }
    }

    private void saveToLocalDatabase(List<TimedCountData> dataList) {
        if (dataList == null || dataList.isEmpty()) return;

        Box<TimedCountDb> timedCountBox = App.get().getBoxStore().boxFor(TimedCountDb.class);
        Box<EntryDb> observationBox = App.get().getBoxStore().boxFor(EntryDb.class);
        Box<PhotoDb> photoBox = App.get().getBoxStore().boxFor(PhotoDb.class);

        App.get().getBoxStore().runInTx(() -> {
            for (TimedCountData data : dataList) {

                TimedCountDb existing = ObjectBoxHelper.getTimedCountByServerId(data.getId());

                if (existing == null) {
                    //
                    // Path 1. New timed count. Not in ObjectBox.
                    //
                    try {
                        // Part 1. Save the new time count
                        TimedCountDb timedCount = TimedCountDb.getTimedCountFromData(data);
                        long localTimeCountId = timedCountBox.put(timedCount);
                        timedCount.setId(localTimeCountId);

                        Log.d(TAG, "Saving Time Count server ID " + data.getId() + " locally (ObjectBox ID " + localTimeCountId + ").");

                        addedTimedCountIds.add(localTimeCountId);

                        // Part 2. Save the activity log for time counts
                        List<FieldObservationDataActivity> timeCountActivityItems = data.getActivity();
                        if (timeCountActivityItems != null && !timeCountActivityItems.isEmpty()) {
                            timedCount.observationActivity.clear();

                            for (FieldObservationDataActivity activity : timeCountActivityItems) {
                                ObservationActivityDb activityLog = FieldObservationDataActivity
                                        .getObservationActivityDb(activity);

                                activityLog.timedCount.setTarget(timedCount);
                                timedCount.observationActivity.add(activityLog);
                            }

                            timedCountBox.put(timedCount);
                        }

                        // Part 3. Save the observations within the time count
                        List<FieldObservationData> fieldObservations = data.getFieldObservations();
                        for (FieldObservationData observation : fieldObservations) {

                            // Part 3a. Check if the observation already exists by serverId
                            EntryDb entryToUse;
                            EntryDb existingEntry = ObjectBoxHelper.getObservationByServerId(observation.getId());
                            if (existingEntry == null) {
                                // New entry
                                entryToUse = EntryDb.getEntryFieldsFromData(observation);
                                Log.d(TAG, "Creating new observation for server ID " + observation.getId());
                            } else {
                                // Existing entry - Update fields
                                entryToUse = existingEntry;
                                EntryDb.syncEntryFieldsFromData(entryToUse, observation);
                                Log.d(TAG, "Updating existing observation for server ID " + observation.getId());
                            }

                            entryToUse.setTimedCoundId(timedCount.getServerId().intValue());
                            long localId = observationBox.put(entryToUse);
                            entryToUse.setId(localId);

                            Log.d(TAG, "Saving Observation server ID " + observation.getId() + " locally (ObjectBox ID " + localId + ").");

                            // Part 3b. Get the photos from each observation
                            if (observation.getPhotos() != null && !observation.getPhotos().isEmpty()) {
                                Log.d(TAG, "Saving " + observation.getPhotos().size() + " photos for entry " + localId + ".");

                                for (FieldObservationDataPhotos photoData : observation.getPhotos()) {

                                    PhotoDb existingPhoto = null;
                                    try (Query<PhotoDb> query = photoBox.query()
                                            .equal(PhotoDb_.serverId, photoData.getId())
                                            .build()) {
                                        existingPhoto = query.findFirst();
                                    } catch (Exception e) {
                                        Log.e(TAG, "Photo query failed: ", e);
                                    }

                                    if (existingPhoto == null) {
                                        PhotoDb photo = PhotoDb.getPhotoFields(photoData);
                                        photo.entry.setTarget(entryToUse);
                                        entryToUse.photos.add(photo);
                                    } else {
                                        String path = existingPhoto.getLocalPath();
                                        if (path == null || path.isEmpty() || !new File(path.replace("file://", "")).exists()) {
                                            Log.d(TAG, "Photo exists in DB but file is missing. Resetting path for re-download.");
                                            existingPhoto.setLocalPath(null);
                                            photoBox.put(existingPhoto);
                                        }
                                    }

                                }

                                observationBox.put(entryToUse);
                            }

                            // Part 3c. Save the activity log
                            List<FieldObservationDataActivity> activityItems = observation.getActivity();
                            if (activityItems != null && !activityItems.isEmpty()) {
                                entryToUse.observationActivity.clear();

                                for (FieldObservationDataActivity activity : activityItems) {
                                    ObservationActivityDb activityLog = FieldObservationDataActivity
                                            .getObservationActivityDb(activity);

                                    activityLog.entry.setTarget(entryToUse);
                                    entryToUse.observationActivity.add(activityLog);
                                }

                                observationBox.put(entryToUse);
                            }

                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error saving to ObjectBox: " + data.getId(), e);
                    }
                } else {
                    //
                    // Path 2. Existing Time Count already stored in ObjectBox.
                    //
                    if (data.getActivity().size() > existing.observationActivity.size()) {
                        Log.d(TAG, "Server has " + data.getActivity().size() + " activities. Updating local time count " + data.getId());

                        // Part 1. Update observation
                        TimedCountDb.syncTimeCountFieldsFromData(existing, data);
                        addedTimedCountIds.add(existing.getId());

                        // Part 2. Save the activity log for time counts
                        List<FieldObservationDataActivity> timeCountActivityItems = data.getActivity();
                        if (timeCountActivityItems != null && !timeCountActivityItems.isEmpty()) {
                            existing.observationActivity.clear();

                            for (FieldObservationDataActivity activity : timeCountActivityItems) {
                                ObservationActivityDb activityLog = FieldObservationDataActivity
                                        .getObservationActivityDb(activity);
                                activityLog.timedCount.setTarget(existing);
                                existing.observationActivity.add(activityLog);
                            }

                            timedCountBox.put(existing);
                        }

                        // Part 3. Update all observations and photos
                        List<FieldObservationData> fieldObservations = data.getFieldObservations();
                        for (FieldObservationData observation : fieldObservations) {

                            EntryDb existingEntry = ObjectBoxHelper.getObservationByServerId(observation.getId());

                            if (existingEntry != null) {

                                List<FieldObservationDataPhotos> serverPhotos = observation.getPhotos();
                                List<PhotoDb> objectBoxPhotos = existingEntry.photos;
                                if (serverPhotos == null) {
                                    serverPhotos = new ArrayList<>();
                                }
                                Log.d(TAG, "Syncing photos: server has " + serverPhotos.size() + ", ObjectBox has " + objectBoxPhotos.size());

                                // Case 1. Delete local photos removed on server
                                for (int i = objectBoxPhotos.size() - 1; i >= 0; i--) {
                                    PhotoDb photoDb = objectBoxPhotos.get(i);

                                    // Only consider photos that have a serverId,
                                    // i.e. don't delete unuploaded photos
                                    if (photoDb.getServerId() != 0) {
                                        boolean foundOnServer = false;
                                        for (FieldObservationDataPhotos serverPhoto : serverPhotos) {
                                            if (serverPhoto.getId() == photoDb.getServerId()) {
                                                foundOnServer = true;
                                                break;
                                            }
                                        }

                                        if (!foundOnServer) {
                                            Log.d(TAG, "Photo ID " + photoDb.getServerId() + " was deleted on server. Cleaning up.");

                                            // Delete the physical file from internal storage
                                            String localPath = photoDb.getLocalPath();
                                            if (localPath != null && !localPath.isEmpty()) {
                                                File file = new File(localPath.replace("file://", ""));
                                                if (file.exists()) {
                                                    if (file.delete()) {
                                                        Log.d(TAG, "Successfully deleted local image file: " + localPath);
                                                    } else {
                                                        Log.e(TAG, "Failed to delete local image file: " + localPath);
                                                    }
                                                }
                                            }

                                            objectBoxPhotos.remove(i);
                                        }
                                    }
                                }

                                // Case 2. Add new server photo to ObjectBox
                                for (FieldObservationDataPhotos photoData : serverPhotos) {
                                    boolean existsLocally = false;
                                    for (PhotoDb photoDb : objectBoxPhotos) {
                                        if (photoDb.getServerId() == photoData.getId()) {
                                            existsLocally = true;
                                            break;
                                        }
                                    }

                                    if (!existsLocally) {
                                        PhotoDb photo = PhotoDb.getPhotoFields(photoData);
                                        photo.entry.setTarget(existingEntry);
                                        objectBoxPhotos.add(photo);
                                    }
                                }

                                // Part 3. Update activity log
                                existingEntry.observationActivity.clear();
                                for (FieldObservationDataActivity activity : observation.getActivity()) {
                                    ObservationActivityDb activityLog = FieldObservationDataActivity.getObservationActivityDb(activity);
                                    activityLog.entry.setTarget(existingEntry);
                                    existingEntry.observationActivity.add(activityLog);
                                }

                                observationBox.put(existingEntry);
                                Log.d(TAG, "Entry " + data.getId() + " synchronized with server state.");

                            }
                        }
                    } else {
                        Log.d(TAG, "Entry " + data.getId() + " is up to date. Skipping.");
                    }
                }
            }
        });

    }
}
