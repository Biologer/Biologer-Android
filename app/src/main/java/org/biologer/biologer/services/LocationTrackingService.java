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

import org.biologer.biologer.R;

public class LocationTrackingService extends Service {

    private static final String TAG = "Biologer.TrackLocation";
    public static final String ACTION_PAUSE = "org.biologer.biologer.action.PAUSE";
    public static final String ACTION_RESUME = "org.biologer.biologer.action.RESUME";
    public static final String ACTION_LOCATION_UPDATE = "org.biologer.biologer.action.LOCATION_UPDATE";
    public static final String EXTRA_LOCATION = "extra_location";
    private boolean isTracking = true;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

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

                    // Broadcast the location on user request (i.e. receive observation location)
                    Intent intent = new Intent(ACTION_LOCATION_UPDATE);
                    intent.putExtra(EXTRA_LOCATION, location);
                    LocalBroadcastManager.getInstance(LocationTrackingService.this).sendBroadcast(intent);

                    // Save location to your route list
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
            }
        }

        return START_STICKY;
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(2500)
                .setFastestInterval(1000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY);

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
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
