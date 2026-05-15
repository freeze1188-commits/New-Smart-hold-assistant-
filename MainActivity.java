package com.freeze1188.smarthold;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView statusText;
    private static final int MIC_PERMISSION_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 100, 48, 48);

        statusText = new TextView(this);
        statusText.setTextSize(20);
        statusText.setText("Smart Hold Test\n\nStep 1: test microphone permission.");

        Button permissionButton = new Button(this);
        permissionButton.setText("Grant Microphone Permission");
        permissionButton.setOnClickListener(v -> requestMicPermission());

        Button startButton = new Button(this);
        startButton.setText("Start Smart Hold");
        startButton.setOnClickListener(v -> {
            if (hasMicPermission()) {
                statusText.setText("Microphone permission granted.\n\nNext step will be detection.");
            } else {
                statusText.setText("Microphone permission is needed first.");
                requestMicPermission();
            }
        });

        Button stillOnHoldButton = new Button(this);
        stillOnHoldButton.setText("Still On Hold");
        stillOnHoldButton.setOnClickListener(v ->
                statusText.setText("Feedback: still on hold.")
        );

        Button correctButton = new Button(this);
        correctButton.setText("Detected Correctly");
        correctButton.setOnClickListener(v ->
                statusText.setText("Feedback: detected correctly.")
        );

        layout.addView(statusText);
        layout.addView(permissionButton);
        layout.addView(startButton);
        layout.addView(stillOnHoldButton);
        layout.addView(correctButton);

        setContentView(layout);
    }

    private boolean hasMicPermission() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMicPermission() {
        if (!hasMicPermission()) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MIC_PERMISSION_REQUEST
            );
        } else {
            statusText.setText("Microphone permission already granted.");
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MIC_PERMISSION_REQUEST) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                statusText.setText("Microphone permission granted.");
            } else {
                statusText.setText("Microphone permission denied.");
            }
        }
    }
}
