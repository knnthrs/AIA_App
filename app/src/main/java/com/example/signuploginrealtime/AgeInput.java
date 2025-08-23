package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AgeInput extends AppCompatActivity {

    private EditText etAge;
    private Button btnNext;
    private String gender;

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

        // Get data from previous activity
        gender = getIntent().getStringExtra("gender");

        // Initialize views
        etAge = findViewById(R.id.etAge);
        btnNext = findViewById(R.id.btnNext);

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
                    etAge.setError("Please enter a valid age");
                    etAge.requestFocus();
                    return;
                }

                Intent intent = new Intent(AgeInput.this, HeightWeightInput.class);
                intent.putExtra("gender", gender);
                intent.putExtra("age", age);
                startActivity(intent);
                finish();

            } catch (NumberFormatException e) {
                etAge.setError("Please enter a valid number");
                etAge.requestFocus();
            }
        });
    }
}