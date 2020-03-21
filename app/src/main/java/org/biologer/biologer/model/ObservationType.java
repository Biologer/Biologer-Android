package org.biologer.biologer.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

@Entity
public class ObservationType {
    @Id
    private Long id;
    private String slug;

    @Generated(hash = 939207341)
    public ObservationType(Long id, String slug) {
        this.id = id;
        this.slug = slug;
    }
    @Generated(hash = 328174704)
    public ObservationType() {
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    void setSlug(String slug) {
        this.slug = slug;
    }
}
