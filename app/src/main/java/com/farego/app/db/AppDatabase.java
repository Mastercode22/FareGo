package com.farego.app.db;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.farego.app.db.dao.FareRateDao;
import com.farego.app.db.dao.RouteHistoryDao;
import com.farego.app.db.dao.UserDao;
import com.farego.app.db.dao.UserProfileDao;
import com.farego.app.db.entity.FareRate;
import com.farego.app.db.entity.RouteHistory;
import com.farego.app.db.entity.User;
import com.farego.app.db.entity.UserProfile;
import com.farego.app.utils.HashUtils;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities     = { FareRate.class, User.class, RouteHistory.class, UserProfile.class },
        version      = 4,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract FareRateDao     fareRateDao();
    public abstract UserDao         userDao();
    public abstract RouteHistoryDao routeHistoryDao();
    public abstract UserProfileDao  userProfileDao();

    private static volatile AppDatabase INSTANCE;

    public static final ExecutorService DB_EXECUTOR =
            Executors.newFixedThreadPool(4);

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "farego_database"
                            )
                            .addCallback(seedCallback)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final RoomDatabase.Callback seedCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            DB_EXECUTOR.execute(() -> {
                AppDatabase database = INSTANCE;
                if (database == null) return;

                FareRateDao fareRateDao = database.fareRateDao();
                UserDao     userDao     = database.userDao();
                long now = System.currentTimeMillis();

                // Seed fare rates with peak and traffic multipliers
                fareRateDao.insertAll(Arrays.asList(
                        new FareRate("TroTro", 1.50, 0.80, 2.00, now),
                        new FareRate("Taxi",   3.00, 1.50, 5.00, now),
                        new FareRate("Uber",   4.00, 2.00, 7.00, now)
                ));

                // Seed default admin account
                String adminHash = HashUtils.sha256("admin123");
                userDao.insertUser(new User(
                        "admin", adminHash, "admin@farego.app", true, now));
            });
        }
    };
}