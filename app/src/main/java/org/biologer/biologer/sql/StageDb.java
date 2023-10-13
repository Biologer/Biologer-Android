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

    public StageDb(long id, String name) {
        this.id = id;
        this.name = name;
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

}

