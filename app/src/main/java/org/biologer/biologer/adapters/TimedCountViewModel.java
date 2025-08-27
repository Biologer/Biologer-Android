package org.biologer.biologer.adapters;


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
    private final MutableLiveData<Integer> windSpeedData = new MutableLiveData<>();
    private final MutableLiveData<String> windDirectionData = new MutableLiveData<>();
    private final MutableLiveData<Integer> pressureData = new MutableLiveData<>();
    private final MutableLiveData<Integer> humidityData = new MutableLiveData<>();
    private final MutableLiveData<String> habitatData = new MutableLiveData<>();
    private final MutableLiveData<String> commentData = new MutableLiveData<>();
    private final MutableLiveData<Long> taxonId = new MutableLiveData<>();
    private final MutableLiveData<Integer> timedCountId = new MutableLiveData<>();
    private final MutableLiveData<Long> elapsedTime = new MutableLiveData<>(0L);
    private final MutableLiveData<Boolean> isRunning = new MutableLiveData<>(false);
    private final MutableLiveData<String> startTimeString = new MutableLiveData<>();
    private final MutableLiveData<String> endTimeString = new MutableLiveData<>();
    private List<Long> newEntryIds = new ArrayList<>();
    private long startTime = 0L;
    private long pausedTime = 0L;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (Boolean.TRUE.equals(isRunning.getValue())) {
                long now = System.currentTimeMillis();
                elapsedTime.setValue((now - startTime) + pausedTime);
                handler.postDelayed(this, 1000); // update every second
            }
        }
    };


    public void setTemperatureData(Double data) {
        temperatureData.setValue(data);
    }
    public LiveData<Double> getTemperatureData() {
        return temperatureData;
    }

    public void setCloudinessData(Integer data) {
        cloudinessData.setValue(data);
    }
    public LiveData<Integer> getCloudinessData() {
        return cloudinessData;
    }

    public MutableLiveData<Integer> getWindSpeedData() {
        return windSpeedData;
    }

    public void setWindSpeedData(Integer data) {
        windSpeedData.setValue(data);
    }

    public MutableLiveData<String> getWindDirectionData() {
        return windDirectionData;
    }

    public void setWindDirectionData(String data) {
        windDirectionData.setValue(data);
    }

    public MutableLiveData<Integer> getPressureData() {
        return pressureData;
    }

    public void setPressureData(Integer data) {
        pressureData.setValue(data);
    }

    public MutableLiveData<Integer> getHumidityData() {
        return humidityData;
    }

    public void setHumidityData(Integer data) {
        humidityData.setValue(data);
    }

    public MutableLiveData<String> getHabitatData() {
        return habitatData;
    }

    public void setHabitatData(String data) {
        habitatData.setValue(data);
    }

    public MutableLiveData<String> getCommentData() {
        return commentData;
    }

    public void setCommentData(String data) {
        commentData.setValue(data);
    }

    public MutableLiveData<Integer> getTimedCountId() {
        return timedCountId;
    }

    public void setTimedCountId(Integer data) {
        timedCountId.setValue(data);
    }

    public LiveData<Long> getElapsedTime() {
        return elapsedTime;
    }

    public LiveData<Boolean> getIsRunning() {
        return isRunning;
    }

    public MutableLiveData<Long> getTaxonId() {
        return taxonId;
    }

    public void setTaxonId(Long data) {
        taxonId.setValue(data);
    }

    public void startTimer() {
        if (Boolean.TRUE.equals(isRunning.getValue())) return;
        startTime = System.currentTimeMillis();
        isRunning.setValue(true);
        handler.post(ticker);
    }

    public void pauseTimer() {
        if (!Boolean.TRUE.equals(isRunning.getValue())) return;
        pausedTime = elapsedTime.getValue() != null ? elapsedTime.getValue() : 0L;
        isRunning.setValue(false);
        handler.removeCallbacks(ticker);
    }

    public void resetTimer() {
        handler.removeCallbacks(ticker);
        isRunning.setValue(false);
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
        isRunning.setValue(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        handler.removeCallbacks(ticker);
    }

    public MutableLiveData<String> getStartTimeString() {
        return startTimeString;
    }

    public void setStartTimeString(String data) {
        startTimeString.setValue(data);
    }

    public MutableLiveData<String> getEndTimeString() {
        return endTimeString;
    }

    public void setEndTimeString(String data) {
        endTimeString.setValue(data);
    }

    public List<Long> getNewEntryIds() {
        return newEntryIds;
    }

    public void addNewEntryId(long newId) {
        newEntryIds.add(newId);
    }
}
