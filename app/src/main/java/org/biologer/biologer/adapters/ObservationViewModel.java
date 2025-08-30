package org.biologer.biologer.adapters;


import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.LatLng;

import org.biologer.biologer.Localisation;
import org.biologer.biologer.services.DateHelper;
import org.biologer.biologer.sql.EntryDb;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class ObservationViewModel extends ViewModel {
    private Long entryId = null;
    private Long taxonId = null;
    private Integer timedCountId = null;
    private String taxonSuggestion = null;
    private final MutableLiveData<Calendar> calendar = new MutableLiveData<>();
    private String comment = null;
    private Integer numberOfSpecimens = null;
    private final MutableLiveData<String> sex = new MutableLiveData<>();
    private final MutableLiveData<Long> stage = new MutableLiveData<>();
    private final MutableLiveData<Long> atlasCode = new MutableLiveData<>();
    private final MutableLiveData<Boolean> dead = new MutableLiveData<>();
    private String causeOfDeath = null;
    private final MutableLiveData<LatLng> coordinates = new MutableLiveData<>();
    private final MutableLiveData<Double> accuracy = new MutableLiveData<>();
    private final MutableLiveData<Double> elevation = new MutableLiveData<>();
    private final MutableLiveData<String> location = new MutableLiveData<>();
    private final MutableLiveData<String> image1 = new MutableLiveData<>();
    private final MutableLiveData<String> image2 = new MutableLiveData<>();
    private final MutableLiveData<String> image3 = new MutableLiveData<>();
    private String project = null;
    private String foundOn = null;
    private String dataLicence = null;
    private Integer imageLicence = null;
    private String habitat = null;
    private Boolean saveEnabled = null;
    private Uri currentImage = null;
    private final ArrayList<String> listNewImages = new ArrayList<>();
    private Boolean taxonFromTheList = false;
    private Boolean locationFromTheMap = false;
    private Boolean callTagSelected = false;
    private Integer callTagIndex = null;
    private final Set<Integer> observationTypeIds = new HashSet<>();

    public void getFromObjectBox(EntryDb entry) {
        setEntryId(entry.getId());
        setTaxonId(entry.getTaxonId() != 0 ? entry.getTaxonId() : null);
        setTimedCountId(entry.getTimedCoundId());
        setTaxonSuggestion(entry.getTaxonSuggestion());
        setCalendar(DateHelper.getCalendar(entry.getYear(), entry.getMonth(), entry.getDay(), entry.getTime()));
        setComment(entry.getComment());
        setNumberOfSpecimens(entry.getNoSpecimens());
        setSex(entry.getSex());
        setStage(entry.getStage());
        setAtlasCode(entry.getAtlasCode());
        setDead(!entry.getDeadOrAlive().equals("true"));
        setCauseOfDeath(entry.getCauseOfDeath());
        setCoordinates(new LatLng(entry.getLattitude(), entry.getLongitude()));
        setAccuracy(entry.getAccuracy());
        setElevation(entry.getElevation());
        setLocation(entry.getLocation());
        setImage1(entry.getSlika1());
        setImage2(entry.getSlika2());
        setImage3(entry.getSlika3());
        setProject(entry.getProjectId());
        setFoundOn(entry.getFoundOn());
        setDataLicence(entry.getDataLicence());
        setImageLicence(entry.getImageLicence());
        setHabitat(entry.getHabitat());
        setObservationTypesFromString(entry.getObservationTypeIds());
    }

    public EntryDb getObservation() {
        Calendar calendar1 = getCalendar().getValue();
        long taxonId = getTaxonId() != null ? getTaxonId() : 0;
        int imageLicense = getImageLicence() != null ? getImageLicence() : 0;
        long entryId = getEntryId() != null ? getEntryId() : 0;
        String isAlive = isDead() ? "false" : "true";

        if (calendar1 != null) {
            return new EntryDb(entryId,
                    taxonId,
                    getTimedCountId(),
                    getTaxonSuggestion(),
                    DateHelper.getYearFromCalendar(calendar1),
                    DateHelper.getMonthFromCalendar(calendar1),
                    DateHelper.getDayFromCalendar(calendar1),
                    getComment(),
                    getNumberOfSpecimens(),
                    getSex().getValue(),
                    getStage().getValue(),
                    getAtlasCode().getValue(),
                    isAlive,
                    getCauseOfDeath(),
                    getLatitude(),
                    getLongitude(),
                    getRoundedAccuracy(),
                    getRoundedElevation(),
                    getLocation().getValue(),
                    getImage1().getValue(),
                    getImage2().getValue(),
                    getImage3().getValue(),
                    getProject(),
                    getFoundOn(),
                    getDataLicence(),
                    imageLicense,
                    DateHelper.getPlainTime(calendar1),
                    getHabitat(),
                    getObservationTypes().toString());
        }
        return null;
    }

    public Double getLatitude() {
        if (coordinates.getValue() != null) {
            return coordinates.getValue().latitude;
        } else {
            return null;
        }
    }

    public String getLatitudeString() {
        if (getLatitude() != null) {
            return String.valueOf(getLatitude());
        } else {
            return "0.0";
        }
    }

    public void setLatitude(Double latitude) {
        Double longitude = getLongitude();
        coordinates.setValue(new LatLng(latitude, longitude));
    }

    public String getLocalizedLatitiudeString() {
        DecimalFormat formatCoordinates = getCoordinatesFormater();
        return formatCoordinates.format(getLatitude());
    }

    public Double getLongitude() {
        if (coordinates.getValue() != null) {
            return coordinates.getValue().longitude;
        } else {
            return null;
        }
    }

    public String getLongitudeString() {
        if (getLongitude() != null) {
            return String.valueOf(getLongitude());
        } else {
            return "0.0";
        }
    }

    public String getLocalizedLongitudeString() {
        DecimalFormat formatCoordinates = getCoordinatesFormater();
        return formatCoordinates.format(getLongitude());
    }

    private DecimalFormat getCoordinatesFormater() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.
                forLanguageTag(Localisation.getLocaleScript()));
        return new DecimalFormat("#,##0.0000", symbols);
    }

    public void setLongitude(Double longitude) {
        Double latitude = getLatitude();
        coordinates.setValue(new LatLng(latitude, longitude));
    }

    public MutableLiveData<LatLng> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(LatLng data) {
        coordinates.setValue(data);
    }

    public MutableLiveData<Calendar> getCalendar() {
        return calendar;
    }

    public void setCalendar(Calendar data) {
        calendar.setValue(data);
    }

    public MutableLiveData<String> getSex() {
        return sex;
    }

    public void setSex(String data) {
        sex.setValue(data);
    }

    public MutableLiveData<Long> getStage() {
        return stage;
    }

    public void setStage(Long data) {
        stage.setValue(data);
    }

    public MutableLiveData<Long> getAtlasCode() {
        return atlasCode;
    }

    public void setAtlasCode(Long data) {
        atlasCode.setValue(data);
    }


    public MutableLiveData<Boolean> getDead() {
        return dead;
    }

    public void setDead(Boolean data) {
        dead.setValue(data);
    }

    public void checkDead() {
        dead.setValue(!isDead());
    }

    public Boolean isDead() {
        if (getDead().getValue() != null) {
            return getDead().getValue();
        } else {
            return false;
        }
    }

    public MutableLiveData<Double> getAccuracy() {
        return accuracy;
    }

    public Double getRoundedAccuracy() {
        if (getAccuracy().getValue() != null) {
            return (double) Math.round(getAccuracy().getValue());
        } else {
            return null;
        }
    }

    public void setAccuracy(Double data) {
        accuracy.setValue(data);
    }


    public MutableLiveData<Double> getElevation() {
        return elevation;
    }

    public double getRoundedElevation() {
        if (getElevation().getValue() != null) {
            return Math.round(getElevation().getValue());
        } else {
            return 0;
        }
    }

    public void setElevation(Double data) {
        elevation.setValue(data);
    }


    public MutableLiveData<String> getLocation() {
        return location;
    }

    public void setLocation(String data) {
        location.setValue(data);
    }


    public MutableLiveData<String> getImage1() {
        return image1;
    }

    public void setImage1(String data) {
        image1.setValue(data);
    }


    public MutableLiveData<String> getImage2() {
        return image2;
    }

    public void setImage2(String data) {
        image2.setValue(data);
    }

    public MutableLiveData<String> getImage3() {
        return image3;
    }

    public void setImage3(String data) {
        image3.setValue(data);
    }

    public ArrayList<String> getListNewImages() {
        return listNewImages;
    }

    public void addItemToListNewImage(String data) {
        listNewImages.add(data);
    }

    public Boolean isTaxonSelectedFromTheList() {
        return taxonFromTheList;
    }


    public void setTaxonSelectedFromTheList(Boolean taxonFromTheList) {
        this.taxonFromTheList = taxonFromTheList;
    }

    public Set<Integer> getObservationTypes() {
        return observationTypeIds;
    }

    public void addObservationType(int id) {
        observationTypeIds.add(id);
    }

    public void removeObservationType(int id) {
        observationTypeIds.remove(id);
    }

    public Long getEntryId() {
        return entryId;
    }

    public void setEntryId(Long entryId) {
        this.entryId = entryId;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public void setTaxonId(Long taxonId) {
        this.taxonId = taxonId;
    }

    public String getTaxonSuggestion() {
        return taxonSuggestion;
    }

    public void setTaxonSuggestion(String taxonSuggestion) {
        this.taxonSuggestion = taxonSuggestion;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCauseOfDeath() {
        return causeOfDeath;
    }

    public void setCauseOfDeath(String causeOfDeath) {
        this.causeOfDeath = causeOfDeath;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getFoundOn() {
        return foundOn;
    }

    public void setFoundOn(String foundOn) {
        this.foundOn = foundOn;
    }

    public String getDataLicence() {
        return dataLicence;
    }

    public void setDataLicence(String dataLicence) {
        this.dataLicence = dataLicence;
    }

    public Integer getImageLicence() {
        return imageLicence;
    }

    public void setImageLicence(Integer imageLicence) {
        this.imageLicence = imageLicence;
    }

    public String getHabitat() {
        return habitat;
    }

    public void setHabitat(String habitat) {
        this.habitat = habitat;
    }

    public Boolean getSaveEnabled() {
        return saveEnabled;
    }

    public void setSaveEnabled(Boolean saveEnabled) {
        this.saveEnabled = saveEnabled;
    }

    public Boolean isSaveEnabled() {
        return Objects.requireNonNullElse(saveEnabled, false);
    }

    public Uri getCurrentImage() {
        return currentImage;
    }

    public void setCurrentImage(Uri currentImage) {
        this.currentImage = currentImage;
    }

    public void setObservationTypesFromString(String observationTypeString) {
        observationTypeIds.clear();
        if (observationTypeString == null || observationTypeString.trim().isEmpty() || "[]".equals(observationTypeString.trim())) {
            return;
        }

        try {
            // Clean the string and split into an array of number-strings
            String[] stringIds = observationTypeString.replaceAll("[\\[\\]\\s]", "").split(",");
            for (String idString : stringIds) {
                int id = Integer.parseInt(idString);
                addObservationType(id);
            }
        } catch (NumberFormatException e) {
            Log.e("ViewModel", "Failed to parse observation types string: " + observationTypeString, e);
        }
    }

    public Boolean getLocationFromTheMap() {
        return locationFromTheMap;
    }

    public Boolean isLocationFromTheMap() {
        if (getLocationFromTheMap() != null) {
            return locationFromTheMap;
        } else {
            return false;
        }
    }

    public void setLocationFromTheMap(Boolean locationFromTheMap) {
        this.locationFromTheMap = locationFromTheMap;
    }

    public Boolean getCallTagSelected() {
        return callTagSelected;
    }

    public Boolean isCallTagSelected() {
        if (callTagSelected != null) {
            return callTagSelected;
        } else {
            return false;
        }
    }

    public void setCallTagSelected(Boolean callTagSelected) {
        this.callTagSelected = callTagSelected;
    }

    public Integer getCallTagIndex() {
        return callTagIndex;
    }

    public void setCallTagIndex(Integer callTagIndex) {
        this.callTagIndex = callTagIndex;
    }

    public Integer getNumberOfSpecimens() {
        return numberOfSpecimens;
    }

    public void setNumberOfSpecimens(Integer numberOfSpecimens) {
        this.numberOfSpecimens = numberOfSpecimens;
    }

    public Integer getTimedCountId() {
        return timedCountId;
    }

    public void setTimedCountId(Integer timedCountId) {
        this.timedCountId = timedCountId;
    }
}
