package com.freeze1188.smarthold

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.concurrent.thread
import kotlin.math.abs

class HoldMonitorService : Service() {

    private var running = false
    private var recorder: AudioRecord? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(100, ongoingNotification())
        startMonitoring()
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        try {
            recorder?.stop()
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMonitoring() {
        if (running) return
        running = true

        thread(name = "SmartHoldAudioThread") {
            val sampleRate = 16000
            val minBuffer = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer
            )

            val buffer = ShortArray(minBuffer / 2)
            recorder?.startRecording()

            var lastAlertMs = 0L

            while (running) {
                val count = recorder?.read(buffer, 0, buffer.size) ?: 0
                if (count <= 0) continue

                val score = averageVolume(buffer, count)

                if (score > 1200) {
                    val now = System.currentTimeMillis()
                    if (now - lastAlertMs > 15000) {
                        lastAlertMs = now
                        showPossibleRepNotification()
                    }
                }
            }
        }
    }

    private fun averageVolume(buffer: ShortArray, count: Int): Int {
        var sum = 0L
        for (i in 0 until count) {
            sum += abs(buffer[i].toInt())
        }
        return (sum / count).toInt()
    }

    private fun ongoingNotification(): Notification {
        return NotificationCompat.Builder(this, STATUS_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Smart Hold active")
            .setContentText("Listening through microphone. Use speakerphone for this test.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showPossibleRepNotification() {
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Possible representative detected")
            .setContentText("Check the call. If still on hold, tap Still On Hold.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(101, notification)
    }

    private fun createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            manager.createNotificationChannel(
                NotificationChannel(STATUS_CHANNEL, "Smart Hold Status", NotificationManager.IMPORTANCE_LOW)
            )

            manager.createNotificationChannel(
                NotificationChannel(ALERT_CHANNEL, "Representative Alerts", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    companion object {
        private const val STATUS_CHANNEL = "smart_hold_status"
        private const val ALERT_CHANNEL = "smart_hold_alerts"
    }
}
