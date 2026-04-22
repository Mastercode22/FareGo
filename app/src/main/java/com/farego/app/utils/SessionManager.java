package com.farego.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME       = "farego_session";
    private static final String KEY_USER_ID     = "user_id";
    private static final String KEY_USERNAME    = "username";
    private static final String KEY_EMAIL       = "email";
    private static final String KEY_IS_ADMIN    = "is_admin";
    private static final String KEY_LOGGED_IN   = "is_logged_in";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Called from AuthActivity after login or register
    public void saveSession(int userId, String username, String email) {
        prefs.edit()
                .putInt(KEY_USER_ID,   userId)
                .putString(KEY_USERNAME, username)
                .putString(KEY_EMAIL,    email)
                .putBoolean(KEY_LOGGED_IN, true)
                .apply();
    }

    // Called if you need to also store admin flag (optional)
    public void saveSession(int userId, String username, String email, boolean isAdmin) {
        prefs.edit()
                .putInt(KEY_USER_ID,      userId)
                .putString(KEY_USERNAME,  username)
                .putString(KEY_EMAIL,     email)
                .putBoolean(KEY_IS_ADMIN, isAdmin)
                .putBoolean(KEY_LOGGED_IN, true)
                .apply();
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1); // -1 = not logged in
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public boolean isAdmin() {
        return prefs.getBoolean(KEY_IS_ADMIN, false);
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false) && getUserId() != -1;
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}