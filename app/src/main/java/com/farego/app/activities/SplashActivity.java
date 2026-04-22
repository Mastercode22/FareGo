package com.farego.app.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.farego.app.R;
import com.farego.app.utils.SessionManager;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 2200L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo   = findViewById(R.id.iv_splash_logo);
        TextView tagline = findViewById(R.id.tv_splash_tagline);

        // Fade-in animation
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(900);
        fadeIn.setFillAfter(true);
        logo.startAnimation(fadeIn);

        AlphaAnimation fadeIn2 = new AlphaAnimation(0f, 1f);
        fadeIn2.setDuration(900);
        fadeIn2.setStartOffset(400);
        fadeIn2.setFillAfter(true);
        tagline.startAnimation(fadeIn2);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager session = new SessionManager(this);
            Intent next;
            if (session.isLoggedIn()) {
                next = new Intent(this, MainActivity.class);
            } else {
                next = new Intent(this, OnboardingActivity.class);
            }
            startActivity(next);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, SPLASH_DURATION);
    }
}
