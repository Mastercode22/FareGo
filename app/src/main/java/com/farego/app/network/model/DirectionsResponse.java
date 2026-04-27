package com.farego.app.network.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * ════════════════════════════════════════════════════════════════════════════
 *  DirectionsResponse.java
 *  Retrofit/Gson model for the Google Directions API JSON response.
 *
 *  Structure:
 *    DirectionsResponse
 *      └─ routes[]
 *           ├─ overview_polyline.points  (encoded polyline for the full route)
 *           └─ legs[]
 *                ├─ distance / duration / duration_in_traffic
 *                └─ steps[]              ← NEW: turn-by-turn steps
 *                     ├─ html_instructions
 *                     ├─ distance / duration
 *                     ├─ start_location / end_location
 *                     └─ maneuver          (e.g. "turn-left", "turn-right")
 *
 *  Changes from v1:
 *    • Added Step inner class with all fields needed for navigation
 *    • Added steps list to Leg
 *    • Added maneuver field (helps map turn direction without parsing HTML)
 * ════════════════════════════════════════════════════════════════════════════
 */
public class DirectionsResponse {

    /** Top-level list of route options (we always use index 0). */
    @SerializedName("routes")
    public List<Route> routes;

    // ── Route ─────────────────────────────────────────────────────────────────

    public static class Route {

        /** Encoded polyline for the entire route overview. */
        @SerializedName("overview_polyline")
        public OverviewPolyline overviewPolyline;

        /** A route has one leg per waypoint pair. We use legs[0] for A→B. */
        @SerializedName("legs")
        public List<Leg> legs;
    }

    // ── Leg ───────────────────────────────────────────────────────────────────

    public static class Leg {

        /** Total route distance (text = "12.3 km", value = metres). */
        @SerializedName("distance")
        public TextValuePair distance;

        /** Duration ignoring live traffic. */
        @SerializedName("duration")
        public TextValuePair duration;

        /**
         * Duration including live traffic — only present when departure_time
         * and traffic_model are supplied in the request (which FareGo does).
         */
        @SerializedName("duration_in_traffic")
        public TextValuePair durationInTraffic;

        /**
         * Turn-by-turn steps for this leg.
         * Each step represents one manoeuvre (e.g. "Turn right onto Ring Road").
         */
        @SerializedName("steps")
        public List<Step> steps;
    }

    // ── Step  (NEW) ───────────────────────────────────────────────────────────

    /**
     * A single navigation step — one instruction the driver must follow.
     *
     * Key fields used by NavigationEngine:
     *   • html_instructions  – human-readable instruction, may contain HTML tags
     *   • end_location       – lat/lng of the point where this step ends
     *                          (i.e. where the next manoeuvre happens)
     *   • distance.value     – step length in metres
     *   • maneuver           – machine-readable turn type ("turn-left", "roundabout-right", …)
     */
    public static class Step {

        /**
         * Plain-English instruction with possible HTML formatting.
         * Example: "Turn <b>right</b> onto <b>Ring Rd E</b>"
         * Use Html.fromHtml() to strip tags before TTS.
         */
        @SerializedName("html_instructions")
        public String htmlInstructions;

        /** Step distance (text = "350 m", value = metres). */
        @SerializedName("distance")
        public TextValuePair distance;

        /** Estimated time to complete this step. */
        @SerializedName("duration")
        public TextValuePair duration;

        /**
         * Where this step begins — useful for snapping the camera when
         * the user is approaching the manoeuvre point.
         */
        @SerializedName("start_location")
        public LatLngLiteral startLocation;

        /**
         * Where this step ends — THIS is the point we measure distance to
         * in order to trigger voice announcements.
         */
        @SerializedName("end_location")
        public LatLngLiteral endLocation;

        /**
         * Machine-readable manoeuvre type provided by Google.
         * Examples: "turn-left", "turn-right", "roundabout-right",
         *           "keep-left", "merge", "straight", "ramp-left", "ferry"
         * May be null for straight-ahead steps.
         */
        @SerializedName("maneuver")
        public String maneuver;

        /**
         * Travel mode for this step ("DRIVING", "WALKING", "TRANSIT").
         * Useful if the route has mixed transport segments.
         */
        @SerializedName("travel_mode")
        public String travelMode;
    }

    // ── Shared primitives ─────────────────────────────────────────────────────

    /** Reusable text + numeric-value pair (used for distance and duration). */
    public static class TextValuePair {
        @SerializedName("text")  public String text;   // "12.3 km"  /  "4 mins"
        @SerializedName("value") public int    value;  // metres     /  seconds
    }

    /** Latitude / longitude pair as returned by the Directions API. */
    public static class LatLngLiteral {
        @SerializedName("lat") public double lat;
        @SerializedName("lng") public double lng;
    }

    /** Encoded overview polyline wrapper. */
    public static class OverviewPolyline {
        @SerializedName("points") public String points;
    }
}