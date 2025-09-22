package com.example.signuploginrealtime.UserInfo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.signuploginrealtime.R;
import com.example.signuploginrealtime.models.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class activity_select_workout_frequency extends AppCompatActivity {

    private MaterialCardView card1Day, card2Days, card3Days, card4Days, card5Days, card6Days, card7Days;
    private MaterialButton btnNext;
    private UserProfile userProfile;
    private int selectedFrequency = -1;
    private MaterialCardView selectedCard = null;

    // Add Firestore instance
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_workout_frequency);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get UserProfile from intent
        userProfile = (UserProfile) getIntent().getSerializableExtra("userProfile");

        // Initialize views
        initializeViews();

        // Set up click listeners
        setupCardClickListeners();

        // Set up next button
        btnNext.setOnClickListener(v -> {
            if (selectedFrequency == -1) {
                Toast.makeText(this, "Please select a workout frequency", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save to UserProfile
            userProfile.setWorkoutDaysPerWeek(selectedFrequency);

            // Save to Firestore immediately
            saveWorkoutFrequencyToFirestore(selectedFrequency);

            // Move to HealthIssues activity
            Intent intent = new Intent(activity_select_workout_frequency.this, HealthIssues.class);
            intent.putExtra("userProfile", userProfile);
            startActivity(intent);
        });
    }

    private void initializeViews() {
        card1Day = findViewById(R.id.card1Day);
        card2Days = findViewById(R.id.card2Days);
        card3Days = findViewById(R.id.card3Days);
        card4Days = findViewById(R.id.card4Days);
        card5Days = findViewById(R.id.card5Days);
        card6Days = findViewById(R.id.card6Days);
        card7Days = findViewById(R.id.card7Days);
        btnNext = findViewById(R.id.btnNext);
    }

    private void setupCardClickListeners() {
        card1Day.setOnClickListener(v -> selectCard(card1Day, 1));
        card2Days.setOnClickListener(v -> selectCard(card2Days, 2));
        card3Days.setOnClickListener(v -> selectCard(card3Days, 3));
        card4Days.setOnClickListener(v -> selectCard(card4Days, 4));
        card5Days.setOnClickListener(v -> selectCard(card5Days, 5));
        card6Days.setOnClickListener(v -> selectCard(card6Days, 6));
        card7Days.setOnClickListener(v -> selectCard(card7Days, 7));
    }

    private void selectCard(MaterialCardView card, int frequency) {
        // Reset previous selection
        if (selectedCard != null) {
            resetCardAppearance(selectedCard);
        }

        // Set new selection
        selectedCard = card;
        selectedFrequency = frequency;

        // Update card appearance
        setSelectedCardAppearance(card);

        // Enable next button
        btnNext.setEnabled(true);
        btnNext.setAlpha(1.0f);
    }

    private void setSelectedCardAppearance(MaterialCardView card) {
        // Change stroke color to indicate selection (you can customize these colors)
        card.setStrokeColor(ContextCompat.getColor(this, R.color.black));
        card.setStrokeWidth(4); // Make stroke thicker for selected state
        card.setCardElevation(8f); // Increase elevation for selected state
    }

    private void resetCardAppearance(MaterialCardView card) {
        // Reset to default appearance
        card.setStrokeColor(ContextCompat.getColor(this, R.color.white));
        card.setStrokeWidth(2); // Default stroke width
        card.setCardElevation(2f); // Default elevation
    }

    // New method to save workout frequency to Firestore
    private void saveWorkoutFrequencyToFirestore(int frequency) {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();

            db.collection("users")
                    .document(userId)
                    .update("workoutDaysPerWeek", frequency)
                    .addOnSuccessListener(aVoid -> {
                        // Successfully updated
                        Toast.makeText(this, "Workout frequency saved!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // Handle error
                        Toast.makeText(this, "Error saving workout frequency", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}