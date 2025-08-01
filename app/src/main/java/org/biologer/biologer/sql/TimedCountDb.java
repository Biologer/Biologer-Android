package org.biologer.biologer.sql;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class TimedCountDb {

    @Id(assignable = true)
    long id;

    private int timedCountId;
    private String startTime;
    private String endTime;
    private int countDurationMinutes;
    private int cloudCoverPercentage;
    private int atmosphericPressureHPa;
    private int humidityPercentage;
    private double temperatureCelsius;
    private String windDirection;
    private int windSpeed;
    private String habitat;
    private String taxonGroup;

    public TimedCountDb() {
        // Default constructor for ObjectBox
    }

    public TimedCountDb(long id, int timedCountId, String startTime, String endTime, int countDurationMinutes,
                        int cloudCoverPercentage, int atmosphericPressureHPa, int humidityPercentage,
                        double temperatureCelsius, String windDirection, int windSpeed, String habitat,
                        String taxonGroup) {
        this.id = id;
        this.timedCountId = timedCountId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.countDurationMinutes = countDurationMinutes;
        this.cloudCoverPercentage = cloudCoverPercentage;
        this.atmosphericPressureHPa = atmosphericPressureHPa;
        this.humidityPercentage = humidityPercentage;
        this.temperatureCelsius = temperatureCelsius;
        this.windDirection = windDirection;
        this.windSpeed = windSpeed;
        this.habitat = habitat;
        this.taxonGroup = taxonGroup;
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getTimedCountId() {
        return timedCountId;
    }

    public void setTimedCountId(int timedCountId) {
        this.timedCountId = timedCountId;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getCountDurationMinutes() {
        return countDurationMinutes;
    }

    public void setCountDurationMinutes(int countDurationMinutes) {
        this.countDurationMinutes = countDurationMinutes;
    }

    public int getCloudCoverPercentage() {
        return cloudCoverPercentage;
    }

    public void setCloudCoverPercentage(int cloudCoverPercentage) {
        this.cloudCoverPercentage = cloudCoverPercentage;
    }

    public int getAtmosphericPressureHPa() {
        return atmosphericPressureHPa;
    }

    public void setAtmosphericPressureHPa(int atmosphericPressureHPa) {
        this.atmosphericPressureHPa = atmosphericPressureHPa;
    }

    public int getHumidityPercentage() {
        return humidityPercentage;
    }

    public void setHumidityPercentage(int humidityPercentage) {
        this.humidityPercentage = humidityPercentage;
    }

    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(String windDirection) {
        this.windDirection = windDirection;
    }

    public int getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(int windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getHabitat() {
        return habitat;
    }

    public void setHabitat(String habitat) {
        this.habitat = habitat;
    }

    public String getTaxonGroup() {
        return taxonGroup;
    }

    public void setTaxonGroup(String taxonGroup) {
        this.taxonGroup = taxonGroup;
    }
}