package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "parent_id",
        "name",
        "rank",
        "rank_level",
        "author",
        "fe_old_id",
        "fe_id",
        "restricted",
        "allochthonous",
        "invasive",
        "uses_atlas_codes",
        "ancestors_names",
        "taxonomy_id",
        "rank_translation",
        "native_name",
        "description",
        "translations"
})

@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldObservationDataTaxon {

    @JsonProperty("id")
    private int id;

    @JsonProperty("parent_id")
    private int parentId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("rank")
    private String rank;

    @JsonProperty("rank_level")
    private int rankLevel;

    @JsonProperty("author")
    private String author;

    @JsonProperty("fe_old_id")
    private String faunaEuropaeaOldId;

    @JsonProperty("fe_id")
    private String faunaEuropaeaId;

    @JsonProperty("restricted")
    private boolean restricted;

    @JsonProperty("allochthonous")
    private boolean allochtonous;

    @JsonProperty("invasive")
    private boolean invasive;

    @JsonProperty("uses_atlas_codes")
    private boolean usesAtlasCode;

    @JsonProperty("ancestors_names")
    private String ancestorsNames;

    @JsonProperty("taxonomy_id")
    private String taxonomyId;

    @JsonProperty("rank_translation")
    private String rankTranslation;

    @JsonProperty("native_name")
    private String nativeName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("translations")
    private String translations;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
