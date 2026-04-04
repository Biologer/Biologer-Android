package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.biologer.biologer.sql.TimedCountDb;

public class APITimedCounts {

    @JsonProperty("end_time")
    private String endTime;
    @JsonProperty("start_time")
    private String startTime;
    @JsonProperty("count_duration")
    private Integer countDurationMinutes;
    @JsonProperty("area")
    private Integer walkedArea;
    @JsonProperty("distance")
    private Integer walkedDistance;
    @JsonProperty("longitude")
    private double longitude;
    @JsonProperty("latitude")
    private double latitude;
    @JsonProperty("geometry")
    private String geometry;
    @JsonProperty("cloud_cover")
    private Integer cloudCoverPercentage;
    @JsonProperty("atmospheric_pressure")
    private Integer atmosphericPressureHPa;
    @JsonProperty("humidity")
    private Integer humidityPercentage;
    @JsonProperty("temperature")
    private Double temperatureCelsius;
    @JsonProperty("wind_direction")
    private String windDirection;
    @JsonProperty("wind_speed")
    private Integer windSpeed;
    @JsonProperty("habitat")
    private String habitat;
    @JsonProperty("view_groups_id")
    private Long taxonGroup;
    @JsonProperty("comments")
    private String comment;
    @JsonProperty("day")
    private Integer day;
    @JsonProperty("month")
    private Integer month;
    @JsonProperty("year")
    private Integer year;

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setCountDurationMinutes(Integer countDurationMinutes) {
        this.countDurationMinutes = countDurationMinutes;
    }

    public void setWalkedArea(Integer walkedArea) {
        this.walkedArea = walkedArea;
    }

    public void setWalkedDistance(Integer walkedDistance) {
        this.walkedDistance = walkedDistance;
    }

    public void setCloudCoverPercentage(Integer cloudCoverPercentage) {
        this.cloudCoverPercentage = cloudCoverPercentage;
    }

    public void setAtmosphericPressureHPa(Integer atmosphericPressureHPa) {
        this.atmosphericPressureHPa = atmosphericPressureHPa;
    }

    public void setHumidityPercentage(Integer humidityPercentage) {
        this.humidityPercentage = humidityPercentage;
    }

    public void setTemperatureCelsius(Double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public void setWindDirection(String windDirection) {
        this.windDirection = windDirection;
    }

    public void setWindSpeed(Integer windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getHabitat() {
        return habitat;
    }

    public void setHabitat(String habitat) {
        this.habitat = habitat;
    }

    public void setTaxonGroup(Long taxonGroup) {
        this.taxonGroup = taxonGroup;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public void getFromTimedCountDatabase(TimedCountDb db) {
        setStartTime(db.getStartTime() + ":00");
        setEndTime(db.getEndTime() + ":00");
        setCountDurationMinutes(db.getCountDurationMinutes());
        setWalkedArea(db.getWalkedArea());
        setWalkedDistance(db.getWalkedDistance());
        setCloudCoverPercentage(db.getCloudCoverPercentage());
        setAtmosphericPressureHPa(db.getAtmosphericPressureHPa());
        setHumidityPercentage(db.getHumidityPercentage());
        setTemperatureCelsius(db.getTemperatureCelsius());
        setWindDirection(db.getWindDirection());
        setWindSpeed(db.getWindSpeed());
        setHabitat(db.getHabitat());
        setTaxonGroup(db.getNewTaxonGroup());
        setComment(db.getComment());
        setDay(db.getNewDay());
        int real_month = db.getNewMonth() + 1; // Add 1 since months range from 0 to 11
        setMonth(real_month);
        setYear(db.getNewYear());
        setLongitude(db.getLongitude());
        setLatitude(db.getLatitude());
        setGeometry(db.getGeometry());
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public String getStartTime() {
        return startTime;
    }
}
