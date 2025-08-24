package org.biologer.biologer.adapters;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class WeatherDataViewModel extends ViewModel {

    private final MutableLiveData<Double> temperatureData = new MutableLiveData<>();
    private final MutableLiveData<Integer> cloudinessData = new MutableLiveData<>();
    private final MutableLiveData<Integer> windSpeedData = new MutableLiveData<>();
    private final MutableLiveData<String> windDirectionData = new MutableLiveData<>();
    private final MutableLiveData<Integer> pressureData = new MutableLiveData<>();
    private final MutableLiveData<Integer> humidityData = new MutableLiveData<>();
    private final MutableLiveData<String> habitatData = new MutableLiveData<>();
    private final MutableLiveData<String> commentData = new MutableLiveData<>();

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
}
