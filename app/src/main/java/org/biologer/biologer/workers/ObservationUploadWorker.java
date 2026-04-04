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
import java.io.IOException;
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
                    int photoLicense = (photoDb.getLicenseId() == 0)
                            ? ObjectBoxHelper.getImageLicense()
                            : photoDb.getLicenseId();
                    existingPhoto.setLicense(photoLicense);
                    photosForApi.add(existingPhoto);
                    Log.d(TAG, "Image license. EntryDb=" + entry.getImageLicence()
                            + ", UserDb=" + ObjectBoxHelper.getImageLicense()
                            + ", PhotoDb=" + photoDb.getLicenseId());
                    Log.d(TAG, "Image already on server, sending ID: " + photoDb.getServerId());
                } else {
                    // New image, should be uploaded first
                    APIEntryPhotos newPhoto = uploadPhotoSync(photoDb);
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
            if (entry.getTimedCoundId() != null) {
                TimedCountDb parentTimeCount = ObjectBoxHelper.getTimedCountByServerId(entry.getTimedCoundId());

                if (parentTimeCount != null && parentTimeCount.isUploaded() && parentTimeCount.getServerId() != null) {
                    // Set the server ID now
                    api.setTimedCountId(parentTimeCount.getServerId().intValue());
                    Log.d(TAG, "Linking observation " + entryId + " to server TimedCount ID: " + parentTimeCount.getServerId());
                } else {
                    Log.w(TAG, "Parent TimedCount not yet uploaded. Retrying later...");
                    return Result.retry();
                }
            } else if (entry.getTimedCoundId() != null && entry.getTimedCoundId() < 0) {
                // Parent has a temporary negative ID = hasn't uploaded yet
                Log.w(TAG, "Parent TimedCount is still pending upload. Holding observation...");
                return Result.retry();
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

            int code = response.code();
            Log.d(TAG, "Server returned response code: " + code);
            if (code == 429 || (code >= 500 && code <= 599)) {
                return Result.retry();
            }

            if (response.isSuccessful() && response.body() != null) {
                APIEntryResponse.Data responseData = response.body().getData();
                Long serverId = responseData.getId();

                // Part 1: Update the entry itself
                entry.setServerId(serverId);
                entry.setUploaded(true);
                entry.setModified(false);

                // Part 2: Update the photos with the ID and URL 1received from the server
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
                        .putLong("uploadedObservationId", entryId)
                        .build();
                return Result.success(output);
            } else {
                String errorBody = !response.message().isEmpty()
                        ? response.message()
                        : "Unknown error!";
                Log.e(TAG, "Error: " + errorBody);
                Data errorData = new Data.Builder()
                        .putInt("error_code", code)
                        .putString("error_message", errorBody)
                        .build();
                return Result.failure(errorData);
            }
        } catch (IOException e) {
            // Connection lost (Retry)
            Log.e(TAG, "Network connection lost during observation upload", e);
            return Result.retry();
        } catch (Exception e) {
            // Other error
            Log.e(TAG, "Upload failed for entry " + entryId, e);
            Data errorData = new Data.Builder()
                    .putInt("error_code", 0)
                    .putString("error_message", e.toString())
                    .build();
            return Result.failure(errorData);
        }
    }

    private APIEntryPhotos uploadPhotoSync(PhotoDb photo) throws Exception {
        File file = new File(getApplicationContext().getFilesDir(), new File(photo.getLocalPath()).getName());
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
            int license = (photo.getLicenseId() == null || photo.getLicenseId() == 0)
                    ? ObjectBoxHelper.getImageLicense()
                    : photo.getLicenseId();
            APIEntryPhotos uploadedPhoto = new APIEntryPhotos();
            uploadedPhoto.setPath(response.body().getFile());
            uploadedPhoto.setLicense(license);
            return uploadedPhoto;
        } else {
            throw new Exception("Error uploading photo: " + response.code());
        }
    }
}
