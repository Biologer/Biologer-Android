package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.biologer.biologer.helpers.ArrayHelper;
import org.biologer.biologer.helpers.ObjectBoxHelper;
import org.biologer.biologer.sql.EntryDb;
import org.biologer.biologer.sql.TimedCountDb;

import java.util.List;

/**
 * Created by brjovanovic on 3/12/2018.
 */

public class APIEntry {

    @JsonProperty("taxon_id")
    private Integer taxonId;
    @JsonProperty("timed_count_id")
    private Long timeCountId;
    @JsonProperty("taxon_suggestion")
    private String taxonSuggestion;
    @JsonProperty("year")
    private Integer year;
    @JsonProperty("month")
    private Integer month;
    @JsonProperty("day")
    private Integer day;
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
    @JsonProperty("reason")
    private String reason;
    @JsonProperty("observed_by_id")
    private Integer observedById;
    @JsonProperty("identified_by_id")
    private Integer identifiedById;

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

    public void getFromEntryDb (EntryDb entry) {
        // Set the ID or set the null if there is no ID.
        if (entry.getTaxonId() != 0) setTaxonId( (int) entry.getTaxonId());
        else setTaxonId(null);

        // Set the time count ID value
        setTimeCountId(null);
        Long timeCountIdValue = entry.getTimeCountId();
        if (timeCountIdValue != null) {
            TimedCountDb timedCount = ObjectBoxHelper.getTimeCountById(timeCountIdValue);
            if (timedCount != null) {
                setTimeCountId(timedCount.getServerId());
            }
        }

        setTaxonSuggestion(entry.getTaxonSuggestion());
        setYear(Integer.parseInt(entry.getYear()));
        // Add 1 to the month since months range from 0 to 11
        int month = Integer.parseInt(entry.getMonth()) + 1;
        setMonth(month);
        setDay(Integer.parseInt(entry.getDay()));
        setLatitude(entry.getLattitude());
        setLongitude(entry.getLongitude());
        setAccuracy(entry.getAccuracy() == 0.0 ? null : (int) entry.getAccuracy());
        setLocation(entry.getLocation());
        setElevation(entry.getElevation() <= 0.0 ? 0 : (int) entry.getElevation());
        setNote(entry.getComment());
        setSex(entry.getSex());
        setNumber(entry.getNoSpecimens());
        setProject(entry.getProjectId());
        setFoundOn(entry.getFoundOn());
        setStageId(entry.getStage());
        setAtlasCode(entry.getAtlasCode());
        setFoundDead(entry.getDeadOrAlive().equals("true") ? 0 : 1);
        setFoundDeadNote(entry.getCauseOfDeath());
        setDataLicense(entry.getDataLicence());
        setTime(entry.getTime());
        int[] observation_types = ArrayHelper.getArrayFromText(entry.getObservationTypeIds());
        if (observation_types == null) observation_types = new int[]{1};
        setTypes(observation_types);
        setHabitat(entry.getHabitat());
        setReason(null);
    }

    public void setAtlasCode(Long atlas_code) {
        this.atlas_code = atlas_code;
    }

    public List<APIEntryPhotos> getPhotos() {
        return photos;
    }

    public void setPhotos(List<APIEntryPhotos> photos) {
        this.photos = photos;
    }

    public void setTimeCountId(Long timeCountId) {
        this.timeCountId = timeCountId;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setObservedById(Integer observedById) {
        this.observedById = observedById;
    }

    public void setIdentifiedById(Integer identifiedById) {
        this.identifiedById = identifiedById;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }
}
