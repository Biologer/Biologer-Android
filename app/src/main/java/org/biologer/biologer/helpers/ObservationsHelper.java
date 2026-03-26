package org.biologer.biologer.helpers;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

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
import org.biologer.biologer.workers.PhotoDownloadWorker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.objectbox.Box;
import io.objectbox.query.Query;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ObservationsHelper {
    private static final String TAG = "Biologer.ObsHelper";

    // direction should be "desc" = descending, "asc" = ascending
    public static void fetchMyObservations(int page, Integer per_page, String timestamp, String direction, Long beforeId, Long afterId, ObservationPageCallback callback) {
        String database = SettingsManager.getDatabaseName();
        if (database == null) {return;}

        RetrofitClient.getService(database)
                .getMyFieldObservations(page, per_page, timestamp, "id", direction, afterId, beforeId)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<FieldObservationResponse> call, @NonNull Response<FieldObservationResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            FieldObservationResponse body = response.body();
                            List<FieldObservationData> data = Arrays.asList(body.getData());
                            if (!data.isEmpty()) {
                                saveToLocalDatabase(data);
                            }

                            boolean hasNext = body.getMeta().getCurrentPage() < body.getMeta().getLastPage();
                            callback.onSuccess(data, hasNext, body.getMeta().getLastPage());

                        } else {
                            callback.onError("HTTP " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<FieldObservationResponse> call, @NonNull Throwable t) {
                        callback.onError(t.getLocalizedMessage());
                    }
                });
    }

    public static void downloadAllMyObservations(int currentPage, SyncCallback syncCallback) {
        fetchMyObservations(currentPage, 25, null, null, null, null, new ObservationPageCallback() {
            @Override
            public void onSuccess(List<FieldObservationData> data, boolean hasNextPage, int totalPages) {
                syncCallback.onPageDownloaded(currentPage, totalPages);

                if (hasNextPage) {
                    downloadAllMyObservations(currentPage + 1, syncCallback);
                } else {
                    syncCallback.onFinished();
                }
            }

            @Override
            public void onError(String error) {
                syncCallback.onError(error);
            }
        });
    }

    public interface ObservationPageCallback {
        void onSuccess(List<FieldObservationData> data, boolean hasNextPage, int totalPages);
        void onError(String error);
    }

    public interface SyncCallback {
        void onPageDownloaded(int current, int total);
        void onFinished();
        void onError(String message);
    }

    public interface ObservationFetchCallback {
        void onSuccess(FieldObservationData data);
        void onError(String error);
    }

    public static void fetchObservation(long observationId, ObservationFetchCallback callback) {
        Call<FieldObservationResponse> call = RetrofitClient
                .getService(SettingsManager.getDatabaseName())
                .getFieldObservation(String.valueOf(observationId));

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<FieldObservationResponse> call, @NonNull Response<FieldObservationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    FieldObservationData[] dataArray = response.body().getData();
                    if (dataArray != null && dataArray.length > 0) {
                        callback.onSuccess(dataArray[0]);
                    } else {
                        callback.onError("No data found for ID: " + observationId);
                    }
                } else {
                    callback.onError("Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<FieldObservationResponse> call, @NonNull Throwable t) {
                callback.onError(t.getLocalizedMessage());
            }
        });
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
                        newEntry.setId(localId);
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

        Log.d(TAG, "Now this should start the worker...");
        // Download all photos using worker
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest downloadWork = new OneTimeWorkRequest.Builder(PhotoDownloadWorker.class)
                .setConstraints(constraints)
                .addTag("PHOTO_DOWNLOAD")
                .build();

        WorkManager.getInstance(App.get()).enqueueUniqueWork(
                "photo_sync",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                downloadWork
        );

    }

}
