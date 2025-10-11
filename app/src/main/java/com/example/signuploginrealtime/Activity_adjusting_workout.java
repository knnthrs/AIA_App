package com.example.signuploginrealtime;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Activity_adjusting_workout extends AppCompatActivity {

    private static final String TAG = "AdjustingWorkout";

    private ProgressBar progressBar;
    private TextView tvPercentage, tvLoadingText;
    private ImageView imgLoading;
    private LinearLayout successContainer;
    private AppCompatButton btnDone;

    private Handler handler = new Handler();
    private int progress = 0;

    private String adjustmentType;
    private String originalFeedback;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences workoutPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_adjusting_workout);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get adjustment details
        adjustmentType = getIntent().getStringExtra("adjustment_type");
        originalFeedback = getIntent().getStringExtra("original_feedback");

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            workoutPrefs = getSharedPreferences("workout_prefs_" + userId, MODE_PRIVATE);
        } else {
            workoutPrefs = getSharedPreferences("workout_prefs_default", MODE_PRIVATE);
        }

        initializeViews();
        startLoadingAnimation();
        startProgress();
    }

    private void initializeViews() {
        progressBar = findViewById(R.id.progressBar);
        tvPercentage = findViewById(R.id.tvPercentage);
        tvLoadingText = findViewById(R.id.tvLoadingText);
        imgLoading = findViewById(R.id.imgLoading);
        successContainer = findViewById(R.id.successContainer);
        btnDone = findViewById(R.id.btnDone);

        btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void startLoadingAnimation() {
        // Rotate the loading icon continuously
        ObjectAnimator rotateAnimation = ObjectAnimator.ofFloat(imgLoading, "rotation", 0f, 360f);
        rotateAnimation.setDuration(1500);
        rotateAnimation.setRepeatCount(ObjectAnimator.INFINITE);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.start();
    }

    private void startProgress() {
        // Simulate progress from 0 to 100%
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (progress <= 100) {
                    progressBar.setProgress(progress);
                    tvPercentage.setText(progress + "%");

                    // When we reach 50%, start the actual adjustment
                    if (progress == 50) {
                        applyWorkoutAdjustment();
                    }

                    progress += 2; // Increment by 2% each time
                    handler.postDelayed(this, 50); // Update every 50ms
                } else {
                    showSuccessScreen();
                }
            }
        }, 100);
    }

    private void applyWorkoutAdjustment() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        // Calculate adjustment multipliers based on feedback
        double adjustmentMultiplier = calculateAdjustmentMultiplier();

        // Save adjustment to Firestore
        Map<String, Object> adjustmentData = new HashMap<>();
        adjustmentData.put("adjustmentType", adjustmentType);
        adjustmentData.put("originalFeedback", originalFeedback);
        adjustmentData.put("adjustmentMultiplier", adjustmentMultiplier);
        adjustmentData.put("timestamp", System.currentTimeMillis());

        db.collection("users")
                .document(userId)
                .collection("workoutAdjustments")
                .add(adjustmentData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Workout adjustment saved");

                    // Update user's workout difficulty preference
                    updateUserDifficultyPreference(userId, adjustmentMultiplier);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving adjustment", e);
                });

        // Save to SharedPreferences for immediate use
        workoutPrefs.edit()
                .putFloat("workout_difficulty_multiplier", (float) adjustmentMultiplier)
                .putLong("last_adjustment_timestamp", System.currentTimeMillis())
                .apply();
    }

    private double calculateAdjustmentMultiplier() {
        // Base multiplier is 1.0 (no change)
        double multiplier = 1.0;

        boolean isTooHard = originalFeedback != null && originalFeedback.contains("hard");

        if (adjustmentType != null) {
            if (adjustmentType.contains("Way")) {
                // Way easier/harder: 20-25% change
                multiplier = isTooHard ? 0.75 : 1.25;
            } else if (adjustmentType.contains("little")) {
                // A little easier/harder: 10-15% change
                multiplier = isTooHard ? 0.85 : 1.15;
            }
        }

        return multiplier;
    }

    private void updateUserDifficultyPreference(String userId, double multiplier) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("workoutDifficultyMultiplier", multiplier);
        updates.put("lastDifficultyAdjustment", System.currentTimeMillis());

        db.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User difficulty preference updated");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating difficulty preference", e);
                });
    }

    private void showSuccessScreen() {
        // Hide loading views
        progressBar.setVisibility(View.GONE);
        tvPercentage.setVisibility(View.GONE);
        tvLoadingText.setVisibility(View.GONE);
        imgLoading.setVisibility(View.GONE);

        // Show success container
        successContainer.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // Clean up handler
    }
}