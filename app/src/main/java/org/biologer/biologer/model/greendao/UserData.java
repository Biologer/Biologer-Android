package org.biologer.biologer.model.greendao;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by brjovanovic on 3/20/2018.
 */

@Entity
public class UserData {
    @org.greenrobot.greendao.annotation.Id
    private Long Id;
    private String username;
    private String email;
    private int data_license;
    private int image_license;
    @Generated(hash = 784653985)
    public UserData(Long Id, String username, String email, int data_license, int image_license) {
        this.Id = Id;
        this.username = username;
        this.email = email;
        this.data_license = data_license;
        this.image_license = image_license;
    }
    @Generated(hash = 1838565001)
    public UserData() {
    }
    public Long getId() {
        return this.Id;
    }
    public void setId(Long Id) {
        this.Id = Id;
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
