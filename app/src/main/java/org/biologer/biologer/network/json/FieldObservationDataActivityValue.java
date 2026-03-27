package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldObservationDataActivityValue {

    @JsonProperty("value")
    private List<Object> value;

    @JsonProperty("label")
    private String label;

    public List<Object> getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}