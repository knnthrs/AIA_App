package com.example.signuploginrealtime.UserInfo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.signuploginrealtime.R;
import com.example.signuploginrealtime.models.UserProfile;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

public class FitnessLevel extends AppCompatActivity {

    private MaterialCardView cardSedentary, cardLight, cardModerate, cardVeryActive;
    private Button btnNext;
    private String selectedActivityLevel = "";

    private UserProfile userProfile; // full profile

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fitness_level); // use your XML

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get full UserProfile from previous activity
        userProfile = (UserProfile) getIntent().getSerializableExtra("userProfile");
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setHealthIssues(new ArrayList<>());
        }

        // Initialize views
        cardSedentary = findViewById(R.id.cardSedentary);
        cardLight = findViewById(R.id.cardLight);
        cardModerate = findViewById(R.id.cardModerate);
        cardVeryActive = findViewById(R.id.cardVeryActive);
        btnNext = findViewById(R.id.btnNext);

        // Set click listeners for activity level selection
        cardSedentary.setOnClickListener(v -> { selectedActivityLevel = "Sedentary"; updateCardSelection(); });
        cardLight.setOnClickListener(v -> { selectedActivityLevel = "Lightly Active"; updateCardSelection(); });
        cardModerate.setOnClickListener(v -> { selectedActivityLevel = "Moderately Active"; updateCardSelection(); });
        cardVeryActive.setOnClickListener(v -> { selectedActivityLevel = "Very Active"; updateCardSelection(); });

        // Next button click listener
        btnNext.setOnClickListener(v -> {
            if (!selectedActivityLevel.isEmpty()) {
                // Save selected activity level in UserProfile
                userProfile.setFitnessLevel(selectedActivityLevel.trim().toLowerCase());
                
                // Pass full userProfile to next activity (FitnessGoal)
                Intent intent = new Intent(FitnessLevel.this, FitnessGoal.class);
                intent.putExtra("userProfile", userProfile);
                startActivity(intent);
            }
        });
    }

    private void updateCardSelection() {
        // Reset all cards to default state
        cardSedentary.setStrokeColor(getResources().getColor(R.color.white));
        cardLight.setStrokeColor(getResources().getColor(R.color.white));
        cardModerate.setStrokeColor(getResources().getColor(R.color.white));
        cardVeryActive.setStrokeColor(getResources().getColor(R.color.white));

        // Highlight selected card
        switch (selectedActivityLevel) {
            case "Sedentary": cardSedentary.setStrokeColor(getResources().getColor(R.color.black)); break;
            case "Lightly Active": cardLight.setStrokeColor(getResources().getColor(R.color.black)); break;
            case "Moderately Active": cardModerate.setStrokeColor(getResources().getColor(R.color.black)); break;
            case "Very Active": cardVeryActive.setStrokeColor(getResources().getColor(R.color.black)); break;
        }

        // Enable next button
        btnNext.setEnabled(true);
        btnNext.setAlpha(1.0f);
    }
}
