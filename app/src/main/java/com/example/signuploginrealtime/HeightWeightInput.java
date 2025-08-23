package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HeightWeightInput extends AppCompatActivity {

    private EditText etHeight, etWeight;
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
                    etHeight.setError("Please enter a valid height");
                    etHeight.requestFocus();
                    return;
                }

                if (weight <= 0 || weight > 500) {
                    etWeight.setError("Please enter a valid weight");
                    etWeight.requestFocus();
                    return;
                }

                Intent intent = new Intent(HeightWeightInput.this, FitnessLevel.class);
                intent.putExtra("gender", gender);
                intent.putExtra("age", age);
                intent.putExtra("height", height);
                intent.putExtra("weight", weight);
                startActivity(intent);
                finish();

            } catch (NumberFormatException e) {
                etHeight.setError("Please enter valid numbers");
                etWeight.setError("Please enter valid numbers");
            }
        });
    }
}