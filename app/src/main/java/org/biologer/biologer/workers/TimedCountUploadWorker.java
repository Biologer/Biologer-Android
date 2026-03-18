package org.biologer.biologer.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.helpers.ObjectBoxHelper;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.json.APITimedCounts;
import org.biologer.biologer.network.json.APITimedCountsResponse;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.TimedCountDb;

import java.util.List;

import retrofit2.Response;

public class TimedCountUploadWorker extends Worker {
    private static final String TAG = "Biologer.UploadTC";

    public TimedCountUploadWorker(
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
            TimedCountDb timedCount = ObjectBoxHelper.getTimedCountById(timedCountId);
            if (timedCount == null) return Result.success();

            APITimedCounts api = new APITimedCounts();
            api.getFromTimedCountDatabase(timedCount);

            Response<APITimedCountsResponse> response;
            if (timedCount.getServerId() != null && timedCount.isUploaded()) {
                // Modified, should reupload
                response = RetrofitClient.getService(SettingsManager.getDatabaseName())
                        .updateTimedCount(timedCount.getServerId(), api)
                        .execute();
            } else {
                // New timed count, should upload
                response = RetrofitClient.getService(SettingsManager.getDatabaseName())
                        .uploadTimedCount(api)
                        .execute();
            }

            if (response.code() == 429) return Result.retry();

            if (response.isSuccessful() && response.body() != null) {
                long serverId = response.body().getData().getId();

                timedCount.setServerId(serverId);
                timedCount.setUploaded(true);
                timedCount.setModified(false);

                ObjectBoxHelper.setTimedCount(timedCount);

                Log.d(TAG, "TimedCount " + timedCountId + " synchronised. Server ID: " + serverId);
                return Result.success();
            } else {
                return Result.retry();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error uploading TimedCount", e);
            return Result.retry();
        }
    }

}
