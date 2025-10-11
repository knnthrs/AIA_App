package com.example.signuploginrealtime;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Activity_workout_feedback extends AppCompatActivity {

    private AppCompatButton btnTooEasy, btnLittleEasy, btnJustRight, btnLittleHard, btnTooHard, btnDone;
    private ImageView btnClose;
    private AppCompatButton selectedButton;
    private String selectedFeedback = "Just right"; // Default selection

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_workout_feedback);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupClickListeners();

        // Set default selection to "Just right"
        selectedButton = btnJustRight;
    }

    private void initializeViews() {
        btnTooEasy = findViewById(R.id.btnTooEasy);
        btnLittleEasy = findViewById(R.id.btnLittleEasy);
        btnJustRight = findViewById(R.id.btnJustRight);
        btnLittleHard = findViewById(R.id.btnLittleHard);
        btnTooHard = findViewById(R.id.btnTooHard);
        btnDone = findViewById(R.id.btnDone);
        btnClose = findViewById(R.id.btnClose);
    }

    private void setupClickListeners() {
        // Close button
        btnClose.setOnClickListener(v -> finish());

        // Feedback option buttons
        View.OnClickListener selectionListener = v -> {
            AppCompatButton clickedButton = (AppCompatButton) v;
            selectButton(clickedButton);
        };

        btnTooEasy.setOnClickListener(selectionListener);
        btnLittleEasy.setOnClickListener(selectionListener);
        btnJustRight.setOnClickListener(selectionListener);
        btnLittleHard.setOnClickListener(selectionListener);
        btnTooHard.setOnClickListener(selectionListener);

        // Done button
        btnDone.setOnClickListener(v -> {
            handleFeedback();
        });
    }

    private void selectButton(AppCompatButton button) {
        // Reset all buttons to unselected state
        resetButton(btnTooEasy);
        resetButton(btnLittleEasy);
        resetButton(btnJustRight);
        resetButton(btnLittleHard);
        resetButton(btnTooHard);

        // Select the clicked button
        button.setBackgroundResource(R.drawable.option_button_selected);
        button.setTextColor(Color.WHITE);

        // Update selected button and feedback
        selectedButton = button;
        selectedFeedback = button.getText().toString();
    }

    private void resetButton(AppCompatButton button) {
        button.setBackgroundResource(R.drawable.option_button_unselected);
        button.setTextColor(Color.BLACK);
    }

    private void handleFeedback() {
        // Check if adjustment is needed
        if (selectedFeedback.equals("Too hard") ||
                selectedFeedback.equals("A little hard") ||
                selectedFeedback.equals("Too easy") ||
                selectedFeedback.equals("A little easy")) {

            // Go to adjustment options screen
            Intent intent = new Intent(this, Activity_prepare_easier_plan.class);
            intent.putExtra("feedback", selectedFeedback);
            startActivity(intent);
            finish();
        } else {
            // Just right - go to main
            Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}