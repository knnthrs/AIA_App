package com.example.signuploginrealtime.UserInfo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.signuploginrealtime.R;
import com.example.signuploginrealtime.models.UserProfile;

public class activity_select_workout_frequency extends AppCompatActivity {

    private RadioGroup radioGroupFrequency;
    private Button btnConfirm;
    private UserProfile userProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_workout_frequency);

        // Assume UserProfile is passed via intent or fetched from storage
        userProfile = (UserProfile) getIntent().getSerializableExtra("userProfile");

        radioGroupFrequency = findViewById(R.id.radioGroupFrequency);
        btnConfirm = findViewById(R.id.btnConfirm);

        btnConfirm.setOnClickListener(v -> {
            int selectedId = radioGroupFrequency.getCheckedRadioButtonId();

            if (selectedId == -1) {
                Toast.makeText(this, "Please select a frequency", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRadio = findViewById(selectedId);
            String text = selectedRadio.getText().toString();
            int frequency = extractDaysFromText(text);

            // Save to UserProfile
            userProfile.setWorkoutDaysPerWeek(frequency);

            // Move to HealthIssues activity
            Intent intent = new Intent(activity_select_workout_frequency.this, HealthIssues.class);
            intent.putExtra("userProfile", userProfile); // pass updated profile
            startActivity(intent);

        });
    }

    private int extractDaysFromText(String text) {
        if (text == null || text.isEmpty()) return 3; // default
        // Extract first number from string
        String numberOnly = text.replaceAll("[^0-9]", "");
        if (numberOnly.isEmpty()) return 3;
        return Integer.parseInt(numberOnly);
    }
}
