package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldObservationDataActivity {

    @JsonProperty("id")
    private long id;
    @JsonProperty("log_name")
    private String logName;
    @JsonProperty("description")
    private String description;
    @JsonProperty("subject_id")
    private long subjectId;
    @JsonProperty("subject_type")
    private String subjectType;
    @JsonProperty("event")
    private String event;
    @JsonProperty("causer_id")
    private long causerId;
    @JsonProperty("causer_type")
    private String causerType;
    @JsonProperty("properties")
    private Object properties;
    @JsonProperty("batch_uuid")
    private String batchUuid;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;
    @JsonProperty("causer")
    private FieldObservationDataActivityCauser causer;

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}