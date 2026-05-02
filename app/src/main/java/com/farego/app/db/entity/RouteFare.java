package com.farego.app.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Represents a GPRTU-approved fixed TroTro fare for a named Origin → Destination pair.
 *
 * Lookups are case-insensitive and use LIKE matching so minor label variations
 * (e.g. "Teshie" vs "Teshie Market") still resolve to the correct fare.
 *
 * The table is pre-seeded from the Accra Full Routes 2025 dataset in AppDatabase.
 */
@Entity(
        tableName = "route_fares",
        indices   = {
                @Index(value = {"origin", "destination"}, unique = true)
        }
)
public class RouteFare {

    @PrimaryKey(autoGenerate = true)
    public int id;

    /** Human-readable origin terminus, e.g. "Circle" */
    @ColumnInfo(name = "origin")
    public String origin;

    /** Human-readable destination terminus, e.g. "Madina" */
    @ColumnInfo(name = "destination")
    public String destination;

    /** Approved GPRTU fare in GH₵ */
    @ColumnInfo(name = "standard_fare")
    public double standardFare;

    /** Route distance in km (informational, not used in fare calculation) */
    @ColumnInfo(name = "distance_km")
    public double distanceKm;

    /** Short description of intermediate stops, e.g. "Nima, 37, Legon, Atomic" */
    @ColumnInfo(name = "stops_description")
    public String stopsDescription;

    /** Timestamp of last data update (epoch ms) */
    @ColumnInfo(name = "last_updated")
    public long lastUpdated;

    public RouteFare() {}

    public RouteFare(String origin, String destination,
                     double standardFare, double distanceKm,
                     String stopsDescription, long lastUpdated) {
        this.origin           = origin;
        this.destination      = destination;
        this.standardFare     = standardFare;
        this.distanceKm       = distanceKm;
        this.stopsDescription = stopsDescription;
        this.lastUpdated      = lastUpdated;
    }
}