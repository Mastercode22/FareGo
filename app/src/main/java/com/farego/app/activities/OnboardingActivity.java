package com.farego.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.farego.app.R;
import com.farego.app.adapters.OnboardingAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button btnNext;
    private TextView tvSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.vp_onboarding);
        btnNext   = findViewById(R.id.btn_onboarding_next);
        tvSkip    = findViewById(R.id.tv_onboarding_skip);
        TabLayout tabDots = findViewById(R.id.tab_dots);

        OnboardingAdapter adapter = new OnboardingAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabDots, viewPager, (tab, position) -> {}).attach();

        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < 2) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                goToAuth();
            }
        });

        tvSkip.setOnClickListener(v -> goToAuth());

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 2) {
                    btnNext.setText("Get Started");
                    tvSkip.setVisibility(View.GONE);
                } else {
                    btnNext.setText("Next");
                    tvSkip.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void goToAuth() {
        startActivity(new Intent(this, AuthActivity.class));
        finish();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}
