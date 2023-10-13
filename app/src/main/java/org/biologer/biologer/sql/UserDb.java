package org.biologer.biologer.sql;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * Created by brjovanovic on 3/20/2018.
 */

@Entity
public class UserDb {
    @Id
    long id;
    private String username;
    private String email;
    private int dataLicense;
    private int imageLicense;

    public UserDb(long id, String username, String email, int dataLicense, int imageLicense) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.dataLicense = dataLicense;
        this.imageLicense = imageLicense;
    }

    public long getId() {
        return this.id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getUsername() {
        return this.username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getEmail() {
        return this.email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public int getDataLicense() {
        return this.dataLicense;
    }
    public void setDataLicense(int data_license) {
        this.dataLicense = data_license;
    }
    public int getImageLicense() {
        return this.imageLicense;
    }
    public void setImageLicense(int image_license) {
        this.imageLicense = image_license;
    }

}
