package com.freeze1188.smarthold

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 100, 48, 48)
        }

        val text = TextView(this).apply {
            textSize = 22f
            text = "Smart Hold Test\n\nApp opened successfully."
        }

        layout.addView(text)
        setContentView(layout)
    }
}
