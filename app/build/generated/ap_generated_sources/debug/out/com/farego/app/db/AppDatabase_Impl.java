package com.farego.app.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.farego.app.db.dao.FareRateDao;
import com.farego.app.db.dao.FareRateDao_Impl;
import com.farego.app.db.dao.RouteHistoryDao;
import com.farego.app.db.dao.RouteHistoryDao_Impl;
import com.farego.app.db.dao.UserDao;
import com.farego.app.db.dao.UserDao_Impl;
import com.farego.app.db.dao.UserProfileDao;
import com.farego.app.db.dao.UserProfileDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile FareRateDao _fareRateDao;

  private volatile UserDao _userDao;

  private volatile RouteHistoryDao _routeHistoryDao;

  private volatile UserProfileDao _userProfileDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(4) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `fare_rates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transport_type` TEXT, `base_rate` REAL NOT NULL, `per_km_rate` REAL NOT NULL, `minimum_fare` REAL NOT NULL, `peak_multiplier` REAL NOT NULL, `traffic_multiplier` REAL NOT NULL, `last_updated` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `username` TEXT, `password_hash` TEXT, `email` TEXT, `is_admin` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `preferred_transport` TEXT, `last_login` INTEGER NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `route_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL, `origin_label` TEXT, `origin_lat` REAL NOT NULL, `origin_lng` REAL NOT NULL, `destination_label` TEXT, `destination_lat` REAL NOT NULL, `destination_lng` REAL NOT NULL, `distance_km` REAL NOT NULL, `duration_minutes` INTEGER NOT NULL, `estimated_fare` REAL NOT NULL, `transport_type` TEXT, `traffic_condition` TEXT, `timestamp` INTEGER NOT NULL, `is_favourite` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_route_history_user_id` ON `route_history` (`user_id`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` INTEGER NOT NULL, `name` TEXT, `email` TEXT, `phone` TEXT, `avatar_path` TEXT, `home_location` TEXT, `work_location` TEXT, FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_profiles_user_id` ON `user_profiles` (`user_id`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'd6ba5b66179a3531a5dcc4d5fcec27cc')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `fare_rates`");
        db.execSQL("DROP TABLE IF EXISTS `users`");
        db.execSQL("DROP TABLE IF EXISTS `route_history`");
        db.execSQL("DROP TABLE IF EXISTS `user_profiles`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsFareRates = new HashMap<String, TableInfo.Column>(8);
        _columnsFareRates.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFareRates.put("transport_type", new TableInfo.Column("transport_type", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFareRates.put("base_rate", new TableInfo.Column("base_rate", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFareRates.put("per_km_rate", new TableInfo.Column("per_km_rate", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFareRates.put("minimum_fare", new TableInfo.Column("minimum_fare", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFareRates.put("peak_multiplier", new TableInfo.Column("peak_multiplier", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFareRates.put("traffic_multiplier", new TableInfo.Column("traffic_multiplier", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFareRates.put("last_updated", new TableInfo.Column("last_updated", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFareRates = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFareRates = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFareRates = new TableInfo("fare_rates", _columnsFareRates, _foreignKeysFareRates, _indicesFareRates);
        final TableInfo _existingFareRates = TableInfo.read(db, "fare_rates");
        if (!_infoFareRates.equals(_existingFareRates)) {
          return new RoomOpenHelper.ValidationResult(false, "fare_rates(com.farego.app.db.entity.FareRate).\n"
                  + " Expected:\n" + _infoFareRates + "\n"
                  + " Found:\n" + _existingFareRates);
        }
        final HashMap<String, TableInfo.Column> _columnsUsers = new HashMap<String, TableInfo.Column>(8);
        _columnsUsers.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("username", new TableInfo.Column("username", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("password_hash", new TableInfo.Column("password_hash", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("email", new TableInfo.Column("email", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("is_admin", new TableInfo.Column("is_admin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("preferred_transport", new TableInfo.Column("preferred_transport", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("last_login", new TableInfo.Column("last_login", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUsers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUsers = new HashSet<TableInfo.Index>(1);
        _indicesUsers.add(new TableInfo.Index("index_users_username", true, Arrays.asList("username"), Arrays.asList("ASC")));
        final TableInfo _infoUsers = new TableInfo("users", _columnsUsers, _foreignKeysUsers, _indicesUsers);
        final TableInfo _existingUsers = TableInfo.read(db, "users");
        if (!_infoUsers.equals(_existingUsers)) {
          return new RoomOpenHelper.ValidationResult(false, "users(com.farego.app.db.entity.User).\n"
                  + " Expected:\n" + _infoUsers + "\n"
                  + " Found:\n" + _existingUsers);
        }
        final HashMap<String, TableInfo.Column> _columnsRouteHistory = new HashMap<String, TableInfo.Column>(15);
        _columnsRouteHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteHistory.put("user_id", new TableInfo.Column("user_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteHistory.put("origin_label", new TableInfo.Column("origin_label", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteHistory.put("origin_lat", new TableInfo.Column("origin_lat", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteHistory.put("origin_lng", new TableInfo.Column("origin_lng", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteHistory.put("destination_label", new TableInfo.Column("destination_label", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteHistory.put("destination_lat", new TableInfo.Column("destination_lat", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteHistory.put("destination_lng", new TableInfo.Column("destination_lng", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteHistory.put("distance_km", new TableInfo.Column("distance_km", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteHistory.put("duration_minutes", new TableInfo.Column("duration_minutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteHistory.put("estimated_fare", new TableInfo.Column("estimated_fare", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteHistory.put("transport_type", new TableInfo.Column("transport_type", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteHistory.put("traffic_condition", new TableInfo.Column("traffic_condition", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteHistory.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRouteHistory.put("is_favourite", new TableInfo.Column("is_favourite", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRouteHistory = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysRouteHistory.add(new TableInfo.ForeignKey("users", "CASCADE", "NO ACTION", Arrays.asList("user_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesRouteHistory = new HashSet<TableInfo.Index>(1);
        _indicesRouteHistory.add(new TableInfo.Index("index_route_history_user_id", false, Arrays.asList("user_id"), Arrays.asList("ASC")));
        final TableInfo _infoRouteHistory = new TableInfo("route_history", _columnsRouteHistory, _foreignKeysRouteHistory, _indicesRouteHistory);
        final TableInfo _existingRouteHistory = TableInfo.read(db, "route_history");
        if (!_infoRouteHistory.equals(_existingRouteHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "route_history(com.farego.app.db.entity.RouteHistory).\n"
                  + " Expected:\n" + _infoRouteHistory + "\n"
                  + " Found:\n" + _existingRouteHistory);
        }
        final HashMap<String, TableInfo.Column> _columnsUserProfiles = new HashMap<String, TableInfo.Column>(8);
        _columnsUserProfiles.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("user_id", new TableInfo.Column("user_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("email", new TableInfo.Column("email", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("phone", new TableInfo.Column("phone", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("avatar_path", new TableInfo.Column("avatar_path", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("home_location", new TableInfo.Column("home_location", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("work_location", new TableInfo.Column("work_location", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserProfiles = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysUserProfiles.add(new TableInfo.ForeignKey("users", "CASCADE", "NO ACTION", Arrays.asList("user_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesUserProfiles = new HashSet<TableInfo.Index>(1);
        _indicesUserProfiles.add(new TableInfo.Index("index_user_profiles_user_id", false, Arrays.asList("user_id"), Arrays.asList("ASC")));
        final TableInfo _infoUserProfiles = new TableInfo("user_profiles", _columnsUserProfiles, _foreignKeysUserProfiles, _indicesUserProfiles);
        final TableInfo _existingUserProfiles = TableInfo.read(db, "user_profiles");
        if (!_infoUserProfiles.equals(_existingUserProfiles)) {
          return new RoomOpenHelper.ValidationResult(false, "user_profiles(com.farego.app.db.entity.UserProfile).\n"
                  + " Expected:\n" + _infoUserProfiles + "\n"
                  + " Found:\n" + _existingUserProfiles);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "d6ba5b66179a3531a5dcc4d5fcec27cc", "7f0f96736fe78a925e168b6c1e0010f4");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "fare_rates","users","route_history","user_profiles");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `fare_rates`");
      _db.execSQL("DELETE FROM `users`");
      _db.execSQL("DELETE FROM `route_history`");
      _db.execSQL("DELETE FROM `user_profiles`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(FareRateDao.class, FareRateDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(UserDao.class, UserDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(RouteHistoryDao.class, RouteHistoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(UserProfileDao.class, UserProfileDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public FareRateDao fareRateDao() {
    if (_fareRateDao != null) {
      return _fareRateDao;
    } else {
      synchronized(this) {
        if(_fareRateDao == null) {
          _fareRateDao = new FareRateDao_Impl(this);
        }
        return _fareRateDao;
      }
    }
  }

  @Override
  public UserDao userDao() {
    if (_userDao != null) {
      return _userDao;
    } else {
      synchronized(this) {
        if(_userDao == null) {
          _userDao = new UserDao_Impl(this);
        }
        return _userDao;
      }
    }
  }

  @Override
  public RouteHistoryDao routeHistoryDao() {
    if (_routeHistoryDao != null) {
      return _routeHistoryDao;
    } else {
      synchronized(this) {
        if(_routeHistoryDao == null) {
          _routeHistoryDao = new RouteHistoryDao_Impl(this);
        }
        return _routeHistoryDao;
      }
    }
  }

  @Override
  public UserProfileDao userProfileDao() {
    if (_userProfileDao != null) {
      return _userProfileDao;
    } else {
      synchronized(this) {
        if(_userProfileDao == null) {
          _userProfileDao = new UserProfileDao_Impl(this);
        }
        return _userProfileDao;
      }
    }
  }
}
