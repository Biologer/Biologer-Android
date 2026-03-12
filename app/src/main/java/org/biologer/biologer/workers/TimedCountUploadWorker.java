package org.biologer.biologer.workers;

import android.content.Context;

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
        int timedCountId = getInputData().getInt("timed_count_id", -1);
        if (timedCountId == -1) {
            return Result.success();
        }

        try {
            TimedCountDb timed = ObjectBoxHelper.getTimedCountById(timedCountId);
            if (timed == null)
                return Result.success();

            APITimedCounts api = new APITimedCounts();
            api.getFromTimedCountDatabase(timed);

            Response<APITimedCountsResponse> response =
                    RetrofitClient.getService(SettingsManager.getDatabaseName())
                            .uploadTimedCount(api)
                            .execute();

            if (response.isSuccessful() && response.body() != null) {
                long serverId = response.body().getData().getId();
                List<EntryDb> children = ObjectBoxHelper.getTimedCountObservations(timedCountId);

                if (!children.isEmpty()) {
                    for (EntryDb obs : children) {
                        obs.setTimedCoundId((int) serverId);
                        ObjectBoxHelper.setObservation(obs);
                    }
                }

                ObjectBoxHelper.removeTimedCountById(timedCountId);
                return Result.success();
            } else {
                return Result.retry();
            }
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
