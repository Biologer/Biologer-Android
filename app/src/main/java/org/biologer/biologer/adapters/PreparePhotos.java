package org.biologer.biologer.adapters;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class PreparePhotos extends Service {

    private static final String TAG = "Biologer.Resize";
    LocalBroadcastManager broadcaster;
    public final static String RESIZED = "org.biologer.biologer.adapters.ResizeImage.RESIZED";

    @Override
    public void onCreate() {
        super.onCreate();
        broadcaster = LocalBroadcastManager.getInstance(this);
        Log.d(TAG, "Starting service for resizing images.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Collect input image Uri from previous Activity
        String input_image = null;
        if (intent.getExtras() != null) {
            input_image = intent.getExtras().getString("image_uri");
        }
        Log.d(TAG, "The image to be resized: " + input_image);

        // Resize the image
        Uri resizedUri = resizeImage(Uri.parse(input_image));

        // When resize is complete send the image to previous Activity and close...
        if (resizedUri != null) {
            sendResult(resizedUri.toString());
        } else {
            sendResult("error");
        }

        return flags;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Uri resizeImage(Uri path_to_image) {
        int rotation = 0;

        try {
            rotation = getExifImageRotation(path_to_image);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Can not open image stream to read EXIF data!");
        }

        Log.d(TAG, "Opening image for resize: " + path_to_image);
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = getContentResolver().openFileDescriptor(path_to_image, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Bitmap input_image;

        FileDescriptor fileDescriptor = null;
        if (parcelFileDescriptor != null) {
            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        }

        if (fileDescriptor != null) {
            // Memory leak workaround = don’t load whole image for resizing, but use inSampleSize.
            int inSampleSize;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true; // just to get image dimensions, don’t load into memory
            BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
            inSampleSize = getInSampleSize(options, 1024);

            options.inSampleSize = inSampleSize;
            options.inJustDecodeBounds = false; // now load the image into memory
            input_image = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

            //  Finally close the FileDescriptor
            try {
                parcelFileDescriptor.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (input_image == null) {
                Log.e(TAG, "It looks like input image does not exist!");
                return null;
            }

            if (Math.max(input_image.getHeight(), input_image.getWidth()) == 1024) {
                Log.d(TAG, "The image fits perfectly! Returning image without resize");
                Bitmap rotatedBitmap = rotateImage(input_image, rotation);
                return saveBitmap(rotatedBitmap);
            }
            else {
                Log.d(TAG, "Resizing image...");
                Bitmap resizedBitmap = resizeBitmap(input_image, 1024);
                Bitmap rotatedBitmap = rotateImage(resizedBitmap, rotation);
                return saveBitmap(rotatedBitmap);
            }

        } else {
            Log.e(TAG, "It looks like input image does not exist. fileDescriptor is null!");
            return null;
        }
    }

    private Uri saveBitmap(Bitmap bitmap) {
        try {

            // Save the file in internal Biologer storage
            String filename = getImageFileName() + ".jpg";
            FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 73, fos);
            fos.flush();
            fos.close();

            // Get the file Uri
            final File file = new File(getFilesDir(), filename);
            Uri photoUri = Uri.fromFile(file);
            Log.i(TAG, "Resized image saved to " + photoUri + ".");

            return photoUri;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "File not found.");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "File IO Exception.");
        }

        return null;

    }

    // Set the filename for image taken through the Camera
    private String getImageFileName() {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String timeStamp = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());

        return timeStamp + "_" + uuid;
    }

    public static int getInSampleSize(BitmapFactory.Options options, int max_dimensions) {
        int inSampleSize = 1;
        int larger_side = Math.max(options.outHeight, options.outWidth);

        if (larger_side > max_dimensions) {

            final int halfSide = larger_side / 2;

            while ((halfSide / inSampleSize) >= max_dimensions) {
                inSampleSize *= 2;
            }

        }
        Log.d(TAG, "Original image dimensions: " + options.outHeight + "×" + options.outWidth + " px. Resize factor value: " + inSampleSize);
        return inSampleSize;
    }

    public Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Log.i(TAG, "Resizing image of " + height + "×" + width + "px to a maximum of " + maxSize + "px.");

        if (height == width) {
            height = maxSize;
            width = maxSize;
        } if (height < width) {
            height = height * maxSize / width;
            width = maxSize;
        } else {
            width = width * maxSize /height;
            height = maxSize;
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, false);
    }

    public void sendResult(String uri) {
        Log.d(TAG, "Sending the result uri to broadcaster and stopping the service!");
        Intent intent = new Intent(RESIZED);
        intent.putExtra(RESIZED, uri);
        broadcaster.sendBroadcast(intent);
        stopSelf();
    }

    private int getExifImageRotation(Uri image) throws IOException {
        int exif_orientation = 0;
        ExifInterface exif_data = null;

        InputStream imageStream = getContentResolver().openInputStream(image);

        if (imageStream != null) {
            exif_data = new ExifInterface(imageStream);
        }
        if (exif_data != null) {
            exif_orientation = exif_data.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        }
        Log.d(TAG, "Exif orientation tag is set to: " + exif_orientation);

        if (imageStream != null) {
            imageStream.close();
        }

        switch (exif_orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

    private static Bitmap rotateImage(Bitmap image, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
    }
}
