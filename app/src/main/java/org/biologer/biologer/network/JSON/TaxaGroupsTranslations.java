package org.biologer.biologer.network.JSON;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TaxaGroupsTranslations {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("view_group_id")
    private Long viewGroupId;
    @JsonProperty("locale")
    private String locale;
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;

    public Long getId() {
        return id;
    }

    public Long getViewGroupId() {
        return viewGroupId;
    }

    public String getLocale() {
        return locale;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
