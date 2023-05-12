package org.biologer.biologer.sql;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * Created by brjovanovic on 3/20/2018.
 */

@Entity
public class UserData {
    @Id
    long id;
    private String username;
    private String email;
    private int data_license;
    private int image_license;

    public UserData(long id, String username, String email, int data_license, int image_license) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.data_license = data_license;
        this.image_license = image_license;
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
    public int getData_license() {
        return this.data_license;
    }
    public void setData_license(int data_license) {
        this.data_license = data_license;
    }
    public int getImage_license() {
        return this.image_license;
    }
    public void setImage_license(int image_license) {
        this.image_license = image_license;
    }

}
