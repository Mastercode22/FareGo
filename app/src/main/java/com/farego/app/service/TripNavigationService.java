package com.farego.app.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.farego.app.FareGoApp;
import com.farego.app.R;
import com.farego.app.activities.MainActivity;

public class TripNavigationService extends Service {

    public static final String ACTION_START = "com.farego.app.START_TRIP";
    public static final String ACTION_STOP  = "com.farego.app.STOP_TRIP";
    public static final String EXTRA_ETA_MS = "eta_ms";

    private static final int NOTIF_ID_ONGOING = 1001;
    private static final int NOTIF_ID_ALERT   = 1002;

    private CountDownTimer countDownTimer;
    private NotificationManager notifManager;
    private long etaMs;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public TripNavigationService getService() { return TripNavigationService.this; }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        if (ACTION_START.equals(intent.getAction())) {
            etaMs = intent.getLongExtra(EXTRA_ETA_MS, 15 * 60 * 1000L);
            startForeground(NOTIF_ID_ONGOING, buildOngoingNotif(etaMs / 60000, "Trip in progress"));
            startCountdown(etaMs);
        } else if (ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
        }
        return START_STICKY;
    }

    private void startCountdown(long durationMs) {
        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(durationMs, 30_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minsLeft = millisUntilFinished / 60000;
                updateOngoingNotif((int) minsLeft);

                if (minsLeft == 10)     fireAlert("🚌 10 minutes to arrival", "You'll be there soon!");
                else if (minsLeft == 5) fireAlert("⏱ 5 minutes remaining", "Almost at your destination.");
                else if (minsLeft == 2) fireAlert("📍 Arriving soon!", "Prepare to exit.");
            }

            @Override
            public void onFinish() {
                fireAlert("✅ You have arrived!", "Welcome to your destination.");
                stopSelf();
            }
        }.start();
    }

    private void updateOngoingNotif(int minsLeft) {
        String msg = minsLeft > 0 ? minsLeft + " min remaining" : "Arriving now!";
        notifManager.notify(NOTIF_ID_ONGOING, buildOngoingNotif(minsLeft, msg));
    }

    private Notification buildOngoingNotif(long minsLeft, String text) {
        Intent tapIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, FareGoApp.CHANNEL_TRIP_ID)
                .setSmallIcon(R.drawable.ic_navigation)
                .setContentTitle("FareGo — Trip in progress")
                .setContentText(text)
                .setOngoing(true)
                .setSilent(true)
                .setContentIntent(pi)
                .build();
    }

    private void fireAlert(String title, String body) {
        Notification n = new NotificationCompat.Builder(this, FareGoApp.CHANNEL_ALERT_ID)
                .setSmallIcon(R.drawable.ic_navigation)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();
        notifManager.notify(NOTIF_ID_ALERT, n);
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onDestroy() {
        if (countDownTimer != null) countDownTimer.cancel();
        super.onDestroy();
    }
}