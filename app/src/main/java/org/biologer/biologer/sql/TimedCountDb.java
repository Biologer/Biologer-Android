package org.biologer.biologer.sql;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class TimedCountDb {

    @Id(assignable = true)
    long id;
    private Integer timedCountId;
    private String startTime;
    private String endTime;
    private Integer countDurationMinutes;
    private Integer walkedArea;
    private Integer walkedDistance;
    private Integer cloudCoverPercentage;
    private Integer atmosphericPressureHPa;
    private Integer humidityPercentage;
    private Double temperatureCelsius;
    private String windDirection;
    private Integer windSpeed;
    private String habitat;
    private String taxonGroup;
    private String comment;
    private String day;
    private String month;
    private String year;

    public TimedCountDb() {
    }
    public TimedCountDb(long id, Integer timedCountId, String startTime, String endTime, Integer countDurationMinutes,
                        Integer walkedArea, Integer walkedDistance, Integer cloudCoverPercentage,
                        Integer atmosphericPressureHPa, Integer humidityPercentage,
                        Double temperatureCelsius, String windDirection, Integer windSpeed, String habitat,
                        String comment, String taxonGroup, String day, String month, String year) {
        this.id = id;
        this.timedCountId = timedCountId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.countDurationMinutes = countDurationMinutes;
        this.walkedArea = walkedArea;
        this.walkedDistance = walkedDistance;
        this.cloudCoverPercentage = cloudCoverPercentage;
        this.atmosphericPressureHPa = atmosphericPressureHPa;
        this.humidityPercentage = humidityPercentage;
        this.temperatureCelsius = temperatureCelsius;
        this.windDirection = windDirection;
        this.windSpeed = windSpeed;
        this.habitat = habitat;
        this.comment = comment;
        this.taxonGroup = taxonGroup;
        this.day = day;
        this.month = month;
        this.year = year;
    }

    public long getId() {
        return id;
    }
    public Integer getTimedCountId() {
        return timedCountId;
    }

    public void setTimedCountId(Integer timedCountId) {
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

    public Integer getCountDurationMinutes() {
        return countDurationMinutes;
    }

    public void setCountDurationMinutes(Integer countDurationMinutes) {
        this.countDurationMinutes = countDurationMinutes;
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
}