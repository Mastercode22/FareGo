package com.farego.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.google.android.libraries.places.api.Places;
import com.farego.app.BuildConfig;

public class FareGoApp extends Application {

    public static final String CHANNEL_TRIP_ID  = "farego_trip";
    public static final String CHANNEL_ALERT_ID = "farego_alerts";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Places SDK once for the whole app
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        }

        // Create notification channels (required on Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);

            // Ongoing silent trip notification
            NotificationChannel tripChannel = new NotificationChannel(
                    CHANNEL_TRIP_ID,
                    "Trip Navigation",
                    NotificationManager.IMPORTANCE_LOW
            );
            tripChannel.setDescription("Ongoing notification while a trip is active");

            // High-priority arrival alerts
            NotificationChannel alertChannel = new NotificationChannel(
                    CHANNEL_ALERT_ID,
                    "Trip Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertChannel.setDescription("Arrival and waypoint alerts during a trip");

            nm.createNotificationChannel(tripChannel);
            nm.createNotificationChannel(alertChannel);
        }
    }
}