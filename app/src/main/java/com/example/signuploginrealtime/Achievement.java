package com.example.signuploginrealtime;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Achievement extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userId;
    private int workoutsCompleted = 0;
    private int currentStreak = 0;

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
        firstStepsBadge = findViewById(R.id.first_steps_badge);
        gettingStrongBadge = findViewById(R.id.getting_strong_badge);
        fitnessProBadge = findViewById(R.id.fitness_pro_badge);
        warriorBadge = findViewById(R.id.warrior_badge);
        legendBadge = findViewById(R.id.legend_badge);

        firstStepsProgress = findViewById(R.id.first_steps_progress);
        gettingStrongProgress = findViewById(R.id.getting_strong_progress);
        fitnessProProgress = findViewById(R.id.fitness_pro_progress);
        warriorProgress = findViewById(R.id.warrior_progress);
        legendProgress = findViewById(R.id.legend_progress);

        onFireProgress = findViewById(R.id.on_fire_progress);
        lightningProgress = findViewById(R.id.lightning_progress);
        championProgress = findViewById(R.id.champion_progress);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.item_4);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.item_1) startActivity(new Intent(this, MainActivity.class));
            else if (id == R.id.item_2) startActivity(new Intent(this, Profile.class));
            else if (id == R.id.item_3) startActivity(new Intent(this, WorkoutList.class));
            return true;
        });
    }

    private void listenUserProgress() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        userId = currentUser.getUid();
        db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot != null && snapshot.exists()) {
                        workoutsCompleted = snapshot.getLong("workoutsCompleted") != null ?
                                snapshot.getLong("workoutsCompleted").intValue() : 0;
                        currentStreak = snapshot.getLong("currentStreak") != null ?
                                snapshot.getLong("currentStreak").intValue() : 0;

                        updateBadgesUI();
                    }
                });
    }

    private void updateBadgesUI() {
        // First Steps - 1 workout
        updateBadgeProgress(firstStepsBadge, firstStepsProgress, workoutsCompleted, 1);

        // Getting Strong - 10 workouts
        updateBadgeProgress(gettingStrongBadge, gettingStrongProgress, workoutsCompleted, 10);

        // Fitness Pro - 25 workouts
        updateBadgeProgress(fitnessProBadge, fitnessProProgress, workoutsCompleted, 25);

        // Warrior - 50 workouts
        updateBadgeProgress(warriorBadge, warriorProgress, workoutsCompleted, 50);

        // Legend - 100 workouts
        updateBadgeProgress(legendBadge, legendProgress, workoutsCompleted, 100);

        // Streak badges (no badge visual for streaks, just text)
        updateBadgeProgress(null, onFireProgress, currentStreak, 3);
        updateBadgeProgress(null, lightningProgress, currentStreak, 7);
        updateBadgeProgress(null, championProgress, currentStreak, 30);
    }

    private void updateBadgeProgress(LinearLayout badge, TextView progressText, int current, int target) {
        if (current >= target) {
            // Achievement completed
            progressText.setText("✓ Done");
            if (badge != null) {
                markBadgeCompleted(badge);
            }
        } else {
            // Achievement in progress - keep full size but reduce opacity
            progressText.setText(current + "/" + target);
            if (badge != null) {
                float progress = (float) current / target;
                badge.setAlpha(0.6f + 0.4f * progress); // Opacity based on progress
                badge.setScaleX(1f); // Keep full size
                badge.setScaleY(1f); // Keep full size
            }
        }
    }

    private void markBadgeCompleted(View badge) {
        badge.setAlpha(1f);
        ObjectAnimator.ofFloat(badge, "scaleX", 0.8f, 1f).setDuration(500).start();
        ObjectAnimator.ofFloat(badge, "scaleY", 0.8f, 1f).setDuration(500).start();
    }

    private void animateBadgeProgress(View badge, float progress) {
        badge.setAlpha(0.6f + 0.4f * progress);
        badge.setScaleX(0.8f + 0.2f * progress);
        badge.setScaleY(0.8f + 0.2f * progress);
    }
}