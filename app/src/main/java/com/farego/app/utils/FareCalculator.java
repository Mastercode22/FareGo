package com.farego.app.utils;

import com.farego.app.db.entity.FareRate;
import com.farego.app.model.FareResult;

import java.util.Calendar;

/**
 * FareCalculator — Core fare estimation engine for FareGo.
 *
 * Calculates fares for TroTro, Taxi, and Uber based on:
 *   • Distance in kilometres
 *   • Time-of-day multiplier (peak hours)
 *   • Traffic condition multiplier
 *   • Transport-type base & per-km rates
 *
 * All fares are in Ghanaian Cedis (GH₵).
 */
public class FareCalculator {

    public enum TransportType {
        TROTRO("TroTro", "🚌"),
        TAXI("Taxi", "🚕"),
        UBER("Uber", "🚗");

        public final String label;
        public final String emoji;

        TransportType(String label, String emoji) {
            this.label = label;
            this.emoji = emoji;
        }
    }

    public enum TrafficCondition {
        LOW(1.0, "Low Traffic", 0xFF4CAF50),
        MODERATE(1.2, "Moderate Traffic", 0xFFFF9800),
        HEAVY(1.5, "Heavy Traffic", 0xFFF44336);

        public final double multiplier;
        public final String label;
        public final int color;

        TrafficCondition(double multiplier, String label, int color) {
            this.multiplier = multiplier;
            this.label = label;
            this.color = color;
        }
    }

    /**
     * Calculate fare with full details.
     *
     * @param distanceKm    route distance in kilometres
     * @param rate          FareRate from local DB
     * @param traffic       current traffic condition
     * @return FareResult with min, max, and display values
     */
    public static FareResult calculate(double distanceKm,
                                       FareRate rate,
                                       TrafficCondition traffic) {
        double timeMultiplier = getPeakMultiplier(rate.peakMultiplier);
        double trafficMult    = traffic.multiplier * rate.trafficMultiplier;

        double baseFare  = rate.baseRate + (distanceKm * rate.perKmRate);
        double rawFare   = baseFare * timeMultiplier * trafficMult;
        double finalFare = Math.max(rawFare, rate.minimumFare);

        // Min/max range: ±10%
        double minFare = Math.max(finalFare * 0.90, rate.minimumFare);
        double maxFare = finalFare * 1.10;

        return new FareResult(
                rate.transportType,
                minFare,
                maxFare,
                finalFare,
                distanceKm,
                traffic,
                timeMultiplier > 1.0
        );
    }

    /**
     * Quick offline estimation without DB rates.
     * Uses hard-coded Ghana transport averages.
     */
    public static FareResult estimateOffline(double distanceKm,
                                              TransportType type,
                                              TrafficCondition traffic) {
        double baseRate, perKmRate, minFare, peakMult;

        switch (type) {
            case TROTRO:
                baseRate  = 2.0;
                perKmRate = 0.80;
                minFare   = 2.0;
                peakMult  = 1.10;
                break;
            case TAXI:
                baseRate  = 8.0;
                perKmRate = 2.50;
                minFare   = 10.0;
                peakMult  = 1.25;
                break;
            case UBER:
            default:
                baseRate  = 12.0;
                perKmRate = 3.80;
                minFare   = 15.0;
                peakMult  = 1.50;
                break;
        }

        double timeMultiplier  = getPeakMultiplier(peakMult);
        double trafficMult     = traffic.multiplier;
        double rawFare         = (baseRate + distanceKm * perKmRate) * timeMultiplier * trafficMult;
        double finalFare       = Math.max(rawFare, minFare);

        FareRate synthetic = new FareRate();
        synthetic.transportType    = type.name();
        synthetic.baseRate         = baseRate;
        synthetic.perKmRate        = perKmRate;
        synthetic.minimumFare      = minFare;
        synthetic.peakMultiplier   = peakMult;
        synthetic.trafficMultiplier = 1.0;

        return calculate(distanceKm, synthetic, traffic);
    }

    /**
     * Derive peak-hour multiplier from current system time.
     * Ghana peak hours: 06:30–09:00 and 16:00–19:30.
     */
    private static double getPeakMultiplier(double configuredMultiplier) {
        Calendar c   = Calendar.getInstance();
        int hour     = c.get(Calendar.HOUR_OF_DAY);
        int minute   = c.get(Calendar.MINUTE);
        int totalMin = hour * 60 + minute;

        boolean morningPeak = totalMin >= 390 && totalMin <= 540;   // 06:30–09:00
        boolean eveningPeak = totalMin >= 960 && totalMin <= 1170;  // 16:00–19:30

        return (morningPeak || eveningPeak) ? configuredMultiplier : 1.0;
    }

    /** Format fare for display, e.g. "GH₵ 12.50" */
    public static String format(double fare) {
        return String.format("GH₵ %.2f", fare);
    }

    /** Format fare range, e.g. "GH₵ 11.00 – GH₵ 14.00" */
    public static String formatRange(double min, double max) {
        return String.format("GH₵ %.0f – GH₵ %.0f", min, max);
    }
}
