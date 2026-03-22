package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FieldObservationLinks {
    @JsonProperty("first")
    private String first;
    @JsonProperty("last")
    private String last;
    @JsonProperty("prev")
    private String prev;
    @JsonProperty("next")
    private String next;

    public String getFirst() {
        return first;
    }

    public String getLast() {
        return last;
    }

    public String getPrev() {
        return prev;
    }

    public String getNext() {
        return next;
    }
}
