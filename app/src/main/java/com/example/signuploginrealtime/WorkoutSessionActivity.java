package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import java.util.Locale;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class WorkoutSessionActivity extends AppCompatActivity {

    private TextView tvExerciseName, tvExerciseDetails, tvExerciseTimer, tvExerciseProgress, tvNoImage;
    private TextView btnPrevious, btnSkip; // Added Previous and Skip buttons
    private ImageView ivExerciseImage;
    private Button btnPause, btnNext;
    private ProgressBar progressBar; // Added progress bar

    private ArrayList<String> exerciseNames;
    private ArrayList<String> exerciseDetails;
    private ArrayList<String> exerciseImageUrls;
    private ArrayList<Integer> exerciseDurations;
    private ArrayList<Integer> exerciseRests;

    private int currentIndex = 0;
    private CountDownTimer timer;
    private boolean isTimerRunning = false;
    private long timeLeftMillis;
    private long workoutStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_session);

        // Initialize existing views
        tvExerciseName = findViewById(R.id.tv_exercise_name);
        tvExerciseDetails = findViewById(R.id.tv_exercise_details);
        tvExerciseTimer = findViewById(R.id.tv_exercise_timer);
        tvExerciseProgress = findViewById(R.id.tv_exercise_progress);
        ivExerciseImage = findViewById(R.id.iv_exercise_image);
        tvNoImage = findViewById(R.id.tvNoImage);
        btnPause = findViewById(R.id.btn_pause);
        btnNext = findViewById(R.id.btn_done);

        // Initialize new views
        btnPrevious = findViewById(R.id.btn_previous);
        btnSkip = findViewById(R.id.btn_skip);
        progressBar = findViewById(R.id.progress_bar);

        // ✅ Use "exerciseImageUrls" instead of "exerciseVideoUrls"
        exerciseNames = getIntent().getStringArrayListExtra("exerciseNames");
        exerciseDetails = getIntent().getStringArrayListExtra("exerciseDetails");
        exerciseImageUrls = getIntent().getStringArrayListExtra("exerciseImageUrls");
        exerciseDurations = getIntent().getIntegerArrayListExtra("exerciseTimes");
        exerciseRests = getIntent().getIntegerArrayListExtra("exerciseRests");

        if (exerciseDurations == null && exerciseNames != null) {
            exerciseDurations = new ArrayList<>();
            for (int i = 0; i < exerciseNames.size(); i++) exerciseDurations.add(30);
        }

        if (exerciseRests == null && exerciseNames != null) {
            exerciseRests = new ArrayList<>();
            for (int i = 0; i < exerciseNames.size(); i++) exerciseRests.add(60);
        }

        if (exerciseNames == null || exerciseDetails == null || exerciseImageUrls == null
                || exerciseNames.size() != exerciseDetails.size()
                || exerciseNames.size() != exerciseImageUrls.size()) {
            Toast.makeText(this, "Exercise data invalid!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get current index if coming from rest
        currentIndex = getIntent().getIntExtra("currentIndex", 0);

        showExercise(currentIndex);

        // Set up button click listeners
        btnNext.setOnClickListener(v -> moveToNextExercise());
        btnPause.setOnClickListener(v -> {
            if (isTimerRunning) {
                if (timer != null) timer.cancel();
                isTimerRunning = false;
                btnPause.setText("RESUME");
            } else {
                startTimer((int) (timeLeftMillis / 1000));
            }
        });

        // ✅ Previous button functionality
        btnPrevious.setOnClickListener(v -> moveToPreviousExercise());

        // ✅ Skip button functionality
        btnSkip.setOnClickListener(v -> skipCurrentExercise());
    }

    private void showExercise(int index) {
        tvExerciseName.setText(exerciseNames.get(index));
        tvExerciseDetails.setText(exerciseDetails.get(index));
        tvExerciseProgress.setText((index + 1) + "/" + exerciseNames.size());

        // Update progress bar
        updateProgressBar();

        // Update button states
        updateButtonStates();

        String imageUrl = exerciseImageUrls.get(index);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            tvNoImage.setVisibility(View.GONE);
            ivExerciseImage.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .load(imageUrl)
                    .into(ivExerciseImage);

        } else {
            ivExerciseImage.setVisibility(View.GONE);
            tvNoImage.setVisibility(View.VISIBLE);
        }

        startTimer(exerciseDurations.get(index));
    }

    private void updateProgressBar() {
        if (progressBar != null && exerciseNames != null) {
            int progress = (int) (((float) (currentIndex + 1) / exerciseNames.size()) * 100);
            progressBar.setProgress(progress);
        }
    }

    private void updateButtonStates() {
        // Disable/enable Previous button based on current position
        if (btnPrevious != null) {
            btnPrevious.setEnabled(currentIndex > 0);
            btnPrevious.setAlpha(currentIndex > 0 ? 1.0f : 0.5f);
        }

        // Disable/enable Skip button based on current position
        if (btnSkip != null) {
            btnSkip.setEnabled(currentIndex < exerciseNames.size() - 1);
            btnSkip.setAlpha(currentIndex < exerciseNames.size() - 1 ? 1.0f : 0.5f);
        }
    }

    // ✅ New method: Move to previous exercise
    private void moveToPreviousExercise() {
        if (currentIndex <= 0) {
            Toast.makeText(this, "This is the first exercise", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cancel current timer
        if (timer != null) timer.cancel();
        isTimerRunning = false;

        // Move to previous exercise
        currentIndex--;
        showExercise(currentIndex);
    }

    // ✅ New method: Skip current exercise
    private void skipCurrentExercise() {
        // Cancel current timer
        if (timer != null) timer.cancel();
        isTimerRunning = false;

        // If this is the last exercise, show confirmation dialog
        if (currentIndex >= exerciseNames.size() - 1) {
            showSkipLastExerciseDialog();
            return;
        }

        // Move to next exercise directly (without rest)
        currentIndex++;
        showExercise(currentIndex);
    }

    // ✅ Show dialog when user wants to skip the last exercise
    private void showSkipLastExerciseDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Finish Workout?")
                .setMessage("This is the last exercise. Skipping it will end your workout session. Do you want to finish the workout now?")
                .setPositiveButton("Yes, Finish Workout", (dialog, which) -> {
                    // End the workout - redirect to completion activity
                    Intent intent = new Intent(WorkoutSessionActivity.this, activity_workout_complete.class);

                    // Pass workout details for Firebase saving
                    intent.putExtra("workout_name", getIntent().getStringExtra("workout_name"));
                    intent.putExtra("total_exercises", exerciseNames.size());
                    intent.putExtra("workout_duration", calculateWorkoutDuration());

                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User cancelled - resume the current exercise
                    startTimer((int) (timeLeftMillis / 1000));
                    dialog.dismiss();
                })
                .setCancelable(false) // Prevent dismissing by clicking outside
                .show();
    }

    private void startTimer(int seconds) {
        if (timer != null) timer.cancel();
        timeLeftMillis = seconds * 1000L;

        timer = new CountDownTimer(timeLeftMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMillis = millisUntilFinished;
                tvExerciseTimer.setText("Timer: " + (int) (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                tvExerciseTimer.setText("Timer: 0s");
                moveToNextExercise();
            }
        }.start();

        isTimerRunning = true;
        btnPause.setText("PAUSE");
    }

    private void moveToNextExercise() {
        if (timer != null) timer.cancel();
        isTimerRunning = false;

        if (currentIndex >= exerciseNames.size() - 1) {

            Intent intent = new Intent(WorkoutSessionActivity.this, activity_workout_complete.class);
            intent.putExtra("workout_name", getIntent().getStringExtra("workout_name"));
            intent.putExtra("total_exercises", exerciseNames.size());
            intent.putExtra("workout_duration", calculateWorkoutDuration());

            startActivity(intent);
            finish();
            return;
        }


        // Go to RestTimerActivity with nextIndex
        int nextIndex = currentIndex + 1;
        Intent intent = new Intent(WorkoutSessionActivity.this, RestTimerActivity.class);
        intent.putExtra("nextIndex", nextIndex); // Integer
        intent.putStringArrayListExtra("exerciseNames", exerciseNames); // String list
        intent.putStringArrayListExtra("exerciseDetails", exerciseDetails); // String list
        intent.putStringArrayListExtra("exerciseImageUrls", exerciseImageUrls); // String list
        intent.putIntegerArrayListExtra("exerciseTimes", exerciseDurations); // Integer list
        intent.putIntegerArrayListExtra("exerciseRests", exerciseRests); // Integer list

        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timer != null) {
            timer.cancel();
            isTimerRunning = false;
            btnPause.setText("RESUME");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }

    // ✅ Helper method to calculate workout duration
    private String calculateWorkoutDuration() {
        long durationMillis = System.currentTimeMillis() - workoutStartTime;
        long minutes = (durationMillis / 1000) / 60;
        long seconds = (durationMillis / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}