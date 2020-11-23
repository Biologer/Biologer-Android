package org.biologer.biologer.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraActivity extends Activity {

    private static final String TAG = "Biologer.Camera";
    int CAMERA = 1;
    Uri uri;

    BroadcastReceiver receiver;

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
            uri = getPhotoUri(getImageFileName());
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(takePictureIntent, CAMERA);
        } else {
            Log.e(TAG, "Take picture intent could not start for some reason.");
        }

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(PreparePhotos.RESIZED);
                // This will be executed after upload is completed
                if (s != null) {
                    Log.d(TAG, "Resize Images returned code: " + s);

                    if (s.equals("error")) {
                        Log.d(TAG, "Unknown error. Can not get resized image!");
                    } else {
                        // Return the resized image to the previous Activity
                        Log.d(TAG, "All done, forwarding the URI to the Activity…");
                        Intent returnPhoto = new Intent();
                        returnPhoto.putExtra("image_string", s);
                        onActivityResult(2, -1, returnPhoto);
                    }
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),
                new IntentFilter(PreparePhotos.RESIZED)
        );
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    private Uri getPhotoUri(String filename) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String directory = Environment.DIRECTORY_PICTURES + "/" + "Biologer";
            Log.i(TAG, "Saving image: " + filename + " into a directory " + directory);
            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename + ".jpg");
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, directory);
            Uri photoUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            Log.i(TAG, "Saving image into: " + photoUri);
            return photoUri;
        } else {
            File photoFile = null;
            // Create the File where the photo should go
            try {
                photoFile = createImageFile(filename);
            } catch (IOException ex) {
                Log.e(TAG, "Could not create image file.");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Uri photoUri = FileProvider.getUriForFile(this, "org.biologer.biologer.files", photoFile);
                    Log.i(TAG, "Saving image into: " + photoUri);
                    return photoUri;
                } else {
                    Uri photoUri = Uri.fromFile(photoFile);
                    Log.i(TAG, "Saving image into: " + photoUri);
                    return photoUri;
                }
            }
            return null;
        }
    }

    // This will create image on Android <= 9.0
    private File createImageFile(String filename) throws IOException {

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

        return File.createTempFile(filename, ".jpg", file);
    }

    // Set the filename for image taken through the Camera
    private String getImageFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return "JPEG_" + timeStamp;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            return;
        }

        if(requestCode == CAMERA) {
            Log.d(TAG, "Camera request code received...");

            // Start another Activity to resize captured image.
            Intent resizeImage = new Intent(this, PreparePhotos.class);
            resizeImage.putExtra("image_uri", String.valueOf(uri));
            startService(resizeImage);
        }

        if(requestCode == 2) {
            setResult(2, data);
            finish();
        }

    }

}