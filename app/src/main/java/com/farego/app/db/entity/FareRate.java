package com.farego.app.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "fare_rates")
public class FareRate {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "transport_type")
    public String transportType;

    @ColumnInfo(name = "base_rate")
    public double baseRate;

    @ColumnInfo(name = "per_km_rate")
    public double perKmRate;

    @ColumnInfo(name = "minimum_fare")
    public double minimumFare;

    @ColumnInfo(name = "peak_multiplier")
    public double peakMultiplier;

    @ColumnInfo(name = "traffic_multiplier")
    public double trafficMultiplier;

    @ColumnInfo(name = "last_updated")
    public long lastUpdated;

    // Required by Room
    public FareRate() {}

    // Used in AppDatabase seed
    @Ignore
    public FareRate(String transportType, double baseRate,
                    double perKmRate, double minimumFare, long lastUpdated) {
        this.transportType     = transportType;
        this.baseRate          = baseRate;
        this.perKmRate         = perKmRate;
        this.minimumFare       = minimumFare;
        this.peakMultiplier    = 1.2;  // default 20% peak surcharge
        this.trafficMultiplier = 1.0;  // default no traffic surcharge
        this.lastUpdated       = lastUpdated;
    }
}