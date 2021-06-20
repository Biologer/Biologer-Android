package org.biologer.biologer.gui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.biologer.biologer.App;
import org.biologer.biologer.BuildConfig;
import org.biologer.biologer.GetTaxaGroups;
import org.biologer.biologer.R;
import org.biologer.biologer.SettingsManager;
import org.biologer.biologer.User;
import org.biologer.biologer.network.JSON.RefreshTokenResponse;
import org.biologer.biologer.network.JSON.UserDataResponse;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.sql.UserData;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "Biologer.Splash";

    int SPLASH_TIME_OUT = 500;
    SharedPreferences prefs = null;
    Call<RefreshTokenResponse> refresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        prefs = getSharedPreferences("org.biologer.biologer", MODE_PRIVATE);
        String token = SettingsManager.getAccessToken();
        String database_name = SettingsManager.getDatabaseName();
        boolean MAIL_CONFIRMED = SettingsManager.isMailConfirmed();

        new Handler().postDelayed(() -> {

            // On the first run show some help
            if (prefs.getBoolean("firstrun", true)) {
                Log.d(TAG, "This is first run of the program.");
                prefs.edit().putBoolean("firstrun", false).apply();
                Intent intent = new Intent(SplashActivity.this, IntroActivity.class);
                startActivity(intent);
            }

            // If already started before
            else {
                if (token != null && MAIL_CONFIRMED) {
                    if (Long.parseLong(SettingsManager.getTokenExpire()) >= System.currentTimeMillis()/1000) {
                        Log.d(TAG, "Token is still OK, email is confirmed.");

                        // If SQL is updated we will try to login in the user
                        if (SettingsManager.isSqlUpdated()) {
                            Log.i(TAG, "SQL database must be updated!");
                            Toast.makeText(this, getString(R.string.sql_updated_message), Toast.LENGTH_LONG).show();
                            User.resetTaxaSettings();

                            // First get the existing groups of taxa so we can fetch them again
                            ConnectivityManager connectivitymanager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                            if (Objects.requireNonNull(connectivitymanager.getActiveNetworkInfo()).isConnected()) {
                                final Intent getTaxaGroups = new Intent(SplashActivity.this, GetTaxaGroups.class);
                                startService(getTaxaGroups);
                            }

                            Call<UserDataResponse> service = RetrofitClient.getService(database_name).getUserData();
                            service.enqueue(new Callback<UserDataResponse>() {
                                @Override
                                public void onResponse(@NonNull Call<UserDataResponse> service, @NonNull Response<UserDataResponse> response) {
                                    if (response.isSuccessful()) {
                                        if (response.body() != null) {
                                            String email = response.body().getData().getEmail();
                                            String name = response.body().getData().getFullName();
                                            int data_license = response.body().getData().getSettings().getDataLicense();
                                            int image_license = response.body().getData().getSettings().getImageLicense();
                                            UserData user = new UserData(null, name, email, data_license, image_license);
                                            App.get().getDaoSession().getUserDataDao().insertOrReplace(user);
                                            SettingsManager.setSqlUpdated(false);
                                            Intent intent = new Intent(SplashActivity.this, LandingActivity.class);
                                            startActivity(intent);
                                        } else {
                                            dialogMessage(getString(R.string.login_after_sql_update_fail));
                                        }
                                    }
                                }
                                @Override
                                public void onFailure(@NonNull Call<UserDataResponse> service, @NonNull Throwable t) {
                                    Log.e(TAG, "Cannot get response from the server (test taxa response)");
                                    dialogMessage(getString(R.string.login_after_sql_update_fail));
                                }
                            });
                        }
                        else {
                            Log.i(TAG, "Everything is OK starting LandingActivity!");
                            Intent intent = new Intent(SplashActivity.this, LandingActivity.class);
                            startActivity(intent);
                        }
                    } else {
                        Log.d(TAG, "Token expired. Refreshing login token.");

                        String refreshToken = SettingsManager.getRefreshToken();
                        String rsKey = BuildConfig.BiologerRS_Key;
                        String hrKey = BuildConfig.BiologerHR_Key;
                        String baKey = BuildConfig.BiologerBA_Key;

                        if (database_name.equals("https://biologer.org")) {
                            Log.d(TAG, "Serbian database selected.");
                            refresh = RetrofitClient.getService(database_name).refresh("refresh_token", "2", rsKey, refreshToken, "*");
                        }
                        if (database_name.equals("https://biologer.hr")) {
                            Log.d(TAG, "Croatian database selected.");
                            refresh = RetrofitClient.getService(database_name).refresh("refresh_token", "2", hrKey, refreshToken, "*");
                        }
                        if (database_name.equals("https://biologer.ba")) {
                            Log.d(TAG, "Bosnian database selected.");
                            refresh = RetrofitClient.getService(database_name).refresh("refresh_token", "2", baKey, refreshToken, "*");
                        }
                        if (database_name.equals("https://dev.biologer.org")) {
                            Log.d(TAG, "Developmental database selected.");
                            refresh = RetrofitClient.getService(database_name).refresh("refresh_token", "2", rsKey, refreshToken,"*");
                        }
                        Log.d(TAG, "Logging into " + database_name + " using refresh token.");

                        refresh.enqueue(new Callback<RefreshTokenResponse>() {
                            @Override
                            public void onResponse(@NonNull Call<RefreshTokenResponse> call, @NonNull Response<RefreshTokenResponse> response) {
                                if(response.isSuccessful()) {
                                    if (response.body() != null) {
                                        String token = response.body().getAccessToken();
                                        String refresh_token = response.body().getRefreshToken();
                                        Log.d(TAG, "Token value is: " + token);
                                        SettingsManager.setAccessToken(token);
                                        SettingsManager.setRefreshToken(refresh_token);
                                        long expire = response.body().getExpiresIn();
                                        long expire_date = (System.currentTimeMillis() / 1000) + expire;
                                        SettingsManager.setTokenExpire(String.valueOf(expire_date));
                                        Log.d(TAG, "Token will expire on timestamp: " + expire_date);
                                        Intent intent = new Intent(SplashActivity.this, LandingActivity.class);
                                        startActivity(intent);
                                    }
                                } else {
                                    Toast.makeText(SplashActivity.this, getString(R.string.refresh_token_failed), Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<RefreshTokenResponse> call, @NonNull Throwable t) {
                                Log.e(TAG, "Cannot get response from the server (refresh token)" + t);
                                Toast.makeText(SplashActivity.this, getString(R.string.refresh_token_failed), Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                } else {
                    Log.d(TAG, "No token or email address not confirmed..");
                    Log.d(TAG, "TOKEN: " + token);
                    Log.d(TAG, "Is mail confirmed: " + MAIL_CONFIRMED);
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    intent.putExtra("TOKEN_EXPIRED", false);
                    startActivity(intent);
                }
                finish();
            }
        }, SPLASH_TIME_OUT);

    }

    private void dialogMessage(String message) {
        final AlertDialog.Builder builder_taxon = new AlertDialog.Builder(SplashActivity.this);
        builder_taxon.setMessage(message)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.OK), (dialog, id) -> {
                    this.finishAffinity();
                    dialog.dismiss();
                });
        final AlertDialog alert = builder_taxon.create();
        alert.show();
    }
}
