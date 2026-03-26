package org.biologer.biologer.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.biologer.biologer.App;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.json.FieldObservationData;
import org.biologer.biologer.network.json.FieldObservationDataPhotos;
import org.biologer.biologer.network.json.FieldObservationDataTypes;
import org.biologer.biologer.network.json.FieldObservationResponse;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.EntryDb_;
import org.biologer.biologer.sql.PhotoDb;
import org.biologer.biologer.sql.PhotoDb_;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.objectbox.Box;
import io.objectbox.query.Query;
import retrofit2.Response;

public class DataSyncWorker extends Worker {
    private static final String TAG = "Biologer.DataSyncWorker";

    public DataSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        long beforeId = getInputData().getLong("beforeId", -1);
        Long serverBeforeId = (beforeId != -1) ? beforeId : null;

        String database = SettingsManager.getDatabaseName();
        if (database == null) return Result.failure();

        try {
            Response<FieldObservationResponse> response = RetrofitClient.getService(database)
                    .getMyFieldObservations(1,
                            25,
                            null,
                            "id",
                            "desc",
                            null,
                            serverBeforeId)
                    .execute();

            if (response.isSuccessful() && response.body() != null) {
                List<FieldObservationData> data = Arrays.asList(response.body().getData());
                if (!data.isEmpty()) {
                    saveToLocalDatabase(data);
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
                        Log.d(TAG, "Saving field observation id " + data.getId() + " to ObjectBox!");
                        Set<Long> observationTypeIds = new HashSet<>();
                        if (data.getTypes() != null && !data.getTypes().isEmpty()) {
                            for (FieldObservationDataTypes type : data.getTypes()) {
                                observationTypeIds.add(type.getId());
                            }
                        }

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
                                String.valueOf(data.isFoundDead()),
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
                                observationTypeIds.toString()
                        );

                        long localId = box.put(newEntry);
                        entryToUse = newEntry;
                        entryToUse.setId(localId);

                        Log.d(TAG, "Saving server ID " + data.getId() + " locally (ObjectBox ID " + localId + ").");

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
                                    box.put(entryToUse);

                                } else {
                                    String path = existingPhoto.getLocalPath();
                                    if (path == null || path.isEmpty() || !new File(path.replace("file://", "")).exists()) {
                                        Log.d(TAG, "Photo exists in DB but file is missing. Resetting path for re-download.");
                                        existingPhoto.setLocalPath(null);
                                        photoBox.put(existingPhoto);
                                    }
                                }

                            }
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error saving to ObjectBox: " + data.getId(), e);
                    }
                } else {
                    Log.d(TAG, "Not saving id " + data.getId() + " to ObjectBox, already there!");
                }
            }
        });

    }
}
