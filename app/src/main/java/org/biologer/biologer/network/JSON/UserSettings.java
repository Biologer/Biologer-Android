package org.biologer.biologer.network.JSON;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by brjovanovic on 3/23/2018.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "data_license",
        "image_license",
        "language"
})
public class UserSettings {

    @JsonProperty("data_license")
    private Integer dataLicense;
    @JsonProperty("image_license")
    private Integer imageLicense;
    @JsonProperty("language")
    private String language;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("data_license")
    public Integer getDataLicense() {
        return dataLicense;
    }

    @JsonProperty("data_license")
    public void setDataLicense(Integer dataLicense) {
        this.dataLicense = dataLicense;
    }

    @JsonProperty("image_license")
    public Integer getImageLicense() {
        return imageLicense;
    }

    @JsonProperty("image_license")
    public void setImageLicense(Integer imageLicense) {
        this.imageLicense = imageLicense;
    }

    @JsonProperty("language")
    public String getLanguage() {
        return language;
    }

    @JsonProperty("language")
    public void setLanguage(String language) {
        this.language = language;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
