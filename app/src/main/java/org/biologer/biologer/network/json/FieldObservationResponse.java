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
    @JsonProperty("links")
    private FieldObservationLinks links;
    @JsonProperty("meta")
    private FieldObservationMeta meta;

    public FieldObservationData[] getData() {
        return data;
    }

    public void setData(FieldObservationData[] data) {
        this.data = data;
    }

    public FieldObservationLinks getLinks() {
        return links;
    }

    public void setLinks(FieldObservationLinks links) {
        this.links = links;
    }

    public FieldObservationMeta getMeta() {
        return meta;
    }

    public void setMeta(FieldObservationMeta meta) {
        this.meta = meta;
    }
}
