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

    private static final String TAG = "Biologer.TimedCountVM";
    private final MutableLiveData<Double> temperatureData = new MutableLiveData<>();
    private final MutableLiveData<Integer> cloudinessData = new MutableLiveData<>();
    private Integer windSpeedData = null;
    private String windDirectionData = null;
    private Integer pressureData = null;
    private Integer humidityData = null;
    private String habitatData = null;
    private String commentData = null;
    private Long taxonId = null;
    private Long taxonGroupId = null;
    private Integer timedCountId = null;
    private final MutableLiveData<Long> elapsedTime = new MutableLiveData<>(0L);
    private Boolean isRunning = null;
    private Boolean isModified = null;
    private String startTimeString = null;
    private String endTimeString = null;
    private Integer area = null;
    private Integer distance = null;
    private final List<Long> newEntryIds = new ArrayList<>();
    private long startTime = 0L;
    private long pausedTime = 0L;
    private int countDuration = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Boolean newEntry = false;

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

    public void setElapsedTime(Long data) {
        elapsedTime.setValue(data);
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
        setTemperatureData(timedCount.getTemperatureCelsius());
        setCloudinessData(timedCount.getCloudCoverPercentage());
        setWindSpeedData(timedCount.getWindSpeed());
        setWindDirectionData(timedCount.getWindDirection());
        setPressureData(timedCount.getAtmosphericPressureHPa());
        setHumidityData(timedCount.getHumidityPercentage());
        setHabitatData(timedCount.getHabitat());
        setCommentData(timedCount.getComment());
        setTimedCountId(timedCount.getTimedCountId());
        setStartTimeString(timedCount.getStartTime());
        setEndTimeString(timedCount.getEndTime());
        setIsRunning(false);
        setNewEntry(false);
        setModified(false);
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
        setModified(true);
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

    public void setIsRunning(Boolean isRunning) {
        this.isRunning = isRunning;
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
        setModified(true);
        this.taxonId = taxonId;
    }

    public Integer getTimedCountId() {
        return timedCountId;
    }

    public void setTimedCountId(Integer timedCountId) {
        this.timedCountId = timedCountId;
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
}
