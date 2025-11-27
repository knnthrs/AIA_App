package com.example.signuploginrealtime;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Activity_prepare_easier_plan extends AppCompatActivity {

    private AppCompatButton btnKeepSame, btnLittleEasier, btnWayEasier, btnDone;
    private ImageView btnBack;
    private TextView tvFeedbackText, tvTitle;
    private AppCompatButton selectedButton;
    private String selectedOption = "Way easier"; // Default selection
    private String originalFeedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_prepare_easier_plan);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get original feedback
        originalFeedback = getIntent().getStringExtra("feedback");

        initializeViews();
        updateUIBasedOnFeedback();
        setupClickListeners();

        // Set default selection
        selectedButton = btnWayEasier;
        selectButton(selectedButton);
    }

    private void initializeViews() {
        btnKeepSame = findViewById(R.id.btnKeepSame);
        btnLittleEasier = findViewById(R.id.btnLittleEasier);
        btnWayEasier = findViewById(R.id.btnWayEasier);
        btnDone = findViewById(R.id.btnDone);
        btnBack = findViewById(R.id.btnBack);
        tvFeedbackText = findViewById(R.id.tvFeedbackText);
        tvTitle = findViewById(R.id.tvTitle);
    }

    private void updateUIBasedOnFeedback() {
        if (originalFeedback == null) return;

        if (originalFeedback.contains("hard")) {
            tvFeedbackText.setText("Too hard? I see...");
            tvTitle.setText("Shall I prepare an easier plan\nfor you?");
            btnLittleEasier.setText("Just a little easier");
            btnWayEasier.setText("Way easier");
        } else if (originalFeedback.contains("easy")) {
            tvFeedbackText.setText("Too easy? I see...");
            tvTitle.setText("Shall I prepare a harder plan\nfor you?");
            btnLittleEasier.setText("Just a little harder");
            btnWayEasier.setText("Way harder");
            selectedOption = "Way harder";
        }
    }

    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Option buttons
        View.OnClickListener selectionListener = v -> {
            AppCompatButton clickedButton = (AppCompatButton) v;
            selectButton(clickedButton);
        };

        btnKeepSame.setOnClickListener(selectionListener);
        btnLittleEasier.setOnClickListener(selectionListener);
        btnWayEasier.setOnClickListener(selectionListener);

        // Done button
        btnDone.setOnClickListener(v -> {
            Intent previousIntent = getIntent();

            if ("No, just keep everything the same".equals(selectedOption)) {
                // No adjustment needed - go straight to summary
                Intent summaryIntent = new Intent(this, WorkoutSummaryActivity.class);

                if (previousIntent.hasExtra("workoutDuration")) {
                    summaryIntent.putExtra("workoutDuration",
                            previousIntent.getIntExtra("workoutDuration", 0));
                }
                if (previousIntent.hasExtra("performanceData")) {
                    summaryIntent.putExtra("performanceData",
                            previousIntent.getSerializableExtra("performanceData"));
                }
                if (previousIntent.hasExtra("workout_name")) {
                    summaryIntent.putExtra("workout_name",
                            previousIntent.getStringExtra("workout_name"));
                }
                if (previousIntent.hasExtra("total_exercises")) {
                    summaryIntent.putExtra("total_exercises",
                            previousIntent.getIntExtra("total_exercises", 0));
                }

                startActivity(summaryIntent);
                finish();
            } else {
                // User wants an easier/harder plan -> go to adjusting screen
                Intent adjustIntent = new Intent(this, Activity_adjusting_workout.class);
                adjustIntent.putExtra("adjustment_type", selectedOption);
                adjustIntent.putExtra("original_feedback", originalFeedback);

                if (previousIntent.hasExtra("workoutDuration")) {
                    adjustIntent.putExtra("workoutDuration",
                            previousIntent.getIntExtra("workoutDuration", 0));
                }
                if (previousIntent.hasExtra("performanceData")) {
                    adjustIntent.putExtra("performanceData",
                            previousIntent.getSerializableExtra("performanceData"));
                }
                if (previousIntent.hasExtra("workout_name")) {
                    adjustIntent.putExtra("workout_name",
                            previousIntent.getStringExtra("workout_name"));
                }
                if (previousIntent.hasExtra("total_exercises")) {
                    adjustIntent.putExtra("total_exercises",
                            previousIntent.getIntExtra("total_exercises", 0));
                }

                startActivity(adjustIntent);
                finish();
            }
        });
    }

    private void selectButton(AppCompatButton button) {
        // Reset all buttons to unselected state
        resetButton(btnKeepSame);
        resetButton(btnLittleEasier);
        resetButton(btnWayEasier);

        // Highlight selected button
        selectedButton = button;
        selectedButton.setBackgroundColor(Color.WHITE);
        selectedButton.setTextColor(Color.BLACK);

        // Update selectedOption based on which button is chosen
        if (button == btnKeepSame) {
            selectedOption = "No, just keep everything the same";
        } else if (button == btnLittleEasier) {
            selectedOption = btnLittleEasier.getText().toString();
        } else if (button == btnWayEasier) {
            selectedOption = btnWayEasier.getText().toString();
        }
    }

    private void resetButton(AppCompatButton button) {
        if (button == null) return;
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setTextColor(Color.WHITE);
    }
}