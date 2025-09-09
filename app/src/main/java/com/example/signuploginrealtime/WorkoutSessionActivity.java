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
import android.widget.FrameLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.Locale;

public class WorkoutSessionActivity extends AppCompatActivity {

    private TextView tvExerciseName, tvExerciseDetails, tvExerciseTimer, tvExerciseProgress, tvNoImage;
    private TextView btnPrevious, btnSkip; // Previous and Skip buttons
    private ImageView ivExerciseImage;
    private Button btnPause, btnNext;
    private ProgressBar progressBar; // Progress bar

    private ArrayList<String> exerciseNames;
    private ArrayList<String> exerciseDetails;
    private ArrayList<String> exerciseImageUrls;
    private ArrayList<Integer> exerciseDurations;
    private ArrayList<Integer> exerciseRests;

    private int currentIndex = 0;
    private CountDownTimer timer;
    private boolean isTimerRunning = false;
    private long timeLeftMillis;
    private long workoutStartTime; // For duration calculation

    private ImageView ivInfo;
    private TextView tvInstructions;
    private FrameLayout flInstructionsOverlay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_session);

        // ✅ Fix notch / status bar overlap
        View rootView = findViewById(R.id.root_layout);
        rootView.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(
                    v.getPaddingLeft(),
                    insets.getSystemWindowInsetTop(),
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );
            return insets.consumeSystemWindowInsets();
        });

        // Initialize views
        tvExerciseName = findViewById(R.id.tv_exercise_name);
        tvExerciseDetails = findViewById(R.id.tv_exercise_details);
        tvExerciseTimer = findViewById(R.id.tv_exercise_timer);
        tvExerciseProgress = findViewById(R.id.tv_exercise_progress);
        ivExerciseImage = findViewById(R.id.iv_exercise_image);
        tvNoImage = findViewById(R.id.tv_no_image);
        btnPause = findViewById(R.id.btn_pause);
        btnNext = findViewById(R.id.btn_done);
        btnPrevious = findViewById(R.id.btn_previous);
        btnSkip = findViewById(R.id.btn_skip);
        progressBar = findViewById(R.id.progress_bar);
        ivInfo = findViewById(R.id.iv_info);
        tvInstructions = findViewById(R.id.tv_instructions); // This is the TextView inside the overlay
        flInstructionsOverlay = findViewById(R.id.fl_instructions_overlay);



        // Get exercise data
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

        // ✅ Start workout timer reference
        workoutStartTime = System.currentTimeMillis();

        // Restore current index if coming from rest
        currentIndex = getIntent().getIntExtra("currentIndex", 0);

        showExercise(currentIndex);

        // Button listeners
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
        btnPrevious.setOnClickListener(v -> moveToPreviousExercise());
        btnSkip.setOnClickListener(v -> skipCurrentExercise());


        // ivInfo OnClickListener
        ivInfo.setOnClickListener(v -> {
            if (exerciseDetails != null && currentIndex < exerciseDetails.size()) {
                tvInstructions.setText(exerciseDetails.get(currentIndex));
            }
            tvInstructions.setVisibility(View.VISIBLE); // Ensure TextView is visible
            flInstructionsOverlay.setVisibility(View.VISIBLE);
            flInstructionsOverlay.setAlpha(0f);
            flInstructionsOverlay.animate().alpha(1f).setDuration(300).start();
        });
        // Make the overlay itself clickable to hide
        flInstructionsOverlay.setOnClickListener(v -> {
            flInstructionsOverlay.animate().alpha(0f).setDuration(300)
                    .withEndAction(() -> flInstructionsOverlay.setVisibility(View.GONE))
                    .start();
        });




    }

    private void showExercise(int index) {
        tvExerciseName.setText(exerciseNames.get(index));
        // The original tvExerciseDetails is kept for reps/sets or other short details if needed.
        // tvExerciseDetails.setText(exerciseDetails.get(index)); 
        tvExerciseProgress.setText((index + 1) + "/" + exerciseNames.size());

        // Set instructions for the toggleable TextView
        if (tvInstructions != null && exerciseDetails != null && index < exerciseDetails.size()) {
            String details = exerciseDetails.get(index);
            tvInstructions.setText(details);
            tvInstructions.setVisibility(View.GONE); // always start hidden
        }

        updateProgressBar();
        updateButtonStates();

        String imageUrl = exerciseImageUrls.get(index);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            tvNoImage.setVisibility(View.GONE);
            ivExerciseImage.setVisibility(View.VISIBLE);

            // ✅ Use Glide with GIF support
            Glide.with(this)
                    .asGif()
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // cache full version
                    .dontTransform() // prevent automatic scaling
                    .override(800, 800) // HD but safe for memory (tweak size if needed)
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
        if (btnPrevious != null) {
            btnPrevious.setEnabled(currentIndex > 0);
            btnPrevious.setAlpha(currentIndex > 0 ? 1.0f : 0.5f);
        }
        if (btnSkip != null) {
            btnSkip.setEnabled(currentIndex < exerciseNames.size() - 1);
            btnSkip.setAlpha(currentIndex < exerciseNames.size() - 1 ? 1.0f : 0.5f);
        }
    }

    private void moveToPreviousExercise() {
        if (currentIndex <= 0) {
            Toast.makeText(this, "This is the first exercise", Toast.LENGTH_SHORT).show();
            return;
        }
        if (timer != null) timer.cancel();
        isTimerRunning = false;
        currentIndex--;
        showExercise(currentIndex);
    }

    private void skipCurrentExercise() {
        if (timer != null) timer.cancel();
        isTimerRunning = false;

        if (currentIndex >= exerciseNames.size() - 1) {
            showSkipLastExerciseDialog();
            return;
        }

        currentIndex++;
        showExercise(currentIndex);
    }

    private void showSkipLastExerciseDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Finish Workout?")
                .setMessage("This is the last exercise. Skipping it will end your workout session. Do you want to finish the workout now?")
                .setPositiveButton("Yes, Finish Workout", (dialog, which) -> {
                    Intent intent = new Intent(WorkoutSessionActivity.this, activity_workout_complete.class);
                    intent.putExtra("workout_name", getIntent().getStringExtra("workout_name"));
                    intent.putExtra("total_exercises", exerciseNames.size());
                    intent.putExtra("workout_duration", calculateWorkoutDuration());
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    startTimer((int) (timeLeftMillis / 1000));
                    dialog.dismiss();
                })
                .setCancelable(false)
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

        int nextIndex = currentIndex + 1;
        Intent intent = new Intent(WorkoutSessionActivity.this, RestTimerActivity.class);
        intent.putExtra("nextIndex", nextIndex);
        intent.putStringArrayListExtra("exerciseNames", exerciseNames);
        intent.putStringArrayListExtra("exerciseDetails", exerciseDetails);
        intent.putStringArrayListExtra("exerciseImageUrls", exerciseImageUrls);
        intent.putIntegerArrayListExtra("exerciseTimes", exerciseDurations);
        intent.putIntegerArrayListExtra("exerciseRests", exerciseRests);

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

    private String calculateWorkoutDuration() {
        long durationMillis = System.currentTimeMillis() - workoutStartTime;
        long minutes = (durationMillis / 1000) / 60;
        long seconds = (durationMillis / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}
