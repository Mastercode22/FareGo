package com.farego.app.utils;

import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.List;

/** Decodes a Google encoded polyline string into a list of LatLng. */
public class PolylineDecoder {

    public static List<LatLng> decode(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) return poly;

        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0; result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            poly.add(new LatLng(lat / 1E5, lng / 1E5));
        }
        return poly;
    }
}
