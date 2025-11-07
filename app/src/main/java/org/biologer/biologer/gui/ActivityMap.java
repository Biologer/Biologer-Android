package org.biologer.biologer.gui;

import org.biologer.biologer.databinding.ActivityMapBinding;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.collections.CircleManager;
import com.google.maps.android.collections.MarkerManager;
import com.google.maps.android.ui.IconGenerator;

import org.biologer.biologer.helpers.Localisation;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.InternetConnection;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.json.ElevationResponse;
import org.biologer.biologer.helpers.GeoJsonHelper;
import org.biologer.biologer.helpers.KmlHelper;

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

public class ActivityMap extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "Biologer.GoogleMaps";
    private ActivityMapBinding binding;
    private GoogleMap mMap;
    private String accuracy, elevation, location_name;
    private LatLng latLong;
    private final String google_map_type = SettingsManager.getGoogleMapType();
    private Circle circle;
    private Marker marker;
    private final List<Marker> utmMarkers = new ArrayList<>();
    private MenuItem menuItemLoadKML;
    private EditText editTextLocation, editTextLongitude, editTextLatitude, editTextPrecision, editTextElevation;
    Slider slider;
    private ImageView imageViewSaveLocationName;
    private Call<ElevationResponse> elevationCall;
    private final Handler locationNameHandler = new Handler(Looper.getMainLooper());
    private String lastSavedLocationName = null;
    private static final int REQUEST_LOCATION_PERMISSION = 101;
    private FusedLocationProviderClient fusedLocationClient;
    private KmlHelper kmlHelper;
    private GeoJsonHelper geoJsonHelper;
    private Marker accuracyHandle;
    private boolean draggingHandle = false;
    private static final int HANDLE_HIT_SLOP_DP = 24;
    int zoom_level = 16;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            getSavedData(savedInstanceState); // To restore on screen rotation
        } else {
            getBundleData();
        }

        setupToolbar();
        getViews();

        // Elevation fetch after views exist
        if (elevation == null || "0".equals(elevation)) {
            updateElevationAndSave(latLong, true, false);
        }
    }

    private void getSavedData(Bundle savedInstanceState) {
        double lat = savedInstanceState.getDouble("STATE_LATITUDE", 0.0);
        double lon = savedInstanceState.getDouble("STATE_LONGITUDE", 0.0);
        accuracy = savedInstanceState.getString("STATE_ACCURACY", "50");
        elevation = savedInstanceState.getString("STATE_ELEVATION", "0");
        location_name = savedInstanceState.getString("STATE_LOCATION_NAME", null);
        latLong = new LatLng(lat, lon);

        Log.d(TAG, "Restored map state from bundle: "
                + lat + ", " + lon + " | accuracy: " + accuracy
                + " | elevation: " + elevation
                + " | location: " + location_name);
    }

    private void getViews() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, getString(R.string.no_map_fragment), Toast.LENGTH_LONG).show();
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        LinearLayout linearLayout = binding.mapLocationName;
        if (!preferences.getBoolean("advanced_interface", false)) {
            linearLayout.setVisibility(View.GONE);
        } else {
            linearLayout.setVisibility(View.VISIBLE);
        }

        imageViewSaveLocationName = binding.mapSaveLocationName;
        imageViewSaveLocationName.setClickable(false);
        imageViewSaveLocationName.setAlpha(0.25f);

        editTextLocation = binding.locationNameText;

        lastSavedLocationName = preferences.getString("location_name", "");
        editTextLocation.setText(location_name != null ? location_name : lastSavedLocationName);
        editTextLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                locationNameHandler.removeCallbacksAndMessages(null);
                locationNameHandler.postDelayed(() -> {
                    String newName = editable.toString().trim();
                    location_name = newName;

                    boolean enableSave = (!newName.equals(lastSavedLocationName));

                    imageViewSaveLocationName.setClickable(enableSave);
                    imageViewSaveLocationName.setAlpha(enableSave ? 1.0f : 0.25f);
                }, 500);
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
                new AlertDialog.Builder(ActivityMap.this)
                        .setTitle(R.string.save_location_name)
                        .setMessage(R.string.save_location_name_desc)
                        .setPositiveButton(R.string.ok, (dialog, which) -> saveLocationName())
                        .show();

            } else {
                saveLocationName();
            }
        });

        // Latitude
        editTextLatitude = binding.mapLatitudeText;
        editTextLatitude.setText(formatCoordinate(latLong.latitude));
        setupCoordinateField(editTextLatitude, true);

        // Longitude
        editTextLongitude = binding.mapLongitudeText;
        editTextLongitude.setText(formatCoordinate(latLong.longitude));
        setupCoordinateField(editTextLongitude, false);

        editTextPrecision = binding.mapPrecisionText;
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
                if (circle != null) circle.setRadius(a);
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

        editTextElevation = binding.mapElevationText;
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

        ImageView floatButtonMapType = binding.floatButtonMapType;
        floatButtonMapType.setImageAlpha(255);
        floatButtonMapType.setOnClickListener(view -> showMapTypeSelectorDialog());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        binding.floatButtonMyLocation.setOnClickListener(v -> moveToCurrentLocation());
    }

    /**
     * Sets up a coordinate EditText (latitude or longitude) so that changes update the marker & circle.
     * @param field       EditText to monitor.
     * @param isLatitude  true → latitude field, false → longitude field.
     */
    private void setupCoordinateField(EditText field, boolean isLatitude) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                String text = editable.toString().trim();
                if (text.isEmpty()) return;

                try {
                    Locale locale = Locale.forLanguageTag(Localisation.getLocaleScript());
                    DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
                    DecimalFormat df = new DecimalFormat("#.#########", symbols);
                    Number parsed = df.parse(text);
                    if (parsed == null) return;

                    double value = parsed.doubleValue();
                    latLong = new LatLng(
                            isLatitude ? value : latLong.latitude,
                            isLatitude ? latLong.longitude : value
                    );

                    // Update map visuals
                    updateMarkerAndCircle(true);

                } catch (ParseException e) {
                    Toast.makeText(ActivityMap.this,
                            getString(R.string.invalid_format) + ": " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }


    private void moveToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        Log.d(TAG, "Current GPS location: " + currentLatLng.latitude + ", " + currentLatLng.longitude);

                        float accuracyValue = location.hasAccuracy() ? location.getAccuracy() : 0;
                        accuracy = String.format(Locale.ENGLISH, "%.0f", accuracyValue);
                        binding.mapPrecisionText.setText(accuracy);
                        binding.mapSlider.setValue(accuracyValue);
                        Log.d(TAG, "Current location accuracy: " + accuracy + " m");

                        latLong = currentLatLng;
                        if (mMap != null) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16));
                        }

                        if (marker != null) marker.setPosition(latLong);
                        if (circle != null) {
                            circle.setCenter(latLong);
                        }

                        editTextLatitude.setText(formatCoordinate(latLong.latitude));
                        editTextLongitude.setText(formatCoordinate(latLong.longitude));

                        updateElevationAndSave(latLong, true, false);
                    } else {
                        Toast.makeText(this, R.string.location_not_available, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting location: ", e);
                    Toast.makeText(this, R.string.location_not_available, Toast.LENGTH_SHORT).show();
                });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.mapActionbar.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle(R.string.google_maps_title);
        }
    }

    private void getBundleData() {
        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            double inLat = bundle.getDouble("LATITUDE", 0.0);
            double inLon = bundle.getDouble("LONGITUDE", 0.0);

            if (inLat == 0.0 || inLon == 0.0) {
                String database = SettingsManager.getDatabaseName();
                if (database.equals("https://biologer.rs")
                        || database.equals("https://dev.biologer.org")) {
                    latLong = new LatLng(44.1, 20.7);
                    zoom_level = 7;
                }
                if (database.equals("https://biologer.hr")) {
                    latLong = new LatLng(45.5, 16.3);
                    zoom_level = 7;
                }
                if (database.equals("https://biologer.ba")) {
                    latLong = new LatLng(44.3, 17.9);
                    zoom_level = 7;
                }
                if (database.equals("https://biologer.me")) {
                    latLong = new LatLng(42.8, 19.1);
                    zoom_level = 9;
                }
            } else {
                latLong = new LatLng(inLat, inLon);
            }

            double accuracy_bundle = bundle.getDouble("ACCURACY", 0);
            double elevation_bundle = bundle.getDouble("ELEVATION", 0);
            elevation = String.format(Locale.ENGLISH, "%.0f", elevation_bundle);
            accuracy = String.format(Locale.ENGLISH, "%.0f", accuracy_bundle);
            location_name = bundle.getString("LOCATION", null);
            Log.d(TAG, "Accuracy from GPS:" + accuracy + "; elevation: " + elevation + ".");

        } else {
            latLong = new LatLng(45.5, 16.3);
            accuracy = "0";
            Log.d(TAG, "Bundle is null for some reason! Setting default LatLong and accuracy.");
        }
        updateMarkerAndCircle(true);
    }

    private void saveLocationName() {
        String location = String.valueOf(editTextLocation.getText()).trim();
        lastSavedLocationName = location;

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
            String uriString = SettingsManager.getKmlFile();

            if (uriString == null) {
                addKmlFile();
            }

            else {
                if (kmlHelper.removeKml(uriString)) {
                    SettingsManager.setKmlFile(null);
                    menuItemLoadKML.setIcon(R.drawable.ic_kmz);
                    Log.d(TAG, "KML removed.");
                }
            }
            return true;
        });

        return true;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);

        kmlHelper = new KmlHelper(this, mMap);
        geoJsonHelper = new GeoJsonHelper(this, mMap);
        geoJsonHelper.loadUtmLines();
        geoJsonHelper.loadUtmPoints();

        MarkerManager markerManager = new MarkerManager(googleMap);

        // Add UTM 10×10 km grid lines and label points over the map
        MarkerManager.Collection markerCollectionUtm = markerManager.newCollection("utm");

        // Handle UTM grid visibility on Zoom
        addZoomHandler(markerCollectionUtm);

        // Load custom KML/KMZ file if chosen
        if (SettingsManager.getKmlFile() != null) {
            loadKmlFile(null);
        }

        // Create a MarkerManager and a custom collection for your markers
        MarkerManager.Collection myMarkers = markerManager.newCollection("my_markers");

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
        mMap.setOnMapClickListener(this::handleUserTap);

        // Add marker at the GPS position on the map or default position
        addLocationMarker(myMarkers);
        // Add a circle
        addCircle();

        SupportMapFragment mapFrag =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        View mapView = mapFrag != null ? mapFrag.getView() : null;

        if (mapView instanceof ViewGroup mapContainer) {

            // Grab the original map child BEFORE adding our overlay
            final View mapContent = mapContainer.getChildAt(0);

            // Avoid adding multiple overlays on config changes etc.
            final String OVERLAY_TAG = "tap_pass_through_overlay";
            View existing = mapContainer.findViewWithTag(OVERLAY_TAG);
            if (existing != null) {
                mapContainer.removeView(existing);
            }

            final View touchOverlay = new View(this) {
                @Override public boolean performClick() {
                    super.performClick();  // accessibility event
                    return true;
                }
            };
            touchOverlay.setTag(OVERLAY_TAG);
            touchOverlay.setClickable(true);
            touchOverlay.setContentDescription("Map touch overlay");
            touchOverlay.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ));
            mapContainer.addView(touchOverlay);

            touchOverlay.setOnTouchListener(new View.OnTouchListener() {
                private static final int TAP_TIMEOUT_MS = 200;
                private static final int LONG_PRESS_MS  = android.view.ViewConfiguration.getLongPressTimeout(); // ~500ms
                private static final int TAP_SLOP_DP    = 14;
                private static final int HIT_SLOP_DP    = 24; // how close you must be to the marker to count as "on marker"

                private long downTime = 0L;
                private float downX, downY;
                private boolean forwarding = false;
                private boolean longPressFired = false;
                private final Runnable longPressRunnable = new Runnable() {
                    @Override public void run() {
                        if (mMap == null || marker == null) return;
                        longPressFired = true;
                        android.graphics.Point mp = mMap.getProjection().toScreenLocation(marker.getPosition());
                        float dx = Math.abs(downX - mp.x);
                        float dy = Math.abs(downY - mp.y);
                        if (dx <= dpToPx(HIT_SLOP_DP) && dy <= dpToPx(HIT_SLOP_DP)) {
                            // Long-pressed on marker code to be executed
                            startAccuracyHandle();
                            draggingHandle = true;
                        }
                    }
                };

                private int dpToPx(int dp) {
                    float d = getResources().getDisplayMetrics().density;
                    return Math.round(dp * d);
                }

                private void postLongPress() {
                    touchOverlay.removeCallbacks(longPressRunnable);
                    longPressFired = false;
                    touchOverlay.postDelayed(longPressRunnable, LONG_PRESS_MS);
                }

                private void cancelLongPress() {
                    touchOverlay.removeCallbacks(longPressRunnable);
                }

                private MotionEvent cloneWithAction(MotionEvent e) {
                    return MotionEvent.obtain(downTime == 0 ? e.getDownTime() : downTime,
                            e.getEventTime(), MotionEvent.ACTION_DOWN, e.getX(), e.getY(), e.getMetaState());
                }

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (mMap == null || mapContent == null) return false;

                    // 0) If we are dragging the accuracy handle, consume events and move it.
                    // Do this BEFORE other gesture logic so the map doesn’t pan.
                    if (draggingHandle) {
                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_MOVE: {
                                moveAccuracyHandleToScreen(event.getX(), event.getY());
                                return true; // consume, don't forward to map
                            }
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL: {
                                moveAccuracyHandleToScreen(event.getX(), event.getY());
                                finishAccuracyHandleDrag();
                                return true;
                            }
                            default:
                                return true;
                        }
                    }

                    int action = event.getActionMasked();

                    if (action == MotionEvent.ACTION_POINTER_DOWN && !forwarding) {
                        // cancel long-press & start forwarding for multitouch gestures
                        cancelLongPress();
                        forwarding = true;
                        MotionEvent syntheticDown = cloneWithAction(event);
                        mapContent.dispatchTouchEvent(syntheticDown);
                        syntheticDown.recycle();
                        mapContent.dispatchTouchEvent(event);
                        return true;
                    }

                    if (forwarding) {
                        cancelLongPress();
                        mapContent.dispatchTouchEvent(event);
                        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                            forwarding = false;
                        }
                        return true;
                    }

                    switch (action) {
                        case MotionEvent.ACTION_DOWN: {
                            if (event.getPointerCount() > 1) return false;
                            // If finger down is near the handle → start dragging the handle
                            if (isNearHandle(event.getX(), event.getY())) {
                                draggingHandle = true;
                                moveAccuracyHandleToScreen(event.getX(), event.getY());
                                return true; // consume; do not forward to map
                            }
                            downTime = System.currentTimeMillis();
                            downX = event.getX();
                            downY = event.getY();
                            forwarding = false;
                            postLongPress();
                            return true;
                        }

                        case MotionEvent.ACTION_MOVE: {
                            float dx = Math.abs(event.getX() - downX);
                            float dy = Math.abs(event.getY() - downY);
                            if (dx > dpToPx(TAP_SLOP_DP) || dy > dpToPx(TAP_SLOP_DP)) {
                                // became a drag → cancel long-press and forward as pan
                                cancelLongPress();
                                forwarding = true;
                                MotionEvent syntheticDown = cloneWithAction(event);
                                mapContent.dispatchTouchEvent(syntheticDown);
                                syntheticDown.recycle();
                                mapContent.dispatchTouchEvent(event);
                            }
                            return true;
                        }

                        case MotionEvent.ACTION_UP: {
                            cancelLongPress();
                            if (!longPressFired) {
                                // treat as tap if quick and small movement
                                long dt = System.currentTimeMillis() - downTime;
                                float dx = Math.abs(event.getX() - downX);
                                float dy = Math.abs(event.getY() - downY);
                                if (dt < TAP_TIMEOUT_MS && dx < dpToPx(TAP_SLOP_DP) && dy < dpToPx(TAP_SLOP_DP)) {
                                    v.performClick();
                                    android.graphics.Point p = new android.graphics.Point(Math.round(event.getX()), Math.round(event.getY()));
                                    LatLng tapped = mMap.getProjection().fromScreenLocation(p);
                                    handleUserTap(tapped);
                                } else {
                                    // end a late drag
                                    if (!forwarding) {
                                        forwarding = true;
                                        MotionEvent syntheticDown = cloneWithAction(event);
                                        mapContent.dispatchTouchEvent(syntheticDown);
                                        syntheticDown.recycle();
                                    }
                                    mapContent.dispatchTouchEvent(event);
                                    forwarding = false;
                                }
                            }
                            return true;
                        }

                        case MotionEvent.ACTION_CANCEL: {
                            cancelLongPress();
                            if (forwarding) {
                                mapContent.dispatchTouchEvent(event);
                                forwarding = false;
                            }
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

    }

    private void handleUserTap(@NonNull LatLng latLng) {
        latLong = latLng;
        Log.d(TAG, "New coordinates of the marker: " + latLng.latitude + ", " + latLng.longitude);

        updateMarkerAndCircle(false);

        editTextLatitude.setText(formatCoordinate(latLong.latitude));
        editTextLongitude.setText(formatCoordinate(latLong.longitude));

        updateElevationAndSave(latLong, true, false);
    }

    private void addZoomHandler(MarkerManager.Collection collection) {
        mMap.setOnCameraIdleListener(() -> {
            float zoomLevel = mMap.getCameraPosition().zoom;
            Log.d(TAG, "Zoom: " + zoomLevel + "; UTM shown: " + SettingsManager.isUtmShown());

            if (SettingsManager.isUtmShown()) {
                removeAllUtmMarkers();

                if (zoomLevel < 8) {
                    geoJsonHelper.hideUtmLines();
                } else {
                    geoJsonHelper.showUtmLines();
                }

                if (zoomLevel > 10) {
                    geoJsonHelper.showUtmLines();
                    final LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                    for (GeoJsonHelper.UTMName utmName : geoJsonHelper.getUtmNames()) {
                        if (bounds.contains(utmName.latLng())) {
                            IconGenerator iconFactory = new IconGenerator(ActivityMap.this);
                            iconFactory.setStyle(IconGenerator.STYLE_GREEN);
                            MarkerOptions markerOptions = new MarkerOptions()
                                    .icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(utmName.name())))
                                    .position(utmName.latLng())
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
        if (uri == null) {
            String savedUri = SettingsManager.getKmlFile();
            if (savedUri != null) {
                kmlHelper.drawKmlOverlay(Uri.parse(savedUri));
            }
        } else {
            String saved = kmlHelper.saveAndDrawKml(uri);
            if (saved != null) {
                SettingsManager.setKmlFile(saved);
                menuItemLoadKML.setIcon(R.drawable.ic_kmz_remove);
            }
        }
    }

    private void removeAllUtmMarkers() {
        for (int r = 0; r < utmMarkers.size(); r++) {
            Marker utmMarker = utmMarkers.get(r);
            utmMarker.remove();
        }
        utmMarkers.clear();
    }

    private void addLocationMarker(MarkerManager.Collection collection) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLong)
                .title(getString(R.string.you_are_here))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker))
                .zIndex(5000)
                .draggable(true);
        marker = collection.addMarker(markerOptions);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong, zoom_level));
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoom_level), 1000, null);
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
            geoJsonHelper.hideUtmLines();
            removeAllUtmMarkers();
        } else {
            Log.d(TAG, "UTM grid is hidden. Showing the grid now.");
            SettingsManager.setUtmShown(true);
            Objects.requireNonNull(menuItem.getIcon()).setAlpha(255);
            geoJsonHelper.showUtmLines();
        }
    }

    private void updateElevationAndSave(LatLng coordinates, boolean update_elevation, boolean save) {
        if (InternetConnection.isConnected(this)) {
            if (update_elevation) {
                // Sometimes the coordinates are null. Don't know why!
                if (coordinates == null) {
                    Log.w(TAG, "updateElevationAndSave called with null coordinates; skipping");
                    Toast.makeText(this, R.string.empty_coordinates, Toast.LENGTH_LONG).show();
                    if (save) saveAndExit();
                    return;
                }

                // Sometimes the database url is null.
                String databaseUrl = SettingsManager.getDatabaseName();
                if (databaseUrl == null || databaseUrl.isEmpty()) {
                    Log.w(TAG, "updateElevationAndSave called with null database URL; skipping");
                    Toast.makeText(this, R.string.empty_database_url, Toast.LENGTH_LONG).show();
                    return;
                }

                if (elevationCall != null && !elevationCall.isCanceled()) {
                    elevationCall.cancel();
                }

                elevationCall = RetrofitClient
                        .getService(databaseUrl)
                        .getElevation(coordinates.latitude, coordinates.longitude);

                Log.d(TAG, "Requesting altitude for Latitude: " + coordinates.latitude + "; Longitude: " + coordinates.longitude);
                elevationCall.enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ElevationResponse> call, @NonNull Response<ElevationResponse> response) {
                        if (response.body() != null && !isFinishing()) {
                            Long elev = response.body().getElevation();
                            elevation = elev != null ? String.valueOf(elev) : "0.0";
                            Log.d(TAG, "Elevation for this point is " + elevation + ".");
                            if (save) saveAndExit(); else editTextElevation.setText(elevation);
                        }
                        elevationCall = null;
                    }

                    @Override
                    public void onFailure(@NonNull Call<ElevationResponse> call, @NonNull Throwable t) {
                        if (!isFinishing()) {
                            Log.w(TAG, "Failed to fetch elevation: " + t.getMessage());
                            elevation = "0.0";
                            if (save) saveAndExit(); else editTextElevation.setText(elevation);
                        }
                        elevationCall = null;
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
        returnLocation.putExtra("google_map_accuracy", String.valueOf(safeAccuracyInt()));
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

    private void addSlider() {
        slider = findViewById(R.id.map_slider);
        float current_value = Float.parseFloat(accuracy);
        float limit = current_value >= 10000 ? 50000 :
                current_value >= 1000  ? 10000 :
                        current_value >= 500   ? 1000  :
                                current_value >= 100   ? 500   : 100;
        slider.setValueTo(limit);
        slider.setLabelFormatter(value -> getString(R.string.precision) + " " + Math.round(value) + " m");
        slider.addOnChangeListener((slider1, value, fromUser) -> {
            if (circle != null) circle.setRadius(value);
        });
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

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble("STATE_LATITUDE",  latLong.latitude);
        outState.putDouble("STATE_LONGITUDE", latLong.longitude);
        outState.putString("STATE_ACCURACY",  accuracy);
        outState.putString("STATE_ELEVATION", elevation);
        outState.putString("STATE_LOCATION_NAME", location_name);
    }

    private static String formatCoordinate(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag(Localisation.getLocaleScript()));
        DecimalFormat df = new DecimalFormat("#,##0.000000", symbols);
        return df.format(value);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                moveToCurrentLocation();
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateMarkerAndCircle(boolean update_radius) {
        if (marker != null) marker.setPosition(latLong);
        if (circle != null) {
            circle.setCenter(latLong);
            if (update_radius) {
                circle.setRadius(Double.parseDouble(accuracy));
            }
        }
    }

    private void startAccuracyHandle() {
        double rMeters = 0;
        try { rMeters = Double.parseDouble(accuracy); } catch (Exception ignored) {}
        if (rMeters <= 0) rMeters = 100;

        // Place handle east of center at current accuracy
        LatLng at = SphericalUtil.computeOffset(latLong, rMeters, /*bearing=*/90.0);

        if (accuracyHandle == null) {
            accuracyHandle = mMap.addMarker(
                    new MarkerOptions()
                            .position(at)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radius))
                            .zIndex(6000f)
                            .draggable(false)
                            .title(getString(R.string.precision))
            );
        } else {
            accuracyHandle.setPosition(at);
            accuracyHandle.setVisible(true);
        }
    }

    // Move handle to a screen x/y (map overlay touch → LatLng)
    private void moveAccuracyHandleToScreen(float x, float y) {
        if (mMap == null || accuracyHandle == null) return;
        android.graphics.Point p = new android.graphics.Point(Math.round(x), Math.round(y));
        LatLng pos = mMap.getProjection().fromScreenLocation(p);
        accuracyHandle.setPosition(pos);

        // Live update accuracy & circle
        double meters = SphericalUtil.computeDistanceBetween(latLong, pos);
        accuracy = String.format(Locale.ENGLISH, "%.0f", meters);
        if (circle != null) circle.setRadius(meters);
        if (slider != null) slider.setValue((float) meters);
        if (editTextPrecision != null) editTextPrecision.setText(accuracy);
        setSliderMeters((float) meters);
    }

    private void setSliderMeters(float meters) {
        if (slider == null) return;
        float max = slider.getValueTo();
        if (meters > max) slider.setValueTo(Math.min(meters + 50_0f, max * 5f)); // grow generously
        slider.setValue(Math.min(meters, slider.getValueTo()));
    }

    // Finish drag (finger up). You can also keep the handle visible for later drags.
    private void finishAccuracyHandleDrag() {
        draggingHandle = false;
        // Hide the handle after setting accuracy:
        if (accuracyHandle != null) accuracyHandle.setVisible(false);
    }

    // Hit-test helper: is (x,y) near the handle on screen?
    private boolean isNearHandle(float x, float y) {
        if (mMap == null || accuracyHandle == null || !accuracyHandle.isVisible()) return false;
        android.graphics.Point hp = mMap.getProjection().toScreenLocation(accuracyHandle.getPosition());
        float dx = Math.abs(x - hp.x);
        float dy = Math.abs(y - hp.y);
        float slop = getResources().getDisplayMetrics().density * HANDLE_HIT_SLOP_DP;
        return dx <= slop && dy <= slop;
    }

    private int safeAccuracyInt() {
        try { return Math.min(250000, Math.max(0, Integer.parseInt(accuracy))); }
        catch (Exception e) { return 0; }
    }

    @Override
    protected void onDestroy() {
        if (elevationCall != null && !elevationCall.isCanceled()) {
            Log.d(TAG, "Cancelling pending elevation request...");
            elevationCall.cancel();
            elevationCall = null;
        }
        locationNameHandler.removeCallbacksAndMessages(null);
        if (isFinishing() && kmlHelper != null) {
            String uri = SettingsManager.getKmlFile();
            if (uri != null) kmlHelper.removeKml(uri);
        }
        binding = null;
        super.onDestroy();
    }

}