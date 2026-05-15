package com.freeze1188.smarthold;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 100, 48, 48);

        statusText = new TextView(this);
        statusText.setTextSize(20);
        statusText.setText("Smart Hold Test\n\nReady.");

        Button permissionButton = new Button(this);
        permissionButton.setText("Grant Microphone Permission");
        permissionButton.setOnClickListener(v -> {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 100);
        });

        Button startButton = new Button(this);
        startButton.setText("Start Smart Hold");
        startButton.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                statusText.setText("Microphone permission needed first.");
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 100);
                return;
            }

            Intent serviceIntent = new Intent(this, HoldMonitorService.class);
            startForegroundService(serviceIntent);
            statusText.setText("Smart Hold started. Check notification bar.");
        });

        Button stopButton = new Button(this);
        stopButton.setText("Stop Smart Hold");
        stopButton.setOnClickListener(v -> {
            stopService(new Intent(this, HoldMonitorService.class));
            statusText.setText("Smart Hold stopped.");
        });

        layout.addView(statusText);
        layout.addView(permissionButton);
        layout.addView(startButton);
        layout.addView(stopButton);

        setContentView(layout);
    }
}
