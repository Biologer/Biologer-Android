package org.biologer.biologer.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

@Entity
public class ObservationTypeLocalization {
    @Id
    private Long id;
    private String locale;
    private String name;

    @Generated(hash = 1639112326)
    public ObservationTypeLocalization(Long id, String locale, String name) {
        this.id = id;
        this.locale = locale;
        this.name = name;
    }
    @Generated(hash = 1923696504)
    public ObservationTypeLocalization() {
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
