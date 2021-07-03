package org.biologer.biologer.gui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
import org.biologer.biologer.network.InternetConnection;
import org.biologer.biologer.network.JSON.RefreshTokenResponse;
import org.biologer.biologer.network.JSON.UserDataResponse;
import org.biologer.biologer.network.RetrofitClient;
import org.biologer.biologer.sql.UserData;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "Biologer.Splash";

    int SPLASH_TIME_OUT = 600;
    SharedPreferences prefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        prefs = getSharedPreferences("org.biologer.biologer", MODE_PRIVATE);

        Handler handler = new Handler();
        Runnable runnable = () -> {

            // On the first run show some help
            if (prefs.getBoolean("firstrun", true)) {
                Log.d(TAG, "This is first run of the program.");
                prefs.edit().putBoolean("firstrun", false).apply();
                Intent intent = new Intent(SplashActivity.this, IntroActivity.class);
                startActivity(intent);
            }

            // If already started before
            else {
                Intent intent = new Intent(SplashActivity.this, LandingActivity.class);
                startActivity(intent);
            }
        };

        handler.postDelayed(runnable, SPLASH_TIME_OUT);
    }
}
