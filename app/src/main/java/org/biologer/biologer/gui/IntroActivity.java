package org.biologer.biologer.gui;

import android.content.Intent;

import androidx.core.content.ContextCompat;
import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntro2Fragment;

import org.biologer.biologer.R;
import org.biologer.biologer.User;

public class IntroActivity extends AppIntro2 {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(AppIntro2Fragment.newInstance(getString(R.string.Slide1_title), getString(R.string.first_slide), R.drawable.intro1,
                ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryLight)));
        addSlide(AppIntro2Fragment.newInstance(getString(R.string.Slide2_title), getString(R.string.second_slide), R.drawable.intro2,
                ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryLight)));
        addSlide(AppIntro2Fragment.newInstance(getString(R.string.Slide3_title), getString(R.string.third_slide), R.drawable.intro3,
                ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryLight)));
        addSlide(AppIntro2Fragment.newInstance(getString(R.string.Slide4_title), getString(R.string.fourth_slide), R.drawable.intro4,
                ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryLight)));
        //setNavBarColor(R.color.colorPrimaryLight);
        setFlowAnimation();
        showStatusBar(false);
    }

    @Override
    public void onSkipPressed() {
        // Do something when users tap on Done button.
        if (User.getUser().tokenPresent()) {
            Intent intent = new Intent(IntroActivity.this, LandingActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(IntroActivity.this, LoginActivity.class);
            startActivity(intent);
        }
        finish();
    }

    @Override
    public void onNextPressed() {
        // Do something when users tap on Next button.
    }

    @Override
    public void onDonePressed() {
        // Do something when users tap on Done button.
        if (User.getUser().tokenPresent()) {
            Intent intent = new Intent(IntroActivity.this, LandingActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(IntroActivity.this, LoginActivity.class);
            startActivity(intent);
        }
        finish();
    }

    @Override
    public void onSlideChanged() {
        // Do something when slide is changed
    }
}
