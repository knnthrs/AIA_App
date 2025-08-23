package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;

public class GenderSelection extends AppCompatActivity {

    private MaterialCardView cardMale, cardFemale;
    private Button btnNext;
    private String selectedGender = "";

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
                Intent intent = new Intent(GenderSelection.this, AgeInput.class);
                intent.putExtra("gender", selectedGender);
                startActivity(intent);
                finish();
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