package org.biologer.biologer.sql;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * Created by brjovanovic on 2/23/2018.
 */
@Entity
public class StageDb {

    @Id(assignable = true)
    long id;
    private String name;
    private long taxonId;

    public StageDb(long id, String name, long taxonId) {
        this.id = id;
        this.name = name;
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
    public long getTaxonId() {
        return this.taxonId;
    }
    public void setTaxonId(long taxonId) {
        this.taxonId = taxonId;
    }

}

