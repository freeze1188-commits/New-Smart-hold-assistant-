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
    private val requestCode = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
        }

        statusText = TextView(this).apply {
            textSize = 18f
            text = "Smart Hold Test\n\nGrant permissions first. Use speakerphone for this test."
        }

        val permissionButton = Button(this).apply {
            text = "Grant Permissions"
            setOnClickListener {
                requestPermissionsIfNeeded()
            }
        }

        val startButton = Button(this).apply {
            text = "Start Smart Hold"
            setOnClickListener {
                if (!hasAudioPermission()) {
                    statusText.text = "Microphone permission is required first."
                    requestPermissionsIfNeeded()
                    return@setOnClickListener
                }

                try {
                    ContextCompat.startForegroundService(
                        this@MainActivity,
                        Intent(this@MainActivity, HoldMonitorService::class.java)
                    )
                    statusText.text = "Smart Hold active. Put the call on speakerphone."
                } catch (e: Exception) {
                    statusText.text = "Could not start service:\n${e.message}"
                }
            }
        }

        val stopButton = Button(this).apply {
            text = "Stop Smart Hold"
            setOnClickListener {
                try {
                    stopService(Intent(this@MainActivity, HoldMonitorService::class.java))
                    statusText.text = "Smart Hold stopped."
                } catch (e: Exception) {
                    statusText.text = "Could not stop service:\n${e.message}"
                }
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
        layout.addView(permissionButton)
        layout.addView(startButton)
        layout.addView(stopButton)
        layout.addView(stillOnHoldButton)
        layout.addView(correctButton)

        setContentView(layout)
    }

    private fun hasAudioPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf<String>()

        if (!hasAudioPermission()) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), requestCode)
        } else {
            statusText.text = "Permissions already granted."
        }
    }
}
