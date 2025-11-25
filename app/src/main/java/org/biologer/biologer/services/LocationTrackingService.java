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

import org.biologer.biologer.helpers.NumbersHelper;
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
    public static final String WALKED_DISTANCE = "walked_distance";
    public static final String CENTROID_LATITUDE = "centroid_latitude";
    public static final String CENTROID_LONGITUDE = "centroid_longitude";
    public static final String GEOMETRY_LINESTRING_WKT = "geometry_linestring_wkt";
    private static final float MIN_DISTANCE_METERS = 2.5f; // Only record if moved at least 2.5m
    private static final float MAX_ACCURACY_METERS = 25.0f; // Discard points with precision worse than 25m
    private Location lastAcceptedLocation = null;
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
                    if (location.hasAccuracy() && location.getAccuracy() > MAX_ACCURACY_METERS) {
                        Log.w(TAG, "Location discarded: Accuracy (" + location.getAccuracy() +
                                "m) is worse than " + MAX_ACCURACY_METERS + "m.");
                        continue;
                    }

                    if (lastAcceptedLocation != null) {
                        float distance = location.distanceTo(lastAcceptedLocation);

                        if (distance < MIN_DISTANCE_METERS) {
                            Log.d(TAG, "Location discarded: Moved only " + distance +
                                    "m. Waiting for " + MIN_DISTANCE_METERS + "m.");
                            continue;
                        }
                    }

                    Log.d(TAG, "Location accepted. Accuracy: " + location.getAccuracy() + "m.");

                    lastAcceptedLocation = location;

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
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2500)
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

            //Log.d(TAG, "UTM coordinates: " + utmCoordinates.x + ", " + utmCoordinates.y + " (zone " + utmZone + ").");
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

    // Calculate total walked distance in meters from UTM coordinates
    public double calculateDistance() {
        if (routeUtmPoints.size() < 2) {
            return 0.0; // Need at least 2 points for distance
        }

        double totalDistance = 0.0;
        for (int i = 0; i < routeUtmPoints.size() - 1; i++) {
            UTMPoint p1 = routeUtmPoints.get(i);
            UTMPoint p2 = routeUtmPoints.get(i + 1);

            double dx = p2.easting - p1.easting;
            double dy = p2.northing - p1.northing;

            totalDistance += Math.sqrt(dx * dx + dy * dy);
        }

        return totalDistance;
    }

    /**
     * Transforms a coordinate from UTM back to WGS-84 (Longitude, Latitude).
     * @param utmCoordinate The ProjCoordinate (x=Easting, y=Northing) in UTM.
     * @param utmZone The UTM zone the coordinate belongs to.
     * @return An Android Location object with WGS-84 coordinates, or null on failure.
     */
    private Location transformUtmToWgs84(ProjCoordinate utmCoordinate, int utmZone) {
        if (utmZone == -1) {
            Log.e(TAG, "UTM zone not set. Cannot perform transformation.");
            return null;
        }

        CoordinateReferenceSystem utm_crs;
        CoordinateReferenceSystem wgs84_crs;
        CoordinateTransform utmToLonLatTransform;

        try {
            // 1. Define the UTM Coordinate Reference System (CRS)
            String[] utmParams = {
                    "+proj=utm",
                    "+zone=" + utmZone,
                    "+ellps=WGS84",
                    "+datum=WGS84",
                    "+units=m",
                    "+no_defs"
            };
            utm_crs = crsFactory.createFromParameters("utm_out", utmParams);

            // 2. Define the target WGS-84 CRS
            String[] wgs84Params = {
                    "+proj=longlat",
                    "+ellps=WGS84",
                    "+datum=WGS84",
                    "+no_defs"
            };
            wgs84_crs = crsFactory.createFromParameters("WGS84_out", wgs84Params);

            // 3. Create the transformation object (FROM UTM TO WGS84)
            utmToLonLatTransform = ctFactory.createTransform(utm_crs, wgs84_crs);
        } catch (Exception e) {
            Log.e(TAG, "Error creating coordinate transformation: " + e.getMessage());
            return null;
        }

        // 4. Perform the transformation
        ProjCoordinate lonLatCoordinates = new ProjCoordinate();
        utmToLonLatTransform.transform(utmCoordinate, lonLatCoordinates);

        // 5. Package and return the result
        Location centroidLocation = new Location("Transformed_Coordinate");
        // Proj4j uses x=longitude, y=latitude for longlat CRS
        centroidLocation.setLatitude(lonLatCoordinates.y);
        centroidLocation.setLongitude(lonLatCoordinates.x);
        return centroidLocation;
    }

    // The values is returned in WGS-84 coordinates
    private Location calculateCentroid() {
        int numPoints = routeUtmPoints.size();
        if (numPoints == 0) {
            return null;
        }

        int utmZone = currentUtmZone;

        // If there are less than 3 points calculate arithmetic mean
        double sumEasting = 0.0;
        double sumNorthing = 0.0;

        for (UTMPoint utmPoint : routeUtmPoints) {
            sumEasting += utmPoint.easting;
            sumNorthing += utmPoint.northing;
        }

        double centroidEasting = sumEasting / numPoints;
        double centroidNorthing = sumNorthing / numPoints;

        // If there are more than 3 points use JTS polygon centroid
        if (numPoints >= 3) {
            GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 0);

            Coordinate[] coordinates = new Coordinate[numPoints + 1];
            for (int i = 0; i < numPoints; i++) {
                UTMPoint utmPoint = routeUtmPoints.get(i);
                coordinates[i] = new Coordinate(utmPoint.easting, utmPoint.northing);
            }
            coordinates[numPoints] = new Coordinate(routeUtmPoints.get(0).easting, routeUtmPoints.get(0).northing);

            LinearRing shell = geometryFactory.createLinearRing(coordinates);
            Polygon polygon = geometryFactory.createPolygon(shell);

            org.locationtech.jts.geom.Point centroidUtm = polygon.getCentroid();

            centroidEasting = centroidUtm.getX();
            centroidNorthing = centroidUtm.getY();
        }

        ProjCoordinate utmCoordinates = new ProjCoordinate(centroidEasting, centroidNorthing);
        return transformUtmToWgs84(utmCoordinates, utmZone);
    }

    /**
     * Converts the entire list of UTM points back to WGS-84 using the existing
     * transformUtmToWgs84 helper, and formats them into a Well-Known Text (WKT) LINESTRING string.
     * @return The WKT LINESTRING string, or an empty string if no points exist.
     */
    private String createGeometryLinestringWkt() {
        if (routeUtmPoints.isEmpty()) {
            return "";
        }

        StringBuilder wktBuilder = new StringBuilder("LINESTRING(");
        int utmZone = currentUtmZone; // Use the zone from the last accepted point

        for (int i = 0; i < routeUtmPoints.size(); i++) {
            UTMPoint utmPoint = routeUtmPoints.get(i);
            ProjCoordinate utmCoordinates = new ProjCoordinate(utmPoint.easting, utmPoint.northing);
            Location wgs84Location = transformUtmToWgs84(utmCoordinates, utmZone);
            if (wgs84Location != null) {
                String formattedLon = NumbersHelper.formatValueEnglish(wgs84Location.getLongitude(), 6);
                String formattedLat = NumbersHelper.formatValueEnglish(wgs84Location.getLatitude(), 6);
                wktBuilder.append(formattedLon).append(" ").append(formattedLat);
                if (i < routeUtmPoints.size() - 1) {
                    wktBuilder.append(",");
                }
            }
        }

        wktBuilder.append(")");
        return wktBuilder.toString();
    }

    private void stopTrackingAndCalculate() {
        fusedLocationClient.removeLocationUpdates(locationCallback);

        double totalArea = calculateArea();
        double totalDistance = calculateDistance();
        Location centroid = calculateCentroid();
        String geometry = createGeometryLinestringWkt();
        Log.d(TAG, "Total area walked: " + totalArea + " m2 (distance = " + totalDistance + " m).");

        // Broadcast the result back to the Activity
        Intent resultIntent = new Intent(ACTION_ROUTE_RESULT);
        resultIntent.putExtra(WALKED_AREA, totalArea);
        resultIntent.putExtra(WALKED_DISTANCE, totalDistance);
        if (centroid != null) {
            double longitude = centroid.getLongitude();
            double latitude = centroid.getLatitude();
            resultIntent.putExtra(CENTROID_LONGITUDE, longitude);
            resultIntent.putExtra(CENTROID_LATITUDE, latitude);
            Log.d(TAG, "Centroid (WGS84): " + centroid.getLongitude() + ", " + centroid.getLatitude());
        }
        resultIntent.putExtra(GEOMETRY_LINESTRING_WKT, geometry);
        Log.d(TAG, "Geometry of the line: " + geometry);
        LocalBroadcastManager.getInstance(this).sendBroadcast(resultIntent);

        stopSelf();
    }

}
