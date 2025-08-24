package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

public class HeightWeightInput extends AppCompatActivity {

    private TextInputEditText etHeight, etWeight;
    private Button btnNext;
    private String gender;
    private int age;

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

        // Get data from previous activities
        gender = getIntent().getStringExtra("gender");
        age = getIntent().getIntExtra("age", 0);

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
            public void afterTextChanged(Editable s) {
                validateInputs();
            }
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

                Intent intent = new Intent(HeightWeightInput.this, FitnessLevel.class);
                intent.putExtra("gender", gender);
                intent.putExtra("age", age);
                intent.putExtra("height", height);
                intent.putExtra("weight", weight);
                startActivity(intent);
                // REMOVED finish(); to allow back navigation

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

                // Check if both values are within valid ranges
                if (height > 0 && height <= 300 && weight > 0 && weight <= 500) {
                    isValid = true;
                }
            } catch (NumberFormatException e) {
                isValid = false;
            }
        }

        // Enable/disable button based on validation
        if (isValid) {
            btnNext.setEnabled(true);
            btnNext.setAlpha(1.0f);
        } else {
            btnNext.setEnabled(false);
            btnNext.setAlpha(0.5f);
        }
    }
}