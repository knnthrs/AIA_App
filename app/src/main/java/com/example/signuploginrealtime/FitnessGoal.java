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

public class FitnessGoal extends AppCompatActivity {

    private MaterialCardView cardWeightLoss, cardMuscleGain, cardEndurance, cardGeneralFitness;
    private Button btnNext;
    private String selectedFitnessGoal = "";
    private UserProfileHelper.UserProfile userProfile; // full profile

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fitness_goal);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get the UserProfile from previous activity
        userProfile = (UserProfileHelper.UserProfile) getIntent().getSerializableExtra("userProfile");
        if (userProfile == null) {
            // fallback if somehow missing
            userProfile = new UserProfileHelper.UserProfile();
            userProfile.setHealthIssues(new ArrayList<>());
            userProfile.setFitnessLevel("Beginner");
            userProfile.setAge(25);
            userProfile.setGender("not specified");
        }

        // Initialize views
        cardWeightLoss = findViewById(R.id.cardWeightLoss);
        cardMuscleGain = findViewById(R.id.cardMuscleGain);
        cardEndurance = findViewById(R.id.cardEndurance);
        cardGeneralFitness = findViewById(R.id.cardGeneralFitness);
        btnNext = findViewById(R.id.btnNext);

        // Set click listeners for fitness goal selection
        cardWeightLoss.setOnClickListener(v -> { selectedFitnessGoal = "Weight Loss"; updateCardSelection(); });
        cardMuscleGain.setOnClickListener(v -> { selectedFitnessGoal = "Muscle Gain"; updateCardSelection(); });
        cardEndurance.setOnClickListener(v -> { selectedFitnessGoal = "Endurance"; updateCardSelection(); });
        cardGeneralFitness.setOnClickListener(v -> { selectedFitnessGoal = "General Fitness"; updateCardSelection(); });

        // Next button click listener
        btnNext.setOnClickListener(v -> {
            if (!selectedFitnessGoal.isEmpty()) {
                // Save selected goal in UserProfile
                userProfile.setFitnessGoal(selectedFitnessGoal);

                // Pass full UserProfile to next activity (HealthIssues)
                Intent intent = new Intent(FitnessGoal.this, HealthIssues.class);
                intent.putExtra("userProfile", userProfile);
                startActivity(intent);
            }
        });
    }

    private void updateCardSelection() {
        // Reset all cards to default state
        cardWeightLoss.setStrokeColor(getResources().getColor(R.color.white));
        cardMuscleGain.setStrokeColor(getResources().getColor(R.color.white));
        cardEndurance.setStrokeColor(getResources().getColor(R.color.white));
        cardGeneralFitness.setStrokeColor(getResources().getColor(R.color.white));

        // Highlight selected card
        switch (selectedFitnessGoal) {
            case "Weight Loss": cardWeightLoss.setStrokeColor(getResources().getColor(R.color.black)); break;
            case "Muscle Gain": cardMuscleGain.setStrokeColor(getResources().getColor(R.color.black)); break;
            case "Endurance": cardEndurance.setStrokeColor(getResources().getColor(R.color.black)); break;
            case "General Fitness": cardGeneralFitness.setStrokeColor(getResources().getColor(R.color.black)); break;
        }

        // Enable next button
        btnNext.setEnabled(true);
        btnNext.setAlpha(1.0f);
    }
}
