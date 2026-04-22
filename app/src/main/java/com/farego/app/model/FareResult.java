package com.farego.app.model;

import com.farego.app.utils.FareCalculator;

/** Encapsulates a single fare calculation result. */
public class  FareResult {

    public final String transportType;
    public final double minFare;
    public final double maxFare;
    public final double estimatedFare;
    public final double distanceKm;
    public final FareCalculator.TrafficCondition trafficCondition;
    public final boolean isPeakHour;

    public FareResult(String transportType,
                      double minFare,
                      double maxFare,
                      double estimatedFare,
                      double distanceKm,
                      FareCalculator.TrafficCondition trafficCondition,
                      boolean isPeakHour) {
        this.transportType    = transportType;
        this.minFare          = minFare;
        this.maxFare          = maxFare;
        this.estimatedFare    = estimatedFare;
        this.distanceKm       = distanceKm;
        this.trafficCondition = trafficCondition;
        this.isPeakHour       = isPeakHour;
    }

    public String getFormattedRange() {
        return FareCalculator.formatRange(minFare, maxFare);
    }

    public String getFormattedEstimate() {
        return FareCalculator.format(estimatedFare);
    }
}
