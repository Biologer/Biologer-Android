package org.biologer.biologer.sql;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class ObservationActivityDb {
    @Id
    public long id;
    public long serverId;
    public String description;
    public String createdAt;
    public String causerName;
    public String reason;

    public ToOne<EntryDb> entry;

    public ObservationActivityDb() {}

    public ObservationActivityDb(long serverId, String description, String causerName, String createdAt, String reason) {
        this.serverId = serverId;
        this.description = description;
        this.causerName = causerName;
        this.createdAt = createdAt;
        this.reason = reason;
    }

    public long getId() {
        return id;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
