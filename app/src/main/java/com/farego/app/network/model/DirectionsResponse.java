package com.farego.app.network.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DirectionsResponse {

    @SerializedName("status")
    public String status;

    @SerializedName("routes")
    public List<Route> routes;

    public static class Route {
        @SerializedName("legs")
        public List<Leg> legs;

        @SerializedName("overview_polyline")
        public Polyline overviewPolyline;
    }

    public static class Leg {
        @SerializedName("distance")
        public TextValue distance;

        @SerializedName("duration")
        public TextValue duration;

        @SerializedName("duration_in_traffic")
        public TextValue durationInTraffic;

        @SerializedName("start_address")
        public String startAddress;

        @SerializedName("end_address")
        public String endAddress;

        @SerializedName("steps")
        public List<Step> steps;
    }

    public static class Step {
        @SerializedName("html_instructions")
        public String htmlInstructions;

        @SerializedName("distance")
        public TextValue distance;

        @SerializedName("duration")
        public TextValue duration;

        @SerializedName("polyline")
        public Polyline polyline;

        @SerializedName("travel_mode")
        public String travelMode;
    }

    public static class TextValue {
        @SerializedName("text")
        public String text;

        @SerializedName("value")
        public int value;  // metres or seconds
    }

    public static class Polyline {
        @SerializedName("points")
        public String points;
    }
}
