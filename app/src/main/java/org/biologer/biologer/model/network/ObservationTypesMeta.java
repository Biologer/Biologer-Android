package org.biologer.biologer.model.network;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ObservationTypesMeta {
    @JsonProperty("updated_after")
    private long last_updated_at;

    @JsonProperty("updated_after")
    public long getLastUpdatedAt() {
        return last_updated_at;
    }

    @JsonProperty("updated_after")
    public void setLastUpdatedAt(long last_updated_at) {
        this.last_updated_at = last_updated_at;
    }
}
