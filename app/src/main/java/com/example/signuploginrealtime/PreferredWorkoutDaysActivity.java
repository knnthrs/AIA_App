package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.signuploginrealtime.UserInfo.BodyFocusSelection;
import com.example.signuploginrealtime.models.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreferredWorkoutDaysActivity extends AppCompatActivity {

    private CardView cardMonday, cardTuesday, cardWednesday, cardThursday, cardFriday, cardSaturday, cardSunday;
    private CheckBox checkboxMonday, checkboxTuesday, checkboxWednesday, checkboxThursday, checkboxFriday, checkboxSaturday, checkboxSunday;
    private Button btnSave;
    private ImageView backButton;
    private TextView tvTitle, tvSubtitle;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private int requiredDays = -1; // How many days user must select (-1 means any number)
    private boolean fromOnboarding = false; // Whether this is part of onboarding flow
    private UserProfile userProfile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferred_workout_days);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get extras from intent
        requiredDays = getIntent().getIntExtra("requiredDays", -1);
        fromOnboarding = getIntent().getBooleanExtra("fromOnboarding", false);
        userProfile = (UserProfile) getIntent().getSerializableExtra("userProfile");

        // Bind views
        backButton = findViewById(R.id.back_button);
        btnSave = findViewById(R.id.btn_save);
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);

        cardMonday = findViewById(R.id.card_monday);
        cardTuesday = findViewById(R.id.card_tuesday);
        cardWednesday = findViewById(R.id.card_wednesday);
        cardThursday = findViewById(R.id.card_thursday);
        cardFriday = findViewById(R.id.card_friday);
        cardSaturday = findViewById(R.id.card_saturday);
        cardSunday = findViewById(R.id.card_sunday);

        checkboxMonday = findViewById(R.id.checkbox_monday);
        checkboxTuesday = findViewById(R.id.checkbox_tuesday);
        checkboxWednesday = findViewById(R.id.checkbox_wednesday);
        checkboxThursday = findViewById(R.id.checkbox_thursday);
        checkboxFriday = findViewById(R.id.checkbox_friday);
        checkboxSaturday = findViewById(R.id.checkbox_saturday);
        checkboxSunday = findViewById(R.id.checkbox_sunday);

        // Update UI based on required days
        updateUIForRequiredDays();

        // Setup card click listeners
        setupCardClickListeners();

        // If 7 days are required, auto-select all days
        if (requiredDays == 7) {
            selectAllDays();
        }

        // Back button
        backButton.setOnClickListener(v -> finish());

        // Load existing preferences if not from onboarding
        if (!fromOnboarding) {
            loadExistingPreferences();
        }

        // Save button
        btnSave.setOnClickListener(v -> savePreferences());
    }

    private void updateUIForRequiredDays() {
        if (requiredDays > 0) {
            if (requiredDays == 7) {
                tvSubtitle.setText("All days selected for your 7-day workout plan");
                btnSave.setText("Continue");
            } else {
                tvSubtitle.setText("Select exactly " + requiredDays + " day(s) for your workout plan");
                btnSave.setText("Continue");
            }
        } else {
            tvSubtitle.setText("Select the days you prefer to exercise");
            btnSave.setText("Save Preferred Days");
        }
    }

    private void setupCardClickListeners() {
        cardMonday.setOnClickListener(v -> toggleCheckbox(checkboxMonday, cardMonday));
        cardTuesday.setOnClickListener(v -> toggleCheckbox(checkboxTuesday, cardTuesday));
        cardWednesday.setOnClickListener(v -> toggleCheckbox(checkboxWednesday, cardWednesday));
        cardThursday.setOnClickListener(v -> toggleCheckbox(checkboxThursday, cardThursday));
        cardFriday.setOnClickListener(v -> toggleCheckbox(checkboxFriday, cardFriday));
        cardSaturday.setOnClickListener(v -> toggleCheckbox(checkboxSaturday, cardSaturday));
        cardSunday.setOnClickListener(v -> toggleCheckbox(checkboxSunday, cardSunday));
    }

    private void toggleCheckbox(CheckBox checkbox, CardView card) {
        // If 7 days required, don't allow deselection
        if (requiredDays == 7) {
            Toast.makeText(this, "All 7 days are required for your workout plan", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean newState = !checkbox.isChecked();

        // Check if we're trying to select a day
        if (newState && requiredDays > 0) {
            // Count currently selected days
            int currentlySelected = countSelectedDays();

            // If already at limit, show message and don't allow selection
            if (currentlySelected >= requiredDays) {
                Toast.makeText(this, "You can only select " + requiredDays + " day(s)", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        checkbox.setChecked(newState);
        updateCardBackground(card, newState);

        // Update the enabled state of all checkboxes
        updateCheckboxStates();
    }

    private int countSelectedDays() {
        int count = 0;
        if (checkboxMonday.isChecked()) count++;
        if (checkboxTuesday.isChecked()) count++;
        if (checkboxWednesday.isChecked()) count++;
        if (checkboxThursday.isChecked()) count++;
        if (checkboxFriday.isChecked()) count++;
        if (checkboxSaturday.isChecked()) count++;
        if (checkboxSunday.isChecked()) count++;
        return count;
    }

    private void updateCheckboxStates() {
        // Only apply blocking if we have a required days constraint
        if (requiredDays <= 0) {
            return; // No restrictions in profile edit mode
        }

        int selectedCount = countSelectedDays();
        boolean atLimit = selectedCount >= requiredDays;

        // Enable/disable checkboxes and update card appearance
        updateCheckboxAndCard(checkboxMonday, cardMonday, atLimit);
        updateCheckboxAndCard(checkboxTuesday, cardTuesday, atLimit);
        updateCheckboxAndCard(checkboxWednesday, cardWednesday, atLimit);
        updateCheckboxAndCard(checkboxThursday, cardThursday, atLimit);
        updateCheckboxAndCard(checkboxFriday, cardFriday, atLimit);
        updateCheckboxAndCard(checkboxSaturday, cardSaturday, atLimit);
        updateCheckboxAndCard(checkboxSunday, cardSunday, atLimit);
    }

    private void updateCheckboxAndCard(CheckBox checkbox, CardView card, boolean atLimit) {
        if (checkbox.isChecked()) {
            // Already selected - keep it enabled so user can deselect
            checkbox.setEnabled(true);
            card.setAlpha(1.0f);
            card.setClickable(true);
        } else {
            // Not selected - disable if we're at the limit
            if (atLimit) {
                checkbox.setEnabled(false);
                card.setAlpha(0.3f); // Make it look disabled
                card.setClickable(false);
            } else {
                checkbox.setEnabled(true);
                card.setAlpha(1.0f);
                card.setClickable(true);
            }
        }
    }

    private void updateCardBackground(CardView card, boolean isChecked) {
        if (isChecked) {
            card.setCardBackgroundColor(getResources().getColor(R.color.yellow));
        } else {
            card.setCardBackgroundColor(getResources().getColor(android.R.color.white));
        }
    }

    private void selectAllDays() {
        checkboxMonday.setChecked(true);
        checkboxTuesday.setChecked(true);
        checkboxWednesday.setChecked(true);
        checkboxThursday.setChecked(true);
        checkboxFriday.setChecked(true);
        checkboxSaturday.setChecked(true);
        checkboxSunday.setChecked(true);

        updateCardBackground(cardMonday, true);
        updateCardBackground(cardTuesday, true);
        updateCardBackground(cardWednesday, true);
        updateCardBackground(cardThursday, true);
        updateCardBackground(cardFriday, true);
        updateCardBackground(cardSaturday, true);
        updateCardBackground(cardSunday, true);

        // Update checkbox states (will disable all for 7-day plans)
        updateCheckboxStates();
    }

    private void loadExistingPreferences() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Object field = snapshot.get("preferredWorkoutDays");
                        if (field instanceof List<?>) {
                            List<String> days = new ArrayList<>();
                            for (Object o : (List<?>) field) {
                                if (o != null) days.add(o.toString());
                            }

                            // Set checkboxes based on loaded preferences
                            if (days.contains("Mon")) {
                                checkboxMonday.setChecked(true);
                                updateCardBackground(cardMonday, true);
                            }
                            if (days.contains("Tue")) {
                                checkboxTuesday.setChecked(true);
                                updateCardBackground(cardTuesday, true);
                            }
                            if (days.contains("Wed")) {
                                checkboxWednesday.setChecked(true);
                                updateCardBackground(cardWednesday, true);
                            }
                            if (days.contains("Thu")) {
                                checkboxThursday.setChecked(true);
                                updateCardBackground(cardThursday, true);
                            }
                            if (days.contains("Fri")) {
                                checkboxFriday.setChecked(true);
                                updateCardBackground(cardFriday, true);
                            }
                            if (days.contains("Sat")) {
                                checkboxSaturday.setChecked(true);
                                updateCardBackground(cardSaturday, true);
                            }
                            if (days.contains("Sun")) {
                                checkboxSunday.setChecked(true);
                                updateCardBackground(cardSunday, true);
                            }

                            // Update checkbox states after loading
                            updateCheckboxStates();
                        }
                    }
                });
    }

    private void savePreferences() {
        List<String> selectedDays = new ArrayList<>();

        if (checkboxMonday.isChecked()) selectedDays.add("Mon");
        if (checkboxTuesday.isChecked()) selectedDays.add("Tue");
        if (checkboxWednesday.isChecked()) selectedDays.add("Wed");
        if (checkboxThursday.isChecked()) selectedDays.add("Thu");
        if (checkboxFriday.isChecked()) selectedDays.add("Fri");
        if (checkboxSaturday.isChecked()) selectedDays.add("Sat");
        if (checkboxSunday.isChecked()) selectedDays.add("Sun");

        // Validation based on required days
        if (requiredDays > 0) {
            if (selectedDays.size() != requiredDays) {
                Toast.makeText(this, "Please select exactly " + requiredDays + " day(s)", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            // General validation - at least one day
            if (selectedDays.isEmpty()) {
                Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("preferredWorkoutDays", selectedDays);
        updates.put("workoutDaysPerWeek", selectedDays.size());

        db.collection("users").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Preferred days saved successfully!", Toast.LENGTH_SHORT).show();

                    // If from onboarding, continue to next step (BodyFocusSelection)
                    if (fromOnboarding && userProfile != null) {
                        Intent intent = new Intent(PreferredWorkoutDaysActivity.this, BodyFocusSelection.class);
                        intent.putExtra("userProfile", userProfile);
                        startActivity(intent);
                        finish(); // Close this activity
                    } else {
                        // If from profile editing, just close
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}

