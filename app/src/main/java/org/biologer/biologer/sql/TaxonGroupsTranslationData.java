package org.biologer.biologer.sql;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

@Entity
public class TaxonGroupsTranslationData {
    @Id(autoincrement = true)
    private Long Id;
    private Long viewGroupId;
    private String locale;
    private String native_name;
    private String description;

    @Generated(hash = 1197764029)
    public TaxonGroupsTranslationData(Long Id, Long viewGroupId, String locale, String native_name, String description) {
        this.Id = Id;
        this.viewGroupId = viewGroupId;
        this.locale = locale;
        this.native_name = native_name;
        this.description = description;
    }

    @Generated(hash = 1534363921)
    public TaxonGroupsTranslationData() {
    }

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }

    public Long getViewGroupId() {
        return viewGroupId;
    }

    public void setViewGroupId(Long viewGroupId) {
        this.viewGroupId = viewGroupId;
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

    public void setNative_name(String native_name) {
        this.native_name = native_name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
