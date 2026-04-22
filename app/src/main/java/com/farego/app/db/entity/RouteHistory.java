package com.farego.app.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName   = "route_history",
        indices     = { @Index("user_id") },
        foreignKeys = @ForeignKey(
                entity        = User.class,
                parentColumns = "id",
                childColumns  = "user_id",
                onDelete      = ForeignKey.CASCADE
        )
)
public class RouteHistory {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "user_id")
    public int userId;

    @ColumnInfo(name = "origin_label")
    public String originLabel;

    @ColumnInfo(name = "origin_lat")
    public double originLat;

    @ColumnInfo(name = "origin_lng")
    public double originLng;

    @ColumnInfo(name = "destination_label")
    public String destinationLabel;

    @ColumnInfo(name = "destination_lat")
    public double destinationLat;

    @ColumnInfo(name = "destination_lng")
    public double destinationLng;

    @ColumnInfo(name = "distance_km")
    public double distanceKm;

    @ColumnInfo(name = "duration_minutes")
    public int durationMinutes;

    @ColumnInfo(name = "estimated_fare")
    public double estimatedFare;

    @ColumnInfo(name = "transport_type")
    public String transportType;

    @ColumnInfo(name = "traffic_condition")
    public String trafficCondition;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @ColumnInfo(name = "is_favourite", defaultValue = "0")
    public boolean isFavourite;

    // Required by Room
    public RouteHistory() {}
}