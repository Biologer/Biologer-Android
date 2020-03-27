package org.biologer.biologer.network.JSON;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "parent_id",
    "name",
    "rank",
    "rank_level",
    "author",
    "ancestry",
    "fe_old_id",
    "fe_id",
    "restricted",
    "allochthonous",
    "invasive",
    "rank_translation",
    "native_name",
    "description",
    "translations"
})
public class Parent {

    @JsonProperty("id")
    private long id;
    @JsonProperty("parent_id")
    private String parentId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("rank")
    private String rank;
    @JsonProperty("rank_level")
    private long rankLevel;
    @JsonProperty("author")
    private Object author;
    @JsonProperty("ancestry")
    private String ancestry;
    @JsonProperty("fe_old_id")
    private Object feOldId;
    @JsonProperty("fe_id")
    private Object feId;
    @JsonProperty("restricted")
    private boolean restricted;
    @JsonProperty("allochthonous")
    private boolean allochthonous;
    @JsonProperty("invasive")
    private boolean invasive;
    @JsonProperty("rank_translation")
    private String rankTranslation;
    @JsonProperty("native_name")
    private String nativeName;
    @JsonProperty("description")
    private Object description;
    @JsonProperty("translations")
    private List<TaxaTranslations> taxaTranslations = null;

    @JsonProperty("id")
    public long getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(long id) {
        this.id = id;
    }

    @JsonProperty("parent_id")
    public String getParentId() {
        return parentId;
    }

    @JsonProperty("parent_id")
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("rank")
    public String getRank() {
        return rank;
    }

    @JsonProperty("rank")
    public void setRank(String rank) {
        this.rank = rank;
    }

    @JsonProperty("rank_level")
    public long getRankLevel() {
        return rankLevel;
    }

    @JsonProperty("rank_level")
    public void setRankLevel(long rankLevel) {
        this.rankLevel = rankLevel;
    }

    @JsonProperty("author")
    public Object getAuthor() {
        return author;
    }

    @JsonProperty("author")
    public void setAuthor(Object author) {
        this.author = author;
    }

    @JsonProperty("ancestry")
    public String getAncestry() {
        return ancestry;
    }

    @JsonProperty("ancestry")
    public void setAncestry(String ancestry) {
        this.ancestry = ancestry;
    }

    @JsonProperty("fe_old_id")
    public Object getFeOldId() {
        return feOldId;
    }

    @JsonProperty("fe_old_id")
    public void setFeOldId(Object feOldId) {
        this.feOldId = feOldId;
    }

    @JsonProperty("fe_id")
    public Object getFeId() {
        return feId;
    }

    @JsonProperty("fe_id")
    public void setFeId(Object feId) {
        this.feId = feId;
    }

    @JsonProperty("restricted")
    public boolean isRestricted() {
        return restricted;
    }

    @JsonProperty("restricted")
    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    @JsonProperty("allochthonous")
    public boolean isAllochthonous() {
        return allochthonous;
    }

    @JsonProperty("allochthonous")
    public void setAllochthonous(boolean allochthonous) {
        this.allochthonous = allochthonous;
    }

    @JsonProperty("invasive")
    public boolean isInvasive() {
        return invasive;
    }

    @JsonProperty("invasive")
    public void setInvasive(boolean invasive) {
        this.invasive = invasive;
    }

    @JsonProperty("rank_translation")
    public String getRankTranslation() {
        return rankTranslation;
    }

    @JsonProperty("rank_translation")
    public void setRankTranslation(String rankTranslation) {
        this.rankTranslation = rankTranslation;
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

    @JsonProperty("translations")
    public List<TaxaTranslations> getTaxaTranslations() {
        return taxaTranslations;
    }

    @JsonProperty("translations")
    public void setTaxaTranslations(List<TaxaTranslations> taxaTranslations) {
        this.taxaTranslations = taxaTranslations;
    }

}
