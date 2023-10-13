package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "field_observation_id",
        "causer_name",
        "curator_name",
        "taxon_name",
})

public class UnreadNotificationData {

    @JsonProperty("field_observation_id")
    private int field_observation_id;
    @JsonProperty("causer_name")
    private String causer_name;
    @JsonProperty("curator_name")
    private String curator_name;
    @JsonProperty("taxon_name")
    private String taxon_name;
    @JsonIgnore
    private Map<String, Object> additionalProperties;

    public UnreadNotificationData() {
        additionalProperties = new HashMap<String, Object>();
    }

    public int getField_observation_id() {
        return field_observation_id;
    }

    public String getCauser_name() {
        return causer_name;
    }

    public String getTaxon_name() {
        return taxon_name;
    }

    public String getCurator_name() {
        return curator_name;
    }
}
