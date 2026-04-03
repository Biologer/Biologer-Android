package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimedCountViewGroup {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("name")
    private String name;

    public Long getId() {
        return id;
    }
}
