package com.farego.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.farego.app.R;
import com.farego.app.db.AppDatabase;
import com.farego.app.db.entity.User;
import com.farego.app.db.entity.UserProfile;
import com.farego.app.utils.HashUtils;
import com.farego.app.utils.SessionManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

public class AuthActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private TextInputEditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnSubmit;
    private View tilEmail, tilConfirmPassword;

    private boolean isLogin = true;
    private AppDatabase db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        db      = AppDatabase.getInstance(this);
        session = new SessionManager(this);

        tabLayout          = findViewById(R.id.tab_auth);
        etUsername         = findViewById(R.id.et_username);
        etEmail            = findViewById(R.id.et_email);
        etPassword         = findViewById(R.id.et_password);
        etConfirmPassword  = findViewById(R.id.et_confirm_password);
        btnSubmit          = findViewById(R.id.btn_auth_submit);
        tilEmail           = findViewById(R.id.til_email);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);

        tabLayout.addTab(tabLayout.newTab().setText("Login"));
        tabLayout.addTab(tabLayout.newTab().setText("Register"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                isLogin = tab.getPosition() == 0;
                updateUiForMode();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnSubmit.setOnClickListener(v -> {
            if (isLogin) doLogin();
            else         doRegister();
        });

        updateUiForMode();
    }

    private void updateUiForMode() {
        if (isLogin) {
            tilEmail.setVisibility(View.GONE);
            tilConfirmPassword.setVisibility(View.GONE);
            btnSubmit.setText("Login");
        } else {
            tilEmail.setVisibility(View.VISIBLE);
            tilConfirmPassword.setVisibility(View.VISIBLE);
            btnSubmit.setText("Create Account");
        }
    }

    private void doLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String hash = HashUtils.sha256(password);
        AppDatabase.DB_EXECUTOR.execute(() -> {
            User user = db.userDao().login(username, hash);
            runOnUiThread(() -> {
                if (user != null) {
                    // Update last login timestamp
                    AppDatabase.DB_EXECUTOR.execute(() ->
                            db.userDao().updateLastLogin(
                                    user.id, System.currentTimeMillis()));

                    // FIX: now matches saveSession(int, String, String)
                    session.saveSession(user.id, user.username, user.email);
                    startActivity(new Intent(this, MainActivity.class));
                    finishAffinity();
                } else {
                    Toast.makeText(this,
                            "Invalid username or password",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void doRegister() {
        String username = etUsername.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm  = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "Please enter a username",     Toast.LENGTH_SHORT).show(); return;
        }
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email",     Toast.LENGTH_SHORT).show(); return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter a password",     Toast.LENGTH_SHORT).show(); return;
        }
        if (TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, "Please confirm your password",Toast.LENGTH_SHORT).show(); return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match",      Toast.LENGTH_SHORT).show(); return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT).show(); return;
        }

        AppDatabase.DB_EXECUTOR.execute(() -> {
            User existing = db.userDao().findByUsername(username);
            if (existing != null) {
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Username already taken",
                                Toast.LENGTH_SHORT).show());
                return;
            }

            // Insert User row
            User user               = new User();
            user.username           = username;
            user.email              = email;
            user.passwordHash       = HashUtils.sha256(password);
            user.preferredTransport = "TROTRO";
            user.createdAt          = System.currentTimeMillis();
            user.lastLogin          = System.currentTimeMillis();

            long newId = db.userDao().insert(user);

            // FIX: Also create a UserProfile row immediately on registration
            // so ProfileActivity always finds data and the form is never blank
            UserProfile profile = new UserProfile();
            profile.userId      = (int) newId;
            profile.name        = username;
            profile.email       = email;
            profile.phone       = "";
            profile.avatarPath  = null;
            db.userProfileDao().upsert(profile);

            runOnUiThread(() -> {
                // FIX: now matches saveSession(int, String, String)
                session.saveSession((int) newId, username, email);
                startActivity(new Intent(this, MainActivity.class));
                finishAffinity();
            });
        });
    }
}