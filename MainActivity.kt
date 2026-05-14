package com.freeze1188.smarthold

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionsIfNeeded()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
        }

        statusText = TextView(this).apply {
            textSize = 18f
            text = "Smart Hold Test\n\nUse speakerphone for this first test."
        }

        val startButton = Button(this).apply {
            text = "Start Smart Hold"
            setOnClickListener {
                requestPermissionsIfNeeded()
                ContextCompat.startForegroundService(
                    this@MainActivity,
                    Intent(this@MainActivity, HoldMonitorService::class.java)
                )
                statusText.text = "Smart Hold is active. Put the call on speakerphone."
            }
        }

        val stopButton = Button(this).apply {
            text = "Stop Smart Hold"
            setOnClickListener {
                stopService(Intent(this@MainActivity, HoldMonitorService::class.java))
                statusText.text = "Smart Hold stopped."
            }
        }

        val stillOnHoldButton = Button(this).apply {
            text = "Still On Hold"
            setOnClickListener {
                statusText.text = "Feedback: still on hold."
            }
        }

        val correctButton = Button(this).apply {
            text = "Detected Correctly"
            setOnClickListener {
                statusText.text = "Feedback: detected correctly."
            }
        }

        layout.addView(statusText)
        layout.addView(startButton)
        layout.addView(stopButton)
        layout.addView(stillOnHoldButton)
        layout.addView(correctButton)

        setContentView(layout)
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), 10)
        }
    }
}
