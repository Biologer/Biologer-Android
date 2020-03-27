package org.biologer.biologer;

/**
 * Created by brjovanovic on 12/24/2017.
 */

public class User {

    private static User user;

    private User() {
    }

    public static User getUser(){
        if(user==null){
            user = new User();
        }
        return user;
    }

    public boolean isLoggedIn(){
        return SettingsManager.getToken()!=null;
    }

    public void logOut(){
        // Delete user token
        SettingsManager.deleteToken();
        // Delete data from greengao database
        App.get().getDaoSession().getStageDao().deleteAll();
    }
}
