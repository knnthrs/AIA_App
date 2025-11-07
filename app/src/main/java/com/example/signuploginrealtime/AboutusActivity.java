package com.example.signuploginrealtime;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AboutusActivity extends AppCompatActivity {

    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aboutus);

        // Apply window insets to handle notch and system bars
        View headerLayout = findViewById(R.id.header_layout);

        ViewCompat.setOnApplyWindowInsetsListener(headerLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Apply top padding to push content below the status bar/notch
            v.setPadding(
                    v.getPaddingLeft(),
                    insets.top + 16, // Status bar height + 16dp padding
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );

            return WindowInsetsCompat.CONSUMED;
        });

        // Initialize back button
        btnBack = findViewById(R.id.btn_back);

        // Set click listener to go back
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        // Handle physical back button press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(0, 0);
            }
        });
    }
}