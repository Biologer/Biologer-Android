package org.biologer.biologer.workers;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.biologer.biologer.services.PhotoUtils;

public class PreparePhotosWorker extends Worker {

    private static final String TAG = "Biologer.ResizeWorker";

    public static final String KEY_INPUT_URI = "image_uri";
    public static final String KEY_OUTPUT_URI = "resized_uri";

    public PreparePhotosWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String inputImage = getInputData().getString(KEY_INPUT_URI);

        if (inputImage == null) {
            Log.e(TAG, "No image_uri passed to Worker!");
            return Result.failure();
        }

        try {
            Uri resultUri = PhotoUtils.resizeAndSave(getApplicationContext(), Uri.parse(inputImage));

            if (resultUri == null) {
                Log.e(TAG, "Resizing returned null for image: " + inputImage);
                return Result.failure(); // explicit failure, no magic string
            }

            Data output = new Data.Builder()
                    .putString(KEY_OUTPUT_URI, resultUri.toString())
                    .build();

            Log.i(TAG, "Resize succeeded: " + resultUri);
            return Result.success(output);

        } catch (Exception e) {
            Log.e(TAG, "Error resizing image: " + inputImage, e);
            // If the exception is transient (I/O, memory), retry; otherwise fail
            return Result.retry();
        }
    }

    public static androidx.work.OneTimeWorkRequest buildRequest(String imageUri) {
        Constraints constraints = new Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .build();

        return new androidx.work.OneTimeWorkRequest.Builder(PreparePhotosWorker.class)
                .setInputData(new Data.Builder()
                        .putString(KEY_INPUT_URI, imageUri)
                        .build())
                .setConstraints(constraints)
                .build();
    }
}
