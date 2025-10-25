package org.biologer.biologer.network;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ListenableFuture;

import org.biologer.biologer.App;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.json.UnreadNotification;
import org.biologer.biologer.network.json.UnreadNotificationsResponse;
import org.biologer.biologer.sql.UnreadNotificationsDb;
import org.biologer.biologer.sql.UnreadNotificationsDb_;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.objectbox.Box;
import io.objectbox.query.Query;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationSyncWorker extends ListenableWorker {
    private SettableFuture<Result> future;
    public static final String KEY_UPDATED_AFTER = "updated_after";
    public static final String SYNC_TAG = "notification_sync_tag";
    private static final String TAG = "Biologer.NotyUpdate";
    private int page = 1;
    private final int perPage = 10;
    public NotificationSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        future = SettableFuture.create();
        page = 1;

        downloadNotifications(page);
        return future;
    }

    public static void enqueueNow(Context context, long updatedAfter) {

        Data inputData = new Data.Builder()
                .putLong(KEY_UPDATED_AFTER, updatedAfter)
                .build();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(NotificationSyncWorker.class)
                .setInputData(inputData)
                .setConstraints(constraints)
                .addTag(SYNC_TAG)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                SYNC_TAG,
                ExistingWorkPolicy.REPLACE,
                syncRequest
        );
    }

    private void downloadNotifications(int page) {
        long timestamp = Long.parseLong(SettingsManager.getNotificationsUpdatedAt());
        long timestampNotification = getInputData().getLong(KEY_UPDATED_AFTER, 0L);
        if (timestamp >= timestampNotification) {
            Log.i(TAG, "Worker skipped — timestamp " + timestampNotification
                    + " <= than already updated timestamp " + timestamp + ".");
            completeWorker(Result.success());
        }
        String databaseName = SettingsManager.getDatabaseName();

        if (databaseName != null) {
            // Get new notifications from the API
            Call<UnreadNotificationsResponse> call
                    = RetrofitClient.getService(databaseName)
                    .getUnreadNotifications(page, perPage, timestamp);

            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<UnreadNotificationsResponse> call,
                                       @NonNull Response<UnreadNotificationsResponse> response) {
                    if (isStopped()) {
                        Log.w(TAG, "Worker stopped while waiting for network response. Aborting processing.");
                        return;
                    }

                    if (response.isSuccessful() && response.body() != null) {
                        saveNotificationsAngGetNext(response.body());
                    } else if (response.code() == 429) {
                        Log.w(TAG, "Rate limit hit, triggering WorkManager retry.");
                        completeWorker(Result.retry());
                    } else if (response.code() == 508) {
                        Log.w(TAG, "Server detected a loop, triggering WorkManager retry.");
                        completeWorker(Result.retry());
                    } else {
                        Log.e(TAG, "Unexpected response code: " + response.code());
                        completeWorker(Result.failure());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<UnreadNotificationsResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "Application could not get data from a server: " + t.getLocalizedMessage());
                    completeWorker(Result.failure());
                }
            });
        } else {
            Log.e(TAG, "The database URL is empty! The notifications will not be updated.");
            completeWorker(Result.failure());
        }
    }

    private void saveNotificationsAngGetNext(UnreadNotificationsResponse response) {
        // Save if there is some data
        List<UnreadNotificationsDb> notifications = getUnreadNotificationsDbs(response);
        if (!notifications.isEmpty()) {
            App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).put(notifications);
            Log.d(TAG, notifications.size() + " notifications should be saved; total "
                    + App.get().getBoxStore().boxFor(UnreadNotificationsDb.class).count() + " notifications.");
        }

        // Download next batch of notifications
        int totalEntries = response.getMeta().getTotal();
        int lastPage = (int) Math.ceil((double) totalEntries / perPage);

        boolean noMoreData = (notifications.isEmpty());
        boolean lastPageReached = (page >= lastPage && lastPage > 0);

        if (noMoreData || lastPageReached) {
            Log.i(TAG, "All notifications successfully updated from server.");
            long timestampNotification = getInputData().getLong(KEY_UPDATED_AFTER, 0L);

            if (timestampNotification == 0) {
                long newTimestamp = System.currentTimeMillis() / 1000L;
                SettingsManager.setNotificationsUpdatedAt(String.valueOf(newTimestamp));
                Log.i(TAG, "Full sync successful. Saved new timestamp: " + newTimestamp);
            } else {
                SettingsManager.setNotificationsUpdatedAt(String.valueOf(timestampNotification));
                Log.i(TAG, "Partial sync successful. Saved new timestamp: " + timestampNotification);
            }
            completeWorker(Result.success());
        } else {
            page++;
            Log.d(TAG, "Downloading notifications from page " + page);
            downloadNotifications(page);
        }

    }

    @NonNull
    private static List<UnreadNotificationsDb> getUnreadNotificationsDbs(UnreadNotificationsResponse response) {
        List<UnreadNotificationsDb> notifications = new ArrayList<>();

        // Exit on empty response
        if (response.getData() == null || response.getData().isEmpty()) {
            Log.d(TAG, "Empty UnreadNotifications response from server.");
            return notifications;
        }

        // 1. Collect all server IDs from the current response batch
        List<String> currentNotificationsId = new ArrayList<>();
        for (UnreadNotification unreadNotification : response.getData()) {
            currentNotificationsId.add(unreadNotification.getId());
        }
        Box<UnreadNotificationsDb> box = App.get().getBoxStore().boxFor(UnreadNotificationsDb.class);
        Query<UnreadNotificationsDb> query = box.query(
                        UnreadNotificationsDb_.realId.oneOf(currentNotificationsId.toArray(new String[0])))
                .build();
        Set<String> existingIds = new HashSet<>(Arrays.asList(
                query.property(UnreadNotificationsDb_.realId).findStrings()
        ));
        query.close();

        // 2. Save only notifications that were not already in the ObjectBox
        for (UnreadNotification unreadNotification : response.getData()) {
            String serverId = unreadNotification.getId();

            if (!existingIds.contains(serverId)) {
                UnreadNotificationsDb notificationDb = new UnreadNotificationsDb(
                        0,
                        unreadNotification.getId(),
                        unreadNotification.getType(),
                        unreadNotification.getNotifiable_type(),
                        unreadNotification.getData().getField_observation_id(),
                        unreadNotification.getData().getCauser_name(),
                        unreadNotification.getData().getCurator_name(),
                        unreadNotification.getData().getTaxon_name(),
                        null,
                        unreadNotification.getUpdated_at(),
                        null, null, null, null,
                        null, null, null, 0);

                notifications.add(notificationDb);
                Log.d(TAG, "Saving notification with ID: " + serverId);
            } else {
                Log.d(TAG, "Skipping duplicate notification ID: " + serverId);
            }
        }

        return notifications;
    }

    private void completeWorker(Result result) {
        if (future != null) {
            future.set(result);
        }
    }

}
