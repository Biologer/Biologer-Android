package org.biologer.biologer.network.json;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponse {

    private String name;
    private MainData main;
    private List<WeatherData> weather;
    private WindData wind;
    private CloudsData clouds;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MainData getMain() {
        return main;
    }

    public void setMain(MainData main) { // This is the setter that has changed
        this.main = main;
    }

    public List<WeatherData> getWeather() {
        return weather;
    }

    public void setWeather(List<WeatherData> weather) {
        this.weather = weather;
    }

    public WindData getWind() {
        return wind;
    }

    public void setWind(WindData wind) {
        this.wind = wind;
    }

    public CloudsData getClouds() {
        return clouds;
    }

    public void setClouds(CloudsData clouds) {
        this.clouds = clouds;
    }
}