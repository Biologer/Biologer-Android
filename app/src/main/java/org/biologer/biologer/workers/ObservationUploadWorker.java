package org.biologer.biologer.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.helpers.ObjectBoxHelper;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.json.APIEntry;
import org.biologer.biologer.network.json.APIEntryPhotos;
import org.biologer.biologer.network.json.APIEntryResponse;
import org.biologer.biologer.network.json.UploadFileResponse;
import org.biologer.biologer.sql.EntryDb;

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
            List<APIEntryPhotos> uploadedPhotos = new ArrayList<>();
            List<String> localPhotos = getLocalPhotoPaths(entry);

            for (String path : localPhotos) {
                APIEntryPhotos photo = uploadPhotoSync(entry, path);
                if (photo != null) {
                    uploadedPhotos.add(photo);
                }
            }

            // Part 2. Upload observation
            APIEntry api = new APIEntry();
            api.getFromEntryDb(entry);
            api.setPhotos(uploadedPhotos);

            Response<APIEntryResponse> response = RetrofitClient
                    .getService(SettingsManager.getDatabaseName())
                    .uploadEntry(api)
                    .execute();

            if (response.code() == 429) {
                return Result.retry();
            }

            if (response.isSuccessful()) {
                ObjectBoxHelper.removeObservationById(entryId);
                return Result.success();
            }
            return Result.retry();
        } catch (Exception e) {
            Log.e(TAG, "Upload failed for entry " + entryId, e);
            return Result.retry();
        }
    }

    private List<String> getLocalPhotoPaths(EntryDb entry) {
        List<String> images = new ArrayList<>();
        if (entry.getSlika1() != null && !entry.getSlika1().isEmpty()) images.add(entry.getSlika1());
        if (entry.getSlika2() != null && !entry.getSlika2().isEmpty()) images.add(entry.getSlika2());
        if (entry.getSlika3() != null && !entry.getSlika3().isEmpty()) images.add(entry.getSlika3());
        return images;
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
            APIEntryPhotos photo = new APIEntryPhotos();
            photo.setPath(response.body().getFile());
            photo.setLicense(entry.getImageLicence());
            return photo;
        } else {
            throw new Exception("Error uploading photo: " + response.code());
        }
    }
}
