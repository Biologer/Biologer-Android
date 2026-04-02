package org.biologer.biologer.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.biologer.biologer.App;
import org.biologer.biologer.helpers.PhotoUtils;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.PhotoDb;
import org.biologer.biologer.sql.PhotoDb_;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.objectbox.Box;
import io.objectbox.query.Query;

public class PhotoDownloadWorker extends Worker {

    private static final String TAG = "Biologer.PhotoDownloadWorker";
    Set<Long> updatedEntryIds = new HashSet<>();

    public PhotoDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Box<PhotoDb> photoBox = App.get().getBoxStore().boxFor(PhotoDb.class);
        Log.d(TAG, "Worker started. Total photos in DB: " + photoBox.count());

        // Find all photos that have a server URL but no local path
        try (Query<PhotoDb> query = photoBox.query()
                .isNull(PhotoDb_.localPath)
                .notNull(PhotoDb_.serverUrl)
                .build()) {
            List<PhotoDb> pendingPhotos = query.find();

            Log.d(TAG, "There are " + pendingPhotos.size() + " photos for download.");

            for (PhotoDb photo : pendingPhotos) {

                if (isStopped()) {
                    Log.d(TAG, "Worker stopped by WorkManager. Remaining photos: " + (pendingPhotos.size() - pendingPhotos.indexOf(photo)));
                    return Result.retry(); // Reschedules the work
                }

                try {
                    String savedPath = PhotoUtils.downloadPhotoFile(getApplicationContext(), photo.getServerUrl());

                    if (savedPath != null) {
                        long photoId = photo.id;
                        App.get().getBoxStore().runInTx(() -> {
                            // 1. Fetch a FRESH instance directly from the Box by ID
                            PhotoDb photoDb = photoBox.get(photoId);

                            if (photoDb != null) {
                                // 2. Apply the path to the FRESH object
                                photoDb.setLocalPath(savedPath);

                                // 3. Re-establish the relation to be safe
                                long observationId = photoDb.entry.getTargetId();
                                if (observationId != 0) {
                                    EntryDb entry = App.get().getBoxStore().boxFor(EntryDb.class).get(observationId);
                                    if (entry != null) {
                                        photoDb.entry.setTarget(entry);
                                        Log.d(TAG, "Photo " + photoId + " linked to entryId: " + photoDb.entry.getTargetId());
                                        updatedEntryIds.add(observationId);
                                    }
                                }

                                // 4. Put the FRESH object back
                                photoBox.put(photoDb);

                                // 5. Verify IMMEDIATELY while still in the transaction
                                Log.d(TAG, "VERIFICATION (Inside TX): Photo " + photoId + " path is: " + photoDb.getLocalPath());
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to download " + photo.getServerUrl(), e);
                    return Result.failure();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving photo from database!", e);
            return Result.failure();
        }

        // Prepare data to return
        long[] idsArray = new long[updatedEntryIds.size()];
        int i = 0;
        for (Long id : updatedEntryIds) {
            idsArray[i++] = id;
        }

        Data output = new Data.Builder()
                .putLongArray("updatedObservationIds", idsArray)
                .build();

        return Result.success(output);
    }
}
