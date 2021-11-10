package org.biologer.biologer.gui;

import android.content.Intent;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

import com.github.appintro.AppIntro2;
import com.github.appintro.AppIntroFragment;

import org.biologer.biologer.R;
import org.biologer.biologer.User;
import org.jetbrains.annotations.Nullable;

public class IntroActivity extends AppIntro2 {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(AppIntroFragment.newInstance(getString(R.string.Slide1_title), getString(R.string.first_slide), R.drawable.intro1,
                ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryLight)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.Slide2_title), getString(R.string.second_slide), R.drawable.intro2,
                ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryLight)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.Slide3_title), getString(R.string.third_slide), R.drawable.intro3,
                ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryLight)));
        addSlide(AppIntroFragment.newInstance(getString(R.string.Slide4_title), getString(R.string.fourth_slide), R.drawable.intro4,
                ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryLight)));
        showStatusBar(false);
    }

    @Override
    protected void onSkipPressed(@Nullable Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        launchNextActivity();
    }

    @Override
    protected void onDonePressed(@Nullable Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        launchNextActivity();
    }

    private void launchNextActivity() {
        if (User.getUser().tokenPresent()) {
            Intent intent = new Intent(IntroActivity.this, LandingActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(IntroActivity.this, LoginActivity.class);
            startActivity(intent);
        }
        finish();
    }
}
