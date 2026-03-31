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
import org.biologer.biologer.helpers.ObjectBoxHelper;
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
                    // Path 1: New observation. Not in ObjectBox.
                    try {
                        // Part 1: Save the new observation
                        EntryDb newEntry = getEntryFields(data);
                        long localId = box.put(newEntry);
                        entryToUse = newEntry;
                        entryToUse.setId(localId);

                        Log.d(TAG, "Saving server ID " + data.getId() + " locally (ObjectBox ID " + localId + ").");

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
                                    PhotoDb photo = getPhotoFields(photoData);
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
                    // Path 2: Existing observation already stored in ObjectBox.
                    if (data.getActivity().size() > existing.observationActivity.size()) {
                        Log.d(TAG, "Server has " + data.getActivity().size() + " activities. Updating local entry " + data.getId());

                        // Part 1. Update observation
                        syncEntryFields(existing, data);

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
                                    break;
                                }
                            }

                            if (!existsLocally) {
                                PhotoDb photo = getPhotoFields(photoData);
                                photo.entry.setTarget(existing);
                                objectBoxPhotos.add(photo);
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

    private static PhotoDb getPhotoFields(FieldObservationDataPhotos data) {
        PhotoDb photo = new PhotoDb();
        photo.setServerId(data.getId());
        photo.setServerUrl(data.getUrl());
        photo.setLocalPath(null);
        photo.setServerPath(data.getPath());
        photo.setAuthor(data.getAuthor());
        photo.setLicenseId(data.getLicense().getId());

        return photo;
    }

    private static EntryDb getEntryFields(FieldObservationData data) {
        return new EntryDb(
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

        int imageLicense = ObjectBoxHelper.getImageLicense();
        if (!serverData.getPhotos().isEmpty()) {
            imageLicense = serverData.getPhotos().get(0).getLicense().getId();
        }

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
        existing.setImageLicence(imageLicense);
        existing.setObservationTypeIds(getObservationTypeIds(serverData));
    }
}
