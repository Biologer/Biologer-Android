package org.biologer.biologer.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class PhotoUtils {

    private static final String TAG = "Biologer.PhotoUtils";
    private static final int MAX_DIMENSION = 1024;
    private static final int JPEG_QUALITY = 80;

    /**
     * Resizes, rotates and saves the image to app's private storage,
     * preserving EXIF metadata.
     */
    public static Uri resizeAndSave(Context context, Uri inputUri) throws IOException {
        ExifInterface sourceExif = getExifData(context, inputUri);
        int rotation = (sourceExif != null) ? getExifImageRotation(sourceExif) : 0;

        Bitmap bitmap = decodeDownsampledBitmap(context, inputUri);
        if (bitmap == null) {
            Log.e(TAG, "Failed to decode bitmap.");
            return null;
        }

        Bitmap result;
        if (Math.max(bitmap.getWidth(), bitmap.getHeight()) > MAX_DIMENSION) {
            result = resizeBitmap(bitmap);
            result = rotateBitmap(result, rotation);
        } else {
            result = rotateBitmap(bitmap, rotation);
        }

        Uri savedUri = saveBitmap(context, result);

        if (sourceExif != null && savedUri != null) {
            copyExifToFile(context, savedUri, sourceExif);
        }

        return savedUri;
    }

    private static Bitmap decodeDownsampledBitmap(Context context, Uri uri) throws IOException {
        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r")) {
            if (pfd == null) return null;
            FileDescriptor fd = pfd.getFileDescriptor();

            // get dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(fd, null, options);

            options.inSampleSize = getInSampleSize(options);
            options.inJustDecodeBounds = false;

            return BitmapFactory.decodeFileDescriptor(fd, null, options);
        }
    }

    private static int getInSampleSize(BitmapFactory.Options options) {
        int inSampleSize = 1;
        int larger = Math.max(options.outHeight, options.outWidth);
        if (larger > PhotoUtils.MAX_DIMENSION) {
            final int halfSide = larger / 2;
            while ((halfSide / inSampleSize) >= PhotoUtils.MAX_DIMENSION) {
                inSampleSize *= 2;
            }
        }
        Log.d(TAG, "inSampleSize chosen: " + inSampleSize);
        return inSampleSize;
    }

    private static Bitmap resizeBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width >= height) {
            float ratio = (float) height / width;
            width = PhotoUtils.MAX_DIMENSION;
            height = Math.round(PhotoUtils.MAX_DIMENSION * ratio);
        } else {
            float ratio = (float) width / height;
            height = PhotoUtils.MAX_DIMENSION;
            width = Math.round(PhotoUtils.MAX_DIMENSION * ratio);
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int degree) {
        if (degree == 0) return bitmap;
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Uri saveBitmap(Context context, Bitmap bitmap) throws IOException {
        String filename = getImageFileName() + ".jpg";
        FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos);
        fos.flush();
        fos.close();

        File file = new File(context.getFilesDir(), filename);
        Uri uri = Uri.fromFile(file);
        Log.i(TAG, "Saved resized bitmap: " + uri);
        return uri;
    }

    private static String getImageFileName() {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String timeStamp = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        return timeStamp + "_" + uuid;
    }

    private static ExifInterface getExifData(Context context, Uri uri) throws IOException {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is != null) {
                return new ExifInterface(is);
            }
        }
        return null;
    }

    private static int getExifImageRotation(ExifInterface exif) {
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        return switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90 -> 90;
            case ExifInterface.ORIENTATION_ROTATE_180 -> 180;
            case ExifInterface.ORIENTATION_ROTATE_270 -> 270;
            default -> 0;
        };
    }

    private static void copyExifToFile(Context context, Uri outputUri, ExifInterface source) throws IOException {
        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(outputUri, "rw")) {
            if (pfd == null) return;
            ExifInterface dest = new ExifInterface(pfd.getFileDescriptor());
            for (String tag : EXIF_TAGS) {
                String val = source.getAttribute(tag);
                if (val != null) dest.setAttribute(tag, val);
            }
            // reset orientation, since we rotated bitmap
            dest.setAttribute(ExifInterface.TAG_ORIENTATION,
                    String.valueOf(ExifInterface.ORIENTATION_NORMAL));
            dest.saveAttributes();
        }
    }

    private static final String[] EXIF_TAGS = {
            ExifInterface.TAG_APERTURE_VALUE,
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_DATETIME_ORIGINAL,
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
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_WHITE_BALANCE,
            ExifInterface.TAG_COLOR_SPACE,
            ExifInterface.TAG_ARTIST
    };
}
