package com.example.signuploginrealtime.UserInfo;

import android.content.Intent;
import android.os.Bundle;
import android.app.DatePickerDialog;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.signuploginrealtime.R;
import com.example.signuploginrealtime.models.UserProfile;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class AgeInput extends AppCompatActivity {

    private TextInputEditText etBirthdate;

    private int calculatedAge = -1; // store computed age
    private Button btnNext;
    private UserProfile userProfile; // ✅ full profile


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
        userProfile = (UserProfile) getIntent().getSerializableExtra("userProfile");
        if (userProfile == null) {
            // fallback if somehow missing
            userProfile = new UserProfile();
            userProfile.setHealthIssues(new ArrayList<>());
            userProfile.setFitnessGoal("general fitness");
            userProfile.setFitnessLevel("beginner");
        }

        // Initialize views
        etBirthdate = findViewById(R.id.etBirthdate);
        btnNext = findViewById(R.id.btnNext);

        // Add TextWatcher to enable/disable button based on input
        etBirthdate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AgeInput.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        calendar.set(selectedYear, selectedMonth, selectedDay);

                        // Format date
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        String birthdate = sdf.format(calendar.getTime());
                        etBirthdate.setText(birthdate);

                        // ✅ Calculate age
                        calculatedAge = getAge(selectedYear, selectedMonth, selectedDay);
                        validateInput();
                    },
                    year, month, day
            );

            // Optional: restrict to 100 years old max and today as latest
            calendar.add(Calendar.YEAR, -100);
            datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

            datePickerDialog.show();
        });


        // Next button click listener
        btnNext.setOnClickListener(v -> {
            if (calculatedAge == -1) {
                etBirthdate.setError("Please select your birthdate");
                etBirthdate.requestFocus();
                return;
            }

            if (calculatedAge < 13 || calculatedAge > 120) {
                etBirthdate.setError("Age must be between 13 and 120");
                etBirthdate.requestFocus();
                return;
            }

            // ✅ Save birthdate & age to profile
            userProfile.setAge(calculatedAge);
            userProfile.setBirthdate(etBirthdate.getText().toString().trim());

            Intent intent = new Intent(AgeInput.this, HeightWeightInput.class);
            intent.putExtra("userProfile", userProfile);
            startActivity(intent);
        });

    }

    // ✅ Correctly placed OUTSIDE onCreate()
    private void validateInput() {
        boolean isValid = (calculatedAge >= 13 && calculatedAge <= 120);

        btnNext.setEnabled(isValid);
        btnNext.setAlpha(isValid ? 1.0f : 0.5f);
    }


    private int getAge(int year, int month, int day) {
        Calendar today = Calendar.getInstance();
        Calendar dob = Calendar.getInstance();
        dob.set(year, month, day);

        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }

}
