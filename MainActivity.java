package com.freeze1188.smarthold;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView statusText;

    private static final String CHANNEL_ID = "smart_hold_alerts";
    private static final int SAMPLE_RATE = 16000;
    private static final int VOLUME_TRIGGER = 1800;

    private boolean monitoring = false;
    private AudioRecord recorder;
    private Thread monitorThread;
    private long lastAlertTime = 0;

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

            startMonitoring();
        });

        Button stopButton = new Button(this);
        stopButton.setText("Stop Smart Hold");
        stopButton.setOnClickListener(v -> stopMonitoring());

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

    private void startMonitoring() {
        if (monitoring) {
            statusText.setText("Already monitoring.");
            return;
        }

        int bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        if (bufferSize <= 0) {
            statusText.setText("Audio buffer error.");
            return;
        }

        recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        monitoring = true;
        recorder.startRecording();

        statusText.setText("Monitoring microphone...\n\nMake sound or talk to test.");

        monitorThread = new Thread(() -> {
            short[] buffer = new short[bufferSize];

            while (monitoring) {
                int read = recorder.read(buffer, 0, buffer.length);

                if (read > 0) {
                    int volume = calculateAverageVolume(buffer, read);

                    runOnUiThread(() ->
                            statusText.setText("Monitoring microphone...\n\nVolume: " + volume)
                    );

                    long now = System.currentTimeMillis();

                    if (volume > VOLUME_TRIGGER && now - lastAlertTime > 10000) {
                        lastAlertTime = now;

                        runOnUiThread(() -> {
                            showAlertNotification();
                            statusText.setText("Possible human/sound detected.\n\nVolume: " + volume);
                        });
                    }
                }
            }
        });

        monitorThread.start();
    }

    private void stopMonitoring() {
        monitoring = false;

        try {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;
            }
        } catch (Exception ignored) {
        }

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.cancel(1);

        statusText.setText("Smart Hold stopped.");
    }

    private int calculateAverageVolume(short[] buffer, int read) {
        long sum = 0;

        for (int i = 0; i < read; i++) {
            sum += Math.abs(buffer[i]);
        }

        return (int) (sum / read);
    }

    private void createNotificationChannel() {
        Uri soundUri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;

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

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    private void showAlertNotification() {
        Notification notification =
                new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle("Possible human detected")
                        .setContentText("Check your call now.")
                        .setStyle(
                                new Notification.BigTextStyle()
                                        .bigText("Possible human voice or loud sound detected.\n\nCheck your call now.")
                        )
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setAutoCancel(true)
                        .build();

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(1, notification);
    }

    @Override
    protected void onDestroy() {
        stopMonitoring();
        super.onDestroy();
    }
}
