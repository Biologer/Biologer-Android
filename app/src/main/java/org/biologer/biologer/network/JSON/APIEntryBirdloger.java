package org.biologer.biologer.network.JSON;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class APIEntryBirdloger {

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
    private List<APIEntryPhotos> photos;
    @JsonProperty("observer")
    private String observer;
    @JsonProperty("identifier")
    private String identifier;
    @JsonProperty("note")
    private String note;
    @JsonProperty("sex")
    private String sex;
    @JsonProperty("number")
    private Integer number;
    @JsonProperty("number_of")
    private String numberOf;
    @JsonProperty("project")
    private String project;
    @JsonProperty("habitat")
    private String habitat;
    @JsonProperty("found_on")
    private String foundOn;
    @JsonProperty("stage_id")
    private Long stageId;
    @JsonProperty("found_dead")
    private boolean foundDead;
    @JsonProperty("found_dead_note")
    private String foundDeadNote;
    @JsonProperty("data_license")
    private String dataLicense;
    @JsonProperty("image_license")
    private String imageLicense;
    @JsonProperty("time")
    private String time;
    @JsonProperty("types")
    private String[] typesField;
    @JsonProperty("observers")
    private String[] observers;
    @JsonProperty("field_observers")
    private List<APIEntryBirdloger.FieldObservers> fieldObservers;
    @JsonProperty("observed_by_id")
    private Integer observedById;
    @JsonProperty("observed_by")
    private String observedBy;
    @JsonProperty("identified_by_id")
    private Integer identifiedById;
    @JsonProperty("dataset")
    private String dataset;
    @JsonProperty("atlas_code")
    private Long atlas_code;
    @JsonProperty("description")
    private String description;
    @JsonProperty("comment")
    private String comment;
    @JsonProperty("fid")
    private String fid;
    @JsonProperty("rid")
    private String rid;
    @JsonProperty("data_provider")
    private String dataProvider;
    @JsonProperty("data_limit")
    private String dataLimit;
    @JsonProperty("observation_types_ids")
    private int[] observation_types_ids;
    @JsonProperty("reason")
    private String reason;

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

    public String getNumberOf() {
        return numberOf;
    }

    public void setNumberOf(String numberOf) {
        this.numberOf = numberOf;
    }

    public Integer getIdentifiedById() {
        return identifiedById;
    }

    public void setIdentifiedById(Integer identifiedById) {
        this.identifiedById = identifiedById;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getFid() {
        return fid;
    }

    public void setFid(String fid) {
        this.fid = fid;
    }

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    public String getDataProvider() {
        return dataProvider;
    }

    public void setDataProvider(String dataProvider) {
        this.dataProvider = dataProvider;
    }

    public String getDataLimit() {
        return dataLimit;
    }

    public void setDataLimit(String dataLimit) {
        this.dataLimit = dataLimit;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getObservedBy() {
        return observedBy;
    }

    public Integer getObservedById() {
        return observedById;
    }

    public List<FieldObservers> getFieldObservers() {
        return fieldObservers;
    }

    public void setFieldObservers(List<FieldObservers> fieldObservers) {
        this.fieldObservers = fieldObservers;
    }

    public String[] getObservers() {
        return observers;
    }

    public void setObservers(String[] observers) {
        this.observers = observers;
    }

    public void setObservedBy(String observedBy) {
        this.observedBy = observedBy;
    }

    public void setObservedById(Integer observedById) {
        this.observedById = observedById;
    }

    public String getImageLicense() {
        return imageLicense;
    }

    public void setImageLicense(String imageLicense) {
        this.imageLicense = imageLicense;
    }

    public String[] getTypesField() {
        return typesField;
    }

    public void setTypesField(String[] typesField) {
        this.typesField = typesField;
    }

    public List<APIEntryPhotos> getPhotos() {
        return photos;
    }

    public void setPhotos(List<APIEntryPhotos> photos) {
        this.photos = photos;
    }

    public boolean isFoundDead() {
        return foundDead;
    }

    public void setFoundDead(boolean foundDead) {
        this.foundDead = foundDead;
    }

    public static class FieldObservers {

        @JsonProperty("firstName")
        private String firstName;

        @JsonProperty("lastName")
        private String lastName;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }

}
