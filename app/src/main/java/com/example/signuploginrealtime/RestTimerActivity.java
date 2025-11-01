package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

public class RestTimerActivity extends AppCompatActivity {

    private TextView tvRestTimer;
    private Button btnSkipRest, btnAddTime;
    private TextView tvNextExerciseLabel;
    private TextView tvNextExerciseName;
    private ImageView ivNextExerciseImage;
    private TextView tvNextExerciseReps;

    private int nextExerciseIndex;
    private CountDownTimer timer;
    private long remainingRestMillis;

    private ArrayList<String> exerciseNames;
    private ArrayList<String> exerciseDetails;
    private ArrayList<String> exerciseImageUrls;
    private ArrayList<Integer> exerciseTimes;
    private ArrayList<Integer> exerciseRests;
    private ArrayList<ExercisePerformanceData> performanceDataList;
    private long workoutStartTime;
    private String workoutName;
    private ArrayList<Integer> completedSetsPerExercise;
    private ArrayList<Integer> totalSetsPerExercise;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rest_timer);

        tvRestTimer = findViewById(R.id.tv_rest_timer);
        btnSkipRest = findViewById(R.id.btn_skip_rest);
        btnAddTime = findViewById(R.id.btn_add_time);

        // New views for next exercise preview
        tvNextExerciseLabel = findViewById(R.id.tv_next_exercise_label);
        tvNextExerciseName = findViewById(R.id.tv_next_exercise_name);
        ivNextExerciseImage = findViewById(R.id.iv_next_exercise_image);
        tvNextExerciseReps = findViewById(R.id.tv_next_exercise_reps);

        // Get data from Intent
        nextExerciseIndex = getIntent().getIntExtra("nextIndex", 0);
        exerciseNames = getIntent().getStringArrayListExtra("exerciseNames");
        exerciseDetails = getIntent().getStringArrayListExtra("exerciseDetails");
        exerciseImageUrls = getIntent().getStringArrayListExtra("exerciseImageUrls");
        exerciseTimes = getIntent().getIntegerArrayListExtra("exerciseTimes");
        exerciseRests = getIntent().getIntegerArrayListExtra("exerciseRests");
        performanceDataList = (ArrayList<ExercisePerformanceData>) getIntent().getSerializableExtra("performanceData");
        workoutStartTime = getIntent().getLongExtra("workoutStartTime", System.currentTimeMillis());
        workoutName = getIntent().getStringExtra("workout_name");

        completedSetsPerExercise = getIntent().getIntegerArrayListExtra("completedSetsPerExercise");
        totalSetsPerExercise = getIntent().getIntegerArrayListExtra("totalSetsPerExercise");

        // Validate data
        if (exerciseNames == null || exerciseDetails == null || exerciseImageUrls == null
                || exerciseTimes == null || exerciseRests == null
                || exerciseNames.size() != exerciseDetails.size()
                || exerciseNames.size() != exerciseImageUrls.size()
                || exerciseNames.size() != exerciseTimes.size()
                || exerciseNames.size() != exerciseRests.size()) {
            finish();
            return;
        }

        // Display next exercise info
        displayNextExerciseInfo();

        remainingRestMillis = exerciseRests.get(nextExerciseIndex) * 1000L;
        updateTimerText();
        startTimer();

        btnSkipRest.setOnClickListener(v -> {
            if (timer != null) timer.cancel();
            goToNextExercise();
        });

        btnAddTime.setOnClickListener(v -> {
            remainingRestMillis += 20_000; // add 20 seconds
            if (timer != null) timer.cancel();
            startTimer();
        });

        // Migrate back handling to OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitWorkoutDialog();
            }
        });
    }

    private void displayNextExerciseInfo() {
        if (nextExerciseIndex < exerciseNames.size()) {
            // Get clean exercise name
            String cleanName = getCleanExerciseName(exerciseNames.get(nextExerciseIndex));
            tvNextExerciseName.setText(cleanName.toUpperCase());

            // Get set and rep info
            int currentSet = completedSetsPerExercise.get(nextExerciseIndex) + 1;
            int totalSets = totalSetsPerExercise.get(nextExerciseIndex);
            String setsRepsInfo = extractSetsRepsInfo(
                    exerciseNames.get(nextExerciseIndex),
                    exerciseDetails.get(nextExerciseIndex)
            );

            // Format the reps display
            String repsDisplay = "X " + extractRepsNumber(setsRepsInfo);
            tvNextExerciseReps.setText(repsDisplay);

            // Load exercise GIF
            String imageUrl = exerciseImageUrls.get(nextExerciseIndex);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .asGif()
                        .load(imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivNextExerciseImage);
            }
        }
    }

    private String getCleanExerciseName(String fullName) {
        String cleaned = fullName;
        cleaned = cleaned.replaceAll("(?i)\\s*(each\\s+side\\s*)?x\\s*\\d+.*$", "");
        cleaned = cleaned.replaceAll("(?i)\\s*\\d+\\s*sets?.*$", "");
        cleaned = cleaned.replaceAll("(?i)\\s*-\\s*\\d+.*$", "");
        return cleaned.trim();
    }

    private String extractSetsRepsInfo(String exerciseNameInput, String exerciseDetails) {
        if (exerciseDetails != null) {
            int sets = 1;
            int reps = 1;
            String[] lines = exerciseDetails.split("\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("Sets: ")) {
                    try {
                        sets = Integer.parseInt(line.substring(6).trim());
                    } catch (NumberFormatException e) { /* Keep default */ }
                } else if (line.startsWith("Reps: ")) {
                    try {
                        reps = Integer.parseInt(line.substring(6).trim());
                    } catch (NumberFormatException e) { /* Keep default */ }
                }
            }
            if (sets > 0 && reps > 0) {
                return sets + " sets x " + reps + " reps";
            }
        }
        return "Follow the instructions";
    }

    private String extractRepsNumber(String setsRepsInfo) {
        // Extract just the number of reps from "X sets x Y reps"
        if (setsRepsInfo.contains(" x ")) {
            String[] parts = setsRepsInfo.split(" x ");
            if (parts.length >= 2) {
                return parts[1].replaceAll("[^0-9]", "");
            }
        }
        return "12"; // Default
    }

    private void startTimer() {
        updateTimerText();
        timer = new CountDownTimer(remainingRestMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingRestMillis = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                goToNextExercise();
            }
        }.start();
    }

    private void updateTimerText() {
        int secondsLeft = (int) (remainingRestMillis / 1000);
        int minutes = secondsLeft / 60;
        int seconds = secondsLeft % 60;
        tvRestTimer.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void goToNextExercise() {
        if (nextExerciseIndex >= exerciseNames.size()) {
            showWorkoutCompleteScreen();
            return;
        }

        Intent intent = new Intent(RestTimerActivity.this, WorkoutSessionActivity.class);
        intent.putExtra("currentIndex", nextExerciseIndex);
        intent.putStringArrayListExtra("exerciseNames", exerciseNames);
        intent.putStringArrayListExtra("exerciseDetails", exerciseDetails);
        intent.putStringArrayListExtra("exerciseImageUrls", exerciseImageUrls);
        intent.putIntegerArrayListExtra("exerciseTimes", exerciseTimes);
        intent.putIntegerArrayListExtra("exerciseRests", exerciseRests);
        intent.putIntegerArrayListExtra("completedSetsPerExercise", completedSetsPerExercise);
        intent.putIntegerArrayListExtra("totalSetsPerExercise", totalSetsPerExercise);
        intent.putExtra("performanceData", performanceDataList);
        intent.putExtra("workoutStartTime", workoutStartTime);
        intent.putExtra("workout_name", workoutName);

        startActivity(intent);
        finish();
    }

    private void showWorkoutCompleteScreen() {
        new AlertDialog.Builder(this)
                .setTitle("Workout Complete!")
                .setMessage("You have finished all exercises.")
                .setPositiveButton("View Summary", (dialog, which) -> {
                    Intent intent = new Intent(RestTimerActivity.this, activity_workout_complete.class);
                    intent.putExtra("workout_name", workoutName);
                    intent.putExtra("total_exercises", exerciseNames != null ? exerciseNames.size() : 0);
                    long durationMillis = System.currentTimeMillis() - workoutStartTime;
                    long minutes = (durationMillis / 1000) / 60;
                    long seconds = (durationMillis / 1000) % 60;
                    intent.putExtra("workout_duration", String.format("%02d:%02d", minutes, seconds));
                    intent.putExtra("performanceData", performanceDataList);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timer != null) timer.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }


    private void showExitWorkoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle("⚠️ Exit Workout?");
        builder.setMessage("Are you sure you want to exit?\n\n" +
                "WARNING: Your workout progress will NOT be saved if you exit now.\n\n" +
                "All completed exercises and performance data will be lost.");

        builder.setPositiveButton("Yes, Exit", (dialog, which) -> {
            if (timer != null) timer.cancel();
            finish();
        });

        builder.setNegativeButton("Continue Workout", (dialog, which) -> {
            dialog.dismiss();
            // Restart the timer since we paused it
            startTimer();
        });

        builder.setCancelable(true);

        AlertDialog dialog = builder.create();

        // Apply rounded background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }

        dialog.show();

        // Style the buttons
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_green_dark));
    }
}