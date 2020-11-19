package org.biologer.biologer.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraActivity extends Activity {

    private static final String TAG = "Biologer.Camera";

    @SuppressLint("QueryPermissionsNeeded")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Starting CameraActivity");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            Log.i(TAG, "There is Camera software installed. All ready to take picture!");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            Uri photoUri = getPhotoUri();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            int CAPTURE_CAMERA = 1;
            startActivityForResult(takePictureIntent, CAPTURE_CAMERA);
            saveAndExit(photoUri);
        } else {
            Log.e(TAG, "Take picture intent could not start for some reason.");
        }

    }

    private Uri getPhotoUri() {
        Uri photoUri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String pictureName = getImageFileName();
            String picturesDir = Environment.DIRECTORY_PICTURES + "/" + "Biologer";
            Log.i(TAG, "Saving image: " + pictureName + " into a directory " + picturesDir);
            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, pictureName + ".jpg");
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, picturesDir);
            photoUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            Log.i(TAG, "Image URI is: " + photoUri + ".");
        } else {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Could not create image file.");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    photoUri = FileProvider.getUriForFile(this, "org.biologer.biologer.files", photoFile);
                } else {
                    photoUri = Uri.fromFile(photoFile);
                }
            }
            Log.i(TAG, "Saving image into: " + photoUri);
        }
        return photoUri;
    }

    // This will create image on Android <= 9.0
    private File createImageFile() throws IOException {

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Biologer");

        if (!mediaStorageDir.exists()) {
            Log.d(TAG, "Media Storage directory does not exist");
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "Media Storage directory could not be created!");
                return null;
            }
            else {
                Log.d(TAG, "Media Storage directory should be created now...");
            }
        }

        return File.createTempFile(getImageFileName(), ".jpg", mediaStorageDir);
    }

    // Set the filename for image taken through the Camera
    private String getImageFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return "JPEG_" + timeStamp;
    }

    private void saveAndExit(Uri uri) {
        Intent returnPhoto = new Intent();
        returnPhoto.putExtra("image_string", String.valueOf(uri));
        setResult(2, returnPhoto);

        Log.d(TAG, "Returning the string of photo uri: " + uri);
        finish();
    }

}
