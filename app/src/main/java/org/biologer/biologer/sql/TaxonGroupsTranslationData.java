package org.biologer.biologer.sql;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class TaxonGroupsTranslationData {
    @Id(assignable = true)
    long id;
    private long viewGroupId;
    private String locale;
    private String native_name;
    private String description;

    public TaxonGroupsTranslationData(long id, long viewGroupId, String locale, String native_name, String description) {
        this.id = id;
        this.viewGroupId = viewGroupId;
        this.locale = locale;
        this.native_name = native_name;
        this.description = description;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        id = id;
    }

    public long getViewGroupId() {
        return viewGroupId;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getNative_name() {
        return native_name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
