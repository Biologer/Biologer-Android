package org.biologer.biologer.network.JSON;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "current_page",
        "from",
        "last_page",
        "links",
        "path",
        "per_page",
        "to",
        "total"
})

public class UnreadNotificationMeta {

    @JsonProperty("total")
    private int total;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public int getTotal() {
        return total;
    }
}
