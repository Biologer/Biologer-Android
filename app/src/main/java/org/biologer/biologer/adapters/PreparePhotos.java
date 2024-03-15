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
    ExifInterface exifInterface;

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
            try {
                exifInterface = getExifData(Uri.parse(input_image));
            } catch (IOException e) {
                Log.d(TAG, "Could not load EXIF data from uri: " + input_image);
                throw new RuntimeException(e);
            }
        }
        Log.d(TAG, "The image to be resized: " + input_image);

        // Resize the image
        Uri resizedUri = resizeImage(Uri.parse(input_image));

        // When resized send the image to previous Activity and close...
        if (resizedUri != null) {
            try {
                copyExifToFile(resizedUri);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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

        if (exifInterface != null) {
            rotation = getExifImageRotation(exifInterface);
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
                return saveBitmap(this, rotatedBitmap);
            }
            else {
                Log.d(TAG, "Resizing image...");
                Bitmap resizedBitmap = resizeBitmap(input_image, 1024);
                Bitmap rotatedBitmap = rotateImage(resizedBitmap, rotation);
                return saveBitmap(this, rotatedBitmap);
            }

        } else {
            Log.e(TAG, "It looks like input image does not exist. fileDescriptor is null!");
            return null;
        }
    }

    public static Uri saveBitmap(Context context, Bitmap bitmap) {
        try {

            // Save the file in internal Biologer storage
            String filename = getImageFileName() + ".jpg";
            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 73, fos);
            fos.flush();
            fos.close();

            // Get the file Uri
            final File file = new File(context.getFilesDir(), filename);
            Uri photoUri = Uri.fromFile(file);
            Log.i(TAG, "Image file saved to " + photoUri + ".");

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
    private static String getImageFileName() {
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

    private static Bitmap rotateImage(Bitmap image, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
    }

    private ExifInterface getExifData(Uri imageUri) throws IOException {
        ExifInterface exifData = null;
        InputStream imageStream = getContentResolver().openInputStream(imageUri);
        if (imageStream != null) {
            exifData = new ExifInterface(imageStream);
            imageStream.close();
        }
        return exifData;
    }

    private int getExifImageRotation(ExifInterface exifInterface) {
        int exif_orientation = 0;

        if (exifInterface != null) {
            exif_orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        }
        Log.d(TAG, "Exif orientation tag is set to: " + exif_orientation);

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

    public void copyExifToFile(Uri imageUri) throws IOException {

        String[] attributes = new String[] {
                ExifInterface.TAG_APERTURE_VALUE,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_DATETIME_DIGITIZED,
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_EXIF_VERSION,
                ExifInterface.TAG_EXPOSURE_PROGRAM,
                ExifInterface.TAG_EXPOSURE_MODE,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_FLASH,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_ISO_SPEED,
                ExifInterface.TAG_SHUTTER_SPEED_VALUE,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_METERING_MODE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_SUBJECT_DISTANCE,
                ExifInterface.TAG_SUBSEC_TIME,
                ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
                ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
                ExifInterface.TAG_WHITE_BALANCE,
                ExifInterface.TAG_COLOR_SPACE,
                ExifInterface.TAG_ARTIST
        };

        ParcelFileDescriptor parcelFileDescriptor;
        parcelFileDescriptor = getContentResolver().openFileDescriptor(imageUri, "rw" );
        FileDescriptor fileDescriptor;
        if (parcelFileDescriptor != null) {
            fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            ExifInterface newExifInterface;
            newExifInterface = new ExifInterface(fileDescriptor);
            for (String attribute : attributes) {
                String value = exifInterface.getAttribute(attribute);
                if (value != null)
                    newExifInterface.setAttribute(attribute, value);
            }
            newExifInterface.saveAttributes();
            Log.i(TAG, "EXIF data copied to the new image.");
        } else {
            Log.e(TAG, "Could not open file descriptor to copy EXIF data.");
        }
    }

}
