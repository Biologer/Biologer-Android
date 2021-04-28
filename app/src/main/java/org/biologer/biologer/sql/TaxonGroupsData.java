package org.biologer.biologer.sql;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

@Entity
public class TaxonGroupsData {
    @Id(autoincrement = true)
    private Long Id;
    private Long prentId;
    private String name;
    private String description;

    @Generated(hash = 964810650)
    public TaxonGroupsData(Long Id, Long prentId, String name, String description) {
        this.Id = Id;
        this.prentId = prentId;
        this.name = name;
        this.description = description;
    }

    @Generated(hash = 1005564454)
    public TaxonGroupsData() {
    }

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }

    public Long getPrentId() {
        return prentId;
    }

    public void setPrentId(Long prentId) {
        this.prentId = prentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
