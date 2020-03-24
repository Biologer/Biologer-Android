package org.biologer.biologer.model.network;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "data",
        "meta"
})

public class ObservationTypesResponse {

    @JsonProperty("data")
    private ObservationTypes[] data;

    @JsonProperty("meta")
    private ObservationTypesMeta meta = null;

    public ObservationTypes[] getData() {
        return data;
    }

    public void setData(ObservationTypes[] data) {
        this.data = data;
    }

    public ObservationTypesMeta getMeta() {
        return meta;
    }

    public void setMeta(ObservationTypesMeta meta) {
        this.meta = meta;
    }

}
