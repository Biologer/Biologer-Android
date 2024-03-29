package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "data",
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
