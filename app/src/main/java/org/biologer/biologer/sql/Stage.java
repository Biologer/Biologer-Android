package org.biologer.biologer.sql;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * Created by brjovanovic on 2/23/2018.
 */
@Entity
public class Stage {

    @Id
    long id;
    private String name;
    private final long stageId;
    private long taxonId;

    public Stage(long id, String name, long stageId, long taxonId) {
        this.id = id;
        this.name = name;
        this.stageId = stageId;
        this.taxonId = taxonId;
    }

    public long getId() {
        return this.id;
    }
    public void setId(long id) {
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

    public long getTaxonId() {
        return this.taxonId;
    }
    public void setTaxonId(long taxonId) {
        this.taxonId = taxonId;
    }

}

