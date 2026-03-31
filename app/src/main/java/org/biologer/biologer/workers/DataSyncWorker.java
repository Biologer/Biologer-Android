package org.biologer.biologer.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.biologer.biologer.App;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.json.FieldObservationData;
import org.biologer.biologer.network.json.FieldObservationDataActivity;
import org.biologer.biologer.network.json.FieldObservationDataPhotos;
import org.biologer.biologer.network.json.FieldObservationDataTypes;
import org.biologer.biologer.network.json.FieldObservationResponse;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.EntryDb_;
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

public class DataSyncWorker extends Worker {
    private static final String TAG = "Biologer.DataSyncWorker";
    boolean firstSync = false;

    public DataSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
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

        // For the first sync we just reset the serverUpdatedAt and serverAfterId to null
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
                            .putLong("updatedAt", updatedAt)
                            .putLong("currentSyncTimestamp", currentSyncTimestamp)
                            .putLong("afterId", afterId)
                            .putInt("page", page + 1)
                            .build();

                    OneTimeWorkRequest nextRequest = new OneTimeWorkRequest.Builder(DataSyncWorker.class)
                            .setInputData(nextInput)
                            .addTag("DATA_SYNC_UPDATED_AT")
                            .build();

                    WorkManager.getInstance(getApplicationContext())
                            .enqueue(nextRequest);
                } else {
                    if (!isSyncOnScroll && currentSyncTimestamp != -1) {
                        SettingsManager.setObservationsUpdatedAt(currentSyncTimestamp);
                        Log.d(TAG, "Observations sync completed. Updating timestamp to: " + currentSyncTimestamp);
                    }
                }

                return Result.success();
            } else {
                return Result.retry();
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error during sync", e);
            return Result.retry();
        }
    }

    private static void saveToLocalDatabase(List<FieldObservationData> dataList) {
        if (dataList == null || dataList.isEmpty()) return;

        Box<EntryDb> box = App.get().getBoxStore().boxFor(EntryDb.class);
        Box<PhotoDb> photoBox = App.get().getBoxStore().boxFor(PhotoDb.class);

        App.get().getBoxStore().runInTx(() -> {
            for (FieldObservationData data : dataList) {

                EntryDb existing;
                try (Query<EntryDb> query = box.query(EntryDb_.serverId.equal(data.getId())).build()) {
                    existing = query.findFirst();
                } catch (Exception e) {
                    Log.e(TAG, "Error in ObjectBox query for serverId: " + data.getId(), e);
                    continue;
                }

                EntryDb entryToUse;

                if (existing == null) {
                    try {
                        // Part 1: Save the observation
                        Log.d(TAG, "Saving field observation id " + data.getId() + " to ObjectBox!");

                        EntryDb newEntry = new EntryDb(
                                0,
                                data.getId(),
                                true,
                                false,
                                data.getTaxonId() != null ? data.getTaxonId() : 0,
                                null,
                                data.getTaxonSuggestion(),
                                String.valueOf(data.getYear()),
                                String.valueOf(data.getMonth() - 1),
                                String.valueOf(data.getDay()),
                                data.getNote(),
                                data.getNumber(),
                                data.getSex(),
                                data.getStageId(),
                                data.getAtlasCode(),
                                String.valueOf(!data.isFoundDead()),
                                data.getFoundDeadNote(),
                                data.getLatitude(),
                                data.getLongitude(),
                                data.getAccuracy(),
                                data.getElevation(),
                                data.getLocation(),
                                null,
                                null,
                                null,
                                data.getProject(),
                                data.getFoundOn(),
                                String.valueOf(data.getDataLicense()),
                                0,
                                data.getTime(),
                                data.getHabitat(),
                                getObservationTypeIds(data)
                        );

                        long localId = box.put(newEntry);
                        entryToUse = newEntry;
                        entryToUse.setId(localId);

                        Log.d(TAG, "Saving server ID " + data.getId() + " locally (ObjectBox ID " + localId + ").");

                        // Part 2: Save the photos

                        if (data.getPhotos() != null && !data.getPhotos().isEmpty()) {
                            Log.d(TAG, "Saving " + data.getPhotos().size() + " photos for entry " + localId + ".");

                            for (FieldObservationDataPhotos photoData : data.getPhotos()) {

                                PhotoDb existingPhoto = photoBox.query()
                                        .equal(PhotoDb_.serverId, photoData.getId())
                                        .build()
                                        .findFirst();

                                if (existingPhoto == null) {
                                    PhotoDb photo = new PhotoDb();
                                    photo.setServerId(photoData.getId());
                                    photo.setServerUrl(photoData.getUrl());
                                    photo.setLocalPath(null);
                                    photo.setServerPath(photoData.getPath());
                                    photo.setAuthor(photoData.getAuthor());
                                    photo.setLicenseId(photoData.getLicense().getId());

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
                    if (data.getActivity().size() > existing.observationActivity.size()) {
                        Log.d(TAG, "Server has " + data.getActivity().size() + " activities. Updating local entry " + data.getId());

                        // Part 1. Update observation
                        syncEntryFields(existing, data);

                        // Part 2: Update photos
                        List<FieldObservationDataPhotos> serverPhotos = data.getPhotos();
                        List<PhotoDb> localPhotos = existing.photos;

                        if (serverPhotos == null) {
                            serverPhotos = new ArrayList<>();
                        }

                        Log.d(TAG, "Syncing photos: Server has " + serverPhotos.size() + ", Local has " + localPhotos.size());

                        for (int i = localPhotos.size() - 1; i >= 0; i--) {
                            PhotoDb lp = localPhotos.get(i);

                            // Only manage photos that have a serverId (don't touch pending local uploads)
                            if (lp.getServerId() != 0) {
                                boolean foundOnServer = false;
                                for (FieldObservationDataPhotos sp : serverPhotos) {
                                    if (sp.getId() == lp.getServerId()) {
                                        foundOnServer = true;
                                        break;
                                    }
                                }

                                if (!foundOnServer) {
                                    Log.d(TAG, "Photo ID " + lp.getServerId() + " was deleted on server. Cleaning up.");

                                    // Delete the physical file from internal storage
                                    String localPath = lp.getLocalPath();
                                    if (localPath != null && !localPath.isEmpty()) {
                                        // Remove the "file://" prefix if it exists to get a valid File path
                                        File file = new File(localPath.replace("file://", ""));
                                        if (file.exists()) {
                                            if (file.delete()) {
                                                Log.d(TAG, "Successfully deleted local file: " + localPath);
                                            } else {
                                                Log.e(TAG, "Failed to delete local file: " + localPath);
                                            }
                                        }
                                    }

                                    // Remove the relationship from the ObjectBox ToMany list
                                    localPhotos.remove(i);
                                }
                            }
                        }

                        for (FieldObservationDataPhotos sp : serverPhotos) {
                            boolean existsLocally = false;
                            for (PhotoDb lp : localPhotos) {
                                if (lp.getServerId() == sp.getId()) {
                                    existsLocally = true;
                                    break;
                                }
                            }

                            if (!existsLocally) {
                                PhotoDb newPhoto = new PhotoDb();
                                newPhoto.setServerId(sp.getId());
                                newPhoto.setServerUrl(sp.getUrl());
                                newPhoto.setServerPath(sp.getPath());
                                newPhoto.setAuthor(sp.getAuthor());
                                newPhoto.setLicenseId(sp.getLicense().getId());
                                newPhoto.entry.setTarget(existing);
                                localPhotos.add(newPhoto);
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


                    Log.d(TAG, "There are " + existing.observationActivity.size() + " activities in this ObjectBox entry!");
                    List<ObservationActivityDb> observationActivityDbs = existing.observationActivity;
                    for (ObservationActivityDb activity : observationActivityDbs) {
                        Log.d(TAG, "ObjectBox entry updated at: " + activity.createdAt);
                    }
                    Log.d(TAG, "There are " + data.getActivity().size() + " activities in JSON!");
                    List<FieldObservationDataActivity> fieldObservationDataActivities = data.getActivity();
                    for (FieldObservationDataActivity activity : fieldObservationDataActivities) {
                        Log.d(TAG, "Server data updated at: " + activity.getCreatedAt());
                    }

                    Log.d(TAG, "Not saving id " + data.getId() + " to ObjectBox, already there!");
                }
            }
        });

    }

    private static String getObservationTypeIds(FieldObservationData data) {
        Set<Long> observationTypeIds = new HashSet<>();
        if (data.getTypes() != null && !data.getTypes().isEmpty()) {
            for (FieldObservationDataTypes type : data.getTypes()) {
                observationTypeIds.add(type.getId());
            }
        }
        return observationTypeIds.toString();
    }

    private static void syncEntryFields(EntryDb existing, FieldObservationData serverData) {

        existing.setTaxonId(serverData.getTaxonId() != null ? serverData.getTaxonId() : 0);
        existing.setTaxonSuggestion(serverData.getTaxonSuggestion());
        existing.setYear(String.valueOf(serverData.getYear()));
        existing.setMonth(String.valueOf(serverData.getMonth() - 1));
        existing.setDay(String.valueOf(serverData.getDay()));
        existing.setTime(serverData.getTime());
        existing.setComment(serverData.getNote());
        existing.setNoSpecimens(serverData.getNumber());
        existing.setSex(serverData.getSex());
        existing.setStage(serverData.getStageId());
        existing.setAtlasCode(serverData.getAtlasCode());
        existing.setDeadOrAlive(String.valueOf(!serverData.isFoundDead()));
        existing.setCauseOfDeath(serverData.getFoundDeadNote());
        existing.setLattitude(serverData.getLatitude());
        existing.setLongitude(serverData.getLongitude());
        existing.setAccuracy(serverData.getAccuracy());
        existing.setElevation(serverData.getElevation());
        existing.setLocation(serverData.getLocation());
        existing.setProjectId(serverData.getProject());
        existing.setFoundOn(serverData.getFoundOn());
        existing.setHabitat(serverData.getHabitat());
        existing.setDataLicence(String.valueOf(serverData.getDataLicense()));
        existing.setImageLicence(existing.getImageLicence());
        existing.setObservationTypeIds(getObservationTypeIds(serverData));

    }
}
