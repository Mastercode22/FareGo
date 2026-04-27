package com.farego.app.model;

import com.google.android.gms.maps.model.LatLng;
import com.farego.app.network.model.DirectionsResponse;
import com.farego.app.utils.FareCalculator;

import java.util.List;

/**
 * ════════════════════════════════════════════════════════════════════════════
 *  RouteInfo.java
 *  Holds all decoded route data from the Directions API response,
 *  including the full list of turn-by-turn navigation steps.
 *
 *  Changes from v1:
 *    • Added `steps` — the list of DirectionsResponse.Step objects
 *    • Updated live navigation state fields (instruction, distanceRemaining, eta)
 * ════════════════════════════════════════════════════════════════════════════
 */
public class RouteInfo {

    // ── Origin / Destination ──────────────────────────────────────────────────

    /** Human-readable label for the starting point (e.g. "My Location"). */
    public String originLabel;

    /** Human-readable label for the destination (e.g. "Accra Mall"). */
    public String destinationLabel;

    /** Coordinates of the starting point. */
    public LatLng originLatLng;

    /** Coordinates of the destination. */
    public LatLng destinationLatLng;

    // ── Route summary ─────────────────────────────────────────────────────────

    /** Total route distance in kilometres (e.g. 12.5). */
    public double distanceKm;

    /** Estimated total travel time including traffic (minutes). */
    public int durationMinutes;

    /** Formatted distance string from the API (e.g. "12.3 km"). */
    public String distanceText;

    /** Formatted duration string from the API (e.g. "25 mins"). */
    public String durationText;

    /** Inferred traffic condition based on duration_in_traffic ratio. */
    public FareCalculator.TrafficCondition trafficCondition;

    /** Decoded lat/lng points for drawing the route polyline on the map. */
    public List<LatLng> polylinePoints;

    // ── Turn-by-turn steps (NEW) ──────────────────────────────────────────────

    /**
     * Ordered list of navigation steps from the API leg.
     *
     * Each step contains:
     *   - htmlInstructions  → instruction text (strip HTML before TTS)
     *   - endLocation       → lat/lng where this manoeuvre occurs
     *   - distance.value    → step length in metres
     *   - maneuver          → machine-readable turn type (may be null)
     *
     * Populated in MainActivity.processRoute() and consumed by NavigationEngine.
     */
    public List<DirectionsResponse.Step> steps;

    // ── Live navigation state ─────────────────────────────────────────────────

    /**
     * The instruction text currently shown on-screen (plain text, no HTML).
     * Updated by NavigationEngine on every GPS tick.
     */
    public String currentInstruction;

    /** Metres remaining to the end of the *current* step. Updated each GPS tick. */
    public double distanceRemainingKm;

    /** Estimated minutes to final destination (recalculated each GPS tick). */
    public int etaMinutes;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Empty constructor — fields populated directly. */
    public RouteInfo() {}

    /**
     * Convenience constructor to set origin/destination immediately.
     *
     * @param origin      Start coordinates
     * @param originLabel "My Location" or an address string
     * @param dest        Destination coordinates
     * @param destLabel   Destination name / address
     */
    public RouteInfo(LatLng origin, String originLabel,
                     LatLng dest,   String destLabel) {
        this.originLatLng      = origin;
        this.originLabel       = originLabel;
        this.destinationLatLng = dest;
        this.destinationLabel  = destLabel;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true if this route has valid step data.
     * Used by NavigationEngine to decide whether to run turn-by-turn guidance.
     */
    public boolean hasSteps() {
        return steps != null && !steps.isEmpty();
    }

    /**
     * Total number of navigation steps in this route.
     * Returns 0 if steps have not been populated.
     */
    public int stepCount() {
        return steps != null ? steps.size() : 0;
    }
}