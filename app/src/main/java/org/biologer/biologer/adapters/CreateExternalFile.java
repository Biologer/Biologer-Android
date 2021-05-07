package org.biologer.biologer.adapters;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

public class CreateExternalFile {

    private static final String TAG = "Biologer.ExternalFile";

    public static Uri newDocumentFile(Context context, String filename, String extension) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename + extension);

            if (extension.equals(".jpg")) {
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
                String directory = Environment.DIRECTORY_PICTURES + "/" + "Biologer";
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, directory);
                Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                Log.i(TAG, "Saving image into: " + uri);
                return uri;
            }

            if (extension.equals(".csv")) {
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                String directory = Environment.DIRECTORY_DOCUMENTS + "/" + "Biologer";
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, directory);
                Uri uri = context.getContentResolver().insert(MediaStore.Files.getContentUri("external"), contentValues);
                Log.i(TAG, "Saving file into: " + uri);
                return uri;
            }

        } else {
            File file = null;
            // Create the File where the photo should go
            try {
                file = createFile(filename, extension);
            } catch (IOException ex) {
                Log.e(TAG, "Could not create file.");
            }
            // Continue only if the File was successfully created
            if (file != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Uri uri = FileProvider.getUriForFile(context, "org.biologer.biologer.files", file);
                    Log.i(TAG, "Saving file into: " + file);
                    return uri;
                } else {
                    Uri photoUri = Uri.fromFile(file);
                    Log.i(TAG, "Saving file into: " + photoUri);
                    return photoUri;
                }
            }
            return null;
        }
        return null;
    }

    // This will create image on Android <= 9.0
    private static File createFile(String filename, String extension) throws IOException {

        if (extension.equals(".jpg")) {
            File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "Biologer");
            if (!file.exists()) {
                Log.d(TAG, "Media Storage directory does not exist");
                if (!file.mkdirs()) {
                    Log.e(TAG, "Media Storage directory could not be created!");
                    return null;
                }
                else {
                    Log.d(TAG, "Media Storage directory should be created now...");
                }
            }
            return File.createTempFile(filename, extension, file);
        }

        if (extension.equals(".csv")) {
            File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), "Biologer");
            if (!file.exists()) {
                Log.d(TAG, "Media Storage directory does not exist");
                if (!file.mkdirs()) {
                    Log.e(TAG, "Media Storage directory could not be created!");
                    return null;
                }
                else {
                    Log.d(TAG, "Media Storage directory should be created now...");
                }
            }
            return File.createTempFile(filename, extension, file);
        }

        else {
            return null;
        }
    }

}
