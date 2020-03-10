package org.biologer.biologer.model.network;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "data"
})

public class ObservationTypesResponse {

    @JsonProperty("data")
    private ObservationTypes[] data;

    public ObservationTypes[] getData() {
        return data;
    }

    public void setData(ObservationTypes[] data) {
        this.data = data;
    }
}
