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
            if (selectedOption.equals("No, just keep everything the same")) {
                // No adjustment needed
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                // Proceed to adjustment
                Intent intent = new Intent(this, Activity_adjusting_workout.class);
                intent.putExtra("adjustment_type", selectedOption);
                intent.putExtra("original_feedback", originalFeedback);
                startActivity(intent);
                finish();
            }
        });
    }

    private void selectButton(AppCompatButton button) {
        // Reset all buttons to unselected state
        resetButton(btnKeepSame);
        resetButton(btnLittleEasier);
        resetButton(btnWayEasier);

        // Select the clicked button
        button.setBackgroundResource(R.drawable.option_button_selected);
        button.setTextColor(Color.WHITE);

        // Update selected button and option
        selectedButton = button;
        selectedOption = button.getText().toString();
    }

    private void resetButton(AppCompatButton button) {
        button.setBackgroundResource(R.drawable.option_button_unselected);
        button.setTextColor(Color.BLACK);
    }
}