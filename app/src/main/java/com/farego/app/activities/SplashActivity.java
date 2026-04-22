package com.farego.app.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.AnimationSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.farego.app.R;
import com.farego.app.utils.SessionManager;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 5000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo   = findViewById(R.id.iv_splash_logo);
        TextView tagline = findViewById(R.id.tv_splash_tagline);

        // --- Zoom in/out pulse for the logo (repeats for ~5 seconds) ---
        ScaleAnimation pulse = new ScaleAnimation(
                1f, 1.2f,   // X: scale from 100% to 120%
                1f, 1.2f,   // Y: scale from 100% to 120%
                Animation.RELATIVE_TO_SELF, 0.5f,  // pivot X = center
                Animation.RELATIVE_TO_SELF, 0.5f   // pivot Y = center
        );
        pulse.setDuration(600);                        // each half-cycle = 600 ms
        pulse.setRepeatMode(Animation.REVERSE);        // zoom back out automatically
        pulse.setRepeatCount(Animation.INFINITE);      // keep looping
        pulse.setFillAfter(true);
        logo.startAnimation(pulse);

        // --- Tagline fade-in (unchanged) ---
        AlphaAnimation fadeIn2 = new AlphaAnimation(0f, 1f);
        fadeIn2.setDuration(900);
        fadeIn2.setStartOffset(400);
        fadeIn2.setFillAfter(true);
        tagline.startAnimation(fadeIn2);

        // --- Navigate after 5 seconds ---
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            logo.clearAnimation(); // stop the loop cleanly
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