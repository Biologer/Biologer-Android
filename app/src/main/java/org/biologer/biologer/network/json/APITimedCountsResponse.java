package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Root response
@JsonIgnoreProperties(ignoreUnknown = true)
public class APITimedCountsResponse {

    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    // Nested "data" object
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private Long id;
        private int year;
        private int month;
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

        private int humidity;
        private int temperature;

        @JsonProperty("wind_direction")
        private String windDirection;

        @JsonProperty("wind_speed")
        private int windSpeed;

        private String habitat;
        private String comments;
        private Long area;

        @JsonProperty("route_length")
        private Long routeLength;

        private List<Activity> activity;

        @JsonProperty("view_group")
        private ViewGroup viewGroup;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }
        public int getMonth() { return month; }
        public void setMonth(int month) { this.month = month; }
        public int getDay() { return day; }
        public void setDay(int day) { this.day = day; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public int getCountDuration() { return countDuration; }
        public void setCountDuration(int countDuration) { this.countDuration = countDuration; }
        public int getCloudCover() { return cloudCover; }
        public void setCloudCover(int cloudCover) { this.cloudCover = cloudCover; }
        public int getAtmosphericPressure() { return atmosphericPressure; }
        public void setAtmosphericPressure(int atmosphericPressure) { this.atmosphericPressure = atmosphericPressure; }
        public int getHumidity() { return humidity; }
        public void setHumidity(int humidity) { this.humidity = humidity; }
        public int getTemperature() { return temperature; }
        public void setTemperature(int temperature) { this.temperature = temperature; }
        public String getWindDirection() { return windDirection; }
        public void setWindDirection(String windDirection) { this.windDirection = windDirection; }
        public int getWindSpeed() { return windSpeed; }
        public void setWindSpeed(int windSpeed) { this.windSpeed = windSpeed; }
        public String getHabitat() { return habitat; }
        public void setHabitat(String habitat) { this.habitat = habitat; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
        public Long getArea() { return area; }
        public void setArea(Long area) { this.area = area; }
        public Long getRouteLength() { return routeLength; }
        public void setRouteLength(Long routeLength) { this.routeLength = routeLength; }
        public List<Activity> getActivity() { return activity; }
        public void setActivity(List<Activity> activity) { this.activity = activity; }
        public ViewGroup getViewGroup() { return viewGroup; }
        public void setViewGroup(ViewGroup viewGroup) { this.viewGroup = viewGroup; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Activity {
        private Long id;

        @JsonProperty("log_name")
        private String logName;

        private String description;

        @JsonProperty("subject_id")
        private Long subjectId;

        @JsonProperty("subject_type")
        private String subjectType;

        @JsonProperty("causer_id")
        private Long causerId;

        @JsonProperty("causer_type")
        private String causerType;

        private List<Object> properties;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("updated_at")
        private String updatedAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getLogName() { return logName; }
        public void setLogName(String logName) { this.logName = logName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Long getSubjectId() { return subjectId; }
        public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
        public String getSubjectType() { return subjectType; }
        public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
        public Long getCauserId() { return causerId; }
        public void setCauserId(Long causerId) { this.causerId = causerId; }
        public String getCauserType() { return causerType; }
        public void setCauserType(String causerType) { this.causerType = causerType; }
        public List<Object> getProperties() { return properties; }
        public void setProperties(List<Object> properties) { this.properties = properties; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ViewGroup {
        private Long id;
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
