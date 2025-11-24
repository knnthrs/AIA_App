package com.example.signuploginrealtime.UserInfo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.signuploginrealtime.R;
import com.example.signuploginrealtime.models.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class BodyFocusSelection extends AppCompatActivity {

    private MaterialCardView cardChest, cardBack, cardShoulders, cardArms, cardLegs, cardAbs;
    private CheckBox cbChest, cbBack, cbShoulders, cbArms, cbLegs, cbAbs;
    private MaterialButton btnNext;
    private UserProfile userProfile;
    private List<String> selectedBodyFocus = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_body_focus);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get UserProfile from intent
        userProfile = (UserProfile) getIntent().getSerializableExtra("userProfile");
        if (userProfile == null) {
            userProfile = new UserProfile();
        }

        // Initialize views
        initializeViews();

        // Set up click listeners
        setupCardClickListeners();

        // Set up next button
        btnNext.setOnClickListener(v -> {
            if (selectedBodyFocus.isEmpty()) {
                Toast.makeText(this, "Please select at least one body part", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save to UserProfile
            userProfile.setBodyFocus(selectedBodyFocus);

            // Save to Firestore immediately
            saveBodyFocusToFirestore(selectedBodyFocus);

            // Move to HealthIssues activity
            Intent intent = new Intent(BodyFocusSelection.this, HealthIssues.class);
            intent.putExtra("userProfile", userProfile);
            startActivity(intent);
        });
    }

    private void initializeViews() {
        cardChest = findViewById(R.id.cardChest);
        cardBack = findViewById(R.id.cardBack);
        cardShoulders = findViewById(R.id.cardShoulders);
        cardArms = findViewById(R.id.cardArms);
        cardLegs = findViewById(R.id.cardLegs);
        cardAbs = findViewById(R.id.cardAbs);

        cbChest = findViewById(R.id.cbChest);
        cbBack = findViewById(R.id.cbBack);
        cbShoulders = findViewById(R.id.cbShoulders);
        cbArms = findViewById(R.id.cbArms);
        cbLegs = findViewById(R.id.cbLegs);
        cbAbs = findViewById(R.id.cbAbs);

        btnNext = findViewById(R.id.btnNext);
    }

    private void setupCardClickListeners() {
        cardChest.setOnClickListener(v -> toggleSelection(cardChest, cbChest, "Chest"));
        cardBack.setOnClickListener(v -> toggleSelection(cardBack, cbBack, "Back"));
        cardShoulders.setOnClickListener(v -> toggleSelection(cardShoulders, cbShoulders, "Shoulders"));
        cardArms.setOnClickListener(v -> toggleSelection(cardArms, cbArms, "Arms"));
        cardLegs.setOnClickListener(v -> toggleSelection(cardLegs, cbLegs, "Legs"));
        cardAbs.setOnClickListener(v -> toggleSelection(cardAbs, cbAbs, "Abs"));
    }

    private void toggleSelection(MaterialCardView card, CheckBox checkBox, String bodyPart) {
        if (checkBox.isChecked()) {
            // Deselect
            checkBox.setChecked(false);
            selectedBodyFocus.remove(bodyPart);
            resetCardAppearance(card);
        } else {
            // Select
            checkBox.setChecked(true);
            selectedBodyFocus.add(bodyPart);
            setSelectedCardAppearance(card);
        }

        // Update button state
        updateButtonState();
    }

    private void setSelectedCardAppearance(MaterialCardView card) {
        card.setStrokeColor(ContextCompat.getColor(this, R.color.black));
        card.setStrokeWidth(4);
        card.setCardElevation(8f);
    }

    private void resetCardAppearance(MaterialCardView card) {
        card.setStrokeColor(ContextCompat.getColor(this, R.color.white));
        card.setStrokeWidth(2);
        card.setCardElevation(2f);
    }

    private void updateButtonState() {
        boolean hasSelection = !selectedBodyFocus.isEmpty();
        btnNext.setEnabled(hasSelection);
        btnNext.setAlpha(hasSelection ? 1.0f : 0.5f);
    }

    private void saveBodyFocusToFirestore(List<String> bodyFocus) {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();

            db.collection("users")
                    .document(userId)
                    .update("bodyFocus", bodyFocus)
                    .addOnSuccessListener(aVoid -> {
                        // Successfully updated
                    })
                    .addOnFailureListener(e -> {
                        // Handle error silently
                    });
        }
    }
}

