package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "data",
        "links",
        "meta"
})

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldObservationResponse {
    @JsonProperty("data")
    private FieldObservationData[] data;

    public FieldObservationData[] getData() {
        return data;
    }

    public void setData(FieldObservationData[] data) {
        this.data = data;
    }
}
