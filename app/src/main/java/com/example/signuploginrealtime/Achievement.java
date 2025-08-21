package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Achievement extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_achievement);

        // Disable enter animation
        overridePendingTransition(0, 0);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Setup bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.item_4);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.item_1) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.item_2) {
                startActivity(new Intent(this, Profile.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.item_3) {
                startActivity(new Intent(this, WorkoutList.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.item_4) {
                return true; // Already on achievements screen
            }
            return false;
        });
    }

    @Override
    public void onBackPressed() {
        // When pressing back from Achievement, go to home
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }
}