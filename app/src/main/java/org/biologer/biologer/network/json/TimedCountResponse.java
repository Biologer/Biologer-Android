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
public class TimedCountResponse {
    @JsonProperty("data")
    private TimedCountData[] data;
    @JsonProperty("links")
    private FieldObservationLinks links;
    @JsonProperty("meta")
    private FieldObservationMeta meta;

    public TimedCountData[] getData() {
        return data;
    }

    public FieldObservationLinks getLinks() {
        return links;
    }

    public FieldObservationMeta getMeta() {
        return meta;
    }
}
