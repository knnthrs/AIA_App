package com.example.signuploginrealtime.UserInfo;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.signuploginrealtime.R;
import com.example.signuploginrealtime.models.UserProfile;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class HeightWeightInput extends AppCompatActivity {

    private TextInputEditText etHeight, etWeight;
    private TextInputLayout tilHeight, tilWeight;
    private Button btnNext;
    private Spinner spinnerHeightUnit, spinnerWeightUnit;
    private UserProfile userProfile;

    private String[] heightUnits = {"cm", "ft", "in"};
    private String[] weightUnits = {"kg", "lbs"};

    private String selectedHeightUnit = "cm";
    private String selectedWeightUnit = "kg";

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
        userProfile = (UserProfile) getIntent().getSerializableExtra("userProfile");
        if (userProfile == null) {
            userProfile = new UserProfile();
        }

        // Initialize views
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);
        tilHeight = findViewById(R.id.tilHeight);
        tilWeight = findViewById(R.id.tilWeight);
        btnNext = findViewById(R.id.btnNext);
        spinnerHeightUnit = findViewById(R.id.spinnerHeightUnit);
        spinnerWeightUnit = findViewById(R.id.spinnerWeightUnit);

        // Setup height unit spinner
        ArrayAdapter<String> heightAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, heightUnits);
        heightAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerHeightUnit.setAdapter(heightAdapter);
        spinnerHeightUnit.setSelection(0); // Default to cm

        // Setup weight unit spinner
        ArrayAdapter<String> weightAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, weightUnits);
        weightAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerWeightUnit.setAdapter(weightAdapter);
        spinnerWeightUnit.setSelection(0); // Default to kg

        // Height unit change listener
        spinnerHeightUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newUnit = heightUnits[position];
                convertHeight(selectedHeightUnit, newUnit);
                selectedHeightUnit = newUnit;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Weight unit change listener
        spinnerWeightUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newUnit = weightUnits[position];
                convertWeight(selectedWeightUnit, newUnit);
                selectedWeightUnit = newUnit;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

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
            if (saveValues()) {
                // Pass full userProfile to next activity (FitnessLevel)
                Intent intent = new Intent(HeightWeightInput.this, FitnessLevel.class);
                intent.putExtra("userProfile", userProfile);
                startActivity(intent);
            }
        });
    }

    private void convertHeight(String fromUnit, String toUnit) {
        if (etHeight.getText() == null || fromUnit.equals(toUnit)) return;

        String currentValue = etHeight.getText().toString().trim();
        if (currentValue.isEmpty()) return;

        try {
            float value = Float.parseFloat(currentValue);
            float convertedValue = value;

            // Convert to cm first (base unit)
            float valueInCm = value;
            switch (fromUnit) {
                case "cm":
                    valueInCm = value;
                    break;
                case "ft":
                    valueInCm = value * 30.48f;
                    break;
                case "in":
                    valueInCm = value * 2.54f;
                    break;
            }

            // Convert from cm to target unit
            switch (toUnit) {
                case "cm":
                    convertedValue = valueInCm;
                    break;
                case "ft":
                    convertedValue = valueInCm / 30.48f;
                    break;
                case "in":
                    convertedValue = valueInCm / 2.54f;
                    break;
            }

            etHeight.setText(String.format(Locale.US, "%.1f", convertedValue));
        } catch (NumberFormatException e) {
            // Ignore invalid input
        }
    }

    private void convertWeight(String fromUnit, String toUnit) {
        if (etWeight.getText() == null || fromUnit.equals(toUnit)) return;

        String currentValue = etWeight.getText().toString().trim();
        if (currentValue.isEmpty()) return;

        try {
            float value = Float.parseFloat(currentValue);
            float convertedValue = value;

            // Convert to kg first (base unit)
            float valueInKg = value;
            if (fromUnit.equals("lbs")) {
                valueInKg = value * 0.453592f;
            }

            // Convert from kg to target unit
            if (toUnit.equals("lbs")) {
                convertedValue = valueInKg / 0.453592f;
            } else {
                convertedValue = valueInKg;
            }

            etWeight.setText(String.format(Locale.US, "%.1f", convertedValue));
        } catch (NumberFormatException e) {
            // Ignore invalid input
        }
    }

    private boolean saveValues() {
        if (etHeight.getText() == null || etWeight.getText() == null) {
            return false;
        }

        String heightText = etHeight.getText().toString().trim();
        String weightText = etWeight.getText().toString().trim();

        if (heightText.isEmpty()) {
            etHeight.setError("Please enter your height");
            etHeight.requestFocus();
            return false;
        }

        if (weightText.isEmpty()) {
            etWeight.setError("Please enter your weight");
            etWeight.requestFocus();
            return false;
        }

        try {
            float height = Float.parseFloat(heightText);
            float weight = Float.parseFloat(weightText);

            // Convert height to cm (base unit for storage)
            float heightInCm = height;
            switch (selectedHeightUnit) {
                case "cm":
                    heightInCm = height;
                    break;
                case "ft":
                    heightInCm = height * 30.48f;
                    break;
                case "in":
                    heightInCm = height * 2.54f;
                    break;
            }

            // Convert weight to kg (base unit for storage)
            float weightInKg = weight;
            if (selectedWeightUnit.equals("lbs")) {
                weightInKg = weight * 0.453592f;
            }

            // Validate converted values
            if (heightInCm <= 0 || heightInCm > 300) {
                etHeight.setError("Please enter a valid height");
                etHeight.requestFocus();
                return false;
            }

            if (weightInKg <= 0 || weightInKg > 500) {
                etWeight.setError("Please enter a valid weight");
                etWeight.requestFocus();
                return false;
            }

            // Save in metric (cm and kg)
            userProfile.setHeight(heightInCm);
            userProfile.setWeight(weightInKg);
            return true;

        } catch (NumberFormatException e) {
            etHeight.setError("Please enter valid numbers");
            etWeight.setError("Please enter valid numbers");
            return false;
        }
    }

    private void validateInputs() {
        if (etHeight.getText() == null || etWeight.getText() == null) {
            btnNext.setEnabled(false);
            btnNext.setAlpha(0.5f);
            return;
        }

        String heightText = etHeight.getText().toString().trim();
        String weightText = etWeight.getText().toString().trim();

        boolean isValid = false;

        if (!heightText.isEmpty() && !weightText.isEmpty()) {
            try {
                float height = Float.parseFloat(heightText);
                float weight = Float.parseFloat(weightText);

                // Basic validation - just check if values are positive
                // Detailed validation will be done in saveValues
                if (height > 0 && weight > 0) {
                    isValid = true;
                }
            } catch (NumberFormatException ignored) {
                // isValid remains false
            }
        }

        // Enable/disable button based on validation
        btnNext.setEnabled(isValid);
        btnNext.setAlpha(isValid ? 1.0f : 0.5f);
    }
}
