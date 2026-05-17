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
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends Activity {

    private TextView statusText;

    private static final String CHANNEL_ID = "smart_hold_alerts";
    private static final int SAMPLE_RATE = 16000;

    private static final int MIN_VOLUME = 300;
    private static final int MIN_VARIATION = 120;
    private static final int REQUIRED_HITS = 3;
    private static final int ALERT_COOLDOWN_MS = 8000;

    private boolean monitoring = false;
    private AudioRecord audioRecorder;
    private Thread monitorThread;
    private long lastAlertTime = 0;

    private int speechLikeHits = 0;
    private int lastVolume = 0;
    private int rollingBaseline = 0;

    private MediaRecorder responseRecorder;
    private MediaPlayer responsePlayer;
    private String responseFilePath;
    private boolean isRecordingResponse = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        responseFilePath = new File(getFilesDir(), "quick_response.3gp").getAbsolutePath();

        createNotificationChannel();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 100, 48, 48);

        statusText = new TextView(this);
        statusText.setTextSize(20);
        statusText.setText("Smart Hold Test\n\nRecord a response first.");

        Button permissionButton = new Button(this);
        permissionButton.setText("Grant Permissions");
        permissionButton.setOnClickListener(v -> requestNeededPermissions());

        Button recordButton = new Button(this);
        recordButton.setText("Record Response");
        recordButton.setOnClickListener(v -> startResponseRecording());

        Button stopRecordButton = new Button(this);
        stopRecordButton.setText("Stop Recording");
        stopRecordButton.setOnClickListener(v -> stopResponseRecording());

        Button playButton = new Button(this);
        playButton.setText("Play Response");
        playButton.setOnClickListener(v -> playResponse());

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
        layout.addView(recordButton);
        layout.addView(stopRecordButton);
        layout.addView(playButton);
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

    private void startResponseRecording() {
        if (!hasMicPermission()) {
            statusText.setText("Microphone permission needed first.");
            requestNeededPermissions();
            return;
        }

        if (monitoring) {
            statusText.setText("Stop Smart Hold before recording a response.");
            return;
        }

        if (isRecordingResponse) {
            statusText.setText("Already recording.");
            return;
        }

        try {
            File oldFile = new File(responseFilePath);
            if (oldFile.exists()) {
                oldFile.delete();
            }

            responseRecorder = new MediaRecorder();
            responseRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            responseRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            responseRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            responseRecorder.setOutputFile(responseFilePath);
            responseRecorder.prepare();
            responseRecorder.start();

            isRecordingResponse = true;
            statusText.setText("Recording response...\n\nSay: Hi, I’m here, one moment please.");

        } catch (Exception e) {
            isRecordingResponse = false;
            statusText.setText("Recording failed:\n" + e.getMessage());
        }
    }

    private void stopResponseRecording() {
        if (!isRecordingResponse) {
            statusText.setText("Not currently recording.");
            return;
        }

        try {
            responseRecorder.stop();
            responseRecorder.release();
            responseRecorder = null;
            isRecordingResponse = false;

            statusText.setText("Response saved.\n\nTap Play Response to test it.");

        } catch (Exception e) {
            isRecordingResponse = false;
            statusText.setText("Stop recording failed:\n" + e.getMessage());
        }
    }

    private boolean hasSavedResponse() {
        File file = new File(responseFilePath);
        return file.exists() && file.length() > 0;
    }

    private void playResponse() {
        if (!hasSavedResponse()) {
            statusText.setText("No response recorded yet.");
            return;
        }

        try {
            if (responsePlayer != null) {
                responsePlayer.release();
                responsePlayer = null;
            }

            responsePlayer = new MediaPlayer();
            responsePlayer.setDataSource(responseFilePath);
            responsePlayer.prepare();
            responsePlayer.start();

            statusText.setText("Playing response...");

        } catch (Exception e) {
            statusText.setText("Playback failed:\n" + e.getMessage());
        }
    }

    private void startMonitoring() {
        if (isRecordingResponse) {
            statusText.setText("Stop recording response first.");
            return;
        }

        if (!hasSavedResponse()) {
            statusText.setText("Record a response before starting Smart Hold.");
            return;
        }

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

        audioRecorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        monitoring = true;
        speechLikeHits = 0;
        lastVolume = 0;
        rollingBaseline = 0;

        audioRecorder.startRecording();

        statusText.setText("Monitoring...\n\nListening for speech-like changes.");

        monitorThread = new Thread(() -> {
            short[] buffer = new short[bufferSize];

            while (monitoring) {
                int read = audioRecorder.read(buffer, 0, buffer.length);

                if (read > 0) {
                    int volume = calculateAverageVolume(buffer, read);

                    if (rollingBaseline == 0) {
                        rollingBaseline = volume;
                    } else {
                        rollingBaseline = (rollingBaseline * 9 + volume) / 10;
                    }

                    int variation = Math.abs(volume - lastVolume);
                    int jumpFromBaseline = Math.abs(volume - rollingBaseline);

                    boolean loudEnough = volume > MIN_VOLUME;
                    boolean changingEnough =
                            variation > MIN_VARIATION ||
                                    jumpFromBaseline > MIN_VARIATION;

                    boolean speechLike = loudEnough && changingEnough;

                    if (speechLike) {
                        speechLikeHits++;
                    } else {
                        speechLikeHits = Math.max(0, speechLikeHits - 1);
                    }

                    lastVolume = volume;

                    int finalVolume = volume;
                    int finalVariation = variation;
                    int finalHits = speechLikeHits;
                    int finalBaseline = rollingBaseline;

                    runOnUiThread(() ->
                            statusText.setText(
                                    "Monitoring...\n\n" +
                                            "Volume: " + finalVolume + "\n" +
                                            "Baseline: " + finalBaseline + "\n" +
                                            "Variation: " + finalVariation + "\n" +
                                            "Speech hits: " + finalHits + " / " + REQUIRED_HITS
                            )
                    );

                    long now = System.currentTimeMillis();

                    if (speechLikeHits >= REQUIRED_HITS &&
                            now - lastAlertTime > ALERT_COOLDOWN_MS) {

                        lastAlertTime = now;
                        speechLikeHits = 0;

                        runOnUiThread(() -> {
                            showAlertNotification();
                            playResponse();
                            statusText.setText(
                                    "Possible speech detected.\n\nResponse played. Check your call."
                            );
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
            if (audioRecorder != null) {
                audioRecorder.stop();
                audioRecorder.release();
                audioRecorder = null;
            }
        } catch (Exception ignored) {
        }

        NotificationManager manager =
                getSystemService(NotificationManager.class);

        manager.cancel(1);

        statusText.setText("Smart Hold stopped.");
    }

    private int calculateAverageVolume(short[] buffer, int read) {
        long sum = 0;

        for (int i = 0; i < read; i++) {
            sum += Math.abs(buffer[i]);
        }

        return (int)(sum / read);
    }

    private void createNotificationChannel() {
        Uri soundUri =
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;

        AudioAttributes audioAttributes =
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();

        NotificationChannel channel =
                new NotificationChannel(
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
                        .setContentTitle("Possible speech detected")
                        .setContentText("Response played. Check your call now.")
                        .setStyle(
                                new Notification.BigTextStyle()
                                        .bigText(
                                                "Possible speech-like audio detected.\n\nYour saved response was played. Check your call now."
                                        )
                        )
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setAutoCancel(true)
                        .build();

        NotificationManager manager =
                getSystemService(NotificationManager.class);

        manager.notify(1, notification);
    }

    @Override
    protected void onDestroy() {
        stopMonitoring();

        try {
            if (responsePlayer != null) {
                responsePlayer.release();
                responsePlayer = null;
            }
        } catch (Exception ignored) {
        }

        try {
            if (responseRecorder != null) {
                responseRecorder.release();
                responseRecorder = null;
            }
        } catch (Exception ignored) {
        }

        super.onDestroy();
    }
}
