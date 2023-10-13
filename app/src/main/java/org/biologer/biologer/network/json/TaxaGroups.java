package org.biologer.biologer.network.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TaxaGroups {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("parent_id")
    private Long parentId;
    @JsonProperty("image_url")
    private String imageUrl;
    @JsonProperty("sort_order")
    private String sortOrder;
    @JsonProperty("only_observed_taxa")
    private boolean onlyObservedTaxa;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("translations")
    private List<TaxaGroupsTranslations> translations;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<TaxaGroupsTranslations> getTranslations() {
        return translations;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getDescription() {
        return description;
    }
}
