package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class APIEntryPhotos {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("path")
    private String path;

    @JsonProperty("path")
    public String getPath() {
        return path;
    }

    @JsonProperty("path")
    public void setPath(String path) {
        this.path = path;
    }

    @JsonProperty("license")
    private int license;

    @JsonProperty("license")
    public int getLicense() {
        return license;
    }

    @JsonProperty("license")
    public void setLicense(int license) {
        this.license = license;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
