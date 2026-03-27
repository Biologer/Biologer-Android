package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldObservationDataActivityProperties {

    @JsonProperty("old")
    private FieldObservationDataActivityOld old;

    @JsonProperty("reason")
    private String reason;

    public FieldObservationDataActivityOld getOld() {
        return old;
    }

    public String getReason() {
        return reason;
    }
}