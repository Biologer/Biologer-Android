package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TaxaSynonym {

    @JsonProperty("id")
    private long id;
    @JsonProperty("name")
    private String name;

    @JsonProperty("id")
    public long getId() { return id; }
    @JsonProperty("id")
    public void setId(long id) { this.id = id; }
    @JsonProperty("name")
    public String getName() { return name; }
    @JsonProperty("name")
    public void setName(String name) { this.name = name; }
}