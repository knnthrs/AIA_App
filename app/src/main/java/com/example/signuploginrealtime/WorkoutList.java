package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class WorkoutList extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_workout_list);

        // Disable enter animation
        overridePendingTransition(0, 0);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.item_3);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.item_1) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish(); // Removes from back stack
                return true;
            } else if (itemId == R.id.item_2) {
                startActivity(new Intent(this, Profile.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.item_3) {
                return true;
            } else if (itemId == R.id.item_4) {
                // Navigate to Achievements activity
                startActivity(new Intent(this, Achievement.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onBackPressed() {
        // When pressing back from WorkoutList, go straight to home without animation
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }
}