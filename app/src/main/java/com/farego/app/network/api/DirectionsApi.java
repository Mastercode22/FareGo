package com.farego.app.network.api;

import com.farego.app.network.model.DirectionsResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DirectionsApi {

    @GET("maps/api/directions/json")
    Call<DirectionsResponse> getRoute(
            @Query("origin")       String origin,
            @Query("destination")  String destination,
            @Query("mode")         String mode,
            @Query("departure_time") String departureTime,
            @Query("traffic_model") String trafficModel,
            @Query("key")          String apiKey
    );
}
