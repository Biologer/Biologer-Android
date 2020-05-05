package org.biologer.biologer.network.JSON;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by brjovanovic on 3/12/2018.
 */

public class APIEntry {

    @JsonProperty("taxon_id")
    private Integer taxonId;
    @JsonProperty("taxon_suggestion")
    private String taxonSuggestion;
    @JsonProperty("year")
    private String year;
    @JsonProperty("month")
    private String month;
    @JsonProperty("day")
    private String day;
    @JsonProperty("latitude")
    private Double latitude;
    @JsonProperty("longitude")
    private Double longitude;
    @JsonProperty("accuracy")
    private Integer accuracy;
    @JsonProperty("elevation")
    private Integer elevation;
    @JsonProperty("location")
    private String location;
    @JsonProperty("photos")
    private List<Photo> photos;
    @JsonProperty("note")
    private String note;
    @JsonProperty("sex")
    private String sex;
    @JsonProperty("number")
    private Integer number;
    @JsonProperty("project")
    private String project;
    @JsonProperty("found_on")
    private String foundOn;
    @JsonProperty("stage_id")
    private Long stageId;
    @JsonProperty("atlas_code")
    private Long atlas_code;
    @JsonProperty("found_dead")
    private int foundDead;
    @JsonProperty("found_dead_note")
    private String foundDeadNote;
    @JsonProperty("data_license")
    private String dataLicense;
    @JsonProperty("time")
    private String time;
    @JsonProperty("observation_types_ids")
    private int[] observation_types_ids;
    @JsonProperty("habitat")
    private String habitat;

    @JsonProperty("taxon_id")
    public Integer getTaxonId() {
        return taxonId;
    }

    @JsonProperty("taxon_id")
    public void setTaxonId(Integer taxonId) {
        this.taxonId = taxonId;
    }

    @JsonProperty("taxon_suggestion")
    public String getTaxonSuggestion() {
        return taxonSuggestion;
    }

    @JsonProperty("taxon_suggestion")
    public void setTaxonSuggestion(String taxonSuggestion) {
        this.taxonSuggestion = taxonSuggestion;
    }

    @JsonProperty("year")
    public String getYear() {
        return year;
    }

    @JsonProperty("year")
    public void setYear(String year) {
        this.year = year;
    }

    @JsonProperty("month")
    public String getMonth() {
        return month;
    }

    @JsonProperty("month")
    public void setMonth(String month) {
        this.month = month;
    }

    @JsonProperty("day")
    public String getDay() {
        return day;
    }

    @JsonProperty("day")
    public void setDay(String day) {
        this.day = day;
    }

    @JsonProperty("latitude")
    public Double getLatitude() {
        return latitude;
    }

    @JsonProperty("latitude")
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    @JsonProperty("longitude")
    public Double getLongitude() {
        return longitude;
    }

    @JsonProperty("longitude")
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    @JsonProperty("accuracy")
    public Integer getAccuracy() {
        return accuracy;
    }

    @JsonProperty("accuracy")
    public void setAccuracy(Integer accuracy) {
        this.accuracy = accuracy;
    }

    @JsonProperty("elevation")
    public Integer getElevation() {
        return elevation;
    }

    @JsonProperty("elevation")
    public void setElevation(Integer elevation) {
        this.elevation = elevation;
    }

    @JsonProperty("location")
    public String getLocation() {
        return location;
    }

    @JsonProperty("location")
    public void setLocation(String location) {
        this.location = location;
    }

    @JsonProperty("photos")
    public List<Photo> getPhotos() {
        return photos;
    }

    @JsonProperty("photos")
    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }

    @JsonProperty("note")
    public String getNote() {
        return note;
    }

    @JsonProperty("note")
    public void setNote(String note) {
        this.note = note;
    }

    @JsonProperty("sex")
    public String getSex() {
        return sex;
    }

    @JsonProperty("sex")
    public void setSex(String sex) {
        this.sex = sex;
    }

    @JsonProperty("number")
    public Integer getNumber() {
        return number;
    }

    @JsonProperty("number")
    public void setNumber(Integer number) {
        this.number = number;
    }

    @JsonProperty("project")
    public String getProject() {
        return project;
    }

    @JsonProperty("project")
    public void setProject(String project) {
        this.project = project;
    }

    @JsonProperty("found_on")
    public String getFoundOn() {
        return foundOn;
    }

    @JsonProperty("found_on")
    public void setFoundOn(String foundOn) {
        this.foundOn = foundOn;
    }

    @JsonProperty("stage_id")
    public Long getStageId() {
        return stageId;
    }

    @JsonProperty("stage_id")
    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    @JsonProperty("found_dead")
    public int getFoundDead() {
        return foundDead;
    }

    @JsonProperty("found_dead")
    public void setFoundDead(int foundDead) {
        this.foundDead = foundDead;
    }

    @JsonProperty("found_dead_note")
    public String getFoundDeadNote() {
        return foundDeadNote;
    }

    @JsonProperty("found_dead_note")
    public void setFoundDeadNote(String foundDeadNote) {
        this.foundDeadNote = foundDeadNote;
    }

    @JsonProperty("data_license")
    public String getDataLicense() {
        return dataLicense;
    }

    @JsonProperty("data_license")
    public void setDataLicense(String dataLicense) {
        this.dataLicense = dataLicense;
    }

    @JsonProperty("time")
    public String getTime() {
        return time;
    }

    @JsonProperty("time")
    public void setTime(String time) {
        this.time = time;
    }

    @JsonProperty("observation_types_ids")
    public int[] getTypes() {
        return observation_types_ids;
    }

    @JsonProperty("observation_types_ids")
    public void setTypes(int[] observation_types_ids) {
        this.observation_types_ids = observation_types_ids;
    }

    @JsonProperty("habitat")
    public String getHabitat() {
        return habitat;
    }

    @JsonProperty("habitat")
    public void setHabitat(String habitat) {
        this.habitat = habitat;
    }

    public Long getAtlasCode() {
        return atlas_code;
    }

    public void setAtlasCode(Long atlas_code) {
        this.atlas_code = atlas_code;
    }

    public static class Photo {

        @JsonProperty("path")
        private String path;

        @JsonProperty("path")
        public String getPath() {
            return path;
        }

        @JsonProperty("path")
        public void setPath(String path) {
            this.path = path;
        }

        @JsonProperty("license")
        private int license;

        @JsonProperty("license")
        public int getLicense() {
            return license;
        }

        @JsonProperty("license")
        public void setLicense(int license) {
            this.license = license;
        }

    }

}
