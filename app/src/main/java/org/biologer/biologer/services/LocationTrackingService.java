package org.biologer.biologer.services;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

import org.biologer.biologer.R;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

import java.util.ArrayList;
import java.util.List;

public class LocationTrackingService extends Service {

    private static final String TAG = "Biologer.TrackLocation";
    public static final String ACTION_PAUSE = "org.biologer.biologer.action.PAUSE";
    public static final String ACTION_RESUME = "org.biologer.biologer.action.RESUME";
    public static final String ACTION_STOP = "org.biologer.biologer.action.STOP";
    public static final String ACTION_LOCATION_UPDATE = "org.biologer.biologer.action.LOCATION_UPDATE";
    public static final String CURRENT_LOCATION = "current_location";
    public static final String ACTION_ROUTE_RESULT = "org.biologer.biologer.action.ROUTE_RESULT";
    public static final String WALKED_AREA = "walked_area";
    private boolean isTracking = true;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private final List<UTMPoint> routeUtmPoints = new ArrayList<>();
    private final CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
    private final CRSFactory crsFactory = new CRSFactory();
    private CoordinateTransform lonLatToUtmTransform;
    private int currentUtmZone = -1;

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!isTracking) return;

                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "Location: " + location.getLatitude() + ", " + location.getLongitude());

                    // Save location to route list
                    UTMPoint utmPoint = convertToUtm(location);
                    if (utmPoint != null) {
                        routeUtmPoints.add(utmPoint);
                    }

                    // Broadcast the location on user request (i.e. receive observation location)
                    Intent intent = new Intent(ACTION_LOCATION_UPDATE);
                    intent.putExtra(CURRENT_LOCATION, location);
                    LocalBroadcastManager.getInstance(LocationTrackingService.this).sendBroadcast(intent);
                }
            }
        };

        startForeground(1, createNotification());
        requestLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PAUSE:
                    isTracking = false;
                    Log.d(TAG, "Tracking paused.");
                    break;
                case ACTION_RESUME:
                    isTracking = true;
                    Log.d(TAG, "Tracking resumed.");
                    break;
                case ACTION_STOP:
                    Log.d(TAG, "Tracking completed, sending results...");
                    stopTrackingAndCalculate();
                    break;
            }
        }
        return START_STICKY;
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2500)
                .setMinUpdateIntervalMillis(1000)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, "biologer_location")
                .setContentTitle("Tracking route")
                .setContentText("GPS tracking is running...")
                .setSmallIcon(R.drawable.ic_notification_location)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not bound
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Also remove location tracking here as a fallback...
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    // A helper class to store UTM coordinates.
    public static class UTMPoint {
        public double easting;
        public double northing;
        public int utmZone;

        public UTMPoint(double easting, double northing, int utmZone) {
            this.easting = easting;
            this.northing = northing;
            this.utmZone = utmZone;
        }
    }

    // Convert geographic coordinates to UTM
    private UTMPoint convertToUtm(Location location) {
        // Determine the UTM zone based on longitude.
        int utmZone = (int) Math.floor((location.getLongitude() + 180) / 6) + 1;

        // If the UTM zone changes, we need to create a new transformation.
        if (utmZone != currentUtmZone) {
            currentUtmZone = utmZone;

            String[] utmParams = {
                    "+proj=utm",
                    "+zone=" + currentUtmZone,
                    "+ellps=WGS84",
                    "+datum=WGS84",
                    "+units=m",
                    "+no_defs"
            };
            CoordinateReferenceSystem utm_crs = crsFactory.createFromParameters("utm", utmParams);

            String[] wgs84Params = {
                    "+proj=longlat",
                    "+ellps=WGS84",
                    "+datum=WGS84",
                    "+no_defs"
            };
            CoordinateReferenceSystem wgs84_crs = crsFactory.createFromParameters("WGS84", wgs84Params);

            lonLatToUtmTransform = ctFactory.createTransform(wgs84_crs, utm_crs);
        }

        if (lonLatToUtmTransform != null) {
            ProjCoordinate lonLatCoordinates = new ProjCoordinate(location.getLongitude(), location.getLatitude());
            ProjCoordinate utmCoordinates = new ProjCoordinate();
            lonLatToUtmTransform.transform(lonLatCoordinates, utmCoordinates);

            Log.d(TAG, "UTM coordinates: " + utmCoordinates.x + ", " + utmCoordinates.y + " (zone " + utmZone + ").");
            return new UTMPoint(utmCoordinates.x, utmCoordinates.y, utmZone);
        }

        return null;
    }

    // Calculate area in square meters from UTM coordinates
    public double calculateArea() {
        if (routeUtmPoints.size() < 3) {
            return 0.0; // A polygon needs at least 3 vertices
        }

        // Use JTS to create a polygon and calculate its area.
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 0);

        // Convert your UTMPoints to JTS Coordinates
        Coordinate[] coordinates = new Coordinate[routeUtmPoints.size() + 1];
        for (int i = 0; i < routeUtmPoints.size(); i++) {
            UTMPoint utmPoint = routeUtmPoints.get(i);
            coordinates[i] = new Coordinate(utmPoint.easting, utmPoint.northing);
        }
        // Close the polygon by adding the first point again
        coordinates[routeUtmPoints.size()] = new Coordinate(routeUtmPoints.get(0).easting, routeUtmPoints.get(0).northing);

        LinearRing shell = geometryFactory.createLinearRing(coordinates);
        Polygon polygon = geometryFactory.createPolygon(shell);

        // The getArea() method of a JTS polygon returns the area in the units of the
        // coordinate system, which is square meters for UTM.
        return polygon.getArea();
    }

    private void stopTrackingAndCalculate() {
        fusedLocationClient.removeLocationUpdates(locationCallback);

        double totalArea = calculateArea();
        Log.d(TAG, "Total area walked: " + totalArea + " square meters.");

        // Broadcast the result back to the Activity
        Intent resultIntent = new Intent(ACTION_ROUTE_RESULT);
        resultIntent.putExtra(WALKED_AREA, totalArea);
        LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);

        stopSelf();
    }

}
