package org.biologer.biologer.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.biologer.biologer.App;
import org.biologer.biologer.helpers.PhotoUtils;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.PhotoDb;
import org.biologer.biologer.sql.PhotoDb_;

import java.util.List;

import io.objectbox.Box;

public class PhotoDownloadWorker extends Worker {

    private static final String TAG = "Biologer.PhotoDownloadWorker";

    public PhotoDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Box<PhotoDb> photoBox = App.get().getBoxStore().boxFor(PhotoDb.class);
        Log.d(TAG, "Worker started. Total photos in DB: " + photoBox.count());

        // Find all photos that have a server URL but no local path
        List<PhotoDb> pendingPhotos = photoBox.query()
                .isNull(PhotoDb_.localPath)
                .notNull(PhotoDb_.serverUrl)
                .build().find();

        Log.d(TAG, "There are " + pendingPhotos.size() + " photos for download.");

        Box<EntryDb> entryBox = App.get().getBoxStore().boxFor(EntryDb.class);

        // DIAGNOSTIC LOGS
        Log.d(TAG, "WORKER CHECK: Total Entries in DB: " + entryBox.count());
        Log.d(TAG, "WORKER CHECK: Total Photos in DB: " + photoBox.count());

        // Check for ANY photo with a server URL, ignoring the localPath for a second
        List<PhotoDb> allWithUrl = photoBox.query()
                .notNull(PhotoDb_.serverUrl)
                .build().find();
        Log.d(TAG, "WORKER CHECK: Photos with Server URLs: " + allWithUrl.size());

        // Now check how many have localPath as null vs empty string
        int nullPaths = 0;
        int emptyPaths = 0;
        for (PhotoDb p : allWithUrl) {
            if (p.getLocalPath() == null) nullPaths++;
            if (p.getLocalPath() != null && p.getLocalPath().isEmpty()) emptyPaths++;
            Log.d(TAG, "This is the local path: " + p.getLocalPath());
        }
        Log.d(TAG, "WORKER CHECK: Null paths: " + nullPaths + " | Empty paths: " + emptyPaths);


        for (PhotoDb photo : pendingPhotos) {
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
            }
        }
        return Result.success();
    }
}
