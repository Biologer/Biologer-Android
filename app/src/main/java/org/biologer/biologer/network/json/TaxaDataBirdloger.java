
package org.biologer.biologer.network.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TaxaDataBirdloger {

    @JsonProperty("id")
    private Long id;
    @JsonProperty("parent_id")
    private Long parentId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("rank")
    private String rank;
    @JsonProperty("rank_level")
    private Long rankLevel;
    @JsonProperty("author")
    private String author;
    @JsonProperty("fe_old_id")
    private Object feOldId;
    @JsonProperty("fe_id")
    private Object feId;
    @JsonProperty("restricted")
    private String restricted;
    @JsonProperty("allochthonous")
    private String allochthonous;
    @JsonProperty("invasive")
    private String invasive;
    @JsonProperty("uses_atlas_codes")
    private boolean uses_atlas_codes;
    @JsonProperty("ancestors_names")
    private String ancestors_names;
    @JsonProperty("type")
    private String type;
    @JsonProperty("spid")
    private String spid;
    @JsonProperty("strictly_protected")
    private boolean strictly_protected_species;
    @JsonProperty("strictly_note")
    private String strictly_note;
    @JsonProperty("protected")
    private boolean protected_species;
    @JsonProperty("protected_note")
    private String protected_note;
    @JsonProperty("iucn_cat")
    private String iucn_cat;
    @JsonProperty("birdlife_seq")
    private Long birdlife_seq;
    @JsonProperty("birdlife_id")
    private Long birdlife_id;
    @JsonProperty("ebba_code")
    private Long ebba_code;
    @JsonProperty("euring_code")
    private Long euring_code;
    @JsonProperty("euring_sci_name")
    private String euring_sci_name;
    @JsonProperty("refer")
    private boolean refer;
    @JsonProperty("prior")
    private boolean prior;
    @JsonProperty("gn_status")
    private String gn_status;
    @JsonProperty("bioras_sci_name")
    private String bioras_sci_name;
    @JsonProperty("full_sci_name")
    private String full_sci_name;
    @JsonProperty("family_id")
    private Long family_id;
    @JsonProperty("order_id")
    private Long order_id;
    @JsonProperty("can_edit")
    private boolean can_edit;
    @JsonProperty("can_delete")
    private boolean can_delete;
    @JsonProperty("rank_translation")
    private String rankTranslation;
    @JsonProperty("native_name")
    private Object nativeName;
    @JsonProperty("description")
    private Object description;
    @JsonProperty("translations")
    private List<TaxaTranslations> taxaTranslations = null;
    @JsonProperty("parent")
    private Parent parent;
    @JsonProperty("stages")
    private List<TaxaStages> stages = null;
    @JsonProperty("activity")
    private List<Object> activity = null;
    @JsonProperty("groups")
    private List<String> groups;

    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty("parent_id")
    public Long getParentId() {
        return parentId;
    }

    @JsonProperty("parent_id")
    public void setParentId(Long parentId) {
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
    public Long getRankLevel() {
        return rankLevel;
    }

    @JsonProperty("rank_level")
    public void setRankLevel(Long rankLevel) {
        this.rankLevel = rankLevel;
    }

    @JsonProperty("author")
    public String getAuthor() {
        return author;
    }

    @JsonProperty("author")
    public void setAuthor(String author) {
        this.author = author;
    }

    @JsonProperty("native_name")
    public Object getNativeName() {
        return nativeName;
    }

    @JsonProperty("native_name")
    public void setNativeName(Object nativeName) {
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

    @JsonProperty("parent")
    public Parent getParent() {
        return parent;
    }

    @JsonProperty("parent")
    public void setParent(Parent parent) {
        this.parent = parent;
    }

    @JsonProperty("stages")
    public List<TaxaStages> getStages() {
        return stages;
    }

    @JsonProperty("stages")
    public void setStages(List<TaxaStages> stages) {
        this.stages = stages;
    }

    @JsonProperty("activity")
    public List<Object> getActivity() {
        return activity;
    }

    @JsonProperty("activity")
    public void setActivity(List<Object> activity) {
        this.activity = activity;
    }

    public boolean isCan_edit() {
        return can_edit;
    }

    public void setCan_edit(boolean can_edit) {
        this.can_edit = can_edit;
    }

    public boolean isCan_delete() {
        return can_delete;
    }

    public void setCan_delete(boolean can_delete) {
        this.can_delete = can_delete;
    }

    public boolean isUses_atlas_codes() {
        return uses_atlas_codes;
    }

    public void setUses_atlas_codes(boolean uses_atlas_codes) {
        this.uses_atlas_codes = uses_atlas_codes;
    }

    public String getAncestors_names() {
        return ancestors_names;
    }

    public void setAncestors_names(String ancestors_names) {
        this.ancestors_names = ancestors_names;
    }

    @JsonProperty("groups")
    public List<String> getGroups() {
        return groups;
    }

    @JsonProperty("groups")
    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public String getRestricted() {
        return restricted;
    }

    public void setRestricted(String restricted) {
        this.restricted = restricted;
    }
}
