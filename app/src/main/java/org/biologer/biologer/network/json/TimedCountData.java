package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.biologer.biologer.helpers.DateHelper;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimedCountData {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("year")
    private int year;
    @JsonProperty("month")
    private int month;
    @JsonProperty("day")
    private int day;
    @JsonProperty("start_time")
    private String startTime;
    @JsonProperty("end_time")
    private String endTime;
    @JsonProperty("count_duration")
    private int countDuration;
    @JsonProperty("cloud_cover")
    private int cloudCover;
    @JsonProperty("atmospheric_pressure")
    private int atmosphericPressure;
    @JsonProperty("humidity")
    private Integer humidity;
    @JsonProperty("temperature")
    private Double temperature;
    @JsonProperty("wind_direction")
    private String windDirection;
    @JsonProperty("wind_speed")
    private int windSpeed;
    @JsonProperty("habitat")
    private String habitat;
    @JsonProperty("comments")
    private String comments;
    @JsonProperty("area")
    private int area;
    @JsonProperty("route_length")
    private Integer routeLength;
    @JsonProperty("geometry")
    private String geometry;
    @JsonProperty("latitude")
    private Double latitude;
    @JsonProperty("longitude")
    private Double longitude;
    @JsonProperty("activity")
    private List<FieldObservationDataActivity> activity;
    @JsonProperty("view_group")
    private TimedCountViewGroup viewGroup;
    @JsonProperty("field_observations")
    private List<FieldObservationData> fieldObservations;

    public List<FieldObservationData> getFieldObservations() {
        return fieldObservations;
    }

    public Long getId() {
        return id;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public String getStartTime() {
        return DateHelper.getHoursAndMinutesFromIso(startTime);
    }

    public String getEndTime() {
        return DateHelper.getHoursAndMinutesFromIso(endTime);
    }

    public int getCountDuration() {
        return countDuration;
    }

    public int getCloudCover() {
        return cloudCover;
    }

    public int getAtmosphericPressure() {
        return atmosphericPressure;
    }

    public Integer getHumidity() {
        return humidity;
    }

    public Double getTemperature() {
        return temperature;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public int getWindSpeed() {
        return windSpeed;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public TimedCountViewGroup getViewGroup() {
        return viewGroup;
    }

    public String getHabitat() {
        return habitat;
    }

    public String getComments() {
        return comments;
    }

    public int getArea() {
        return area;
    }

    public String getGeometry() {
        return geometry;
    }

    public Integer getRouteLength() {
        return routeLength;
    }

    public List<FieldObservationDataActivity> getActivity() {
        return activity;
    }
}
