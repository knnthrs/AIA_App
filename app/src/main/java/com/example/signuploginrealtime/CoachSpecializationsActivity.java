package com.example.signuploginrealtime;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoachSpecializationsActivity extends AppCompatActivity {

    private ChipGroup chipGroupSelected, chipGroupAvailable;
    private TextView tvEmptyState;
    private TextInputEditText etExperience;
    private MaterialButton btnSave;
    private ImageView btnBack;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String coachId;
    private List<String> selectedSkills = new ArrayList<>();

    // All available specializations
    private final List<String> ALL_SKILLS = Arrays.asList(
            // Weight Management
            "Weight Loss",
            "Weight Gain",
            "Body Transformation",

            // Strength & Muscle
            "Strength Training",
            "Muscle Building",
            "Powerlifting",
            "Bodybuilding",

            // Cardio & Endurance
            "Cardio Training",
            "HIIT",
            "Endurance Training",
            "Marathon Training",

            // Functional & CrossFit
            "Functional Training",
            "CrossFit",
            "Circuit Training",

            // Sports Specific
            "Sports Training",
            "Athletic Performance",
            "Speed Training",
            "Agility Training",

            // Flexibility & Recovery
            "Flexibility",
            "Mobility Training",
            "Yoga",
            "Pilates",
            "Stretching",

            // Specialized
            "Rehabilitation",
            "Injury Prevention",
            "Senior Fitness",
            "Youth Training",
            "Prenatal Fitness",
            "Postnatal Fitness",

            // Nutrition & Wellness
            "Nutrition Guidance",
            "Meal Planning",
            "Supplement Advice",

            // General
            "General Fitness"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coach_specializations);

        db = FirebaseFirestore.getInstance();
        coachId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize views
        chipGroupSelected = findViewById(R.id.chipGroupSelected);
        chipGroupAvailable = findViewById(R.id.chipGroupAvailable);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        etExperience = findViewById(R.id.etExperience);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveSpecializations());

        // Load existing data
        loadCoachData();

        // Populate available skills
        populateAvailableSkills();
    }

    private void loadCoachData() {
        db.collection("coaches").document(coachId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Load skills
                        List<String> skills = (List<String>) documentSnapshot.get("skills");
                        if (skills != null && !skills.isEmpty()) {
                            selectedSkills.addAll(skills);
                            updateSelectedChips();
                        }

                        // Load experience
                        Long experience = documentSnapshot.getLong("yearsOfExperience");
                        if (experience != null && etExperience != null) {
                            etExperience.setText(String.valueOf(experience));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void populateAvailableSkills() {
        chipGroupAvailable.removeAllViews();

        for (String skill : ALL_SKILLS) {
            Chip chip = new Chip(this);
            chip.setText(skill);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(android.R.color.white);
            chip.setChipStrokeColorResource(R.color.black);
            chip.setChipStrokeWidth(2f);
            chip.setTextColor(Color.BLACK);
            chip.setTextSize(14);

            // Check if already selected
            chip.setChecked(selectedSkills.contains(skill));

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!selectedSkills.contains(skill)) {
                        selectedSkills.add(skill);
                    }
                } else {
                    selectedSkills.remove(skill);
                }
                updateSelectedChips();
            });

            chipGroupAvailable.addView(chip);
        }
    }

    private void updateSelectedChips() {
        chipGroupSelected.removeAllViews();

        if (selectedSkills.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);

            for (String skill : selectedSkills) {
                Chip chip = new Chip(this);
                chip.setText(skill);
                chip.setCloseIconVisible(true);
                chip.setChipBackgroundColorResource(R.color.black);
                chip.setTextColor(Color.WHITE);
                chip.setCloseIconTintResource(android.R.color.white);
                chip.setTextSize(14);

                chip.setOnCloseIconClickListener(v -> {
                    selectedSkills.remove(skill);
                    updateSelectedChips();
                    // Update available chips
                    populateAvailableSkills();
                });

                chipGroupSelected.addView(chip);
            }
        }
    }

    private void saveSpecializations() {
        if (selectedSkills.isEmpty()) {
            Toast.makeText(this, "Please select at least one specialization", Toast.LENGTH_SHORT).show();
            return;
        }

        String experienceText = etExperience.getText() != null ? etExperience.getText().toString().trim() : "";
        int experience = 0;

        if (!experienceText.isEmpty()) {
            try {
                experience = Integer.parseInt(experienceText);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid number for years of experience", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("skills", selectedSkills);
        updates.put("yearsOfExperience", experience);

        db.collection("coaches").document(coachId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Specializations saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Changes");
                });
    }
}

