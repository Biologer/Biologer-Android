package org.biologer.biologer.sql;

import org.biologer.biologer.network.json.FieldObservationDataPhotos;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class PhotoDb {
    @Id
    public long id;
    private String localPath;
    private String serverPath;
    private String serverUrl;
    private long serverId;
    private String author;
    private Integer licenseId;

    public ToOne<EntryDb> entry;

    public static PhotoDb getPhotoFields(FieldObservationDataPhotos data) {
            PhotoDb photo = new PhotoDb();
            photo.setServerId(data.getId());
            photo.setServerUrl(data.getUrl());
            photo.setLocalPath(null);
            photo.setServerPath(data.getPath());
            photo.setAuthor(data.getAuthor());
            photo.setLicenseId(data.getLicense().getId());

            return photo;
    }

    public long getId() {
        return id;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getServerPath() {
        return serverPath;
    }

    public void setServerPath(String serverPath) {
        this.serverPath = serverPath;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Integer getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(Integer licenseId) {
        this.licenseId = licenseId;
    }
}