package org.biologer.biologer.model.network;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.biologer.biologer.model.ObservationType;
import org.biologer.biologer.model.ObservationTypeLocalization;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "locale",
        "name"
})

public class ObservationTypesTranslations {
    @JsonProperty("id")
    private long id;
    @JsonProperty("locale")
    private String locale;
    @JsonProperty("name")
    private String name;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
