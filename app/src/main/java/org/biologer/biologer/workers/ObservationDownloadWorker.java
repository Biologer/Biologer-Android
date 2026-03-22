package org.biologer.biologer.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.biologer.biologer.helpers.ObservationsHelper;

import java.util.concurrent.CountDownLatch;

public class ObservationDownloadWorker extends Worker {
        public ObservationDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {true};

            ObservationsHelper.downloadAllMyObservations(1, new ObservationsHelper.SyncCallback() {
                @Override
                public void onPageDownloaded(int current, int total) {
                    setProgressAsync(new Data.Builder().putInt("progress", current).putInt("total", total).build());
                }

                @Override
                public void onFinished() {
                    latch.countDown();
                }

                @Override
                public void onError(String message) {
                    success[0] = false;
                    latch.countDown();
                }
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                return Result.retry();
            }

            return success[0] ? Result.success() : Result.retry();
        }
}
