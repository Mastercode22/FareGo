package com.farego.app.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.farego.app.db.entity.RouteHistory;
import java.util.List;

@Dao
public interface RouteHistoryDao {

    @Query("SELECT * FROM route_history WHERE user_id = :userId ORDER BY timestamp DESC")
    LiveData<List<RouteHistory>> getHistoryForUser(int userId);

    @Query("SELECT * FROM route_history WHERE user_id = :userId ORDER BY timestamp DESC")
    LiveData<List<RouteHistory>> getByUser(int userId);

    @Query("SELECT * FROM route_history WHERE id = :id LIMIT 1")
    RouteHistory getById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RouteHistory history);

    @Query("UPDATE route_history SET is_favourite = :fav WHERE id = :id")
    void setFavourite(int id, boolean fav);

    @Query("DELETE FROM route_history WHERE user_id = :userId")
    void clearHistory(int userId);

    @Query("DELETE FROM route_history WHERE user_id = :userId")
    void clearForUser(int userId);

    @Query("SELECT * FROM route_history WHERE user_id = :userId AND is_favourite = 1 ORDER BY timestamp DESC")
    LiveData<List<RouteHistory>> getFavourites(int userId);

    // ── Stats for ProfileActivity ─────────────────────────────────────────────

    /** Total trip count */
    @Query("SELECT COUNT(*) FROM route_history WHERE user_id = :userId")
    int countForUser(int userId);

    /** Sum of all distances */
    @Query("SELECT COALESCE(SUM(distance_km), 0) FROM route_history WHERE user_id = :userId")
    double totalKmForUser(int userId);

    /** Most-used transport type (empty string if no trips) */
    @Query("SELECT COALESCE(transport_type, '') FROM route_history " +
            "WHERE user_id = :userId " +
            "GROUP BY transport_type " +
            "ORDER BY COUNT(*) DESC " +
            "LIMIT 1")
    String favTransportForUser(int userId);
}