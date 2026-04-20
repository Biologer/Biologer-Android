package org.biologer.biologer.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.helpers.ObjectBoxHelper;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.json.APITimedCounts;
import org.biologer.biologer.network.json.APITimedCountsResponse;
import org.biologer.biologer.sql.TimedCountDb;

import java.io.IOException;

import retrofit2.Response;

public class TimeCountsUploadWorker extends Worker {
    private static final String TAG = "Biologer.UploadTC";

    public TimeCountsUploadWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params
    ) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        long timedCountId = getInputData().getLong("timed_count_id", -1);
        if (timedCountId == -1) {
            return Result.success();
        }

        try {
            TimedCountDb timedCount = ObjectBoxHelper.getTimeCountById(timedCountId);
            if (timedCount == null) return Result.success();

            APITimedCounts api = new APITimedCounts();
            api.getFromTimedCountDatabase(timedCount);
            Log.d(TAG, "TIME COUNT API start time: " + api.getStartTime());

            Response<APITimedCountsResponse> response;
            if (timedCount.getServerId() != null && timedCount.isUploaded()) {
                Log.d(TAG, "Time count modified, should reupload.");
                response = RetrofitClient.getService(SettingsManager.getDatabaseName())
                        .updateTimedCount(timedCount.getServerId(), api)
                        .execute();
            } else {
                Log.d(TAG, "New time count, should upload.");
                response = RetrofitClient.getService(SettingsManager.getDatabaseName())
                        .uploadTimedCount(api)
                        .execute();
            }

            int code = response.code();
            if (code == 429 || (code >= 500 && code <= 599)) {
                return Result.retry();
            }

            if (response.isSuccessful() && response.body() != null) {
                long newServerId = response.body().getData().getId();

                timedCount.setServerId(newServerId);
                timedCount.setUploaded(true);
                timedCount.setModified(false);

                long localId = ObjectBoxHelper.setTimedCount(timedCount);

                Log.d(TAG, "TimedCount " + timedCountId
                        + " (returned local ID " + localId +
                        ") synchronised. Server ID: " + newServerId);
                Data output = new Data.Builder()
                        .putLong("uploadedTimeCountId", timedCountId)
                        .build();
                return Result.success(output);
            }
            String errorBody = !response.message().isEmpty()
                    ? response.message()
                    : "Unknown error!";
            Data errorData = new Data.Builder()
                    .putInt("error_code", code)
                    .putString("error_message", errorBody)
                    .build();
            Log.e(TAG, "Error: " + errorBody);
            return Result.failure(errorData);

        } catch (IOException e) {
            // Connection lost (Retry)
            Log.e(TAG, "Network connection lost during time count upload", e);
            return Result.retry();
        } catch (Exception e) {
            // Other error
            Log.e(TAG, "Upload failed for entry " + timedCountId + ": " + e);
            Data errorData = new Data.Builder()
                    .putInt("error_code", 0)
                    .putString("error_message", e.toString())
                    .build();
            return Result.failure(errorData);
        }
    }

}
