package org.biologer.biologer.services;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.data.kml.KmlLayer;

import org.biologer.biologer.R;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class KmlHelper {

    private static final String TAG = "Biologer.KmlHelper";
    private final Context context;
    private final GoogleMap map;
    private KmlLayer currentLayer;

    public KmlHelper(Context context, GoogleMap map) {
        this.context = context;
        this.map = map;
    }

    /** Reads a KML/KMZ Uri, saves it into app files, and draws it on the map. */
    public String saveAndDrawKml(Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return null;

            String uriString = uri.toString();
            String extension = uriString.substring(uriString.lastIndexOf("."));
            String filename = "KmlOverlay" + extension;

            File file = new File(context.getFilesDir(), filename);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.flush();
            }

            Uri savedUri = Uri.fromFile(file);
            drawKmlOverlay(savedUri);
            return savedUri.toString();

        } catch (IOException e) {
            Toast.makeText(context, context.getString(R.string.io_error) + e, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error saving KML/KMZ: ", e);
            return null;
        }
    }

    /** Draws a KML layer directly from Uri (must point to readable file). */
    public void drawKmlOverlay(Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return;

            // Remove existing layer if already visible
            if (currentLayer != null && currentLayer.isLayerOnMap()) {
                currentLayer.removeLayerFromMap();
            }

            currentLayer = new KmlLayer(map, inputStream, context);
            currentLayer.addLayerToMap();
            Log.d(TAG, "KML/KMZ overlay drawn successfully.");
        } catch (XmlPullParserException | IOException e) {
            Toast.makeText(context, context.getString(R.string.io_error) + e, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error drawing KML: ", e);
        }
    }

    /** Removes currently loaded layer and deletes local file copy. */
    public boolean removeKml(String uriString) {
        // Remove overlay from map first
        if (currentLayer != null && currentLayer.isLayerOnMap()) {
            currentLayer.removeLayerFromMap();
            Log.d(TAG, "KML/KMZ overlay removed from map.");
        }

        // Then delete local file
        if (uriString == null) return false;
        try {
            String fileExtension = uriString.substring(uriString.lastIndexOf("."));
            File file = new File(context.getFilesDir(), "KmlOverlay" + fileExtension);
            boolean deleted = file.delete();
            Log.d(TAG, "Deleting KML/KMZ file returned: " + deleted);
            currentLayer = null;
            return deleted;
        } catch (Exception e) {
            Log.w(TAG, "Failed to delete KML/KMZ: ", e);
            return false;
        }
    }
}
