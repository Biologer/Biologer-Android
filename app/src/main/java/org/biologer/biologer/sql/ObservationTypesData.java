package org.biologer.biologer.sql;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

@Entity
public class ObservationTypesData {
    @Id
    private Long localeId;
    private Long observationId;
    private String slug;
    private String locale;
    private String name;

    @Generated(hash = 652229580)
    public ObservationTypesData(Long localeId, Long observationId, String slug, String locale, String name) {
        this.localeId = localeId;
        this.observationId = observationId;
        this.slug = slug;
        this.locale = locale;
        this.name = name;
    }
    @Generated(hash = 1635477211)
    public ObservationTypesData() {
    }
    public Long getLocaleId() {
        return localeId;
    }

    public void setLocaleId(Long localeId) {
        this.localeId = localeId;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getObservationId() {
        return observationId;
    }

    public void setObservationId(Long observationId) {
        this.observationId = observationId;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
