package com.farego.app.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName   = "user_profiles",
        indices     = { @Index("user_id") },
        foreignKeys = @ForeignKey(
                entity        = User.class,
                parentColumns = "id",
                childColumns  = "user_id",
                onDelete      = ForeignKey.CASCADE
        )
)
public class UserProfile {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "user_id")
    public int userId;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "email")
    public String email;

    @ColumnInfo(name = "phone")
    public String phone;

    @ColumnInfo(name = "avatar_path")
    public String avatarPath;

    // ── NEW: saved location strings ───────────────────────────────────────────
    @ColumnInfo(name = "home_location")
    public String homeLocation;

    @ColumnInfo(name = "work_location")
    public String workLocation;
    // ─────────────────────────────────────────────────────────────────────────

    // Required by Room
    public UserProfile() {}
}