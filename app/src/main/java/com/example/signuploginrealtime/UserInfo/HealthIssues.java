package com.example.signuploginrealtime.UserInfo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.signuploginrealtime.MainActivity;
import com.example.signuploginrealtime.R;
import com.example.signuploginrealtime.models.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HealthIssues extends AppCompatActivity {

    private static final String TAG = "HealthIssues";

    private CheckBox cbJointPain, cbBackPain, cbHeartCondition, cbHighBloodPressure, cbRespiratoryIssues, cbNone, cbOther;
    private EditText etOther;
    private Button btnNext;
    private UserProfile userProfile;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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
        db = FirebaseFirestore.getInstance();

        // Get full UserProfile from previous activity
        userProfile = (UserProfile) getIntent().getSerializableExtra("userProfile");
        if (userProfile == null) {
            userProfile = new UserProfile();
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

            // Capture free-text "Other" input
            if (cbOther.isChecked()) {
                String otherInput = etOther.getText().toString().trim();
                if (!otherInput.isEmpty()) {
                    healthIssues.add(otherInput); // Add to health issues list
                    userProfile.setOtherHealthIssue(otherInput); // Save separately for generator use
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

            // Save complete profile to Firestore
            saveUserProfileToFirestore();
        });

        // Initialize button state
        updateButtonState();
    }

    private void saveUserProfileToFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        // Create map with all user profile data
        Map<String, Object> userProfileData = new HashMap<>();


        // Add all profile data
        userProfileData.put("gender", userProfile.getGender());
        userProfileData.put("age", userProfile.getAge());
        userProfileData.put("height", userProfile.getHeight());
        userProfileData.put("weight", userProfile.getWeight());
        userProfileData.put("fitnessLevel", userProfile.getFitnessLevel());
        userProfileData.put("fitnessGoal", userProfile.getFitnessGoal());
        userProfileData.put("healthIssues", userProfile.getHealthIssues());
        userProfileData.put("otherHealthIssue", userProfile.getOtherHealthIssue());
        userProfileData.put("workoutDaysPerWeek", userProfile.getWorkoutDaysPerWeek());
        userProfileData.put("birthdate", userProfile.getBirthdate());




        // Add additional fields with default values
        userProfileData.put("availableEquipment", new ArrayList<>());
        userProfileData.put("dislikedExercises", new ArrayList<>());
        userProfileData.put("hasGymAccess", false);

        // Add timestamp
        userProfileData.put("profileCompletedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        Log.d(TAG, "Saving user profile to Firestore for uid: " + uid);
        Log.d(TAG, "Profile data: " + userProfileData.toString());

        // Disable button to prevent multiple submissions
        btnNext.setEnabled(false);
        btnNext.setText("Saving...");

        // Save to Firestore
        db.collection("users").document(uid)
                .set(userProfileData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile successfully saved to Firestore!");
                    SharedPreferences prefs = getSharedPreferences("user_profile_prefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("profile_complete_firebase", true);
                    editor.apply();

                    Toast.makeText(HealthIssues.this, "Profile setup complete!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(HealthIssues.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user profile to Firestore", e);
                    Toast.makeText(HealthIssues.this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnNext.setEnabled(true);
                    btnNext.setText("Complete Setup");
                });
    }

    // Helper method to enable/disable button based on selection
    private void updateButtonState() {
        boolean anyChecked = cbJointPain.isChecked() || cbBackPain.isChecked() || cbHeartCondition.isChecked()
                || cbHighBloodPressure.isChecked() || cbRespiratoryIssues.isChecked() || cbOther.isChecked() || cbNone.isChecked();
        btnNext.setEnabled(anyChecked);
        btnNext.setAlpha(anyChecked ? 1f : 0.5f);
    }
}
