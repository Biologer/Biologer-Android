package org.biologer.biologer.services;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.biologer.biologer.network.UpdateUnreadNotifications;

public class UnreadNotificationsWorker extends Worker {

    public UnreadNotificationsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        Intent update = new Intent(ctx, UpdateUnreadNotifications.class);
        update.putExtra("download", true);
        ctx.startService(update);
        return Result.success();
    }
}
