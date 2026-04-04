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
import org.biologer.biologer.network.json.FieldObservationResponse;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.ObservationActivityDb;
import org.biologer.biologer.sql.PhotoDb;
import org.biologer.biologer.sql.PhotoDb_;

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

public class ObservationsDownloadWorker extends Worker {
    private static final String TAG = "Biologer.ObsSyncWorker";
    boolean firstSync = false;
    Set<Long> addedObservationIds = new HashSet<>();

    public ObservationsDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        boolean isSyncOnScroll = getInputData().getBoolean("isSyncOnScroll", false);
        long beforeId = getInputData().getLong("beforeId", -1);
        long afterId = getInputData().getLong("afterId", -1);
        long updatedAt = getInputData().getLong("observationsUpdatedAt", -1);
        int page = getInputData().getInt("page", 1);
        long currentSyncTimestamp = getInputData().getLong("currentSyncTimestamp", -1);

        Long serverBeforeId = (beforeId != -1) ? beforeId : null;
        Long serverAfterId = (afterId != -1) ? afterId : null;
        Long serverUpdatedAt = (updatedAt != -1) ? updatedAt : null;

        String database = SettingsManager.getDatabaseName();
        if (database == null) return Result.failure();

        // For the first sync we set the serverUpdatedAt and serverAfterId to null
        if (updatedAt == 0 && !isSyncOnScroll) {
            Log.d(TAG, "First sync of observations initiated.");
            serverUpdatedAt = null;
            serverAfterId = null;
            firstSync = true;
        }

        if (currentSyncTimestamp == -1 && (firstSync || afterId != -1 || updatedAt != -1)) {
            currentSyncTimestamp = System.currentTimeMillis() / 1000;
        }

        try {
            Response<FieldObservationResponse> response = RetrofitClient.getService(database)
                    .getMyFieldObservations(
                            page,
                            25,
                            serverUpdatedAt,
                            "id",
                            "desc",
                            serverAfterId,
                            serverBeforeId
                    ).execute();

            if (response.isSuccessful() && response.body() != null) {
                List<FieldObservationData> data = Arrays.asList(response.body().getData());
                if (!data.isEmpty()) {
                    saveToLocalDatabase(data);
                }

                // Resume download if there are new data on the server compared to local data
                // In other cases we will download data when the user scrolls down the recycler view
                boolean hasNextPage = response.body().getMeta().getCurrentPage() < response.body().getMeta().getLastPage();
                if (hasNextPage && (serverUpdatedAt != null || serverAfterId != null)) {

                    Data nextInput = new Data.Builder()
                            .putBoolean("isSyncOnScroll", false)
                            .putLong("observationsUpdatedAt", updatedAt)
                            .putLong("currentSyncTimestamp", currentSyncTimestamp)
                            .putLong("afterId", afterId)
                            .putInt("page", page + 1)
                            .build();

                    OneTimeWorkRequest nextRequest = new OneTimeWorkRequest.Builder(ObservationsDownloadWorker.class)
                            .setInputData(nextInput)
                            .addTag("OBSERVATIONS_DOWNLOAD_UPDATED_AT")
                            .addTag("UPDATED_AT_SYNC")
                            .build();

                    WorkManager.getInstance(getApplicationContext())
                            .enqueueUniqueWork("observation_sync_chain",
                                    ExistingWorkPolicy.APPEND, nextRequest);
                } else {
                    if (!isSyncOnScroll && currentSyncTimestamp != -1) {
                        SettingsManager.setObservationsUpdatedAt(currentSyncTimestamp);
                        Log.d(TAG, "Observations sync completed. Updating timestamp to: " + currentSyncTimestamp);
                    }
                }

                // Prepare data to return
                long[] idsArray = new long[addedObservationIds.size()];
                int i = 0;
                for (Long id : addedObservationIds) {
                    idsArray[i++] = id;
                }

                Data output = new Data.Builder()
                        .putLongArray("updatedObservationIds", idsArray)
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

    private void saveToLocalDatabase(List<FieldObservationData> dataList) {
        if (dataList == null || dataList.isEmpty()) return;

        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        Box<PhotoDb> photoBox = App.get().getBoxStore().boxFor(PhotoDb.class);

        App.get().getBoxStore().runInTx(() -> {
            for (FieldObservationData data : dataList) {

                EntryDb existing = ObjectBoxHelper.getObservationByServerId(data.getId());
                EntryDb entryToUse;

                if (existing == null) {
                    //
                    // Path 1: New observation. Not in ObjectBox.
                    //
                    try {
                        // Part 1: Save the new observation
                        EntryDb newEntry = EntryDb.getEntryFieldsFromData(data);
                        long localId = box.put(newEntry);
                        entryToUse = newEntry;
                        entryToUse.setId(localId);

                        Log.d(TAG, "Saving server ID " + data.getId() + " locally (ObjectBox ID " + localId + ").");

                        addedObservationIds.add(localId);

                        // Part 2: Save the photos
                        if (data.getPhotos() != null && !data.getPhotos().isEmpty()) {
                            Log.d(TAG, "Saving " + data.getPhotos().size() + " photos for entry " + localId + ".");

                            for (FieldObservationDataPhotos photoData : data.getPhotos()) {

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

                            box.put(entryToUse);
                        }

                        // Part 3: Save the activity log
                        List<FieldObservationDataActivity> activityItems = data.getActivity();
                        if (activityItems != null && !activityItems.isEmpty()) {
                            entryToUse.observationActivity.clear(); // just in case...

                            for (FieldObservationDataActivity activity : activityItems) {
                                ObservationActivityDb activityLog = FieldObservationDataActivity
                                        .getObservationActivityDb(activity);

                                activityLog.entry.setTarget(entryToUse);
                                entryToUse.observationActivity.add(activityLog);
                            }

                            box.put(entryToUse);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error saving to ObjectBox: " + data.getId(), e);
                    }
                } else {
                    //
                    // Path 2: Existing observation already stored in ObjectBox.
                    //
                    if (data.getActivity().size() > existing.observationActivity.size()) {
                        Log.d(TAG, "Server has " + data.getActivity().size() + " activities. Updating local entry " + data.getId());

                        // Part 1. Update observation
                        EntryDb.syncEntryFieldsFromData(existing, data);
                        addedObservationIds.add(existing.getId());

                        // Part 2: Update photos
                        List<FieldObservationDataPhotos> serverPhotos = data.getPhotos();
                        List<PhotoDb> objectBoxPhotos = existing.photos;

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

                                    // Remove the relationship from the ObjectBox ToMany list
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

                                    // Update ObjectBox if already exists
                                    if (photoData.getLicense() != null) {
                                        int serverLicenseId = photoData.getLicense().getId();
                                        if (photoDb.getLicenseId() != serverLicenseId) {
                                            Log.d(TAG, "Updating license for photo " + photoDb.getServerId() + " to " + serverLicenseId);
                                            photoDb.setLicenseId(serverLicenseId);
                                            App.get().getBoxStore().boxFor(PhotoDb.class).put(photoDb);
                                        }
                                    }

                                    break;
                                }
                            }

                            if (!existsLocally) {
                                PhotoDb photo = PhotoDb.getPhotoFields(photoData);
                                photo.entry.setTarget(existing);
                                objectBoxPhotos.add(photo);
                                Log.d(TAG, "Adding new photo " + photo.id
                                        + ", ServerID=" + photo.getServerId()
                                        + ", ServerURL=" + photo.getServerUrl()
                                        + ", license=" + photo.getLicenseId());
                            }
                        }

                        // Part 3. Update activity log
                        existing.observationActivity.clear();
                        for (FieldObservationDataActivity activity : data.getActivity()) {
                            ObservationActivityDb activityLog = FieldObservationDataActivity.getObservationActivityDb(activity);
                            activityLog.entry.setTarget(existing);
                            existing.observationActivity.add(activityLog);
                        }

                        box.put(existing);
                        Log.d(TAG, "Entry " + data.getId() + " synchronized with server state.");
                    } else {
                        Log.d(TAG, "Entry " + data.getId() + " is up to date. Skipping.");
                    }
                }
            }
        });

    }
}
