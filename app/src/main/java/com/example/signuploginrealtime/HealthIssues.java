package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class HealthIssues extends AppCompatActivity {

    private CheckBox cbJointPain, cbBackPain, cbHeartCondition, cbHighBloodPressure, cbRespiratoryIssues, cbNone, cbOther;
    private EditText etOther;
    private Button btnNext;

    private UserProfileHelper.UserProfile userProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_health_issues);

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
            userProfile.setFitnessGoal("general fitness");
            userProfile.setFitnessLevel("beginner");
        }

        // Initialize checkboxes
        cbJointPain = findViewById(R.id.cbJointProblems);
        cbBackPain = findViewById(R.id.cbBackProblems);
        cbHeartCondition = findViewById(R.id.cbHeartProblems);
        cbHighBloodPressure = findViewById(R.id.cbBloodPressure);
        cbRespiratoryIssues = findViewById(R.id.cbRespiratory);
        cbNone = findViewById(R.id.cbNone);
        cbOther = findViewById(R.id.cbOther);
        etOther = findViewById(R.id.etOther);

        // Initialize button
        btnNext = findViewById(R.id.btnComplete);

        // Show/hide EditText when "Other" is checked
        cbOther.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etOther.setVisibility(isChecked ? EditText.VISIBLE : EditText.GONE);
            updateButtonState();
        });

        // Handle "None" selection → uncheck others
        cbNone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cbJointPain.setChecked(false);
                cbBackPain.setChecked(false);
                cbHeartCondition.setChecked(false);
                cbHighBloodPressure.setChecked(false);
                cbRespiratoryIssues.setChecked(false);
                cbOther.setChecked(false);
            }
            updateButtonState();
        });

        // Update button state for all other checkboxes
        cbJointPain.setOnCheckedChangeListener((buttonView, isChecked) -> updateButtonState());
        cbBackPain.setOnCheckedChangeListener((buttonView, isChecked) -> updateButtonState());
        cbHeartCondition.setOnCheckedChangeListener((buttonView, isChecked) -> updateButtonState());
        cbHighBloodPressure.setOnCheckedChangeListener((buttonView, isChecked) -> updateButtonState());
        cbRespiratoryIssues.setOnCheckedChangeListener((buttonView, isChecked) -> updateButtonState());

        // Set button click listener
        btnNext.setOnClickListener(v -> {
            ArrayList<String> healthIssues = new ArrayList<>();

            if (cbJointPain.isChecked()) healthIssues.add("Joint Pain");
            if (cbBackPain.isChecked()) healthIssues.add("Back Pain");
            if (cbHeartCondition.isChecked()) healthIssues.add("Heart Condition");
            if (cbHighBloodPressure.isChecked()) healthIssues.add("High Blood Pressure");
            if (cbRespiratoryIssues.isChecked()) healthIssues.add("Respiratory Issues");

            if (cbOther.isChecked()) {
                String otherInput = etOther.getText().toString().trim();
                if (!otherInput.isEmpty()) {
                    healthIssues.add(otherInput);
                }
            }

            if (cbNone.isChecked() && healthIssues.isEmpty()) {
                healthIssues.add("None");
            }

            if (healthIssues.isEmpty()) {
                Toast.makeText(this, "Please select at least one option", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save health issues into full UserProfile
            userProfile.setHealthIssues(healthIssues);

            // Pass full UserProfile to WorkoutList
            Intent intent = new Intent(HealthIssues.this, WorkoutList.class);
            intent.putExtra("userProfile", userProfile);
            startActivity(intent);
        });

        // Initialize button state
        updateButtonState();
    }

    // Helper method to enable/disable button based on selection
    private void updateButtonState() {
        boolean anyChecked = cbJointPain.isChecked() || cbBackPain.isChecked() || cbHeartCondition.isChecked()
                || cbHighBloodPressure.isChecked() || cbRespiratoryIssues.isChecked() || cbOther.isChecked() || cbNone.isChecked();
        btnNext.setEnabled(anyChecked);
        btnNext.setAlpha(anyChecked ? 1f : 0.5f);
    }
}
