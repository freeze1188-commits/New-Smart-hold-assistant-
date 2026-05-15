package com.freeze1188.smarthold;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class HoldMonitorService extends Service {

    private static final String CHANNEL_ID = "smart_hold_status";

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Smart Hold Status",
                NotificationManager.IMPORTANCE_LOW
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Smart Hold active")
                .setContentText("Monitoring service is running.")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build();

        startForeground(1, notification);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
