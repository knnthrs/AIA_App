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

import java.util.ArrayList;
import java.util.List;

public class HealthIssues extends AppCompatActivity {

    private CheckBox cbNone, cbDiabetes, cbHeartProblems, cbBloodPressure,
            cbJointProblems, cbBackProblems, cbRespiratory, cbOther;
    private Button btnComplete;
    private String gender, fitnessLevel, fitnessGoal;
    private int age;
    private float height, weight;

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
        });

        // Handle other checkboxes
        CheckBox[] otherCheckboxes = {cbDiabetes, cbHeartProblems, cbBloodPressure,
                cbJointProblems, cbBackProblems, cbRespiratory, cbOther};

        for (CheckBox cb : otherCheckboxes) {
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    cbNone.setChecked(false);
                }
            });
        }

        // Complete button click listener
        btnComplete.setOnClickListener(v -> {
            List<String> healthIssues = getSelectedHealthIssues();

            // Save all user data (you can implement saving to SharedPreferences or Firebase here)
            saveUserProfile(healthIssues);

            // Navigate to MainActivity
            Intent intent = new Intent(HealthIssues.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
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

    private void saveUserProfile(List<String> healthIssues) {
        // TODO: Implement saving user profile data to your preferred storage
        // This could be SharedPreferences, Room database, or Firebase

        // Example with SharedPreferences:
        /*
        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("gender", gender);
        editor.putInt("age", age);
        editor.putFloat("height", height);
        editor.putFloat("weight", weight);
        editor.putString("fitnessLevel", fitnessLevel);
        editor.putString("fitnessGoal", fitnessGoal);
        editor.putStringSet("healthIssues", new HashSet<>(healthIssues));
        editor.putBoolean("profileCompleted", true);
        editor.apply();
        */

        Toast.makeText(this, "Profile setup completed!", Toast.LENGTH_SHORT).show();
    }
}