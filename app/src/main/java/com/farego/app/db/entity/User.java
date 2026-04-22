package com.farego.app.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "users",
        indices   = { @Index(value = "username", unique = true) }
)
public class User {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "username")
    public String username;

    @ColumnInfo(name = "password_hash")
    public String passwordHash;

    @ColumnInfo(name = "email")
    public String email;

    @ColumnInfo(name = "is_admin")
    public boolean isAdmin;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "preferred_transport")
    public String preferredTransport;

    @ColumnInfo(name = "last_login")
    public long lastLogin;

    // Required by Room
    public User() {}

    // Used in AppDatabase seed
    @Ignore
    public User(String username, String passwordHash,
                String email, boolean isAdmin, long createdAt) {
        this.username     = username;
        this.passwordHash = passwordHash;
        this.email        = email;
        this.isAdmin      = isAdmin;
        this.createdAt    = createdAt;
    }
}