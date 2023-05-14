package org.biologer.biologer.sql;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class UnreadNotificationsDb {
    @Id
    long id;
    private String realId;
    private String type;
    private String notifiableType;
    private int fieldObservationId;
    private String causerName;
    private String curatorName;
    private String taxonName;
    private String updatedAt;

    public UnreadNotificationsDb (long id, String realId, String type,
                                  String notifiableType, int fieldObservationId,
                                  String causerName, String curatorName,
                                  String taxonName, String updatedAt) {
        this.id = id;
        this.realId = realId;
        this.type = type;
        this.notifiableType = notifiableType;
        this.fieldObservationId = fieldObservationId;
        this.causerName = causerName;
        this.curatorName = curatorName;
        this.taxonName = taxonName;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public String getRealId() {
        return realId;
    }

    public void setRealId(String realId) {
        this.realId = realId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNotifiableType() {
        return notifiableType;
    }

    public void setNotifiableType(String notifiableType) {
        this.notifiableType = notifiableType;
    }

    public int getFieldObservationId() {
        return fieldObservationId;
    }

    public void setFieldObservationId(int fieldObservationId) {
        this.fieldObservationId = fieldObservationId;
    }

    public String getCauserName() {
        return causerName;
    }

    public void setCauserName(String causerName) {
        this.causerName = causerName;
    }

    public String getCuratorName() {
        return curatorName;
    }

    public void setCuratorName(String curatorName) {
        this.curatorName = curatorName;
    }

    public String getTaxonName() {
        return taxonName;
    }

    public void setTaxonName(String taxonName) {
        this.taxonName = taxonName;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
