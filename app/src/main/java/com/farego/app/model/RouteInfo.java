package com.farego.app.model;

import com.google.android.gms.maps.model.LatLng;
import com.farego.app.utils.FareCalculator;

import java.util.List;

/** Holds decoded route data from Directions API. */
public class RouteInfo {

    public String originLabel;
    public String destinationLabel;
    public LatLng originLatLng;
    public LatLng destinationLatLng;

    public double distanceKm;
    public int durationMinutes;
    public String durationText;
    public String distanceText;

    public FareCalculator.TrafficCondition trafficCondition;
    public List<LatLng> polylinePoints;

    // Current navigation state
    public String currentInstruction;
    public double distanceRemainingKm;
    public int etaMinutes;

    public RouteInfo() {}

    public RouteInfo(LatLng origin, String originLabel,
                     LatLng dest, String destLabel) {
        this.originLatLng      = origin;
        this.originLabel       = originLabel;
        this.destinationLatLng = dest;
        this.destinationLabel  = destLabel;
    }
}
