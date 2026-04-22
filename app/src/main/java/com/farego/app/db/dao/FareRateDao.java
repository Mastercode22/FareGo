package com.farego.app.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.farego.app.db.entity.FareRate;
import java.util.List;

@Dao
public interface FareRateDao {

    @Query("SELECT * FROM fare_rates ORDER BY id ASC")
    LiveData<List<FareRate>> getAllRates();

    /**
     * Called as getByType() in MainActivity — kept both names via
     * a single method with the correct name used app-wide.
     */
    @Query("SELECT * FROM fare_rates WHERE transport_type = :type LIMIT 1")
    FareRate getByType(String type);

    @Query("SELECT MAX(last_updated) FROM fare_rates")
    long getLatestTimestamp();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FareRate> rates);

    @Query("DELETE FROM fare_rates")
    void clearAll();
}