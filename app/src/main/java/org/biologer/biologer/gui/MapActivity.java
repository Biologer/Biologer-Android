package org.biologer.biologer.gui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

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
import com.google.android.material.slider.Slider;
import com.google.maps.android.collections.CircleManager;
import com.google.maps.android.collections.MarkerManager;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.kml.KmlLayer;
import com.google.maps.android.ui.IconGenerator;

import org.biologer.biologer.Localisation;
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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
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
    private String accuracy, elevation, location_name;
    private LatLng latLong;
    private final String google_map_type = SettingsManager.getGoogleMapType();
    private final String database_name = SettingsManager.getDatabaseName();
    private Circle circle;
    private Marker marker;
    private final List<Marker> utmMarkers = new ArrayList<>();
    private GeoJsonLayer utmGridLines;
    private KmlLayer kmlLayer;
    private MenuItem menuItemLoadKML;
    private MarkerManager.Collection myMarkers;
    private EditText editTextLocation, editTextLongitude, editTextLatitude, editTextPrecision, editTextElevation;
    Slider slider;
    private ImageView imageViewSaveLocationName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Add a toolbar to the Activity
        Toolbar toolbar = findViewById(R.id.map_actionbar);
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
            location_name = bundle.getString("LOCATION", null);
            Log.d(TAG, "Accuracy from GPS:" + accuracy + "; elevation: " + elevation + ".");
        } else {
            latLong = new LatLng(45.5, 16.3);
            accuracy = "50"; // Default accuracy
            Log.d(TAG, "Bundle is null for some reason! Setting default LatLong and accuracy.");
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, getString(R.string.no_map_fragment), Toast.LENGTH_LONG).show();
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        LinearLayout linearLayout = findViewById(R.id.map_location_name);
        if (!preferences.getBoolean("advanced_interface", false)) {
            linearLayout.setVisibility(View.GONE);
        } else {
            linearLayout.setVisibility(View.VISIBLE);
        }

        imageViewSaveLocationName = findViewById(R.id.map_save_location_name);
        imageViewSaveLocationName.setClickable(false);
        imageViewSaveLocationName.setAlpha(0.25f);

        editTextLocation = findViewById(R.id.location_name_text);
        String locationNameFromPreferences = preferences.getString("location_name", "");
        if (location_name != null) {
            editTextLocation.setText(location_name);
        } else {
            editTextLocation.setText(locationNameFromPreferences);
        }
        editTextLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                location_name = String.valueOf(editTextLocation.getText()).trim();
                imageViewSaveLocationName.setClickable(true);
                imageViewSaveLocationName.setAlpha(1.0f);
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
        });

        imageViewSaveLocationName.setOnClickListener(view -> {
            if (SettingsManager.getDisplayLocationNameInfo()) {
                SettingsManager.setDisplayLocationNameInfo(false);
                new AlertDialog.Builder(MapActivity.this)
                        .setTitle(R.string.save_location_name)
                        .setMessage(R.string.save_location_name_desc)
                        .setPositiveButton(R.string.ok, (dialog, which) -> saveLocationName())
                        .show();

            } else {
                saveLocationName();
            }
        });

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag(Localisation.getLocaleScript()));
        DecimalFormat formatCoordinates = new DecimalFormat("#,##0.000000", symbols);
        editTextLatitude = findViewById(R.id.map_latitude_text);
        editTextLatitude.setText(formatCoordinates.format(latLong.latitude));
        editTextLatitude.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                Locale currentLocale = Locale.forLanguageTag(Localisation.getLocaleScript());
                DecimalFormatSymbols symbols = new DecimalFormatSymbols(currentLocale);
                DecimalFormat decimalFormat = new DecimalFormat("#.#########", symbols);
                String latitudeString = editTextLatitude.getText().toString();
                try {
                    Number number = decimalFormat.parse(latitudeString);
                    double latitude;
                    if (number != null) {
                        latitude = number.doubleValue();
                        double currentLongitude = latLong.longitude;
                        latLong = new LatLng(latitude, currentLongitude);
                        marker.setPosition(latLong);
                        circle.setCenter(latLong);
                        circle.setRadius(Double.parseDouble(accuracy));
                    }
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
        });

        editTextLongitude = findViewById(R.id.map_longitude_text);
        editTextLongitude.setText(formatCoordinates.format(latLong.longitude));
        editTextLongitude.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                Locale currentLocale = Locale.forLanguageTag(Localisation.getLocaleScript());
                DecimalFormatSymbols symbols = new DecimalFormatSymbols(currentLocale);
                DecimalFormat decimalFormat = new DecimalFormat("#.#########", symbols);
                String longitudeString = editTextLongitude.getText().toString();
                try {
                    Number number = decimalFormat.parse(longitudeString);
                    double longitude;
                    if (number != null) {
                        longitude = number.doubleValue();
                        double currentLatitude = latLong.latitude;
                        latLong = new LatLng(currentLatitude, longitude);
                        marker.setPosition(latLong);
                        circle.setCenter(latLong);
                        circle.setRadius(Double.parseDouble(accuracy));
                    }
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
        });

        editTextPrecision = findViewById(R.id.map_precision_text);
        editTextPrecision.setText(String.valueOf(accuracy));
        editTextPrecision.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                String accuracy_from_input = String.valueOf(editTextPrecision.getText());
                if (accuracy_from_input.isEmpty()) {
                    accuracy = "0";
                } else {
                    accuracy = accuracy_from_input;
                }
                double a = Double.parseDouble(accuracy);
                circle.setRadius(a);
                double a_min = (a - 50);
                if (a_min < 0) {
                    slider.setValueFrom(0);
                    slider.setValueTo(100);
                } else {
                    slider.setValueFrom((float) a_min);
                    slider.setValueTo((float) (a + 50));
                }
                slider.setValue((float) a);

            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
        });

        editTextElevation = findViewById(R.id.map_elevation_text);
        editTextElevation.setText(String.valueOf(elevation));
        editTextElevation.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                String elevation_from_input = String.valueOf(editTextElevation.getText());
                if (elevation_from_input.isEmpty()) {
                    elevation = "0";
                } else {
                    elevation = elevation_from_input;
                }
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
        });

        ImageView floatButtonMapType = findViewById(R.id.float_button_map_type);
        floatButtonMapType.setImageAlpha(255);
        floatButtonMapType.setOnClickListener(view -> showMapTypeSelectorDialog());
    }

    private void saveLocationName() {
        String location = String.valueOf(editTextLocation.getText()).trim();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("location_name", location);
        editor.apply();

        if (location.isEmpty()) {
            SettingsManager.setPreviousLocationLong(null);
            SettingsManager.setPreviousLocationLat(null);
        } else {
            SettingsManager.setPreviousLocationLat(String.valueOf(latLong.latitude));
            SettingsManager.setPreviousLocationLong(String.valueOf(latLong.longitude));
        }
        imageViewSaveLocationName.setClickable(false);
        imageViewSaveLocationName.setAlpha(0.25f);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_activity_menu, menu);

        // Save menu
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

        MarkerManager markerManager = new MarkerManager(googleMap);

        // Add UTM 10×10 km grid lines and label points over the map
        MarkerManager.Collection markerCollectionUtm = markerManager.newCollection("utm");
        addUTMLines();
        addUTMPoints();

        // Handle UTM grid visibility on Zoom
        addZoomHandler(markerCollectionUtm);

        // Load custom KML/KMZ file if chosen
        if (SettingsManager.getKmlFile() != null) {
            loadKmlFile(null);
        }

        // Create a MarkerManager and a custom collection for your markers
        myMarkers = markerManager.newCollection("my_markers");

        // Set up map type
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

        // Add slider for setting coordinate precision
        addSlider();

        // Click on the map should change the locality
        mMap.setOnMapClickListener(latLng -> {
            latLong = latLng;
            Log.d(TAG, "New coordinates of the marker: " + latLng.latitude + ", " + latLng.longitude);
            updateElevationAndSave(latLong, true, false);

            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag(Localisation.getLocaleScript()));
            DecimalFormat formatCoordinates = new DecimalFormat("#,##0.000000", symbols);
            editTextLatitude.setText(formatCoordinates.format(latLong.latitude));
            editTextLongitude.setText(formatCoordinates.format(latLong.longitude));
            marker.setPosition(latLong);
            circle.setCenter(latLong);
        });

        initMapObjects();
    }

    private void initMapObjects() {
        // Add marker at the GPS position on the map or default position
        if (latLong.latitude == 0.0) {
            if (database_name.equals("https://biologer.hr")) {
                latLong = new LatLng(45.5, 16.3);
                addLocationMarker(7, myMarkers);
            }
            if (database_name.equals("https://biologer.ba")) {
                latLong = new LatLng(44.3, 17.9);
                addLocationMarker(7, myMarkers);
            }
            if (database_name.equals("https://biologer.me")) {
                latLong = new LatLng(42.8, 19.1);
                addLocationMarker(9, myMarkers);
            }
            if (database_name.equals("https://biologer.rs") || database_name.equals("https://dev.biologer.org")) {
                latLong = new LatLng(44.1, 20.7);
                addLocationMarker(7, myMarkers);
            }
        } else {
            addLocationMarker(16, myMarkers);
        }

        // Add a circle and resize handle
        addCircle();
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
                    File filesDir = getFilesDir();
                    if (!filesDir.exists()) {
                        boolean created = filesDir.mkdirs();
                        Log.d(TAG, "Creating directory for KML/KMZ file returned " + created);
                    }
                    final File file = new File(getFilesDir(), filename);
                    if (!file.exists()) {
                        boolean created = file.createNewFile();
                        Log.d(TAG, "Creating directory for KML/KMZ file returned " + created);
                    }
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
            geoJsonLineStringStyle.setColor(0xFF669900);
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

    private void addLocationMarker(Integer zoom, MarkerManager.Collection collection) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLong)
                .title(getString(R.string.you_are_here))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker))
                .zIndex(5000)
                .draggable(false);
        marker = collection.addMarker(markerOptions);

        if (zoom != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong, zoom));
            mMap.animateCamera(CameraUpdateFactory.zoomIn());
            mMap.animateCamera(CameraUpdateFactory.zoomTo(zoom), 1000, null);
        }
    }

    private void addCircle() {
        CircleManager circleManager = new CircleManager(mMap);
        CircleManager.Collection circleManagerCollection = circleManager.newCollection();
        CircleOptions circleOptions = new CircleOptions()
                .center(latLong)
                .radius(Double.parseDouble(accuracy))
                .fillColor(0x66c5e1a5)
                .clickable(false)
                .zIndex(5)
                .strokeColor(0xff4CAF50)
                .strokeWidth(4.0f);
        circle = circleManagerCollection.addCircle(circleOptions);
    }

    private void showMapTypeSelectorDialog() {
        final String fDialogTitle = getString(R.string.select_map_type);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(fDialogTitle);

        CharSequence[] list_of_map_types =
                {"Normal", "Hybrid", "Terrain", "Satellite"};
        list_of_map_types[0] = getString(R.string.normal);
        list_of_map_types[1] = getString(R.string.hybrid);
        list_of_map_types[2] = getString(R.string.terrain);
        list_of_map_types[3] = getString(R.string.sattelite);

        int checkItem = mMap.getMapType() - 1;

        builder.setSingleChoiceItems(
                list_of_map_types,
                checkItem,
                (dialog, item) -> {
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
        AlertDialog fMapTypeDialog = builder.create();
        fMapTypeDialog.setCanceledOnTouchOutside(true);
        fMapTypeDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
            return true;
        }
        if (id == R.id.action_save) {
            updateElevationAndSave(latLong, elevation == null || elevation.isEmpty(), true);
        }
        return true;
    }

    private void toggleUtmGrid(MenuItem menuItem) {
        float zoomLevel = mMap.getCameraPosition().zoom;
        mMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel));
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

    private void updateElevationAndSave(LatLng coordinates, boolean update_elevation, boolean save) {
        if (InternetConnection.isConnected(this)) {
            if (update_elevation) {
                Call<ElevationResponse> call = RetrofitClient.getService(SettingsManager.getDatabaseName()).getElevation(coordinates.latitude, coordinates.longitude);
                Log.d(TAG, "Requesting altitude for Latitude: " + coordinates.latitude + "; Longitude: " + coordinates.longitude);
                call.enqueue(new Callback<>() {
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
                        if (save) {
                            saveAndExit();
                        } else {
                            editTextElevation.setText(elevation);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ElevationResponse> call, @NonNull Throwable t) {
                        Log.d(TAG, "No elevation returned from server...");
                        elevation = "0.0";
                        if (save) {
                            saveAndExit();
                        } else {
                            editTextElevation.setText(elevation);
                        }
                    }
                });
            } else {
                if (save) {
                    saveAndExit();
                }
            }
        } else {
            Log.d(TAG, "No internet connection, won’t fetch the altitude!");
            elevation = "0.0";
            if (save) {
                saveAndExit();
            } else {
                editTextElevation.setText(elevation);
            }
        }
    }

    private void saveAndExit() {
        Intent returnLocation = new Intent();
        if (Integer.parseInt(accuracy) > 100000) {
            accuracy = "100000";
        }
        returnLocation.putExtra("google_map_accuracy", accuracy);
        returnLocation.putExtra("google_map_lat", String.valueOf(latLong.latitude));
        returnLocation.putExtra("google_map_long", String.valueOf(latLong.longitude));
        returnLocation.putExtra("google_map_elevation", elevation);
        returnLocation.putExtra("google_map_location_name", location_name);
        setResult(3, returnLocation);

        Log.d(TAG, "Latitude: " + latLong.latitude);
        Log.d(TAG, "Longitude: " + latLong.longitude);
        Log.d(TAG, "Accuracy: " + accuracy);
        Log.d(TAG, "Elevation: " + elevation);
        Log.d(TAG, "Location name: " + location_name);

        finish();
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

    private void addSlider() {
        slider = findViewById(R.id.map_slider);
        float current_value = Float.parseFloat(accuracy);
        if (current_value >= 100) {
            slider.setValueTo(500);
        } if (current_value >= 500) {
            slider.setValueTo(1000);
        } if (current_value >= 1000) {
            slider.setValueTo(10000);
        } if (current_value >= 10000) {
            slider.setValueTo(50000);
        }
        slider.setValue(current_value);
        slider.setLabelFormatter(value -> getString(R.string.precision) + " " + Math.round(value) + " m");
        slider.addOnChangeListener((slider1, value, fromUser) -> circle.setRadius(value));
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {

            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                Log.d(TAG, "The final value: " + slider.getValue());
                int value = Math.round(slider.getValue());
                if (value == 100) {slider.setValueTo(500);}
                if (value == 500) {slider.setValueTo(1000);}
                if (value == 1000) {slider.setValueTo(10000);}
                if (value == 10000) {slider.setValueTo(50000);}
                accuracy = String.valueOf(value);
                editTextPrecision.setText(accuracy);
            }
        });
    }
}