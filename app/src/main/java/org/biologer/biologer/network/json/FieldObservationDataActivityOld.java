package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldObservationDataActivityOld {

    @JsonProperty("photos")
    private FieldObservationDataActivityValue photos;

    @JsonProperty("types")
    private FieldObservationDataActivityValue types;

    @JsonProperty("found_dead")
    private FieldObservationDataActivityValue foundDead;

    public FieldObservationDataActivityValue getPhotos() {
        return photos;
    }

    public FieldObservationDataActivityValue getTypes() {
        return types;
    }
}