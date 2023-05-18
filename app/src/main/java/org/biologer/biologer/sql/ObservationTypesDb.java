package org.biologer.biologer.sql;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class ObservationTypesDb {
    @Id
    long id;
    private final long localeId;
    private long observationId;
    private final String slug;
    private String locale;
    private String name;

    public ObservationTypesDb(long id, long localeId, long observationId, String slug, String locale, String name) {
        this.id = id;
        this.localeId = localeId;
        this.observationId = observationId;
        this.slug = slug;
        this.locale  = locale;
        this.name = name;
    }

    public long getLocaleId() {
        return localeId;
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

    public long getObservationId() {
        return observationId;
    }

    public String getSlug() {
        return slug;
    }

}
