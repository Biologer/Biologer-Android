package org.biologer.biologer.services;

import android.location.Location;

public interface LocationResultCallback {
    void onLocationSuccess(Location location);
    void onLocationFailure(String errorMessage);
}