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

import org.biologer.biologer.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class FileManipulation {

    private static final String TAG = "Biologer.ExternalFile";

    public static Uri newExternalDocumentFile(Context context, String filename, String extension) {

        if (filename == null) {
            filename = "JPEG_" + getFileNameFromDate();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename + extension);

            if (extension.equals(".jpg")) {
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
                String directory = Environment.DIRECTORY_PICTURES + "/" + context.getString(R.string.biologer);
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, directory);
                Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                Log.i(TAG, "Saving image into: " + uri);
                return uri;
            }

            if (extension.equals(".csv")) {
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                String directory = Environment.DIRECTORY_DOCUMENTS + "/" + context.getString(R.string.biologer);
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, directory);
                Uri uri = context.getContentResolver().insert(MediaStore.Files.getContentUri("external"), contentValues);
                Log.i(TAG, "Saving file into: " + uri);
                return uri;
            }

        } else {
            File file = null;
            // Create the File where the photo should go
            try {
                file = createExternalFile(context, filename, extension);
            } catch (IOException ex) {
                Log.e(TAG, "Could not create file: " + ex);
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
    private static File createExternalFile(Context context, String filename, String extension) throws IOException {

        if (extension.equals(".jpg")) {
            File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), context.getString(R.string.biologer));
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
                    Environment.DIRECTORY_DOCUMENTS), context.getString(R.string.biologer));
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

    private static String getFileNameFromDate() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }

    public static void createExternalDocumentsFolder(Context context, String dir_name) {
        Log.i(TAG, "Creating new directory in Biologer Document " + dir_name);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, context.getString(R.string.biologer));
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS + "/" + context.getString(R.string.biologer));
            context.getContentResolver().insert(MediaStore.Files.getContentUri("external"), contentValues);

            contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, dir_name);
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS + "/" + context.getString(R.string.biologer) + "/" + dir_name);
            context.getContentResolver().insert(MediaStore.Files.getContentUri("external"), contentValues);
        } else {
            File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), context.getString(R.string.biologer));
            file.mkdirs();
            file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES) + "/" + context.getString(R.string.biologer), dir_name);
            file.mkdirs();
        }
    }

    public static String getFilenameFromUri(Uri uri) {
        return uri.getLastPathSegment();
    }

    public static File getInternalFileFromName(Context context, String filename) {
        return new File(context.getFilesDir(),
                Objects.requireNonNull(filename, "Filename of the thumbnail returned null!"));
    }

    public static File getInternalFileFromUri(Context context, Uri uri) {
        return new File(context.getFilesDir(),
                Objects.requireNonNull(getFilenameFromUri(uri), "Filename of the thumbnail returned null!"));
    }

    public static void deleteInternalFileFromUri(Context context, Uri uri) {
        File file = getInternalFileFromUri(context, uri);
        boolean deleted = file.delete();
        Log.d(TAG, "Deleting file " + file.getName() + ", size: " + file.getTotalSpace() + ", returned: " + deleted);
    }

    // Returns the type of uri, i.e. the word starting the uri address
    public static String uriType(String uri) {
        int end = uri.indexOf(':');
        if (end != -1) {
            return uri.substring(0 , end);
        } else {
            return null;
        }
    }

}
