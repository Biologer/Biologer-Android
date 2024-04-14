package org.biologer.biologer.gui;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.textview.MaterialTextView;
import com.google.maps.android.collections.CircleManager;
import com.google.maps.android.collections.MarkerManager;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.kml.KmlLayer;
import com.google.maps.android.ui.IconGenerator;

import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.InternetConnection;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.json.ElevationResponse;
import org.json.JSONException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "Biologer.GoogleMaps";
    private final List<UTMName> utmNames = new ArrayList<>();
    private GoogleMap mMap;
    private String accuracy;
    private String elevation;
    ImageView floatButtonMapType;
    private LatLng latLong;
    String google_map_type = SettingsManager.getGoogleMapType();
    String database_name = SettingsManager.getDatabaseName();
    Circle circle;
    Marker marker, temporaryMarker;
    private final List<Marker> utmMarkers = new ArrayList<>();
    MaterialTextView textView;
    GeoJsonLayer utmGridLines;
    KmlLayer kmlLayer;
    MenuItem menuItemLoadKML;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Add a toolbar to the Activity
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        // Add the back button to the toolbar
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setDisplayShowHomeEnabled(true);
            actionbar.setTitle(R.string.google_maps_title);
        }

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            latLong = new LatLng(bundle.getDouble("LATITUDE"), bundle.getDouble("LONGITUDE"));
            double accuracy_bundle = bundle.getDouble("ACCURACY", 0);
            double elevation_bundle = bundle.getDouble("ELEVATION", 0);
            accuracy = String.format(Locale.ENGLISH, "%.0f", accuracy_bundle);
            elevation = String.format(Locale.ENGLISH, "%.0f", elevation_bundle);
            //int in = Integer.parseInt(acc);
            Log.d(TAG, "Accuracy from GPS:" + accuracy + "; elevation: " + elevation + ".");
        } else {
            latLong = new LatLng(45.5, 16.3);
            Log.d(TAG, "Bundle is null for some reason! Setting default LatLong.");
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, getString(R.string.no_map_fragment), Toast.LENGTH_LONG).show();
        }

        floatButtonMapType = findViewById(R.id.fbtn_mapType);
        floatButtonMapType.setOnClickListener(view -> showMapTypeSelectorDialog());

        textView = findViewById(R.id.coordinate_accuracy_text);

    }

    // Get the action bar (toolbar)
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_activity_menu, menu);

        // Save manu
        MenuItem menuItemSave = menu.findItem(R.id.action_save);
        Objects.requireNonNull(menuItemSave.getIcon()).setAlpha(255);

        // Show UTM menu
        MenuItem menuItemUtm = menu.findItem(R.id.action_show_utm);
        if (!SettingsManager.isUtmShown()) {
            Objects.requireNonNull(menuItemUtm.getIcon()).setAlpha(128);
        }
        menuItemUtm.setOnMenuItemClickListener(menuItem -> {
            toggleUtmGrid(menuItem);
            return false;
        });

        // Load KMZ/KML menu
        menuItemLoadKML = menu.findItem(R.id.action_load_kml);
        if (SettingsManager.getKmlFile() != null) {
            menuItemLoadKML.setIcon(R.drawable.ic_kmz_remove);
        }
        menuItemLoadKML.setOnMenuItemClickListener(menuItem -> {
            // If there is no KML/KMZ file pick one.
            if (SettingsManager.getKmlFile() == null) {
                addKmlFile();
            }
            // If KML/KMZ file exist we should remove it.
            else {
                String uri_string = String.valueOf(SettingsManager.getKmlFile());
                SettingsManager.setKmlFile(null);
                if (kmlLayer.isLayerOnMap()) {
                    kmlLayer.removeLayerFromMap();
                }
                menuItemLoadKML.setIcon(R.drawable.ic_kmz);
                String fileExtension = uri_string.substring(uri_string.lastIndexOf("."));
                final File file = new File(getFilesDir(), "KmlOverlay" + fileExtension);
                boolean b = file.delete();
                Log.d(TAG, "Deleting KML/KMZ file returned: " + b + "; Files dir: " + getFilesDir());
            }
            return false;
        });
        return true;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Update text
        String accuracy_text = getString(R.string.accuracy_a1) +
                " " + accuracy + " " + getString(R.string.meter) +
                "\n" + getString(R.string.drag_marker);
        textView.setText(accuracy_text);

        // Select the type of the map according to the user’s settings
        if (google_map_type.equals("NORMAL")) {
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
        if (google_map_type.equals("SATELLITE")) {
            googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
        if (google_map_type.equals("TERRAIN")) {
            googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }
        if (google_map_type.equals("HYBRID")) {
            googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }

        // Add handlers to be able to set marker
        MarkerManager markerManager = new MarkerManager(googleMap);
        MarkerManager.Collection markerCollectionLocation = markerManager.newCollection("location");
        MarkerManager.Collection markerCollectionUtm = markerManager.newCollection("utm");

        // Add marker at the GPS position on the map
        if (latLong.latitude == 0.0) {
            if (database_name.equals("https://biologer.hr")) {
                latLong = new LatLng(45.5, 16.3);
                addLocationMarker(7, markerManager);
            }
            if (database_name.equals("https://biologer.ba")) {
                latLong = new LatLng(44.3, 17.9);
                addLocationMarker(7, markerManager);
            }
            if (database_name.equals("https://biologer.me")) {
                latLong = new LatLng(42.8, 19.1);
                addLocationMarker(9, markerManager);
            }
            if (database_name.equals("https://biologer.rs") || database_name.equals("https://dev.biologer.org")) {
                latLong = new LatLng(44.1, 20.7);
                addLocationMarker(7, markerManager);
            }
        } else {
            addLocationMarker(16, markerManager);
        }

        // Add a circle to display coordinate precision.
        addCircle();

        // Add UTM 10×10 km grid lines and label points over the map
        addUTMLines();
        addUTMPoints();

        // Handle UTM grid visibility on Zoom
        addZoomHandler(markerCollectionUtm);

        // Load custom KML/KMZ file if chosen
        if (SettingsManager.getKmlFile() != null) {loadKmlFile(null);}

        // Click on the map should change the locality
        mMap.setOnMapClickListener(latLng -> {
            setLatLong(latLng.latitude, latLng.longitude);
            Log.d(TAG, "New coordinates of the marker: " + latLng.latitude + ", " + latLng.longitude);
            if (marker == null) {
                MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(getString(R.string.you_are_here)).draggable(true);
                marker = markerCollectionLocation.addMarker(markerOptions);
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker));
            } else {
                marker.setPosition(latLng);
            }
            circle.setCenter(latLng);
        });
    }

    private void addZoomHandler(MarkerManager.Collection collection) {

        mMap.setOnCameraIdleListener(() -> {
            float zoomLevel = mMap.getCameraPosition().zoom;
            Log.d(TAG, "Zoom: " + zoomLevel + "; UTM shown: " + SettingsManager.isUtmShown());

            if (SettingsManager.isUtmShown()) {
                // Remove the UTM markers first
                removeAllUtmMarkers();

                // Add UTM lines
                if (zoomLevel < 8) {
                    if (utmGridLines.isLayerOnMap()) {utmGridLines.removeLayerFromMap();}
                } else {
                    if (!utmGridLines.isLayerOnMap()) {utmGridLines.addLayerToMap();}
                }

                // Re-add the UTM markers
                if (zoomLevel > 10) {
                    utmGridLines.addLayerToMap();
                    final LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                    for (UTMName utmName : utmNames) {
                        if (bounds.contains(utmName.getLatLng())) {
                            IconGenerator iconFactory = new IconGenerator(MapActivity.this);
                            iconFactory.setStyle(IconGenerator.STYLE_GREEN);

                            MarkerOptions markerOptions = new MarkerOptions()
                                    .icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(utmName.getName())))
                                    .position(utmName.getLatLng())
                                    .anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());
                            Marker utmMarker = collection.addMarker(markerOptions);
                            utmMarkers.add(utmMarker);
                        }
                    }
                }
            }
        });
    }

    private void addKmlFile() {
        openDoc.launch(new String[]{"application/vnd.google-earth.kml+xml", "application/vnd.google-earth.kmz"});
    }

    private final ActivityResultLauncher<String[]> openDoc =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        Log.d(TAG, "KML/KMZ uri address is set to: " + uri);
                        loadKmlFile(uri);
                    });

    private void loadKmlFile(Uri uri) {
        // If uri is not given, take it from the Settings
        if (uri == null) {
            String uri_string = SettingsManager.getKmlFile();
            if (uri_string != null) {
                uri = Uri.parse(uri_string);
                drawKmlOverlay(uri);
            }
        }

        // If the uri is given, save the file and open it.
        else {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    String uri_string = String.valueOf(uri);
                    String extension = uri_string.substring(uri_string.lastIndexOf("."));
                    String filename = "KmlOverlay" + extension;
                    final File file = new File(getFilesDir(), filename);
                    Uri fileUri = Uri.fromFile(file);
                    Log.d(TAG, "KML/KMZ file should be saved to " + fileUri + ".");
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    int read;
                    byte[] bytes = new byte[8192];
                    while ((read = inputStream.read(bytes)) != -1) {
                        fileOutputStream.write(bytes, 0, read);
                    }
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    inputStream.close();
                    SettingsManager.setKmlFile(String.valueOf(fileUri));
                    menuItemLoadKML.setIcon(R.drawable.ic_kmz_remove);

                    drawKmlOverlay(fileUri);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void drawKmlOverlay(Uri uri) {
        InputStream inputStream;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                kmlLayer = new KmlLayer(mMap, inputStream, this);
                kmlLayer.addLayerToMap();
                inputStream.close();
            }
        } catch (XmlPullParserException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addUTMLines() {
        try {
            utmGridLines = new GeoJsonLayer(mMap, R.raw.utm_grid_lines, this);
            GeoJsonLineStringStyle geoJsonLineStringStyle = utmGridLines.getDefaultLineStringStyle();
            geoJsonLineStringStyle.setColor(Color.parseColor("#ff669900"));
            geoJsonLineStringStyle.setZIndex(300);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void addUTMPoints() {
        try {
            GeoJsonLayer utmGridPoints = new GeoJsonLayer(mMap, R.raw.utm_grid_points, this);
            Iterable<GeoJsonFeature> pointNames = utmGridPoints.getFeatures();
            for(GeoJsonFeature i: pointNames){
                utmNames.add(new UTMName(i.getProperty("Name"), (LatLng)i.getGeometry().getGeometryObject()));
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeAllUtmMarkers() {
        for (int r = 0; r < utmMarkers.size(); r++) {
            Marker utmMarker = utmMarkers.get(r);
            utmMarker.remove();
        }
        utmMarkers.clear();
    }

    private void addLocationMarker(int zoom, MarkerManager markerManager) {
        MarkerManager.Collection collection = markerManager.getCollection("location");
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLong)
                .title(getString(R.string.you_are_here))
                .draggable(true);

        marker = collection.addMarker(markerOptions);
        if (marker != null) {
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker));
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong, zoom));
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoom), 1000, null);

        collection.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(@NonNull Marker marker) {
                Log.d(TAG, "Location at starting point: " + latLong.latitude + "; " + latLong.longitude + ".");
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radius));
                MarkerOptions markerOptions = new MarkerOptions().position(latLong);
                temporaryMarker = collection.addMarker(markerOptions);
                if (temporaryMarker != null) {
                    temporaryMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker));
                }
            }

            @Override
            public void onMarkerDragEnd(@NonNull Marker marker) {
                Location old_location = new Location("");
                old_location.setLongitude(latLong.longitude);
                old_location.setLatitude(latLong.latitude);
                Location new_location = new Location("");
                new_location.setLongitude(marker.getPosition().longitude);
                new_location.setLatitude(marker.getPosition().latitude);
                double distance = old_location.distanceTo(new_location);
                Log.d(TAG, "Distance between central point and the drown circle is " + distance);
                accuracy = String.format(Locale.ENGLISH, "%.0f", distance);
                circle.setRadius(Double.parseDouble(accuracy));
                String text = getString(R.string.accuracy_a1) + " " + accuracy + " m";
                textView.setText(text);
            }

            @Override
            public void onMarkerDragStart(@NonNull Marker marker) {
                Log.d(TAG, "Location at ending point: " + latLong.latitude + "; " + latLong.longitude + ".");
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker));
                marker.setPosition(latLong);
                temporaryMarker.remove();
            }
        });

    }

    private void addCircle() {
        CircleManager circleManager = new CircleManager(mMap);
        CircleManager.Collection circleManagerCollection = circleManager.newCollection();
        CircleOptions circleOptions = new CircleOptions()
                .center(latLong)
                .radius(Double.parseDouble(accuracy))
                .fillColor(0x66c5e1a5)
                .clickable(true)
                .zIndex(5)
                .strokeColor(0xff689f38)
                .strokeWidth(4.0f);
        circle = circleManagerCollection.addCircle(circleOptions);
    }

    private void showMapTypeSelectorDialog() {
        // Prepare the dialog by setting up a Builder.
        final String fDialogTitle = getString(R.string.select_map_type);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(fDialogTitle);

        // Create array to fill in the dropdown list
        CharSequence[] list_of_map_types =
                {"Normal", "Hybrid", "Terrain", "Satellite"};
        list_of_map_types[0] = getString(R.string.normal);
        list_of_map_types[1] = getString(R.string.hybrid);
        list_of_map_types[2] = getString(R.string.terrain);
        list_of_map_types[3] = getString(R.string.sattelite);

        // Find the current map type to pre-check the item representing the current state.
        int checkItem = mMap.getMapType() - 1;

        // Add an OnClickListener to the dialog, so that the selection will be handled.
        builder.setSingleChoiceItems(
                list_of_map_types,
                checkItem,
                (dialog, item) -> {
                    // Locally create a finalised object.

                    // Perform an action depending on which item was selected.
                    switch (item) {
                        case 1:
                            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                            SettingsManager.setGoogleMapType("HYBRID");
                            break;
                        case 2:
                            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                            SettingsManager.setGoogleMapType("TERRAIN");
                            break;
                        case 3:
                            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                            SettingsManager.setGoogleMapType("SATELLITE");
                            break;
                        default:
                            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                            SettingsManager.setGoogleMapType("NORMAL");
                    }
                    dialog.dismiss();
                }
        );

        // Build the dialog and show it.
        AlertDialog fMapTypeDialog = builder.create();
        fMapTypeDialog.setCanceledOnTouchOutside(true);
        fMapTypeDialog.show();
    }

    // Process running after clicking the toolbar buttons (back and save)
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
            return true;
        }
        if (id == R.id.action_save) {
            // Get elevation from biologer server, save all data and exit
            updateElevationAndSave(latLong);
        }
        return true;
    }

    private void toggleUtmGrid(MenuItem menuItem) {
        // Trigger zoom update to refresh the view
        float zoomLevel = mMap.getCameraPosition().zoom;
        mMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel));
        // Update the view and preferences
        if (SettingsManager.isUtmShown()) {
            Log.d(TAG, "UTM grid is shown. Hiding the grid now.");
            SettingsManager.setUtmShown(false);
            Objects.requireNonNull(menuItem.getIcon()).setAlpha(128);
            if (utmGridLines.isLayerOnMap()) {utmGridLines.removeLayerFromMap();}
            removeAllUtmMarkers();
        } else {
            Log.d(TAG, "UTM grid is hidden. Showing the grid now.");
            SettingsManager.setUtmShown(true);
            Objects.requireNonNull(menuItem.getIcon()).setAlpha(255);
            if (utmGridLines.isLayerOnMap()) {utmGridLines.addLayerToMap();}
        }
    }

    private void updateElevationAndSave(LatLng coordinates) {
        if (InternetConnection.isConnected(this)) {
            Call<ElevationResponse> call = RetrofitClient.getService(SettingsManager.getDatabaseName()).getElevation(coordinates.latitude, coordinates.longitude);
            Log.d(TAG, "Requesting altitude for Latitude: " + coordinates.latitude + "; Longitude: " + coordinates.longitude);
            call.enqueue(new Callback<ElevationResponse>() {
                @Override
                public void onResponse(@NonNull Call<ElevationResponse> call, @NonNull Response<ElevationResponse> response) {
                    if (response.body() != null) {
                        Long elev = response.body().getElevation();
                        if (elev != null) {
                            elevation = String.valueOf(elev);
                        } else {
                            elevation = "0.0";
                        }
                    } else {
                        elevation = "0.0";
                    }
                    Log.d(TAG, "Elevation for this point is " + elevation + ".");
                    saveAndExit();
                }

                @Override
                public void onFailure(@NonNull Call<ElevationResponse> call, @NonNull Throwable t) {
                    Log.d(TAG, "No elevation returned from server...");
                    elevation = "0.0";
                    saveAndExit();
                }
            });
        } else {
            Log.d(TAG, "No internet connection, won’t fetch the altitude!");
            elevation = "0.0";
            saveAndExit();
        }
    }

    private void saveAndExit() {
        // Forward the result to previous Activity
        Intent returnLocation = new Intent();
        // If the coordinate accuracy is too large, set it to 100000
        if (Integer.parseInt(accuracy) > 100000) {
            accuracy = "100000";
        }
        returnLocation.putExtra("google_map_accuracy", accuracy);
        returnLocation.putExtra("google_map_lat", String.valueOf(latLong.latitude));
        returnLocation.putExtra("google_map_long", String.valueOf(latLong.longitude));
        returnLocation.putExtra("google_map_elevation", elevation);
        setResult(3, returnLocation);

        Log.d(TAG, "Latitude: " + latLong.latitude);
        Log.d(TAG, "Longitude: " + latLong.longitude);
        Log.d(TAG, "Accuracy: " + accuracy);
        Log.d(TAG, "Elevation: " + elevation);

        finish();
    }

    public void setAccuracy(String accuracy) {
        this.accuracy = accuracy;
    }

    public void setLatLong(double lat, double lon) {
        this.latLong = new LatLng(lat, lon);
    }

    private static class UTMName {
        private String name;
        private final LatLng latLng;

        public UTMName(String name, LatLng latLng) {
            this.name = name;
            this.latLng = latLng;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public LatLng getLatLng() {
            return latLng;
        }
    }

}
