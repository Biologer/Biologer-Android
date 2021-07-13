package org.biologer.biologer.gui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import org.biologer.biologer.R;

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
