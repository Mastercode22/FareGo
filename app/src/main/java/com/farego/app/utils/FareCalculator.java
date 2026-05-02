package com.farego.app.utils;

import com.farego.app.db.entity.FareRate;
import com.farego.app.db.entity.RouteFare;
import com.farego.app.model.FareResult;

import java.util.Calendar;

/**
 * FareCalculator — Core fare estimation engine for RouteWise (Ghana).
 *
 * Fare logic by transport type:
 *
 *   TroTro  → Fixed GPRTU fare from the RouteFare table.
 *             Falls back to a flat distance average (no traffic/peak multipliers)
 *             if the exact route is not in the database, or to a per-km offline
 *             estimate when there is no network.
 *
 *   Taxi / Uber → Dynamic calculation:
 *             Base rate + (distance × per-km rate) × traffic multiplier × peak multiplier.
 *
 * All fares are in Ghanaian Cedis (GH₵).
 */
public class FareCalculator {

    // ── Transport types ───────────────────────────────────────────────────────

    public enum TransportType {
        TROTRO("TroTro", "🚌"),
        TAXI  ("Taxi",   "🚕"),
        UBER  ("Uber",   "🚗");

        public final String label;
        public final String emoji;

        TransportType(String label, String emoji) {
            this.label = label;
            this.emoji = emoji;
        }
    }

    // ── Traffic conditions ────────────────────────────────────────────────────

    public enum TrafficCondition {
        LOW     (1.0, "Low Traffic",      0xFF4CAF50),
        MODERATE(1.2, "Moderate Traffic", 0xFFFF9800),
        HEAVY   (1.5, "Heavy Traffic",    0xFFF44336);

        public final double multiplier;
        public final String label;
        public final int    color;

        TrafficCondition(double multiplier, String label, int color) {
            this.multiplier = multiplier;
            this.label      = label;
            this.color      = color;
        }
    }

    // ── Fare-type tag embedded in FareResult ──────────────────────────────────

    /**
     * Distinguishes how a fare was derived, so the UI can label it correctly.
     *
     *   FIXED_STANDARD   → TroTro route found in the RouteFare table.
     *   DISTANCE_AVERAGE → TroTro route NOT in table; distance-based average used.
     *   OFFLINE_ESTIMATE → No network; hard-coded per-km average used as last resort.
     *   DYNAMIC_ESTIMATE → Taxi or Uber; full Base + Distance + multipliers model.
     */
    public enum FareType {
        FIXED_STANDARD,
        DISTANCE_AVERAGE,
        OFFLINE_ESTIMATE,
        DYNAMIC_ESTIMATE
    }

    // =========================================================================
    //  PUBLIC API
    // =========================================================================

    /**
     * Primary entry point.  Called with a live DB rate and, optionally, a
     * matching RouteFare row from the RouteFare table.
     *
     * @param distanceKm  route distance in kilometres
     * @param rate        FareRate row for the chosen transport type
     * @param routeFare   matching RouteFare row, or {@code null} if not found
     * @param traffic     current traffic condition (ignored for TroTro)
     * @return FareResult ready for display
     */
    public static FareResult calculate(double distanceKm,
                                       FareRate rate,
                                       RouteFare routeFare,
                                       TrafficCondition traffic) {

        boolean isTroTro = TransportType.TROTRO.label.equalsIgnoreCase(rate.transportType);

        if (isTroTro) {
            return calculateTroTro(distanceKm, rate, routeFare);
        } else {
            return calculateDynamic(distanceKm, rate, traffic);
        }
    }

    /**
     * Offline fallback — called when the DB is unreachable.
     * Uses hard-coded Ghana averages.  Always labelled OFFLINE_ESTIMATE.
     */
    public static FareResult estimateOffline(double distanceKm,
                                             TransportType type,
                                             TrafficCondition traffic) {

        if (type == TransportType.TROTRO) {
            // TroTro offline: flat GH₵ 1.20 per km, no multipliers.
            double fare = Math.max(distanceKm * 1.20, 2.00);
            return new FareResult(
                    type.label,
                    fare, fare, fare,
                    distanceKm,
                    TrafficCondition.LOW,   // traffic irrelevant for TroTro
                    false,
                    FareType.OFFLINE_ESTIMATE
            );
        }

        // Taxi / Uber offline: dynamic model with hard-coded rates.
        double baseRate, perKmRate, minFare, peakMult;
        switch (type) {
            case TAXI:
                baseRate  =  8.0;
                perKmRate =  2.50;
                minFare   = 10.0;
                peakMult  =  1.25;
                break;
            case UBER:
            default:
                baseRate  = 12.0;
                perKmRate =  3.80;
                minFare   = 15.0;
                peakMult  =  1.50;
                break;
        }

        double timeMultiplier = getPeakMultiplier(peakMult);
        double rawFare        = (baseRate + distanceKm * perKmRate)
                * timeMultiplier
                * traffic.multiplier;
        double finalFare      = Math.max(rawFare, minFare);

        return new FareResult(
                type.label,
                Math.max(finalFare * 0.90, minFare),
                finalFare * 1.10,
                finalFare,
                distanceKm,
                traffic,
                timeMultiplier > 1.0,
                FareType.OFFLINE_ESTIMATE
        );
    }

    // =========================================================================
    //  FORMATTING HELPERS
    // =========================================================================

    /** e.g. "GH₵ 12.50" */
    public static String format(double fare) {
        return String.format("GH₵ %.2f", fare);
    }

    /** e.g. "GH₵ 11 – GH₵ 14" */
    public static String formatRange(double min, double max) {
        return String.format("GH₵ %.0f – GH₵ %.0f", min, max);
    }

    // =========================================================================
    //  PRIVATE — TroTro logic
    // =========================================================================

    /**
     * TroTro fare resolution in priority order:
     *
     *  1. Exact route match in RouteFare table → FIXED_STANDARD
     *  2. No table match → distance-based flat average → DISTANCE_AVERAGE
     *
     * Traffic and peak-hour multipliers are NEVER applied to TroTro.
     */
    private static FareResult calculateTroTro(double distanceKm,
                                              FareRate rate,
                                              RouteFare routeFare) {

        if (routeFare != null) {
            // ── Fixed GPRTU fare ──────────────────────────────────────────────
            double fixed = routeFare.standardFare;
            return new FareResult(
                    TransportType.TROTRO.label,
                    fixed, fixed, fixed,
                    distanceKm,
                    TrafficCondition.LOW,   // traffic does not affect this fare
                    false,
                    FareType.FIXED_STANDARD
            );
        }

        // ── Distance-based average fallback (no multipliers) ─────────────────
        // Uses the DB per-km rate; ignores baseRate so short trips aren't inflated.
        double fare = Math.max(distanceKm * rate.perKmRate, rate.minimumFare);
        return new FareResult(
                TransportType.TROTRO.label,
                fare, fare, fare,
                distanceKm,
                TrafficCondition.LOW,
                false,
                FareType.DISTANCE_AVERAGE
        );
    }

    // =========================================================================
    //  PRIVATE — Taxi / Uber dynamic logic
    // =========================================================================

    /**
     * Full dynamic calculation:
     *   fare = (baseRate + distance × perKmRate) × peakMultiplier × trafficMultiplier
     *
     * A ±10 % range is shown to reflect driver negotiation / surge variability.
     */
    private static FareResult calculateDynamic(double distanceKm,
                                               FareRate rate,
                                               TrafficCondition traffic) {

        double timeMultiplier = getPeakMultiplier(rate.peakMultiplier);
        double trafficMult    = traffic.multiplier * rate.trafficMultiplier;

        double baseFare  = rate.baseRate + (distanceKm * rate.perKmRate);
        double rawFare   = baseFare * timeMultiplier * trafficMult;
        double finalFare = Math.max(rawFare, rate.minimumFare);

        double minFare = Math.max(finalFare * 0.90, rate.minimumFare);
        double maxFare = finalFare * 1.10;

        return new FareResult(
                rate.transportType,
                minFare,
                maxFare,
                finalFare,
                distanceKm,
                traffic,
                timeMultiplier > 1.0,
                FareType.DYNAMIC_ESTIMATE
        );
    }

    // =========================================================================
    //  PRIVATE — Peak-hour detection
    // =========================================================================

    /**
     * Returns the configured multiplier during Ghana peak hours, otherwise 1.0.
     * Peak windows: 06:30–09:00 and 16:00–19:30.
     */
    private static double getPeakMultiplier(double configuredMultiplier) {
        Calendar c       = Calendar.getInstance();
        int totalMin     = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        boolean morning  = totalMin >= 390 && totalMin <= 540;   // 06:30–09:00
        boolean evening  = totalMin >= 960 && totalMin <= 1170;  // 16:00–19:30
        return (morning || evening) ? configuredMultiplier : 1.0;
    }
}