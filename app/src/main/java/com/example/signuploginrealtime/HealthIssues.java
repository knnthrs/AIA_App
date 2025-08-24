package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HealthIssues extends AppCompatActivity {

    private CheckBox cbNone, cbDiabetes, cbHeartProblems, cbBloodPressure,
            cbJointProblems, cbBackProblems, cbRespiratory, cbOther;
    private Button btnComplete;
    private String gender, fitnessLevel, fitnessGoal;
    private int age;
    private float height, weight;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

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

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Get data from previous activities
        gender = getIntent().getStringExtra("gender");
        age = getIntent().getIntExtra("age", 0);
        height = getIntent().getFloatExtra("height", 0);
        weight = getIntent().getFloatExtra("weight", 0);
        fitnessLevel = getIntent().getStringExtra("fitnessLevel");
        fitnessGoal = getIntent().getStringExtra("fitnessGoal");

        // Initialize views
        cbNone = findViewById(R.id.cbNone);
        cbDiabetes = findViewById(R.id.cbDiabetes);
        cbHeartProblems = findViewById(R.id.cbHeartProblems);
        cbBloodPressure = findViewById(R.id.cbBloodPressure);
        cbJointProblems = findViewById(R.id.cbJointProblems);
        cbBackProblems = findViewById(R.id.cbBackProblems);
        cbRespiratory = findViewById(R.id.cbRespiratory);
        cbOther = findViewById(R.id.cbOther);
        btnComplete = findViewById(R.id.btnComplete);

        // Handle "None" checkbox logic
        cbNone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Uncheck all other checkboxes
                cbDiabetes.setChecked(false);
                cbHeartProblems.setChecked(false);
                cbBloodPressure.setChecked(false);
                cbJointProblems.setChecked(false);
                cbBackProblems.setChecked(false);
                cbRespiratory.setChecked(false);
                cbOther.setChecked(false);
            }
            // Check if button should be enabled
            validateSelection();
        });

        // Handle other checkboxes
        CheckBox[] otherCheckboxes = {cbDiabetes, cbHeartProblems, cbBloodPressure,
                cbJointProblems, cbBackProblems, cbRespiratory, cbOther};

        for (CheckBox cb : otherCheckboxes) {
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    cbNone.setChecked(false);
                }
                // Check if button should be enabled
                validateSelection();
            });
        }

        // Complete button click listener
        btnComplete.setOnClickListener(v -> {
            List<String> healthIssues = getSelectedHealthIssues();
            saveUserProfileToFirebase(healthIssues);
        });
    }

    // NEW METHOD: Validate if any checkbox is selected
    private void validateSelection() {
        boolean hasSelection = cbNone.isChecked() ||
                cbDiabetes.isChecked() ||
                cbHeartProblems.isChecked() ||
                cbBloodPressure.isChecked() ||
                cbJointProblems.isChecked() ||
                cbBackProblems.isChecked() ||
                cbRespiratory.isChecked() ||
                cbOther.isChecked();

        if (hasSelection) {
            btnComplete.setEnabled(true);
            btnComplete.setAlpha(1.0f);
        } else {
            btnComplete.setEnabled(false);
            btnComplete.setAlpha(0.5f);
        }
    }

    private List<String> getSelectedHealthIssues() {
        List<String> healthIssues = new ArrayList<>();

        if (cbNone.isChecked()) {
            healthIssues.add("None");
        } else {
            if (cbDiabetes.isChecked()) healthIssues.add("Diabetes");
            if (cbHeartProblems.isChecked()) healthIssues.add("Heart Problems");
            if (cbBloodPressure.isChecked()) healthIssues.add("High Blood Pressure");
            if (cbJointProblems.isChecked()) healthIssues.add("Joint/Bone Problems");
            if (cbBackProblems.isChecked()) healthIssues.add("Back Problems");
            if (cbRespiratory.isChecked()) healthIssues.add("Respiratory Issues");
            if (cbOther.isChecked()) healthIssues.add("Other");
        }

        return healthIssues;
    }

    private void saveUserProfileToFirebase(List<String> healthIssues) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // Create user profile data
            Map<String, Object> userProfile = new HashMap<>();
            userProfile.put("gender", gender);
            userProfile.put("age", age);
            userProfile.put("height", height);
            userProfile.put("weight", weight);
            userProfile.put("fitnessLevel", fitnessLevel);
            userProfile.put("fitnessGoal", fitnessGoal);
            userProfile.put("healthIssues", healthIssues);
            userProfile.put("profileCompleted", true);
            userProfile.put("lastUpdated", System.currentTimeMillis());

            // Calculate BMI
            float bmi = calculateBMI(height, weight);
            userProfile.put("bmi", bmi);

            // Save to Firebase under users/{userId}/profile
            mDatabase.child("users").child(userId).child("profile")
                    .setValue(userProfile)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(HealthIssues.this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();

                        // MODIFIED: Navigate to WorkoutDisplayActivity instead of MainActivity
                        Intent intent = new Intent(HealthIssues.this, WorkoutDisplayActivity.class);

                        // Pass all the profile data to WorkoutDisplayActivity
                        intent.putExtra("fitness_goal", fitnessGoal);
                        intent.putExtra("fitness_level", fitnessLevel);
                        intent.putExtra("gender", gender);
                        intent.putExtra("age", age);
                        intent.putExtra("weight", (double) weight);  // Cast to double
                        intent.putExtra("height", (double) height);  // Cast to double

                        // Optional: Pass health issues for future use
                        intent.putStringArrayListExtra("health_issues", new ArrayList<>(healthIssues));

                        // Clear the back stack so user can't go back to setup screens
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(HealthIssues.this, "Failed to save profile: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        } else {
            Toast.makeText(this, "User not authenticated. Please login again.", Toast.LENGTH_LONG).show();
            // Redirect to login activity
            Intent intent = new Intent(HealthIssues.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private float calculateBMI(float height, float weight) {
        // Assuming height is in cm and weight is in kg
        float heightInMeters = height / 100;
        return weight / (heightInMeters * heightInMeters);
    }
}