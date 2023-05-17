package org.biologer.biologer.network.JSON;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

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
