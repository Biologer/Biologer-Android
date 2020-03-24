package org.biologer.biologer.model.network;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.biologer.biologer.model.ObservationType;
import org.biologer.biologer.model.Taxon;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "slug",
        "translations",
})

public class ObservationTypes {
    @JsonProperty("id")
    private Integer id;
    @JsonProperty("slug")
    private String slug;
    @JsonProperty("translations")
    private List<ObservationTypesTranslations> translations;

    public ObservationType toObservationType(){
        return new ObservationType(Long.valueOf(id), slug);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public List<ObservationTypesTranslations> getTranslations() {
        return translations;
    }

    public void setTranslations(List<ObservationTypesTranslations> translations) {
        this.translations = translations;
    }
}
