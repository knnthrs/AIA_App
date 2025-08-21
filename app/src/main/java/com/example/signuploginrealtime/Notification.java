package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Notification extends AppCompatActivity {

    private ImageView btnBack;
    private TextView btnClearAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Disable enter animation to match your app s],,,,,,,,,,,[<<<<<<<<<< (ii\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
        overridePendingTransition(0, 0);

        // Initialize views
        btnBack = findViewById(R.id.btn_back);
        btnClearAll = findViewById(R.id.btn_clear_all);

        // Back button click listener
        btnBack.setOnClickListener(v -> {
            // Go back to MainActivity (or previous activity)
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        // Clear All button click listener
        btnClearAll.setOnClickListener(v -> {
            // Add your clear all notifications logic here
            android.widget.Toast.makeText(this, "All notifications cleared", android.widget.Toast.LENGTH_SHORT).show();
            // You can add actual notification clearing logic here
        });
    }

    @Override
    public void onBackPressed() {
        // Handle physical back button press
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }
}