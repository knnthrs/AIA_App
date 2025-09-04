package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

public class HeightWeightInput extends AppCompatActivity {

    private TextInputEditText etHeight, etWeight;
    private Button btnNext;
    private UserProfileHelper.UserProfile userProfile; // full profile object

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_height_weight_input);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get full UserProfile from previous activity
        userProfile = (UserProfileHelper.UserProfile) getIntent().getSerializableExtra("userProfile");
        if (userProfile == null) {
            userProfile = new UserProfileHelper.UserProfile();
        }

        // Initialize views
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);
        btnNext = findViewById(R.id.btnNext);

        // Add TextWatchers to both fields
        TextWatcher inputWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) { validateInputs(); }
        };

        etHeight.addTextChangedListener(inputWatcher);
        etWeight.addTextChangedListener(inputWatcher);

        // Next button click listener
        btnNext.setOnClickListener(v -> {
            String heightText = etHeight.getText().toString().trim();
            String weightText = etWeight.getText().toString().trim();

            if (heightText.isEmpty()) {
                etHeight.setError("Please enter your height");
                etHeight.requestFocus();
                return;
            }

            if (weightText.isEmpty()) {
                etWeight.setError("Please enter your weight");
                etWeight.requestFocus();
                return;
            }

            try {
                float height = Float.parseFloat(heightText);
                float weight = Float.parseFloat(weightText);

                if (height <= 0 || height > 300) {
                    etHeight.setError("Please enter a valid height (1-300 cm)");
                    etHeight.requestFocus();
                    return;
                }

                if (weight <= 0 || weight > 500) {
                    etWeight.setError("Please enter a valid weight (1-500 kg)");
                    etWeight.requestFocus();
                    return;
                }

                // ✅ Save height and weight into userProfile
                userProfile.setHeight(height);
                userProfile.setWeight(weight);

                // ✅ Pass full userProfile to next activity (FitnessLevel)
                Intent intent = new Intent(HeightWeightInput.this, FitnessLevel.class);
                intent.putExtra("userProfile", userProfile);
                startActivity(intent);

            } catch (NumberFormatException e) {
                etHeight.setError("Please enter valid numbers");
                etWeight.setError("Please enter valid numbers");
            }
        });
    }

    private void validateInputs() {
        String heightText = etHeight.getText().toString().trim();
        String weightText = etWeight.getText().toString().trim();

        boolean isValid = false;

        if (!heightText.isEmpty() && !weightText.isEmpty()) {
            try {
                float height = Float.parseFloat(heightText);
                float weight = Float.parseFloat(weightText);
                if (height > 0 && height <= 300 && weight > 0 && weight <= 500) {
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
