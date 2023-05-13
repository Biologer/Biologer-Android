package org.biologer.biologer.network.JSON;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "data",
        "links",
        "meta"
})

public class UnreadNotificationsResponse {
    @JsonProperty("data")
    private List<UnreadNotification> data;
    @JsonProperty("meta")
    private UnreadNotificationMeta meta;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    @JsonProperty("data")
    public List<UnreadNotification> getData() {
        return data;
    }

    public UnreadNotificationMeta getMeta() {
        return meta;
    }
}
