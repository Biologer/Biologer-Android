package org.biologer.biologer.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.biologer.biologer.App;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.helpers.ObjectBoxHelper;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.json.APIEntry;
import org.biologer.biologer.network.json.APIEntryPhotos;
import org.biologer.biologer.network.json.APIEntryResponse;
import org.biologer.biologer.network.json.FieldObservationDataActivity;
import org.biologer.biologer.network.json.UploadFileResponse;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.ObservationActivityDb;
import org.biologer.biologer.sql.PhotoDb;
import org.biologer.biologer.sql.TimedCountDb;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

public class ObservationUploadWorker extends Worker {

    private static final String TAG = "Biologer.UploadObs";

    public ObservationUploadWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params
    ) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        long entryId = getInputData().getLong("observation_id", -1);
        EntryDb entry = ObjectBoxHelper.getObservationById(entryId);

        if (entry == null) {
            return Result.success();
        }

        try {
            // Part 1. Upload photos
            List<APIEntryPhotos> photosForApi = new ArrayList<>();

            for (PhotoDb photoDb : entry.photos) {
                if (photoDb.getServerId() > 0) {
                    // Image uploaded, sending its ID
                    APIEntryPhotos existingPhoto = new APIEntryPhotos();
                    existingPhoto.setId(photoDb.getServerId());
                    existingPhoto.setPath(null);
                    existingPhoto.setLicense(entry.getImageLicence());
                    photosForApi.add(existingPhoto);
                    Log.d(TAG, "Image already on server, sending ID: " + photoDb.getServerId());
                } else {
                    // New image, should be uploaded first
                    APIEntryPhotos newPhoto = uploadPhotoSync(entry, photoDb.getLocalPath());
                    if (newPhoto != null) {
                        photosForApi.add(newPhoto);
                    }
                }
            }

            // Part 2. Upload observation
            APIEntry api = new APIEntry();
            api.getFromEntryDb(entry);
            api.setPhotos(photosForApi);

            // Link with parent Timed Count
            if (entry.getTimedCoundId() != null && entry.getTimedCoundId() != -1) {
                TimedCountDb parentTimeCount = ObjectBoxHelper.getTimedCountById(entry.getTimedCoundId());

                if (parentTimeCount != null && parentTimeCount.isUploaded() && parentTimeCount.getServerId() != null) {
                    // Set the server ID now
                    api.setTimedCountId(parentTimeCount.getServerId().intValue());
                    Log.d(TAG, "Linking observation " + entryId + " to server TimedCount ID: " + parentTimeCount.getServerId());
                } else {
                    Log.w(TAG, "Parent TimedCount not yet uploaded. Retrying later...");
                    return Result.retry();
                }
            }

            Response<APIEntryResponse> response;
            if (entry.getServerId() != null && entry.getServerId() != 0) {
                // Update
                Integer userId = ObjectBoxHelper.getUserId();
                api.setObservedById(userId);
                api.setIdentifiedById(userId);
                api.setReason("User updated observation via Android app");
                Log.d(TAG, "Updating existing field observation with server ID: " + entry.getServerId());
                response = RetrofitClient.getService(SettingsManager.getDatabaseName())
                        .updateEntry(entry.getServerId(), api)
                        .execute();
            } else {
                // Create
                Log.d(TAG, "Uploading new field observation.");
                response = RetrofitClient.getService(SettingsManager.getDatabaseName())
                        .uploadEntry(api)
                        .execute();
            }

            if (response.code() == 429) {
                return Result.retry();
            }

            if (response.isSuccessful() && response.body() != null) {
                APIEntryResponse.Data responseData = response.body().getData();
                Long serverId = responseData.getId();

                // Part 1: Update the entry itself
                entry.setServerId(serverId);
                entry.setUploaded(true);
                entry.setModified(false);

                // Part 2: Update the photos with the ID and URL received from the server
                List<APIEntryResponse.PhotoResponseData> photosFromServer = responseData.getPhotos();
                if (photosFromServer != null && !photosFromServer.isEmpty()) {

                    for (PhotoDb localPhoto : entry.photos) {
                        String localFileName = new File(localPhoto.getLocalPath()).getName();

                        for (APIEntryResponse.PhotoResponseData serverPhoto : photosFromServer) {
                            Log.d(TAG, "Local: " + localFileName + "; Server: " + serverPhoto.getPath());
                            if (serverPhoto.getPath() != null && serverPhoto.getPath().contains(localFileName)) {
                                localPhoto.setServerId(serverPhoto.getId());
                                localPhoto.setServerPath(serverPhoto.getPath());
                                localPhoto.setServerUrl(serverPhoto.getUrl());
                                Log.d(TAG, "Photo updated: Local " + localFileName + " now has Server ID " + serverPhoto.getId());
                                break;
                            }
                        }
                    }
                }

                // Part 3: Update activity logs from server response
                List<FieldObservationDataActivity> activityItems = responseData.getActivity();
                if (activityItems != null && !activityItems.isEmpty()) {

                    App.get().getBoxStore().runInTx(() -> {
                        // Clear existing activity from ObjectBox
                        entry.observationActivity.clear();

                        for (FieldObservationDataActivity activity : activityItems) {
                            ObservationActivityDb activityLog = FieldObservationDataActivity
                                    .getObservationActivityDb(activity);

                            activityLog.entry.setTarget(entry);
                            entry.observationActivity.add(activityLog);
                        }

                        App.get().getBoxStore().boxFor(EntryDb.class).put(entry);
                    });
                }

                ObjectBoxHelper.setObservation(entry);

                Log.d(TAG, "Entry " + entryId + " successfully synced with server ID: " + serverId);
                Data output = new Data.Builder()
                        .putLong("updatedObservationId", entryId)
                        .build();
                return Result.success(output);
            }
            return Result.retry();
        } catch (Exception e) {
            Log.e(TAG, "Upload failed for entry " + entryId, e);
            return Result.retry();
        }
    }

    private APIEntryPhotos uploadPhotoSync(EntryDb entry, String path) throws Exception {
        File file = new File(getApplicationContext().getFilesDir(), new File(path).getName());
        if (!file.exists()) {
            Log.e(TAG, "There is no file at the path: " + file.getAbsolutePath());
            return null;
        }

        RequestBody reqFile = RequestBody.create(file, MediaType.parse("image/*"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), reqFile);
        Response<UploadFileResponse> response = RetrofitClient
                .getService(SettingsManager.getDatabaseName())
                .uploadFile(body)
                .execute();

        if (response.code() == 429) {
            throw new Exception("429");
        }

        if (response.isSuccessful() && response.body() != null) {
            int license = (entry.getImageLicence() == 0) ? ObjectBoxHelper.getImageLicense(): entry.getImageLicence();
            APIEntryPhotos photo = new APIEntryPhotos();
            photo.setPath(response.body().getFile());
            photo.setLicense(license);
            return photo;
        } else {
            throw new Exception("Error uploading photo: " + response.code());
        }
    }
}
