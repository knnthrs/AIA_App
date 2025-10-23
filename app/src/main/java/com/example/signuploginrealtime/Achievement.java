package com.example.signuploginrealtime;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Achievement extends AppCompatActivity {

    private static final String TAG = "Achievement";
    private FirebaseFirestore db;
    private String userId;
    private int workoutsCompleted = 0;
    private int currentStreak = 0;

    // Badge views
    private LinearLayout firstStepsBadge, gettingStrongBadge, fitnessProBadge, warriorBadge, legendBadge;
    private TextView firstStepsProgress, gettingStrongProgress, fitnessProProgress, warriorProgress, legendProgress;
    private TextView onFireProgress, lightningProgress, championProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_achievement);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        setupBottomNavigation();
        initializeViews();
        listenUserProgress();
    }

    private void initializeViews() {
        // Workout milestone badges
        firstStepsBadge = findViewById(R.id.first_steps_badge);
        gettingStrongBadge = findViewById(R.id.getting_strong_badge);
        fitnessProBadge = findViewById(R.id.fitness_pro_badge);
        warriorBadge = findViewById(R.id.warrior_badge);
        legendBadge = findViewById(R.id.legend_badge);

        // Progress text views for workout milestones
        firstStepsProgress = findViewById(R.id.first_steps_progress);
        gettingStrongProgress = findViewById(R.id.getting_strong_progress);
        fitnessProProgress = findViewById(R.id.fitness_pro_progress);
        warriorProgress = findViewById(R.id.warrior_progress);
        legendProgress = findViewById(R.id.legend_progress);

        // Progress text views for streak milestones
        onFireProgress = findViewById(R.id.on_fire_progress);
        lightningProgress = findViewById(R.id.lightning_progress);
        championProgress = findViewById(R.id.champion_progress);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.item_4);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.item_1) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
            } else if (id == R.id.item_2) {
                startActivity(new Intent(this, Profile.class));
                overridePendingTransition(0, 0);
            } else if (id == R.id.item_3) {
                startActivity(new Intent(this, WorkoutList.class));
                overridePendingTransition(0, 0);
            }
            return true;
        });
    }
    private void listenUserProgress() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user found");
            return;
        }

        userId = currentUser.getUid();
        db = FirebaseFirestore.getInstance();

        // Listen for real-time updates to user progress
        db.collection("users")
                .document(userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to user progress", error);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        // Get current values
                        workoutsCompleted = snapshot.getLong("workoutsCompleted") != null ?
                                snapshot.getLong("workoutsCompleted").intValue() : 0;
                        currentStreak = snapshot.getLong("currentStreak") != null ?
                                snapshot.getLong("currentStreak").intValue() : 0;

                        // Update UI with latest progress
                        updateBadgesUI();
                    } else {
                        Log.d(TAG, "User document doesn't exist yet");
                    }
                });
    }

    private void updateBadgesUI() {
        // Update workout milestone badges
        updateBadgeProgress(firstStepsBadge, firstStepsProgress, workoutsCompleted, 1);
        updateBadgeProgress(gettingStrongBadge, gettingStrongProgress, workoutsCompleted, 10);
        updateBadgeProgress(fitnessProBadge, fitnessProProgress, workoutsCompleted, 25);
        updateBadgeProgress(warriorBadge, warriorProgress, workoutsCompleted, 50);
        updateBadgeProgress(legendBadge, legendProgress, workoutsCompleted, 100);

        // Update streak milestone progress
        if (onFireProgress != null) {
            updateStreakProgress(onFireProgress, currentStreak, 3);
        }
        if (lightningProgress != null) {
            updateStreakProgress(lightningProgress, currentStreak, 7);
        }
        if (championProgress != null) {
            updateStreakProgress(championProgress, currentStreak, 30);
        }
    }

    private void updateBadgeProgress(LinearLayout badge, TextView progressText, int current, int target) {
        if (progressText == null) return;

        if (current >= target) {
            // Achievement completed
            progressText.setText("✓ Done");
            if (badge != null) {
                markBadgeCompleted(badge);
            }
        } else {
            // Show progress towards achievement
            progressText.setText(current + "/" + target);
            if (badge != null) {
                // Partial transparency based on progress
                float progress = (float) current / target;
                badge.setAlpha(0.6f + 0.4f * progress);
            }
        }
    }

    private void updateStreakProgress(TextView progressText, int currentStreak, int target) {
        if (currentStreak >= target) {
            progressText.setText("✓ Done");
        } else {
            progressText.setText(currentStreak + "/" + target);
        }
    }

    private void markBadgeCompleted(View badge) {
        badge.setAlpha(1f);
        badge.setScaleX(1f);
        badge.setScaleY(1f);
    }

    // Animation method for when achievements are unlocked (can be called from other activities)
    public void animateAchievementUnlocked(int milestone) {
        View badgeToAnimate = null;

        // Find the corresponding badge
        switch (milestone) {
            case 1: badgeToAnimate = firstStepsBadge; break;
            case 10: badgeToAnimate = gettingStrongBadge; break;
            case 25: badgeToAnimate = fitnessProBadge; break;
            case 50: badgeToAnimate = warriorBadge; break;
            case 100: badgeToAnimate = legendBadge; break;
        }

        if (badgeToAnimate != null) {
            // Celebration animation - scale up and back down
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(badgeToAnimate, "scaleX", 1f, 1.2f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(badgeToAnimate, "scaleY", 1f, 1.2f, 1f);
            scaleX.setDuration(600);
            scaleY.setDuration(600);
            scaleX.start();
            scaleY.start();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0); // Remove animation when pressing back button
    }
}