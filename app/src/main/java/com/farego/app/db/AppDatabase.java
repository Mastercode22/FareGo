package com.farego.app.db;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.farego.app.db.dao.FareRateDao;
import com.farego.app.db.dao.RouteHistoryDao;
import com.farego.app.db.dao.RouteFareDao;
import com.farego.app.db.dao.UserDao;
import com.farego.app.db.dao.UserProfileDao;
import com.farego.app.db.entity.FareRate;
import com.farego.app.db.entity.RouteHistory;
import com.farego.app.db.entity.RouteFare;
import com.farego.app.db.entity.User;
import com.farego.app.db.entity.UserProfile;
import com.farego.app.utils.HashUtils;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities     = { FareRate.class, User.class, RouteHistory.class,
                UserProfile.class, RouteFare.class },
        version      = 5,          // bumped from 4 → 5 for the new table
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract FareRateDao     fareRateDao();
    public abstract UserDao         userDao();
    public abstract RouteHistoryDao routeHistoryDao();
    public abstract UserProfileDao  userProfileDao();
    public abstract RouteFareDao    routeFareDao();   // ← NEW

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

    // =========================================================================
    //  Seed callback
    // =========================================================================
    private static final RoomDatabase.Callback seedCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            DB_EXECUTOR.execute(() -> {
                AppDatabase database = INSTANCE;
                if (database == null) return;

                long now = System.currentTimeMillis();

                // ── Dynamic fare rates (Taxi / Uber stay unchanged) ───────────
                database.fareRateDao().insertAll(Arrays.asList(
                        // TroTro: per-km rate used only as a distance-average
                        // fallback when the route isn't in the RouteFare table.
                        new FareRate("TroTro", 1.50, 0.80, 2.00, now),
                        new FareRate("Taxi",   3.00, 1.50, 5.00, now),
                        new FareRate("Uber",   4.00, 2.00, 7.00, now)
                ));

                // ── Fixed GPRTU TroTro fares (Accra Full Routes 2025) ─────────
                database.routeFareDao().insertAll(Arrays.asList(
                        new RouteFare("Circle",   "Madina",   8.50, 12, "Nima, 37, Legon, Atomic",     now),
                        new RouteFare("Circle",   "Adenta",  10.00, 15, "37, Legon, Madina",            now),
                        new RouteFare("Circle",   "Achimota", 6.00,  8, "Lapaz, Tesano",                now),
                        new RouteFare("Circle",   "Taifa",   10.00, 14, "Achimota, Dome",               now),
                        new RouteFare("Circle",   "Kasoa",   15.00, 28, "Kaneshie, Mallam, Weija",      now),
                        new RouteFare("Circle",   "Tema",    18.00, 30, "Motorway, Tetteh Quarshie",    now),
                        new RouteFare("Circle",   "Dansoman", 7.00, 10, "Odorkor, Russia",              now),
                        new RouteFare("Circle",   "Lapaz",    5.00,  6, "Graphic Road",                 now),
                        new RouteFare("Kaneshie", "Kasoa",   12.00, 25, "Mallam, Weija",                now),
                        new RouteFare("Kaneshie", "Circle",   4.00,  5, "Graphic Road",                 now),
                        new RouteFare("Kaneshie", "Achimota", 5.50,  7, "Lapaz",                        now),
                        new RouteFare("Kaneshie", "Tema",    18.50, 32, "Circle, Motorway",             now),
                        new RouteFare("Achimota", "Madina",   7.50, 10, "Dome, Atomic",                 now),
                        new RouteFare("Achimota", "Legon",    6.00,  8, "Haatso",                       now),
                        new RouteFare("Achimota", "Circle",   6.00,  8, "Lapaz",                        now),
                        new RouteFare("37",       "Madina",   7.50, 10, "Legon, Atomic",                now),
                        new RouteFare("37",       "Tema",    15.00, 28, "Spintex, Nungua",              now),
                        new RouteFare("Airport",  "East Legon",5.50, 6, "Shiashie",                     now),
                        new RouteFare("Tema",     "Circle",  18.00, 30, "Ashaiman, Motorway",           now),
                        new RouteFare("Tema",     "Nungua",   7.00, 10, "Baatsona",                     now),
                        new RouteFare("Spintex",  "Circle",  12.00, 18, "Airport, 37",                  now),
                        new RouteFare("Teshie",   "Circle",   9.00, 14, "La, Osu",                      now),
                        new RouteFare("Nungua",   "Tema",     8.00, 12, "Beach Road",                   now),
                        new RouteFare("Circle",   "Kaneshie", 4.00,  5, "",                             now),
                        new RouteFare("Circle",   "Osu",      5.00,  6, "Ridge",                        now),
                        new RouteFare("Lapaz",    "Circle",   5.50,  6, "",                             now)
                ));

                // ── Default admin account ─────────────────────────────────────
                String adminHash = HashUtils.sha256("admin123");
                database.userDao().insertUser(new User(
                        "admin", adminHash, "admin@farego.app", true, now));
            });
        }
    };
}