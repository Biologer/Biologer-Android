package org.biologer.biologer.gui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import org.biologer.biologer.R;
import org.biologer.biologer.User;

public class SplashActivity extends AppCompatActivity {

    int SPLASH_TIME_OUT = 500;
    SharedPreferences prefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        prefs = getSharedPreferences("org.biologer.biologer", MODE_PRIVATE);

        new Handler().postDelayed(() -> {

            if (prefs.getBoolean("firstrun", true)){
                prefs.edit().putBoolean("firstrun", false).apply();
                Intent intent = new Intent(SplashActivity.this, IntroActivity.class);
                startActivity(intent);
            }
            else {
                if (User.getUser().isLoggedIn()) {
                    Intent intent = new Intent(SplashActivity.this, LandingActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
                finish();
            }
        }, SPLASH_TIME_OUT);


    }
}
