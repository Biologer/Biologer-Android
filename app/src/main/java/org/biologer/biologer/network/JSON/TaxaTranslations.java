package org.biologer.biologer.network.JSON;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private Long id;
    @JsonProperty("taxon_id")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    private int taxonId;
    @JsonProperty("locale")
    private String locale;
    @JsonProperty("native_name")
    private String nativeName;
    @JsonProperty("description")
    private String description;

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty("taxon_id")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    public int getTaxonId() {
            return taxonId;
    }

    @JsonProperty("taxon_id")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    public void setTaxonId(int taxonId) {
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
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

}
