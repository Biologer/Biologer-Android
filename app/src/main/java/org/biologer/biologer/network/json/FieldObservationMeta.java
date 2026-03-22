package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FieldObservationMeta {
    @JsonProperty("current_page")
    private int currentPage;

    @JsonProperty("from")
    private int from;

    @JsonProperty("last_page")
    private int lastPage;

    @JsonProperty("links")
    private Object links;

    @JsonProperty("path")
    private Object path;

    @JsonProperty("per_page")
    private int perPage;

    @JsonProperty("to")
    private int to;

    @JsonProperty("total")
    private int total;

    public int getCurrentPage() {
        return currentPage;
    }

    public int getFrom() {
        return from;
    }

    public int getLastPage() {
        return lastPage;
    }

    public int getPerPage() {
        return perPage;
    }

    public int getTo() {
        return to;
    }

    public int getTotal() {
        return total;
    }
}
