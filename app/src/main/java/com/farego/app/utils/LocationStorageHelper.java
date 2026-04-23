package com.farego.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

/**
 * Lightweight helper that stores saved Home / Work locations in SharedPreferences
 * (lat, lng, address) so they survive offline and process restarts.
 *
 * The address string is ALSO written into UserProfile.homeLocation / workLocation
 * in the Room database — that keeps the Profile UI in sync without extra queries.
 */
public class LocationStorageHelper {

    private static final String PREFS_NAME = "farego_saved_locations";

    // Key prefixes — appended with _lat / _lng / _address
    private static final String HOME_PREFIX = "home";
    private static final String WORK_PREFIX = "work";

    // ── Public data class ─────────────────────────────────────────────────────

    public static class SavedLocation {
        public final double lat;
        public final double lng;
        public final String address;

        public SavedLocation(double lat, double lng, String address) {
            this.lat     = lat;
            this.lng     = lng;
            this.address = address;
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    /**
     * Saves a location to SharedPreferences.
     * @param type "HOME" or "WORK"
     */
    public static void saveLocation(Context context, String type,
                                    double lat, double lng, String address) {
        String prefix = prefix(type);
        prefs(context).edit()
                .putFloat(prefix + "_lat",     (float) lat)
                .putFloat(prefix + "_lng",     (float) lng)
                .putString(prefix + "_address", address)
                .apply();
    }

    // ── Retrieve ──────────────────────────────────────────────────────────────

    /**
     * Returns the saved location, or null if none exists.
     * @param type "HOME" or "WORK"
     */
    @Nullable
    public static SavedLocation getLocation(Context context, String type) {
        String prefix = prefix(type);
        SharedPreferences sp = prefs(context);

        String address = sp.getString(prefix + "_address", null);
        if (address == null) return null;   // never saved

        double lat = sp.getFloat(prefix + "_lat", 0f);
        double lng = sp.getFloat(prefix + "_lng", 0f);
        return new SavedLocation(lat, lng, address);
    }

    /**
     * Convenience: returns just the display address (or "Not set").
     */
    public static String getDisplayAddress(Context context, String type) {
        SavedLocation loc = getLocation(context, type);
        return (loc != null && loc.address != null) ? loc.address : "Not set";
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public static void clearLocation(Context context, String type) {
        String prefix = prefix(type);
        prefs(context).edit()
                .remove(prefix + "_lat")
                .remove(prefix + "_lng")
                .remove(prefix + "_address")
                .apply();
    }

    public static void clearAll(Context context) {
        prefs(context).edit().clear().apply();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String prefix(String type) {
        return "WORK".equalsIgnoreCase(type) ? WORK_PREFIX : HOME_PREFIX;
    }
}