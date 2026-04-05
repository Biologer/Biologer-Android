package org.biologer.biologer.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.biologer.biologer.App;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.EntryDb_;
import org.biologer.biologer.sql.TimedCountDb;
import org.biologer.biologer.sql.TimedCountDb_;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.objectbox.query.Query;

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
        WorkManager wm = WorkManager.getInstance(getApplicationContext());

        // 1. Get the timed counts first
        List<TimedCountDb> pendingTimedCounts;
        try (Query<TimedCountDb> queryTimedCounts = App.get().getBoxStore().boxFor(TimedCountDb.class)
                .query()
                .equal(TimedCountDb_.uploaded, false)
                .or()
                .equal(TimedCountDb_.modified, true)
                .build()) {
            pendingTimedCounts = queryTimedCounts.find();
        }

        // 2. Get the species observations
        List<EntryDb> pendingEntries;
        try (Query<EntryDb> queryEntries = App.get().getBoxStore().boxFor(EntryDb.class)
                .query()
                .equal(EntryDb_.uploaded, false)
                .or()
                .equal(EntryDb_.modified, true)
                .build()) {
            pendingEntries = queryEntries.find();
        }

        if (pendingTimedCounts.isEmpty() && pendingEntries.isEmpty()) {
            return Result.success();
        }

        WorkContinuation continuation = null;

        // Upload Timed Counts first!
        if (!pendingTimedCounts.isEmpty()) {
            List<OneTimeWorkRequest> tcRequests = new ArrayList<>();
            for (int i = 0; i < pendingTimedCounts.size(); i++) {
                TimedCountDb timeCount = pendingTimedCounts.get(i);
                Data data = new Data.Builder()
                        .putLong("timed_count_id", timeCount.getId())
                        .build();
                tcRequests.add(new OneTimeWorkRequest.Builder(TimedCountUploadWorker.class)
                        .setInputData(data)
                        .addTag("UPLOAD_WORK")
                        .setInitialDelay(i * 500L, TimeUnit.MILLISECONDS) // Delay next worker for 0.5s
                        .setBackoffCriteria(
                                BackoffPolicy.EXPONENTIAL,
                                30,
                                TimeUnit.SECONDS)
                        .build());
            }
            continuation = wm.beginWith(tcRequests);
        }

        // After Timed Counts are uploaded add the Observations
        if (!pendingEntries.isEmpty()) {
            List<OneTimeWorkRequest> entryRequests = new ArrayList<>();
            for (int i = 0; i < pendingEntries.size(); i++) {
                EntryDb entry = pendingEntries.get(i);
                Data data = new Data.Builder()
                        .putLong("observation_id", entry.getId())
                        .build();
                entryRequests.add(new OneTimeWorkRequest.Builder(ObservationUploadWorker.class)
                        .setInputData(data)
                        .addTag("UPLOAD_WORK")
                        .setInitialDelay(i * 500L, TimeUnit.MILLISECONDS) // Delay next worker for 0.5s
                        .setBackoffCriteria(
                                BackoffPolicy.EXPONENTIAL,
                                30,
                                TimeUnit.SECONDS)
                        .build());
            }

            if (continuation == null) {
                continuation = wm.beginWith(entryRequests);
            } else {
                continuation = continuation.then(entryRequests);
            }
        }

        continuation.enqueue();

        return Result.success();
    }
}