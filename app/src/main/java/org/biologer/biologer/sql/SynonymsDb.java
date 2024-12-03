package org.biologer.biologer.sql;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * Created by Miloš Popović on 3/12/2024.
 */
@Entity
public class SynonymsDb {

    @Id(assignable = true)
    long id;
    long taxonId;
    private String name;

    public SynonymsDb(long id, long taxonId, String name) {
        this.id = id;
        this.taxonId = taxonId;
        this.name = name;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTaxonId() {
        return this.taxonId;
    }

    public void setTaxonId(long taxonId) {
        this.taxonId = taxonId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

}

