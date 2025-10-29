package org.biologer.biologer.helpers;

public class WeatherUtils {

    /**
     * Converts wind direction from degrees to a cardinal direction.
     * @param degrees The wind direction in degrees (0-360).
     * @return A string representing the cardinal direction (e.g., "N", "SW").
     */
    public static String getWindDirection(int degrees) {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
        return directions[(int) Math.round(((double) degrees % 360) / 45)];
    }

    /**
     * Converts wind speed from meters per second (m/s) to the Beaufort scale.
     * @param speed The wind speed in meters per second.
     * @return An integer representing the Beaufort scale number (0-12).
     */
    public static int getBeaufortScale(double speed) {
        if (speed < 0.3) {
            return 0;
        } else if (speed <= 1.5) {
            return 1;
        } else if (speed <= 3.3) {
            return 2;
        } else if (speed <= 5.4) {
            return 3;
        } else if (speed <= 7.9) {
            return 4;
        } else if (speed <= 10.7) {
            return 5;
        } else if (speed <= 13.8) {
            return 6;
        } else if (speed <= 17.1) {
            return 7;
        } else if (speed <= 20.7) {
            return 8;
        } else if (speed <= 24.4) {
            return 9;
        } else if (speed <= 28.4) {
            return 10;
        } else if (speed <= 32.6) {
            return 11;
        } else {
            return 12;
        }
    }
}