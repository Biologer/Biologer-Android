package org.biologer.biologer.services;

public class UTMPoint {
    private final double easting;
    private final double northing;
    private final int utmZone;
    private final boolean isNorth;

    public UTMPoint(double easting, double northing, int utmZone, boolean isNorth) {
        this.easting = easting;
        this.northing = northing;
        this.utmZone = utmZone;
        this.isNorth = isNorth;
    }

    public double getEasting() {
        return easting;
    }

    public double getNorthing() {
        return northing;
    }

    public int getUtmZone() {
        return utmZone;
    }

    public boolean isNorth() {
        return isNorth;
    }

}