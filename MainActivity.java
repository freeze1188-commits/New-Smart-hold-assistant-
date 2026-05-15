startButton.setOnClickListener(v -> {
    if (hasMicPermission()) {
        try {
            Intent serviceIntent = new Intent(this, HoldMonitorService.class);
            startForegroundService(serviceIntent);
            statusText.setText("Smart Hold started. Persistent notification should be visible.");
        } catch (Exception e) {
            statusText.setText("Error starting Smart Hold:\n" + e.getMessage());
        }
    } else {
        statusText.setText("Microphone permission is needed first.");
        requestMicPermission();
    }
});
