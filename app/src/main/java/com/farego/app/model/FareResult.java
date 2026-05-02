package com.farego.app.model;

import com.farego.app.utils.FareCalculator;

/**
 * Encapsulates a single fare calculation result.
 *
 * The {@code fareType} field drives how the UI labels the fare:
 *
 *   FIXED_STANDARD   → "Standard Fare"      (TroTro — exact GPRTU route match)
 *   DISTANCE_AVERAGE → "Approx. Fare"        (TroTro — no route in DB)
 *   OFFLINE_ESTIMATE → "Offline Estimate"    (any type — no network)
 *   DYNAMIC_ESTIMATE → "Estimated Fare Range" (Taxi / Uber)
 */
public class FareResult {

    public final String transportType;
    public final double minFare;
    public final double maxFare;
    public final double estimatedFare;
    public final double distanceKm;
    public final FareCalculator.TrafficCondition trafficCondition;
    public final boolean isPeakHour;
    public final FareCalculator.FareType fareType;

    public FareResult(String transportType,
                      double minFare,
                      double maxFare,
                      double estimatedFare,
                      double distanceKm,
                      FareCalculator.TrafficCondition trafficCondition,
                      boolean isPeakHour,
                      FareCalculator.FareType fareType) {
        this.transportType    = transportType;
        this.minFare          = minFare;
        this.maxFare          = maxFare;
        this.estimatedFare    = estimatedFare;
        this.distanceKm       = distanceKm;
        this.trafficCondition = trafficCondition;
        this.isPeakHour       = isPeakHour;
        this.fareType         = fareType;
    }

    // ── Display helpers ───────────────────────────────────────────────────────

    /**
     * Returns the fare string for display.
     *
     *   Fixed fares (TroTro standard)  →  single value,  e.g. "GH₵ 9.00"
     *   Dynamic fares (Taxi / Uber)    →  range,          e.g. "GH₵ 14 – GH₵ 17"
     *   Estimates / offline            →  single value with ~ prefix, e.g. "~GH₵ 8.40"
     */
    public String getFormattedFare() {
        switch (fareType) {
            case FIXED_STANDARD:
                return FareCalculator.format(estimatedFare);

            case DYNAMIC_ESTIMATE:
                return FareCalculator.formatRange(minFare, maxFare);

            case DISTANCE_AVERAGE:
            case OFFLINE_ESTIMATE:
            default:
                return "~" + FareCalculator.format(estimatedFare);
        }
    }

    /**
     * Short label describing how the fare was derived.
     * Shown beneath the fare figure in the trip card and summary dialog.
     */
    public String getFareLabel() {
        switch (fareType) {
            case FIXED_STANDARD:   return "Standard Fare";
            case DYNAMIC_ESTIMATE: return "Estimated Fare Range";
            case DISTANCE_AVERAGE: return "Approx. Fare";
            case OFFLINE_ESTIMATE: return "Offline Estimate";
            default:               return "";
        }
    }

    // ── Legacy helpers (kept for compatibility with existing call-sites) ───────

    /** @deprecated Prefer {@link #getFormattedFare()} */
    @Deprecated
    public String getFormattedRange() {
        return getFormattedFare();
    }

    /** @deprecated Prefer {@link #getFormattedFare()} */
    @Deprecated
    public String getFormattedEstimate() {
        return FareCalculator.format(estimatedFare);
    }
}