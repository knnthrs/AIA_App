package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class AgeInput extends AppCompatActivity {

    private TextInputEditText etAge;
    private Button btnNext;
    private UserProfileHelper.UserProfile userProfile; // ✅ full profile

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_age_input);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ✅ Get the UserProfile from previous activity (Gender selection)
        userProfile = (UserProfileHelper.UserProfile) getIntent().getSerializableExtra("userProfile");
        if (userProfile == null) {
            // fallback if somehow missing
            userProfile = new UserProfileHelper.UserProfile();
            userProfile.setHealthIssues(new ArrayList<>());
            userProfile.setFitnessGoal("general fitness");
            userProfile.setFitnessLevel("beginner");
        }

        // Initialize views
        etAge = findViewById(R.id.etAge);
        btnNext = findViewById(R.id.btnNext);

        // Add TextWatcher to enable/disable button based on input
        etAge.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) { validateInput(); }
        });

        // Next button click listener
        btnNext.setOnClickListener(v -> {
            String ageText = etAge.getText().toString().trim();

            if (ageText.isEmpty()) {
                etAge.setError("Please enter your age");
                etAge.requestFocus();
                return;
            }

            try {
                int age = Integer.parseInt(ageText);
                if (age < 13 || age > 120) {
                    etAge.setError("Please enter a valid age (13-120)");
                    etAge.requestFocus();
                    return;
                }

                // ✅ Save age into full UserProfile
                userProfile.setAge(age);

                Intent intent = new Intent(AgeInput.this, HeightWeightInput.class);
                intent.putExtra("userProfile", userProfile);
                startActivity(intent);


            } catch (NumberFormatException e) {
                etAge.setError("Please enter a valid number");
                etAge.requestFocus();
            }
        });
    }

    // ✅ Correctly placed OUTSIDE onCreate()
    private void validateInput() {
        String ageText = etAge.getText().toString().trim();
        boolean isValid = false;

        if (!ageText.isEmpty()) {
            try {
                int age = Integer.parseInt(ageText);
                if (age >= 13 && age <= 120) {
                    isValid = true;
                }
            } catch (NumberFormatException e) {
                isValid = false;
            }
        }

        // Enable/disable button based on validation
        btnNext.setEnabled(isValid);
        btnNext.setAlpha(isValid ? 1.0f : 0.5f);
    }
}
