package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "taxon",
        "taxon_id",
        "taxon_suggestion",
        "day",
        "month",
        "year",
        "location",
        "latitude",
        "longitude",
        "mgrs10k",
        "accuracy",
        "elevation",
        "photos",
        "observer",
        "identifier",
        "license",
        "sex",
        "stage_id",
        "number",
        "note",
        "project",
        "habitat",
        "found_on",
        "found_dead",
        "found_dead_note",
        "data_license",
        "time",
        "status",
        "activity",
        "types",
        "observed_by_id",
        "observed_by",
        "identified_by_id",
        "identified_by",
        "dataset",
        "atlas_code"
})

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldObservationData {

    @JsonProperty("id")
    private int id;

    @JsonProperty("taxon")
    private Object taxon;

    @JsonProperty("taxon_id")
    private String taxonId;

    @JsonProperty("taxon_suggestion")
    private String taxonSuggestion;

    @JsonProperty("day")
    private int day;

    @JsonProperty("month")
    private int month;

    @JsonProperty("year")
    private int year;

    @JsonProperty("location")
    private String location;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("mgrs10k")
    private String mgrs10k;

    @JsonProperty("accuracy")
    private int accuracy;

    @JsonProperty("elevation")
    private int elevation;

    @JsonProperty("photos")
    private List<FieldObservationDataPhotos> photos = null;

    @JsonProperty("observer")
    private String observer;

    @JsonProperty("identifier")
    private String identifier;

    @JsonProperty("license")
    private int license;

    @JsonProperty("sex")
    private String sex;

    @JsonProperty("stage_id")
    private int stageId;

    @JsonProperty("number")
    private int number;

    @JsonProperty("note")
    private String note;

    @JsonProperty("project")
    private String project;

    @JsonProperty("habitat")
    private String habitat;

    @JsonProperty("found_on")
    private String foundOn;

    @JsonProperty("found_dead")
    private boolean foundDead;

    @JsonProperty("found_dead_note")
    private String foundDeadNote;

    @JsonProperty("data_license")
    private int dataLicense;

    @JsonProperty("time")
    private String time;

    @JsonProperty("status")
    private String status;

    @JsonProperty("activity")
    private Object activity;

    @JsonProperty("types")
    private Object types;

    @JsonProperty("observed_by_id")
    private String observedById;

    @JsonProperty("observed_by")
    private Object observedBy;

    @JsonProperty("identified_by_id")
    private String identifiedById;

    @JsonProperty("identified_by")
    private Object identifiedBy;

    @JsonProperty("dataset")
    private String dataset;

    @JsonProperty("atlas_code")
    private String atlasCode;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTaxonSuggestion() {
        return taxonSuggestion;
    }

    public void setTaxonSuggestion(String taxonSuggestion) {
        this.taxonSuggestion = taxonSuggestion;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getObserver() {
        return observer;
    }

    public void setObserver(String observer) {
        this.observer = observer;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    public String getObservedById() {
        return observedById;
    }

    public void setObservedById(String observedById) {
        this.observedById = observedById;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getAtlasCode() {
        return atlasCode;
    }

    public void setAtlasCode(String atlasCode) {
        this.atlasCode = atlasCode;
    }

    public String getTaxonId() {
        return taxonId;
    }

    public void setTaxonId(String taxonId) {
        this.taxonId = taxonId;
    }

    public String getMgrs10k() {
        return mgrs10k;
    }

    public void setMgrs10k(String mgrs10k) {
        this.mgrs10k = mgrs10k;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }

    public int getElevation() {
        return elevation;
    }

    public void setElevation(int elevation) {
        this.elevation = elevation;
    }

    public int getLicense() {
        return license;
    }

    public void setLicense(int license) {
        this.license = license;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public int getStageId() {
        return stageId;
    }

    public void setStageId(int stageId) {
        this.stageId = stageId;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getHabitat() {
        return habitat;
    }

    public void setHabitat(String habitat) {
        this.habitat = habitat;
    }

    public String getFoundOn() {
        return foundOn;
    }

    public void setFoundOn(String foundOn) {
        this.foundOn = foundOn;
    }

    public boolean isFoundDead() {
        return foundDead;
    }

    public void setFoundDead(boolean foundDead) {
        this.foundDead = foundDead;
    }

    public String getFoundDeadNote() {
        return foundDeadNote;
    }

    public void setFoundDeadNote(String foundDeadNote) {
        this.foundDeadNote = foundDeadNote;
    }

    public int getDataLicense() {
        return dataLicense;
    }

    public void setDataLicense(int dataLicense) {
        this.dataLicense = dataLicense;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<FieldObservationDataPhotos> getPhotos() {
        return photos;
    }

    public void setPhotos(List<FieldObservationDataPhotos> photos) {
        this.photos = photos;
    }
}
