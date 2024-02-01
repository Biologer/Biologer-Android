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
    private String thumbnail, image1, image2, image3;
    private String date, location, project;

    public UnreadNotificationsDb (long id, String realId, String type,
                                  String notifiableType, int fieldObservationId,
                                  String causerName, String curatorName,
                                  String taxonName, String updatedAt,
                                  String thumbnail, String image1, String image2,  String image3,
                                  String date, String location, String project) {
        this.id = id;
        this.realId = realId;
        this.type = type;
        this.notifiableType = notifiableType;
        this.fieldObservationId = fieldObservationId;
        this.causerName = causerName;
        this.curatorName = curatorName;
        this.taxonName = taxonName;
        this.updatedAt = updatedAt;
        this.image1 = image1;
        this.image2 = image2;
        this.image3 = image3;
        this.thumbnail = thumbnail;
        this.date = date;
        this.location = location;
        this.project = project;
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

    public String getImage1() {
        return image1;
    }

    public void setImage1(String image1) {
        this.image1 = image1;
    }

    public String getImage2() {
        return image2;
    }

    public void setImage2(String image2) {
        this.image2 = image2;
    }

    public String getImage3() {
        return image3;
    }

    public void setImage3(String image3) {
        this.image3 = image3;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }
}
