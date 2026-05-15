package com.freeze1188.smarthold;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 100, 48, 48);

        TextView text = new TextView(this);
        text.setTextSize(22);
        text.setText("Smart Hold Test\n\nApp opened successfully.");

        layout.addView(text);
        setContentView(layout);
    }
}
