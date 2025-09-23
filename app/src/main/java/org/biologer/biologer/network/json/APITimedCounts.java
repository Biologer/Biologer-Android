package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.biologer.biologer.sql.TimedCountDb;

public class APITimedCounts {

    @JsonProperty("start_time")
    private String startTime;
    @JsonProperty("end_time")
    private String endTime;
    @JsonProperty("count_duration")
    private Integer countDurationMinutes;
    @JsonProperty("area")
    private Integer walkedArea;
    @JsonProperty("distance")
    private Integer walkedDistance;
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
    private String taxonGroup;
    @JsonProperty("comment")
    private String comment;
    @JsonProperty("day")
    private String day;
    @JsonProperty("month")
    private String month;
    @JsonProperty("year")
    private String year;

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

    public Integer getCountDurationMinutes() {
        return countDurationMinutes;
    }

    public void setCountDurationMinutes(Integer countDurationMinutes) {
        this.countDurationMinutes = countDurationMinutes;
    }

    public Integer getWalkedArea() {
        return walkedArea;
    }

    public void setWalkedArea(Integer walkedArea) {
        this.walkedArea = walkedArea;
    }

    public Integer getWalkedDistance() {
        return walkedDistance;
    }

    public void setWalkedDistance(Integer walkedDistance) {
        this.walkedDistance = walkedDistance;
    }

    public Integer getCloudCoverPercentage() {
        return cloudCoverPercentage;
    }

    public void setCloudCoverPercentage(Integer cloudCoverPercentage) {
        this.cloudCoverPercentage = cloudCoverPercentage;
    }

    public Integer getAtmosphericPressureHPa() {
        return atmosphericPressureHPa;
    }

    public void setAtmosphericPressureHPa(Integer atmosphericPressureHPa) {
        this.atmosphericPressureHPa = atmosphericPressureHPa;
    }

    public Integer getHumidityPercentage() {
        return humidityPercentage;
    }

    public void setHumidityPercentage(Integer humidityPercentage) {
        this.humidityPercentage = humidityPercentage;
    }

    public Double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(Double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(String windDirection) {
        this.windDirection = windDirection;
    }

    public Integer getWindSpeed() {
        return windSpeed;
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

    public String getTaxonGroup() {
        return taxonGroup;
    }

    public void setTaxonGroup(String taxonGroup) {
        this.taxonGroup = taxonGroup;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
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
        setTaxonGroup(db.getTaxonGroup());
        setComment(db.getComment());
        setDay(db.getDay());
        int real_month = Integer.parseInt(db.getMonth()) + 1; // Add 1 since months range from 0 to 11
        setMonth(String.valueOf(real_month));
        setYear(db.getYear());
    }
}
