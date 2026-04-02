package org.biologer.biologer.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class TimedCountsDownloadWorker extends Worker {
    private static final String TAG = "Biologer.TimedCountsWorker";

    public TimedCountsDownloadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
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


        return Result.success();
    }
}
