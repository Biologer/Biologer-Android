package org.biologer.biologer.helpers;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;

import org.biologer.biologer.R;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to handle GeoJSON layers (e.g., UTM grid lines and point labels).
 */
public class GeoJsonHelper {

    private static final String TAG = "Biologer.GeoJsonHelper";
    private final Context context;
    private final GoogleMap map;

    private GeoJsonLayer utmGridLinesLayer;
    private final List<UTMName> utmNames = new ArrayList<>();

    public GeoJsonHelper(Context context, GoogleMap map) {
        this.context = context;
        this.map = map;
    }

    /** Loads and styles UTM 10×10 km grid lines from res/raw/utm_grid_lines.json */
    public void loadUtmLines() {
        try {
            utmGridLinesLayer = new GeoJsonLayer(map, R.raw.utm_grid_lines, context);
            GeoJsonLineStringStyle style = utmGridLinesLayer.getDefaultLineStringStyle();
            style.setColor(0xFF669900);
            style.setZIndex(300);
            Log.d(TAG, "UTM grid lines loaded successfully.");
        } catch (IOException | JSONException e) {
            Toast.makeText(context, context.getString(R.string.error) + ": " + e, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error loading UTM grid lines: ", e);
        }
    }

    /** Adds the line layer to the map (if loaded). */
    public void showUtmLines() {
        if (utmGridLinesLayer == null) {
            Log.w(TAG, "UTM grid layer not loaded yet. Loading now...");
            loadUtmLines();
        }
        if (utmGridLinesLayer != null && !utmGridLinesLayer.isLayerOnMap()) {
            utmGridLinesLayer.addLayerToMap();
        }
    }

    /** Removes the line layer from the map. */
    public void hideUtmLines() {
        if (utmGridLinesLayer != null && utmGridLinesLayer.isLayerOnMap()) {
            utmGridLinesLayer.removeLayerFromMap();
        }
    }

    /** Loads UTM grid points (centroid labels) from res/raw/utm_grid_points.json */
    public void loadUtmPoints() {
        try {
            GeoJsonLayer utmGridPoints = new GeoJsonLayer(map, R.raw.utm_grid_points, context);
            for (GeoJsonFeature f : utmGridPoints.getFeatures()) {
                utmNames.add(new UTMName(
                        f.getProperty("Name"),
                        (LatLng) f.getGeometry().getGeometryObject()
                ));
            }
            Log.d(TAG, "UTM grid point labels loaded: " + utmNames.size());
        } catch (IOException | JSONException e) {
            Toast.makeText(context, context.getString(R.string.error) + ": " + e, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error loading UTM grid points: ", e);
        }
    }

    /** Returns the cached list of UTM label points (used by ActivityMap to draw text markers). */
    public List<UTMName> getUtmNames() {
        return utmNames;
    }

    /**
     * Simple data holder for grid label name and coordinates.
     */
        public record UTMName(String name, LatLng latLng) {
    }
}
