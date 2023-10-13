package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "data"
})

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnouncementsResponse {
    @JsonProperty("data")
    private AnnouncementsData data[];

    public AnnouncementsData[] getData() {
        return data;
    }

    public void setData(AnnouncementsData[] data) {
        this.data = data;
    }
}
