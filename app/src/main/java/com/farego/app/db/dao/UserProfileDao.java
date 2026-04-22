package com.farego.app.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.farego.app.db.entity.UserProfile;

@Dao
public interface UserProfileDao {

    @Query("SELECT * FROM user_profiles WHERE user_id = :userId LIMIT 1")
    UserProfile getByUserId(int userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserProfile profile);

    @Query("DELETE FROM user_profiles WHERE user_id = :userId")
    void deleteByUserId(int userId);
}