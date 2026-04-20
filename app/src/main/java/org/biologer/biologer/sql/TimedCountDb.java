package org.biologer.biologer.sql;

import org.biologer.biologer.network.json.TimedCountData;
import org.biologer.biologer.viewmodels.TimedCountViewModel;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class TimedCountDb {

    @Id(assignable = true)
    long id;
    private Long serverId;
    private boolean uploaded;
    private boolean modified;
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
    private String comment;
    private double longitude;
    private double latitude;
    private String geometry;
    public ToMany<ObservationActivityDb> observationActivity;

    // Correct variable types, should be made default in the future
    private Integer newDay;
    private Integer newMonth;
    private Integer newYear;
    private Long newTaxonGroup;

    // Obsolete, should be removed
    private String day;
    private String month;
    private String year;
    private String taxonGroup;


    public TimedCountDb() {
    }
    public TimedCountDb(long id, Long serverId, boolean uploaded, boolean modified,
                        String startTime, String endTime, Integer countDurationMinutes,
                        Integer walkedArea, Integer walkedDistance, Integer cloudCoverPercentage,
                        Integer atmosphericPressureHPa, Integer humidityPercentage,
                        Double temperatureCelsius, String windDirection, Integer windSpeed, String habitat,
                        String comment, Long newTaxonGroup, Integer newDay, Integer newMonth, Integer newYear,
                        double longitude, double latitude, String geometry) {
        this.id = id;
        this.serverId = serverId;
        this.uploaded = uploaded;
        this.modified = modified;
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
        this.newTaxonGroup = newTaxonGroup;
        this.newDay = newDay;
        this.newMonth = newMonth;
        this.newYear = newYear;
        this.longitude = longitude;
        this.latitude = latitude;
        this.geometry = geometry;
    }

    public static TimedCountDb getTimedCountFromData(TimedCountData data) {
        return new TimedCountDb(
                0L,
                data.getId(),
                true,
                false,
                data.getStartTime(),
                data.getEndTime(),
                data.getCountDuration(),
                data.getArea(),
                data.getRouteLength(),
                data.getCloudCover(),
                data.getAtmosphericPressure(),
                data.getHumidity(),
                data.getTemperature(),
                data.getWindDirection(),
                data.getWindSpeed(),
                data.getHabitat(),
                data.getComments(),
                data.getViewGroup().getId(),
                data.getDay(),
                data.getMonth(),
                data.getYear(),
                data.getLongitude() != null ? data.getLongitude() : 0,
                data.getLatitude() != null ? data.getLatitude(): 0,
                data.getGeometry()
        );
    }

    public void getFromViewModel(TimedCountViewModel viewModel) {
        setServerId(viewModel.getServerId());
        setUploaded(viewModel.isUploaded());
        setModified(true);
        setStartTime(viewModel.getStartTimeString());
        setEndTime(viewModel.getEndTimeString());
        setCountDurationMinutes(viewModel.getCountDuration());
        setWalkedArea(viewModel.getArea());
        setWalkedDistance(viewModel.getDistance());
        if (viewModel.getCloudinessData() != null && viewModel.getCloudinessData().getValue() != null) {
            setCloudCoverPercentage(viewModel.getCloudinessData().getValue());
        }
        if (viewModel.getPressureData() != null) {
            setAtmosphericPressureHPa(viewModel.getPressureData());
        }
        if (viewModel.getHumidityData() != null) {
            setHumidityPercentage(viewModel.getHumidityData());
        }
        if (viewModel.getTemperatureData() != null && viewModel.getTemperatureData().getValue() != null) {
            setTemperatureCelsius(viewModel.getTemperatureData().getValue());
        }
        setWindDirection(viewModel.getWindDirectionData());
        setWindSpeed(viewModel.getWindSpeedData());
        setHabitat(viewModel.getHabitatData());
        setComment(viewModel.getCommentData());
        setNewTaxonGroup(viewModel.getTaxonGroupId());
        setNewDay(viewModel.getDay());
        setNewMonth(viewModel.getMonth());
        setNewYear(viewModel.getYear());
        setLongitude(viewModel.getCentroidLongitude());
        setLatitude(viewModel.getCentroidLatitude());
        setGeometry(viewModel.getGeometry());
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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

    public Long getServerId() {
        return serverId;
    }

    public void setServerId(Long serverId) {
        this.serverId = serverId;
    }

    public boolean isUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public static void syncTimeCountFieldsFromData(TimedCountDb existing, TimedCountData serverData) {

        existing.setServerId(serverData.getId() != null ? serverData.getId() : 0);
        existing.setUploaded(true);
        existing.setModified(false);
        existing.setStartTime(serverData.getStartTime());
        existing.setEndTime(serverData.getEndTime());
        existing.setCountDurationMinutes(serverData.getCountDuration());
        existing.setWalkedArea(serverData.getArea());
        existing.setWalkedDistance(serverData.getRouteLength());
        existing.setCloudCoverPercentage(serverData.getCloudCover());
        existing.setAtmosphericPressureHPa(serverData.getAtmosphericPressure());
        existing.setHumidityPercentage(serverData.getHumidity());
        existing.setTemperatureCelsius(serverData.getTemperature());
        existing.setWindDirection(serverData.getWindDirection());
        existing.setWindSpeed(serverData.getWindSpeed());
        existing.setHabitat(serverData.getHabitat());
        existing.setComment(serverData.getComments());
        existing.setNewTaxonGroup(serverData.getViewGroup().getId());
        existing.setNewDay(serverData.getDay());
        existing.setNewMonth(serverData.getMonth());
        existing.setNewYear(serverData.getYear());
        existing.setLongitude(serverData.getLongitude());
        existing.setLatitude(serverData.getLatitude());
        existing.setGeometry(serverData.getGeometry());

    }

    public Integer getNewDay() {
        return newDay;
    }

    public void setNewDay(Integer newDay) {
        this.newDay = newDay;
    }

    public Integer getNewMonth() {
        return newMonth;
    }

    public void setNewMonth(Integer newMonth) {
        this.newMonth = newMonth;
    }

    public Integer getNewYear() {
        return newYear;
    }

    public void setNewYear(Integer newYear) {
        this.newYear = newYear;
    }

    public Long getNewTaxonGroup() {
        return newTaxonGroup;
    }

    public void setNewTaxonGroup(Long newTaxonGroup) {
        this.newTaxonGroup = newTaxonGroup;
    }

    public String getDay() {
        return day;
    }

    public String getMonth() {
        return month;
    }

    public String getYear() {
        return year;
    }

    public String getTaxonGroup() {
        return taxonGroup;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public void setTaxonGroup(String taxonGroup) {
        this.taxonGroup = taxonGroup;
    }
}