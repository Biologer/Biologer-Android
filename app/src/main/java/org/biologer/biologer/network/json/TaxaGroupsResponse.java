package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "data"
})

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaxaGroupsResponse {
    @JsonProperty("data")
    private List<TaxaGroups> data = null;

    public List<TaxaGroups> getData() {
        return data;
    }

    public void setData(List<TaxaGroups> data) {
        this.data = data;
    }
}
