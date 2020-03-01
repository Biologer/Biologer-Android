package org.biologer.biologer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import android.util.Log;

import org.biologer.biologer.model.RetrofitClient;
import org.biologer.biologer.model.UserData;
import org.biologer.biologer.model.network.UserDataResponse;
import org.biologer.biologer.model.network.UserDataSer;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateLicenses extends Service {

    private static final String TAG = "Biologer.UpdateLicense";

    private static final int TOTAL_RETRIES = 3;
    private int retryCount = 0;

    // Get the user data from a GreenDao database
    List<UserData> userdata_list = App.get().getDaoSession().getUserDataDao().loadAll();

    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Update licence activity started...");
        updateLicense();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Check if user selected custom Data and Image Licenses. If not, update them from the server.
    public void updateLicense() {
        // Get the values from Shared Preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String data_license = preferences.getString("data_license", "0");
        final String image_license = preferences.getString("image_license", "0");

        if (data_license != null && image_license != null) {
            if (data_license.equals("0") || image_license.equals("0")) {
                // Get User data from a server
                Call<UserDataResponse> call = RetrofitClient.getService(SettingsManager.getDatabaseName()).getUserData();
                call.enqueue(new Callback<UserDataResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<UserDataResponse> call, @NonNull Response<UserDataResponse> response) {
                        if (response.isSuccessful()) {
                            if (response.body().getData() != null) {
                                UserDataSer user = response.body().getData();
                                final String email = user.getEmail();
                                String name = user.getFullName();
                                int server_data_license = user.getSettings().getDataLicense();
                                int server_image_license = user.getSettings().getImageLicense();

                                // If both data and image licence should be retrieved from server
                                if (data_license.equals("0") && image_license.equals("0")) {
                                    UserData uData = new UserData(getUserID(), email, name, server_data_license, server_image_license);
                                    App.get().getDaoSession().getUserDataDao().insertOrReplace(uData);
                                    Log.d(TAG, "Image and data licenses updated from the server.");
                                }
                                // If only Data License should be retrieved from server
                                if (data_license.equals("0") && !image_license.equals("0")) {
                                    UserData uData = new UserData(getUserID(), email, name, server_data_license, Integer.parseInt(image_license));
                                    App.get().getDaoSession().getUserDataDao().insertOrReplace(uData);
                                    Log.d(TAG, "Data licenses updated from the server. Image licence set by user to: " + image_license);
                                }
                                // If only Image License should be retrieved from server
                                if (!data_license.equals("0") && image_license.equals("0")) {
                                    UserData uData = new UserData(getUserID(), email, name, Integer.parseInt(data_license), server_image_license);
                                    App.get().getDaoSession().getUserDataDao().insertOrReplace(uData);
                                    Log.d(TAG, "Image licenses updated from the server. Data license set by user to: " + data_license);
                                }
                            } else {
                                Log.e(TAG, "Application could not get user’s licences from the server.");
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<UserDataResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "Application could not get user’s licences from the server.");
                        if (retryCount++ < TOTAL_RETRIES) {
                            Log.d(TAG, "Retrying request to get licences from server (" + retryCount + " out of " + TOTAL_RETRIES + ")");
                            updateLicense();
                        }
                    }
                });
            } else {
                // If both data ind image license should be taken from preferences
                Log.d(TAG, "User selected custom licences for images (" + image_license + ") and data (" + data_license + ").");
                UserData uData = new UserData(getUserID(), getUserEmail(), getUserName(), Integer.parseInt(data_license), Integer.parseInt(image_license));
                App.get().getDaoSession().getUserDataDao().insertOrReplace(uData);
            }
        }
        stopSelf();
    }

    // Get the data from GreenDao database
    private UserData getLoggedUser() {
        if (userdata_list.isEmpty()) {
            clearUserData(this);
            userLoggedOut();
        }
        return userdata_list.get(0);
    }

    public Long getUserID() {
        return getLoggedUser().getId();
    }

    private String getUserName() {
        return getLoggedUser().getUsername();
    }

    private String getUserEmail() {
        return getLoggedUser().getEmail();
    }

    private void userLoggedOut() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public static void clearUserData(Context context) {
        // Delete user token
        SettingsManager.deleteToken();
        // Set the default preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().clear().apply();
        SettingsManager.setTaxaDatabaseUpdated("0");
        SettingsManager.setProjectName(null);
        SettingsManager.setTaxaLastPageFetched("1");
        // Maybe also to delete database...
        App.get().getDaoSession().getTaxonDao().deleteAll();
        App.get().getDaoSession().getStageDao().deleteAll();
        App.get().getDaoSession().getUserDataDao().deleteAll();
    }
}
