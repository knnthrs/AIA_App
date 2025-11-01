package com.example.signuploginrealtime.UserInfo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.signuploginrealtime.LoginActivity;
import com.example.signuploginrealtime.R;
import com.example.signuploginrealtime.models.UserProfile;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class GenderSelection extends AppCompatActivity {

    private MaterialCardView cardMale, cardFemale;
    private Button btnNext;
    private String selectedGender = "";
    private UserProfile userProfile; // ✅ full profile

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gender_selection);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ✅ Initialize UserProfile
        userProfile = (UserProfile) getIntent().getSerializableExtra("userProfile");
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setHealthIssues(new ArrayList<>());
            userProfile.setFitnessGoal("general fitness");
            userProfile.setFitnessLevel("beginner");
        }

        // Initialize views
        cardMale = findViewById(R.id.cardMale);
        cardFemale = findViewById(R.id.cardFemale);
        btnNext = findViewById(R.id.btnNext);

        // Set click listeners for gender selection
        cardMale.setOnClickListener(v -> {
            selectedGender = "Male";
            updateCardSelection();
        });

        cardFemale.setOnClickListener(v -> {
            selectedGender = "Female";
            updateCardSelection();
        });

        // Next button click listener
        btnNext.setOnClickListener(v -> {
            if (!selectedGender.isEmpty()) {
                // ✅ Save gender into full UserProfile
                userProfile.setGender(selectedGender);

                // Pass full UserProfile to next activity (AgeInput)
                Intent intent = new Intent(GenderSelection.this, AgeInput.class);
                intent.putExtra("userProfile", userProfile);
                startActivity(intent);
            }
        });

        // Migrate back handling to OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new androidx.appcompat.app.AlertDialog.Builder(GenderSelection.this)
                        .setTitle("Exit Setup?")
                        .setMessage("Your profile setup is not complete. Do you want to exit to login?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(GenderSelection.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
    }

    private void updateCardSelection() {
        // Reset all cards to default state
        cardMale.setStrokeColor(getResources().getColor(R.color.white));
        cardFemale.setStrokeColor(getResources().getColor(R.color.white));

        // Highlight selected card
        if (selectedGender.equals("Male")) {
            cardMale.setStrokeColor(getResources().getColor(R.color.black));
        } else if (selectedGender.equals("Female")) {
            cardFemale.setStrokeColor(getResources().getColor(R.color.black));
        }

        // Enable next button
        btnNext.setEnabled(true);
        btnNext.setAlpha(1.0f);
    }
}
