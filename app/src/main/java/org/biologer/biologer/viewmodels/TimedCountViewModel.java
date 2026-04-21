package org.biologer.biologer.viewmodels;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.biologer.biologer.sql.TimedCountDb;

import java.util.ArrayList;
import java.util.List;

public class TimedCountViewModel extends ViewModel {

    private final MutableLiveData<Double> temperatureData = new MutableLiveData<>();
    private final MutableLiveData<Integer> cloudinessData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> speciesDataChanged = new MutableLiveData<>(false);
    private Integer windSpeedData = null;
    private String windDirectionData = null;
    private Integer pressureData = null;
    private Integer humidityData = null;
    private String habitatData = null;
    private String commentData = null;
    private Long taxonId = null;
    private Long serverId = null;
    private Long objectBoxId = null;
    private boolean uploaded;
    private Long taxonGroupId = null;
    private final MutableLiveData<Long> elapsedTime = new MutableLiveData<>(0L);
    private Boolean isRunning = null;
    private Boolean isModified = false;
    private String startTimeString = null;
    private String endTimeString = null;
    private Integer area = 0;
    private Integer distance = 0;
    private final List<Long> newEntryIds = new ArrayList<>();
    private long startTime = 0L;
    private long pausedTime = 0L;
    private int countDuration = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Boolean newEntry = false;
    private double centroidLongitude;
    private double centroidLatitude;
    private String geometry;
    private Integer day = null;
    private Integer month = null;
    private Integer year = null;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (Boolean.TRUE.equals(isRunning)) {
                long now = System.currentTimeMillis();
                elapsedTime.setValue((now - startTime) + pausedTime);
                handler.postDelayed(this, 1000); // update every second
            }
        }
    };

    public void setTemperatureData(Double data) {
        setModified(true);
        temperatureData.setValue(data);
    }

    public LiveData<Double> getTemperatureData() {
        return temperatureData;
    }

    public LiveData<Long> getElapsedTime() {
        return elapsedTime;
    }

    public void setCloudinessData(Integer data) {
        setModified(true);
        cloudinessData.setValue(data);
    }
    public LiveData<Integer> getCloudinessData() {
        return cloudinessData;
    }

    public LiveData<Boolean> getSpeciesDataChanged() {
        return speciesDataChanged;
    }

    public void setSpeciesChanged(boolean changed) {
        speciesDataChanged.postValue(changed);
    }

    public void startTimer() {
        if (Boolean.TRUE.equals(isRunning)) return;
        startTime = System.currentTimeMillis();
        isRunning = true;
        handler.post(ticker);
    }

    public void pauseTimer() {
        if (!Boolean.TRUE.equals(isRunning)) return;
        pausedTime = elapsedTime.getValue() != null ? elapsedTime.getValue() : 0L;
        isRunning = false;
        handler.removeCallbacks(ticker);
    }

    public void resetTimer() {
        handler.removeCallbacks(ticker);
        isRunning = false;
        elapsedTime.setValue(0L);
        startTime = 0L;
        pausedTime = 0L;
    }

    public void getFromObjectBox(TimedCountDb timedCount) {
        startTimeString = timedCount.getStartTime();
        endTimeString = timedCount.getEndTime();
        countDuration = timedCount.getCountDurationMinutes() != null ? timedCount.getCountDurationMinutes() : 0;
        area = timedCount.getWalkedArea();
        distance = timedCount.getWalkedDistance();
        centroidLongitude = timedCount.getLongitude();
        centroidLatitude = timedCount.getLatitude();
        geometry = timedCount.getGeometry();
        taxonGroupId = timedCount.getNewTaxonGroup();
        temperatureData.setValue(timedCount.getTemperatureCelsius());
        cloudinessData.setValue(timedCount.getCloudCoverPercentage());
        windSpeedData = timedCount.getWindSpeed();
        windDirectionData = timedCount.getWindDirection();
        pressureData = timedCount.getAtmosphericPressureHPa();
        humidityData = timedCount.getHumidityPercentage();
        habitatData = timedCount.getHabitat();
        commentData = timedCount.getComment();
        isRunning = false;
        newEntry = false;
        serverId = timedCount.getServerId();
        objectBoxId = timedCount.getId();
        day = timedCount.getNewDay();
        month = timedCount.getNewMonth();
        year = timedCount.getNewYear();
        uploaded = timedCount.isUploaded();
        isModified = timedCount.isModified();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        handler.removeCallbacks(ticker);
    }

    public List<Long> getNewEntryIds() {
        return newEntryIds;
    }

    public void addNewEntryId(long newId) {
        newEntryIds.add(newId);
    }

    public Boolean getNewEntry() {
        return newEntry;
    }

    public boolean isNewEntry() {
        return Boolean.TRUE.equals(getNewEntry());
    }

    public void setNewEntry(Boolean newEntry) {
        this.newEntry = newEntry;
    }

    public Boolean getIsRunning() {
        return isRunning;
    }

    public boolean isRunning() {
        return Boolean.TRUE.equals(getIsRunning());
    }

    public Integer getWindSpeedData() {
        return windSpeedData;
    }

    public void setWindSpeedData(Integer windSpeedData) {
        setModified(true);
        this.windSpeedData = windSpeedData;
    }

    public String getWindDirectionData() {
        return windDirectionData;
    }

    public void setWindDirectionData(String windDirectionData) {
        setModified(true);
        this.windDirectionData = windDirectionData;
    }

    public Integer getPressureData() {
        return pressureData;
    }

    public void setPressureData(Integer pressureData) {
        setModified(true);
        this.pressureData = pressureData;
    }

    public Integer getHumidityData() {
        return humidityData;
    }

    public void setHumidityData(Integer humidityData) {
        setModified(true);
        this.humidityData = humidityData;
    }

    public String getHabitatData() {
        return habitatData;
    }

    public void setHabitatData(String habitatData) {
        setModified(true);
        this.habitatData = habitatData;
    }

    public String getCommentData() {
        return commentData;
    }

    public void setCommentData(String commentData) {
        setModified(true);
        this.commentData = commentData;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public void setTaxonId(Long taxonId) {
        this.taxonId = taxonId;
    }

    public String getStartTimeString() {
        return startTimeString;
    }

    public void setStartTimeString(String startTimeString) {
        this.startTimeString = startTimeString;
    }

    public String getEndTimeString() {
        return endTimeString;
    }

    public void setEndTimeString(String endTimeString) {
        this.endTimeString = endTimeString;
    }

    public Boolean getModified() {
        return isModified;
    }

    public void setModified(Boolean modified) {
        isModified = modified;
    }

    public Boolean isModified() {
        return Boolean.TRUE.equals(getModified());
    }

    public Integer getArea() {
        return area;
    }

    public void setArea(Integer area) {
        this.area = area;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    public int getCountDuration() {
        return countDuration;
    }

    public void setCountDuration(int countDuration) {
        this.countDuration = countDuration;
    }

    public Long getTaxonGroupId() {
        return taxonGroupId;
    }

    public void setTaxonGroupId(Long taxonGroupId) {
        this.taxonGroupId = taxonGroupId;
    }

    public double getCentroidLongitude() {
        return centroidLongitude;
    }

    public void setCentroidLongitude(double centroidLongitude) {
        this.centroidLongitude = centroidLongitude;
    }

    public double getCentroidLatitude() {
        return centroidLatitude;
    }

    public void setCentroidLatitude(double centroidLatitude) {
        this.centroidLatitude = centroidLatitude;
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

    public Long getObjectBoxId() {
        return objectBoxId;
    }

    public void setObjectBoxId(Long objectBoxId) {
        this.objectBoxId = objectBoxId;
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
}
