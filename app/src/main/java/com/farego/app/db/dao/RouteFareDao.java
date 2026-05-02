package com.farego.app.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.farego.app.db.entity.RouteFare;
import java.util.List;

@Dao
public interface RouteFareDao {

    /**
     * Bidirectional lookup for named terminus → named terminus routes.
     * Both origin and destination must match a seeded row (in either order).
     *
     * LIKE with '%…%' absorbs minor geocoder label variations
     * (e.g. "Teshie-Nungua" vs "Teshie").
     *
     * Returns the first match; LIMIT 1 ensures a single result.
     */
    @Query("SELECT * FROM route_fares " +
            "WHERE (origin LIKE '%' || :origin || '%' AND destination LIKE '%' || :destination || '%') " +
            "   OR (origin LIKE '%' || :destination || '%' AND destination LIKE '%' || :origin || '%') " +
            "LIMIT 1")
    RouteFare findByOriginAndDestination(String origin, String destination);

    /**
     * Destination-only lookup — used when the user's origin is a live GPS
     * position ("My Location") rather than a named terminus.
     *
     * Matches any seeded route whose destination (or origin, for return trips)
     * contains the destination label supplied by the geocoder / Places API.
     *
     * Returns the closest match; if multiple rows match the destination string
     * the one seeded first (lowest id) is returned.
     */
    @Query("SELECT * FROM route_fares " +
            "WHERE destination LIKE '%' || :destination || '%' " +
            "   OR origin      LIKE '%' || :destination || '%' " +
            "ORDER BY id ASC LIMIT 1")
    RouteFare findByDestination(String destination);

    @Query("SELECT * FROM route_fares ORDER BY origin ASC")
    List<RouteFare> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RouteFare> fares);

    @Query("DELETE FROM route_fares")
    void clearAll();

    @Query("SELECT MAX(last_updated) FROM route_fares")
    long getLatestTimestamp();
}