package org.biologer.biologer.network.JSON;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "taxon_id",
    "locale",
    "native_name",
    "description"
})
public class TaxaTranslations {

    @JsonProperty("id")
    private long id;
    @JsonProperty("taxon_id")
    private String taxonId;
    @JsonProperty("locale")
    private String locale;
    @JsonProperty("native_name")
    private String nativeName;
    @JsonProperty("description")
    private Object description;

    @JsonProperty("id")
    public long getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(long id) {
        this.id = id;
    }

    @JsonProperty("taxon_id")
    public String getTaxonId() {
        return taxonId;
    }

    @JsonProperty("taxon_id")
    public void setTaxonId(String taxonId) {
        this.taxonId = taxonId;
    }

    @JsonProperty("locale")
    public String getLocale() {
        return locale;
    }

    @JsonProperty("locale")
    public void setLocale(String locale) {
        this.locale = locale;
    }

    @JsonProperty("native_name")
    public String getNativeName() {
        return nativeName;
    }

    @JsonProperty("native_name")
    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    @JsonProperty("description")
    public Object getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(Object description) {
        this.description = description;
    }

}
