package com.freeze1188.smarthold;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView statusText;
    private static final String CHANNEL_ID = "smart_hold_alerts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createNotificationChannel();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 100, 48, 48);

        statusText = new TextView(this);
        statusText.setTextSize(20);
        statusText.setText("Smart Hold Test\n\nReady.");

        Button permissionButton = new Button(this);
        permissionButton.setText("Grant Permissions");
        permissionButton.setOnClickListener(v -> requestNeededPermissions());

        Button startButton = new Button(this);
        startButton.setText("Start Smart Hold");
        startButton.setOnClickListener(v -> {

            if (!hasMicPermission()) {
                statusText.setText("Microphone permission needed first.");
                requestNeededPermissions();
                return;
            }

            if (!hasNotificationPermission()) {
                statusText.setText("Notification permission needed first.");
                requestNeededPermissions();
                return;
            }

            showAlertNotification();

            statusText.setText("ALERT notification sent.");
        });

        Button stopButton = new Button(this);
        stopButton.setText("Stop Smart Hold");
        stopButton.setOnClickListener(v -> {
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.cancel(1);
            statusText.setText("Notification removed.");
        });

        layout.addView(statusText);
        layout.addView(permissionButton);
        layout.addView(startButton);
        layout.addView(stopButton);

        setContentView(layout);
    }

    private boolean hasMicPermission() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasNotificationPermission() {

        if (android.os.Build.VERSION.SDK_INT < 33) {
            return true;
        }

        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNeededPermissions() {

        if (android.os.Build.VERSION.SDK_INT >= 33) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    100
            );

        } else {

            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    100
            );
        }
    }

    private void createNotificationChannel() {

        Uri soundUri =
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;

        AudioAttributes audioAttributes =
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Smart Hold Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );

        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 500, 300, 500});
        channel.enableLights(true);
        channel.setSound(soundUri, audioAttributes);

        NotificationManager manager =
                getSystemService(NotificationManager.class);

        manager.createNotificationChannel(channel);
    }

    private void showAlertNotification() {

        Notification notification =
                new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle("CALL DETECTED")
                        .setContentText("Potential human voice detected.")
                        .setStyle(
                                new Notification.BigTextStyle()
                                        .bigText(
                                                "Potential human voice detected.\n\nReturn to your call now."
                                        )
                        )
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setAutoCancel(true)
                        .build();

        NotificationManager manager =
                getSystemService(NotificationManager.class);

        manager.notify(1, notification);
    }
}
