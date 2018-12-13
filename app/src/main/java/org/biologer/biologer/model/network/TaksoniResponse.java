package org.biologer.biologer.model.network;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "data",
    "meta"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaksoniResponse {

    @JsonProperty("data")
    private List<Taxa> data = null;

    @JsonProperty("meta")
    public TaxaMeta meta = null;

    @JsonProperty("data")
    public List<Taxa> getData() {
        return data;
    }

    @JsonProperty("data")
    public void setData(List<Taxa> data) {
        this.data = data;
    }

    @JsonProperty("meta")
    public TaxaMeta getMeta() {
        return meta;
    }

    @JsonProperty("meta")
    public void setMeta(TaxaMeta meta) {
        this.meta = meta;

        TaxaMeta test = meta;
    }

}
