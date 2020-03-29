package org.biologer.biologer.network.JSON;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaxaMeta {
    @JsonProperty("current_page")
    private int currentPage;
    @JsonProperty("from")
    private int from;
    @JsonProperty("last_page")
    private int lastPage;
    @JsonProperty("path")
    private String path;
    @JsonProperty("per_page")
    private int perPage;
    @JsonProperty("to")
    private int to;
    @JsonProperty("total")
    private long total;

    @JsonProperty("from")
    public int getFrom() {
        return from;
    }

    @JsonProperty("from")
    public void setFrom(int from) {
        this.from = from;
    }

    @JsonProperty("current_page")
    public int getCurrentPage() {
        return currentPage;
    }

    @JsonProperty("current_page")
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    @JsonProperty("last_page")
    public int getLastPage() {
        return lastPage;
    }

    @JsonProperty("last_page")
    public void setLastPage(int lastPage) {
        this.lastPage = lastPage;
    }

    @JsonProperty("path")
    public String getPath() {
        return path;
    }

    @JsonProperty("path")
    public void setPath(String path) {
        this.path = path;
    }

    @JsonProperty("per_page")
    public int getPerPage() {
        return perPage;
    }

    @JsonProperty("per_page")
    public void setPerPage(int perPage) {
        this.perPage = perPage;
    }

    @JsonProperty("to")
    public int getTo() {
        return to;
    }

    @JsonProperty("to")
    public void setTo(int to) {
        this.to = to;
    }

    @JsonProperty("total")
    public long getTotal() {
        return total;
    }

    @JsonProperty("total")
    public void setTotal(long total) {
        this.total = total;
    }
}
