package org.biologer.biologer.gui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import org.biologer.biologer.R;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "Biologer.Splash";

    int SPLASH_TIME_OUT = 600;
    SharedPreferences sharedPreferences = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        sharedPreferences = getSharedPreferences("org.biologer.biologer", MODE_PRIVATE);


        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = () -> {

            // On the first run show some help
            if (sharedPreferences.getBoolean("first run", true)) {
                Log.d(TAG, "This is first run of the program.");
                sharedPreferences.edit().putBoolean("first run", false).apply();
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
