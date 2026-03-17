package org.biologer.biologer.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.biologer.biologer.adapters.LandingFragmentItems;

import java.util.ArrayList;
import java.util.List;

public class UploadWorker extends Worker {

    public UploadWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params
    ) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        List<LandingFragmentItems> entries =
                LandingFragmentItems.loadAllEntries(getApplicationContext());
        WorkManager wm = WorkManager.getInstance(getApplicationContext());
        List<OneTimeWorkRequest> timedCountRequests = new ArrayList<>();
        List<OneTimeWorkRequest> observationRequests = new ArrayList<>();

        for (LandingFragmentItems item : entries) {
            Data data = new Data.Builder()
                    .putLong("observation_id", item.getObservationId())
                    .putInt("timed_count_id", item.getTimedCountId() == null ? -1 : item.getTimedCountId())
                    .build();

            if (item.getTimedCountId() != null) {
                timedCountRequests.add(new OneTimeWorkRequest.Builder(TimedCountUploadWorker.class)
                        .setInputData(data)
                        .addTag("UPLOAD_WORK")
                        .build());
            } else {
                observationRequests.add(new OneTimeWorkRequest.Builder(ObservationUploadWorker.class)
                        .setInputData(data)
                        .addTag("UPLOAD_WORK")
                        .build());
            }
        }

        if (timedCountRequests.isEmpty() && observationRequests.isEmpty()) {
            return Result.success();
        }

        WorkContinuation continuation;
        if (!timedCountRequests.isEmpty()) {
            continuation = wm.beginWith(timedCountRequests);
            if (!observationRequests.isEmpty()) {
                continuation = continuation.then(observationRequests);
            }
        } else {
            continuation = wm.beginWith(observationRequests);
        }

        continuation.enqueue();
        return Result.success();
    }
}