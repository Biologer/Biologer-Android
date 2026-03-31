package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import org.biologer.biologer.sql.ObservationActivityDb;
import org.jetbrains.annotations.NotNull;

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
    private JsonNode properties;
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

    public FieldObservationDataActivityCauser getCauser() {
        return causer;
    }

    public long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public static @NotNull ObservationActivityDb
    getObservationActivityDb(FieldObservationDataActivity activity) {
        String causerName = (activity.getCauser() != null) ?
                activity.getCauser().getFullName() : "Unknown";

        String reason = "";
        JsonNode props = activity.getProperties();
        if (props != null && props.isObject() && props.has("reason")) {
            reason = props.get("reason").asText();
        }

        return new ObservationActivityDb(
                activity.getId(),
                activity.getDescription(),
                causerName,
                activity.getCreatedAt(),
                reason
        );
    }

    public JsonNode getProperties() {
        return properties;
    }
}