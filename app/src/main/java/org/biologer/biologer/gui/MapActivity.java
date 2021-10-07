package org.biologer.biologer.gui;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.textview.MaterialTextView;

import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.network.InternetConnection;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.network.JSON.ElevationResponse;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "Biologer.GoogleMaps";

    private GoogleMap mMap;
    private String accuracy;
    private String elevation;
    ImageView floatButtonMapType;
    private LatLng latLong;
    String google_map_type = SettingsManager.getGoogleMapType();
    String database_name = SettingsManager.getDatabaseName();
    Circle circle;
    Marker marker;
    Marker temporaryMarker;
    MaterialTextView textView;

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
            latLong = bundle.getParcelable("LAT_LONG");
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

    // Add Save button in the right part of the toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu, menu);
        MenuItem item = menu.findItem(R.id.action_save);
        item.getIcon().setAlpha(255);
        return true;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Update text
        String accuracy_text = getString(R.string.accuracy_a1) +
                " " + accuracy + " " + getString(R.string.meter) + "\n" + getString(R.string.drag_marker);
        textView.setText(accuracy_text);

        // Select the type of the map according to the user’s settings
        if (google_map_type.equals("NORMAL")) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
        if (google_map_type.equals("SATELLITE")) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
        if (google_map_type.equals("TERRAIN")) {
            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }
        if (google_map_type.equals("HYBRID")) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }

        // Add marker at the GPS position on the map
        if (latLong.latitude == 0.0) {
            if (database_name.equals("https://biologer.hr")) {
                addMarker(new LatLng(45.5, 16.3), 7);
            }
            if (database_name.equals("https://biologer.ba")) {
                addMarker(new LatLng(44.3, 17.9), 7);
            }
            if (database_name.equals("https://biologer.org") || database_name.equals("https://dev.biologer.org")) {
                addMarker(new LatLng(44.1, 20.7), 7);
            }
        } else {
            addMarker(latLong, 16);
        }

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(@NonNull Marker marker) {
                Log.d(TAG, "Location at starting point: " + latLong.latitude + "; " + latLong.longitude + ".");
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radius));
                temporaryMarker = mMap.addMarker(new MarkerOptions().position(latLong));
                if (temporaryMarker != null) {
                    temporaryMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker));
                }
            }

            @Override
            public void onMarkerDrag(@NonNull Marker marker) {
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
            public void onMarkerDragEnd(@NonNull Marker marker) {
                Log.d(TAG, "Location at ending point: " + latLong.latitude + "; " + latLong.longitude + ".");
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker));
                marker.setPosition(latLong);
                temporaryMarker.remove();
            }

        });

        mMap.setOnMapClickListener(latLng -> {
            setLatLong(latLng.latitude, latLng.longitude);
            marker.setPosition(latLng);
            circle.setCenter(latLng);
        });

        addCircle();

    }

    private void addMarker(LatLng latLng, int zoom) {
        marker = mMap.addMarker(new MarkerOptions().position(latLng).title(getString(R.string.you_are_here)).draggable(true));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoom), 1000, null);
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker));
    }

    private void addCircle() {
        circle = mMap.addCircle(new CircleOptions()
                .center(latLong)
                .radius(Double.parseDouble(accuracy))
                .fillColor(0x66c5e1a5)
                .clickable(true)
                .strokeColor(0xff689f38)
                .strokeWidth(4.0f));
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
        returnLocation.putExtra("google_map_latLong", latLong);
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
}
