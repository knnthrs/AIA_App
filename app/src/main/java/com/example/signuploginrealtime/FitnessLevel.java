package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

public class FitnessLevel extends AppCompatActivity {

    private MaterialCardView cardBeginner, cardIntermediate, cardAdvanced;
    private Button btnNext;
    private String selectedFitnessLevel = "";

    private UserProfileHelper.UserProfile userProfile; // full profile

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fitness_level);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get full UserProfile from previous activity
        userProfile = (UserProfileHelper.UserProfile) getIntent().getSerializableExtra("userProfile");
        if (userProfile == null) {
            userProfile = new UserProfileHelper.UserProfile();
            userProfile.setHealthIssues(new ArrayList<>());
        }

        // Initialize views
        cardBeginner = findViewById(R.id.cardBeginner);
        cardIntermediate = findViewById(R.id.cardIntermediate);
        cardAdvanced = findViewById(R.id.cardAdvanced);
        btnNext = findViewById(R.id.btnNext);

        // Set click listeners for fitness level selection
        cardBeginner.setOnClickListener(v -> { selectedFitnessLevel = "Beginner"; updateCardSelection(); });
        cardIntermediate.setOnClickListener(v -> { selectedFitnessLevel = "Intermediate"; updateCardSelection(); });
        cardAdvanced.setOnClickListener(v -> { selectedFitnessLevel = "Advanced"; updateCardSelection(); });

        // Next button click listener
        btnNext.setOnClickListener(v -> {
            if (!selectedFitnessLevel.isEmpty()) {
                // Save selected fitness level in UserProfile
                userProfile.setFitnessLevel(selectedFitnessLevel);

                // Pass full userProfile to next activity (FitnessGoal)
                Intent intent = new Intent(FitnessLevel.this, FitnessGoal.class);
                intent.putExtra("userProfile", userProfile);
                startActivity(intent);
            }
        });
    }

    private void updateCardSelection() {
        // Reset all cards to default state
        cardBeginner.setStrokeColor(getResources().getColor(R.color.white));
        cardIntermediate.setStrokeColor(getResources().getColor(R.color.white));
        cardAdvanced.setStrokeColor(getResources().getColor(R.color.white));

        // Highlight selected card
        switch (selectedFitnessLevel) {
            case "Beginner": cardBeginner.setStrokeColor(getResources().getColor(R.color.black)); break;
            case "Intermediate": cardIntermediate.setStrokeColor(getResources().getColor(R.color.black)); break;
            case "Advanced": cardAdvanced.setStrokeColor(getResources().getColor(R.color.black)); break;
        }

        // Enable next button
        btnNext.setEnabled(true);
        btnNext.setAlpha(1.0f);
    }
}
