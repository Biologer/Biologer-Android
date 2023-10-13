package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "type",
        "notifiable_type",
        "notifiable_id",
        "data",
        "read_at",
        "created_at",
        "updated_at"
})

public class UnreadNotification {

    @JsonProperty("id")
    private String id;
    @JsonProperty("type")
    private String type;
    @JsonProperty("notifiable_type")
    private String notifiable_type;
    @JsonProperty("notifiable_id")
    private int notifiable_id;
    @JsonProperty("data")
    private UnreadNotificationData data;
    @JsonProperty("data")
    public UnreadNotificationData getData() {
        return data;
    }
    @JsonProperty("read_at")
    private String read_at;
    @JsonProperty("created_at")
    private String created_at;
    @JsonProperty("updated_at")
    private String updated_at;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getNotifiable_type() {
        return notifiable_type;
    }

    public String getUpdated_at() {
        return updated_at;
    }
}
