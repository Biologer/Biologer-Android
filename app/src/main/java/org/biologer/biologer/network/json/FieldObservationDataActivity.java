package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    private FieldObservationDataActivityProperties properties;
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

    public static @NotNull ObservationActivityDb getObservationActivityDb(FieldObservationDataActivity activity) {
        String causerName = (activity.getCauser() != null) ?
                activity.getCauser().getFullName() : "Unknown";

        return new ObservationActivityDb(
                activity.getId(),
                activity.getDescription(),
                causerName,
                activity.getCreatedAt()
        );
    }
}