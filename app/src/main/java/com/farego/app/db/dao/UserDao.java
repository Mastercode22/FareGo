package com.farego.app.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.farego.app.db.entity.User;

@Dao
public interface UserDao {

    // Used in AppDatabase seed
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertUser(User user);

    // Used in AuthActivity doRegister()
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(User user);

    @Update
    void updateUser(User user);

    // Used in AuthActivity doLogin()
    @Query("SELECT * FROM users WHERE username = :username AND password_hash = :hash LIMIT 1")
    User login(String username, String hash);

    // Used in AuthActivity doLogin() after successful login
    @Query("UPDATE users SET last_login = :time WHERE id = :id")
    void updateLastLogin(int id, long time);

    // Used in AuthActivity doRegister() to check duplicate username
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User findByUsername(String username);

    // Used in AppDatabase seed and splash check
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    User findById(int userId);

    // Used at login with hash
    @Query("SELECT * FROM users WHERE username = :username AND password_hash = :hash LIMIT 1")
    User findByCredentials(String username, String hash);

    @Query("DELETE FROM users WHERE id = :userId")
    void deleteById(int userId);
}