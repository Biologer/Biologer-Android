package org.biologer.biologer.sql;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class TaxonGroupsDb {
    @Id(assignable = true)
    long id;
    private long parentId;
    private String name;
    private String description;

    public TaxonGroupsDb(long id, long parentId, String name, String description) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.description = description;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        id = id;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
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
