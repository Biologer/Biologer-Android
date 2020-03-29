package org.biologer.biologer.sql;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by brjovanovic on 2/23/2018.
 */
@Entity
public class Stage {

    @Id (autoincrement = true)
    private Long id;
    private String name;
    private long stageId;
    private long taxonId;
    @Generated(hash = 1764394544)
    public Stage(Long id, String name, long stageId, long taxonId) {
        this.id = id;
        this.name = name;
        this.stageId = stageId;
        this.taxonId = taxonId;
    }
    @Generated(hash = 709184509)
    public Stage() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public long getStageId() {
        return this.stageId;
    }
    public void setStageId(long stageId) {
        this.stageId = stageId;
    }
    public long getTaxonId() {
        return this.taxonId;
    }
    public void setTaxonId(long taxonId) {
        this.taxonId = taxonId;
    }

}

