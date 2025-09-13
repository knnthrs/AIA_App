package com.example.signuploginrealtime;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.util.Log; // Added for logging if needed
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.Locale;

public class WorkoutSessionActivity extends AppCompatActivity {

    private TextView tvExerciseName, tvExerciseDetails, tvExerciseTimer, tvNoImage;
    private TextView btnPrevious, btnSkip;
    private ImageView ivExerciseImage;
    private Button btnPause, btnNext;
    private ProgressBar progressBar;

    private ArrayList<String> exerciseNames;
    private ArrayList<String> exerciseDetails;
    private ArrayList<String> exerciseImageUrls;
    private ArrayList<Integer> exerciseDurations;
    private ArrayList<Integer> exerciseRests;

    private int currentIndex = 0;
    private CountDownTimer timer;
    private CountDownTimer repTimer;
    private CountDownTimer readyTimer;
    private boolean isTimerRunning = false;
    private boolean isReadyCountdown = false;
    private long timeLeftMillis;
    private long workoutStartTime;

    private ImageView ivInfo;
    private TextView tvInstructions;
    private FrameLayout flInstructionsOverlay;

    private TextToSpeech tts;
    private int currentRepCount = 0;
    private boolean isRepetitionBased = false;
    private boolean isCounterPaused = false;

    private ArrayList<ExercisePerformanceData> performanceDataList;
    private long currentExerciseStartTimeMillis;
    private static final String TAG = "WorkoutSessionActivity"; // For logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_session);

        performanceDataList = new ArrayList<>();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());
            }
        });

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

        tvExerciseName = findViewById(R.id.tv_exercise_name);
        tvExerciseDetails = findViewById(R.id.tv_exercise_details);
        tvExerciseTimer = findViewById(R.id.tv_exercise_timer);
        ivExerciseImage = findViewById(R.id.iv_exercise_image);
        tvNoImage = findViewById(R.id.tv_no_image);
        btnPause = findViewById(R.id.btn_pause);
        btnNext = findViewById(R.id.btn_done);
        btnPrevious = findViewById(R.id.btn_previous);
        btnSkip = findViewById(R.id.btn_skip);
        progressBar = findViewById(R.id.progress_bar);
        ivInfo = findViewById(R.id.iv_info);
        tvInstructions = findViewById(R.id.tv_instructions);
        flInstructionsOverlay = findViewById(R.id.fl_instructions_overlay);

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

        workoutStartTime = System.currentTimeMillis();
        currentIndex = getIntent().getIntExtra("currentIndex", 0);
        showExercise(currentIndex);

        btnNext.setOnClickListener(v -> {
            if (!isReadyCountdown) {
                if (isRepetitionBased) {
                    recordAndLogExercisePerformance(currentRepCount, 0, "completed");
                } else {
                    int actualDuration = 0;
                    if (exerciseDurations != null && currentIndex < exerciseDurations.size()) {
                        actualDuration = exerciseDurations.get(currentIndex) - (int) (timeLeftMillis / 1000);
                        if (timeLeftMillis == 0) { // If timer finished, actualDuration is targetDuration
                            actualDuration = exerciseDurations.get(currentIndex);
                        }
                    }
                    recordAndLogExercisePerformance(0, Math.max(0, actualDuration), "completed");
                }
                moveToNextExercise();
            }
        });

        btnPause.setOnClickListener(v -> {
            if (isReadyCountdown) {
                if (isTimerRunning) {
                    if (readyTimer != null) readyTimer.cancel();
                    isTimerRunning = false;
                    btnPause.setText("RESUME");
                } else {
                    startReadyCountdown();
                }
            } else if (isRepetitionBased) {
                if (isCounterPaused) {
                    resumeRepCounter();
                } else {
                    pauseRepCounter();
                }
            } else {
                if (isTimerRunning) {
                    if (timer != null) timer.cancel();
                    isTimerRunning = false;
                    btnPause.setText("RESUME");
                } else {
                    startTimer((int) (timeLeftMillis / 1000));
                }
            }
        });
        btnPrevious.setOnClickListener(v -> moveToPreviousExercise());
        btnSkip.setOnClickListener(v -> skipCurrentExercise());

        ivInfo.setOnClickListener(v -> {
            if (exerciseDetails != null && currentIndex < exerciseDetails.size()) {
                String fullInstructions = getFullExerciseInstructions(currentIndex);
                tvInstructions.setText(fullInstructions);
            }
            tvInstructions.setVisibility(View.VISIBLE);
            flInstructionsOverlay.setVisibility(View.VISIBLE);
            flInstructionsOverlay.setAlpha(0f);
            flInstructionsOverlay.animate().alpha(1f).setDuration(300).start();
        });
        flInstructionsOverlay.setOnClickListener(v ->
                flInstructionsOverlay.animate().alpha(0f).setDuration(300)
                        .withEndAction(() -> flInstructionsOverlay.setVisibility(View.GONE))
                        .start()
        );
    }

    // MODIFIED: Added status parameter
    private void recordAndLogExercisePerformance(int actualRepsAchieved, int actualDurationAchievedSeconds, String status) {
        if (currentIndex < 0 || currentIndex >= exerciseNames.size()) {
            Log.e(TAG, "recordAndLogExercisePerformance: Invalid currentIndex: " + currentIndex);
            return;
        }

        String name = getCleanExerciseName(exerciseNames.get(currentIndex));
        int targetReps = 0;
        int targetDuration = 0;

        if (isRepetitionBased) {
            targetReps = getTargetReps(currentIndex);
        } else {
            if (exerciseDurations != null && currentIndex < exerciseDurations.size()) {
                targetDuration = exerciseDurations.get(currentIndex);
            }
        }

        ExercisePerformanceData performance = new ExercisePerformanceData(
                name,
                targetReps,
                actualRepsAchieved,
                targetDuration,
                actualDurationAchievedSeconds,
                status // MODIFIED: Pass status
        );
        Log.d(TAG, "Recording performance: " + performance.toString());
        performanceDataList.add(performance);
    }

    // FIXED: Updated showExercise method
    private void showExercise(int index) {
        String cleanExerciseName = getCleanExerciseName(exerciseNames.get(index));
        tvExerciseName.setText(cleanExerciseName);
        String setsRepsInfo = extractSetsRepsInfo(exerciseNames.get(index), exerciseDetails.get(index));
        tvExerciseDetails.setText(setsRepsInfo);

        if (tvInstructions != null && exerciseDetails != null && index < exerciseDetails.size()) {
            String fullInstructions = getFullExerciseInstructions(index);
            tvInstructions.setText(fullInstructions);
            tvInstructions.setVisibility(View.GONE);
        }

        updateProgressBar();
        updateButtonStates();
        loadExerciseImage(index);

        startReadyCountdown();
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

    private String getFullExerciseInstructions(int index) {
        if (exerciseDetails != null && index < exerciseDetails.size()) {
            return exerciseDetails.get(index);
        }
        return "No instructions available";
    }

    private void loadExerciseImage(int index) {
        String imageUrl = exerciseImageUrls.get(index);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            tvNoImage.setVisibility(View.GONE);
            ivExerciseImage.setVisibility(View.VISIBLE);
            Glide.with(this).asGif().load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .dontTransform().override(800, 800)
                    .into(ivExerciseImage);
        } else {
            ivExerciseImage.setVisibility(View.GONE);
            tvNoImage.setVisibility(View.VISIBLE);
        }
    }

    // FIXED: Updated ready countdown method
    private void startReadyCountdown() {
        isReadyCountdown = true;
        isTimerRunning = true;
        btnPause.setText("PAUSE");

        // FIXED: Hide timer during ready countdown (as you wanted)
        tvExerciseTimer.setVisibility(View.INVISIBLE);

        readyTimer = new CountDownTimer(5000, 1000) { // 5 seconds: 4,3,2,1,Start
            int countdownNumber = 4;

            @Override
            public void onTick(long millisUntilFinished) {
                if (countdownNumber > 0) {
                    // FIXED: Only TTS, no visual display during ready countdown
                    if (tts != null) {
                        if (countdownNumber == 4) {
                            // First announcement with exercise name
                            String exerciseName = getCleanExerciseName(exerciseNames.get(currentIndex));
                            tts.speak("Get ready for " + exerciseName, TextToSpeech.QUEUE_FLUSH, null, "GET_READY");
                            // Then start countdown
                            tts.speak(String.valueOf(countdownNumber), TextToSpeech.QUEUE_ADD, null, "READY_" + countdownNumber);
                        } else {
                            tts.speak(String.valueOf(countdownNumber), TextToSpeech.QUEUE_ADD, null, "READY_" + countdownNumber);
                        }
                    }
                    countdownNumber--;
                } else {
                    // Final "Start!" announcement
                    if (tts != null) {
                        tts.speak("Start!", TextToSpeech.QUEUE_ADD, null, "READY_START");
                    }
                }
            }

            @Override
            public void onFinish() {
                startActualExercise();
            }
        };
        readyTimer.start();
    }

    // FIXED: Updated startActualExercise method
    private void startActualExercise() {
        currentExerciseStartTimeMillis = System.currentTimeMillis();
        isReadyCountdown = false;
        btnNext.setText("▶");

        String exerciseName = exerciseNames.get(currentIndex).toLowerCase();
        String exerciseDetailText = (exerciseDetails != null && currentIndex < exerciseDetails.size())
                ? exerciseDetails.get(currentIndex).toLowerCase() : "";

        isRepetitionBased = exerciseName.contains("reps") || exerciseName.contains("repetition")
                || exerciseName.contains(" x ") || exerciseName.contains("sets")
                || exerciseDetailText.contains("reps:") || exerciseDetailText.contains("sets:")
                || exerciseDetailText.contains("repetition");

        if (exerciseDurations != null && currentIndex < exerciseDurations.size() && exerciseDurations.get(currentIndex) <= 0) {
            isRepetitionBased = true;
        }

        // FIXED: Show timer only when actual exercise starts
        tvExerciseTimer.setVisibility(View.VISIBLE);

        if (isRepetitionBased) {
            startRepCounter();
        } else {
            if (exerciseDurations != null && currentIndex < exerciseDurations.size()) {
                startTimer(exerciseDurations.get(currentIndex));
            } else {
                startTimer(30);
            }
        }
    }

    // FIXED: Updated startRepCounter method
    private void startRepCounter() {
        currentRepCount = 0;
        isCounterPaused = false;
        isTimerRunning = true;
        int targetReps = getTargetReps(currentIndex);

        // FIXED: Show reps counter immediately
        tvExerciseTimer.setVisibility(View.VISIBLE);
        tvExerciseTimer.setText("0/" + targetReps);
        btnPause.setText("PAUSE");

        startActualRepCounting(targetReps);
    }

    // FIXED: Updated startActualRepCounting method
    private void startActualRepCounting(int targetReps) {
        if (repTimer != null) repTimer.cancel();

        repTimer = new CountDownTimer(Long.MAX_VALUE, 3000) { // 3s per rep
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isCounterPaused && currentRepCount < targetReps) {
                    currentRepCount++;

                    // FIXED: Update visual reps counter
                    tvExerciseTimer.setText(currentRepCount + "/" + targetReps);

                    // FIXED: TTS announces each rep count
                    if (tts != null) {
                        // Add motivational phrases at key points
                        if (currentRepCount == targetReps / 2 && targetReps > 4) {
                            tts.speak("Halfway! " + currentRepCount, TextToSpeech.QUEUE_ADD, null, "REP_HALFWAY");
                        } else if (currentRepCount == targetReps - 2 && targetReps > 3) {
                            tts.speak("Last 2! " + currentRepCount, TextToSpeech.QUEUE_ADD, null, "REP_LAST2");
                        } else {
                            // FIXED: Always announce each rep count
                            tts.speak(String.valueOf(currentRepCount), TextToSpeech.QUEUE_ADD, null, "REP_" + currentRepCount);
                        }
                    }

                    if (currentRepCount >= targetReps) {
                        repTimer.cancel();
                        isTimerRunning = false;
                        if (tts != null) {
                            tts.speak("Exercise complete! Great job!", TextToSpeech.QUEUE_ADD, null, "REP_COMPLETE");
                        }
                        recordAndLogExercisePerformance(currentRepCount, 0, "completed");
                        tvExerciseTimer.postDelayed(() -> moveToNextExercise(), 2000);
                    }
                }
            }

            @Override
            public void onFinish() { }
        };

        repTimer.start();
    }

    private int getTargetReps(int exerciseIndex) {
        if (exerciseNames != null && exerciseIndex < exerciseNames.size()){
            String exerciseName = exerciseNames.get(exerciseIndex);
            // Attempt to parse reps like "... x 10 ..." or "... 10 reps ..."
            if (exerciseName.toLowerCase().contains("x ")) {
                try {
                    String afterX = exerciseName.substring(exerciseName.toLowerCase().indexOf("x ") + 2).trim();
                    String[] parts = afterX.split("\\s+");
                    if (parts.length > 0) {
                        return Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                    }
                } catch (Exception e) { /* Continue to next check */ }
            }
        }
        if (exerciseDetails != null && exerciseIndex < exerciseDetails.size()) {
            String details = exerciseDetails.get(exerciseIndex);
            if (details.contains("Reps: ")) {
                try {
                    String repsLine = details.substring(details.indexOf("Reps: ") + 6);
                    String repsStr = repsLine.split("\\n")[0].trim(); // Get first line after "Reps: "
                    return Integer.parseInt(repsStr.replaceAll("[^0-9]", ""));
                } catch (Exception e) { /* Fallback to default */ }
            }
        }
        return 15; // Default target reps
    }

    // FIXED: Updated pauseRepCounter method
    private void pauseRepCounter() {
        isCounterPaused = true;
        isTimerRunning = false;
        btnPause.setText("RESUME");
    }

    // FIXED: Updated resumeRepCounter method
    private void resumeRepCounter() {
        isCounterPaused = false;
        isTimerRunning = true;
        btnPause.setText("PAUSE");
    }

    private void updateProgressBar() {
        if (progressBar != null && exerciseNames != null && !exerciseNames.isEmpty()) {
            int progress = (int) (((float) (currentIndex + 1) / exerciseNames.size()) * 100);
            progressBar.setProgress(progress);
        }
    }

    private void updateButtonStates() {
        if (btnPrevious != null) {
            btnPrevious.setEnabled(currentIndex > 0);
            btnPrevious.setAlpha(currentIndex > 0 ? 1.0f : 0.5f);
        }
    }

    private void moveToPreviousExercise() {
        if (currentIndex <= 0) {
            Toast.makeText(this, "This is the first exercise", Toast.LENGTH_SHORT).show();
            return;
        }
        cancelAllTimers();
        currentIndex--;
        showExercise(currentIndex);
    }

    private void skipCurrentExercise() {
        Log.d(TAG, "Skipping exercise: " + exerciseNames.get(currentIndex));
        recordAndLogExercisePerformance(0, 0, "skipped");
        cancelAllTimers();
        if (currentIndex >= exerciseNames.size() - 1) {
            showSkipLastExerciseDialog();
            return;
        }
        currentIndex++;
        showExercise(currentIndex);
    }

    private void cancelAllTimers() {
        if (timer != null) timer.cancel();
        if (repTimer != null) repTimer.cancel();
        if (readyTimer != null) readyTimer.cancel();
        isTimerRunning = false;
        isReadyCountdown = false;
    }

    private void showSkipLastExerciseDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Finish Workout?")
                .setMessage("This is the last exercise. Skipping it will end your workout session. Do you want to finish the workout now?")
                .setPositiveButton("Yes, Finish Workout", (dialog, which) -> {
                    // recordAndLogExercisePerformance(0, 0, "skipped"); // Already recorded by skipCurrentExercise
                    Intent intent = new Intent(WorkoutSessionActivity.this, activity_workout_complete.class);
                    intent.putExtra("workout_name", getIntent().getStringExtra("workout_name"));
                    intent.putExtra("total_exercises", exerciseNames != null ? exerciseNames.size() : 0);
                    intent.putExtra("workout_duration", calculateWorkoutDuration());
                    intent.putExtra("performanceData", performanceDataList);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    if (isReadyCountdown) {
                        startReadyCountdown();
                    } else if (isRepetitionBased) {
                        if (!isCounterPaused) resumeRepCounter(); // Resume rep counter if it was running
                    } // No need to resume duration timer as it would have auto-finished or been paused manually
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    // FIXED: Updated startTimer method with enhanced TTS
    private void startTimer(int seconds) {
        if (timer != null) timer.cancel();
        timeLeftMillis = seconds * 1000L;
        final int totalDurationForThisExercise = seconds;

        // Show timer for timed exercises
        tvExerciseTimer.setVisibility(View.VISIBLE);
        tvExerciseTimer.setText(seconds + "s");

        timer = new CountDownTimer(timeLeftMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMillis = millisUntilFinished;
                int secondsLeft = (int) (millisUntilFinished / 1000);
                tvExerciseTimer.setText(secondsLeft + "s");

                // TTS announcements for timed exercises
                if (tts != null) {
                    switch (secondsLeft) {
                        case 30:
                            if (totalDurationForThisExercise > 30) {
                                tts.speak("30 seconds remaining", TextToSpeech.QUEUE_ADD, null, "THIRTY_SEC");
                            }
                            break;
                        case 20:
                            if (totalDurationForThisExercise > 20) {
                                tts.speak("20 seconds", TextToSpeech.QUEUE_ADD, null, "TWENTY_SEC");
                            }
                            break;
                        case 10:
                            tts.speak("10 seconds left", TextToSpeech.QUEUE_ADD, null, "TEN_SEC");
                            break;
                        case 5:
                        case 4:
                        case 3:
                        case 2:
                        case 1:
                            tts.speak(String.valueOf(secondsLeft), TextToSpeech.QUEUE_ADD, null, "COUNT_" + secondsLeft);
                            break;
                    }

                    // Motivational phrase at halfway point
                    if (secondsLeft == totalDurationForThisExercise / 2 && totalDurationForThisExercise >= 20) {
                        String[] motivationalPhrases = {
                                "Keep going!", "You're doing great!", "Halfway there!", "Stay strong!"
                        };
                        String phrase = motivationalPhrases[currentIndex % motivationalPhrases.length];
                        tts.speak(phrase, TextToSpeech.QUEUE_ADD, null, "MOTIVATION");
                    }
                }
            }

            @Override
            public void onFinish() {
                tvExerciseTimer.setText("DONE!");
                timeLeftMillis = 0;
                isTimerRunning = false;

                if (tts != null) {
                    tts.speak("Exercise complete! Well done!", TextToSpeech.QUEUE_ADD, null, "TIMER_COMPLETE");
                }

                recordAndLogExercisePerformance(0, totalDurationForThisExercise, "completed");
                tvExerciseTimer.postDelayed(() -> moveToNextExercise(), 2000);
            }
        }.start();

        isTimerRunning = true;
        btnPause.setText("PAUSE");
    }

    private void moveToNextExercise() {
        cancelAllTimers();

        if (exerciseNames == null || currentIndex >= exerciseNames.size() - 1) { // Last exercise completed or skipped
            Intent intent = new Intent(WorkoutSessionActivity.this, activity_workout_complete.class);
            intent.putExtra("workout_name", getIntent().getStringExtra("workout_name"));
            intent.putExtra("total_exercises", exerciseNames != null ? exerciseNames.size() : 0);
            intent.putExtra("workout_duration", calculateWorkoutDuration());
            intent.putExtra("performanceData", performanceDataList);
            startActivity(intent);
            finish();
            return;
        }

        // Move to rest period before next exercise
        int nextIndex = currentIndex + 1; // current exercise is done, prepare for rest then next one
        Intent intent = new Intent(WorkoutSessionActivity.this, RestTimerActivity.class);
        // Pass all necessary data for RestTimerActivity to eventually restart WorkoutSessionActivity
        intent.putExtra("nextIndex", nextIndex);
        intent.putStringArrayListExtra("exerciseNames", exerciseNames);
        intent.putStringArrayListExtra("exerciseDetails", exerciseDetails);
        intent.putStringArrayListExtra("exerciseImageUrls", exerciseImageUrls);
        intent.putIntegerArrayListExtra("exerciseTimes", exerciseDurations);
        intent.putIntegerArrayListExtra("exerciseRests", exerciseRests);
        intent.putExtra("workout_name", getIntent().getStringExtra("workout_name"));
        intent.putExtra("performanceData", performanceDataList); // Pass along accumulated performance data
        intent.putExtra("workoutStartTime", workoutStartTime); // Pass workout start time


        startActivity(intent);
        finish(); // Finish current session, RestTimerActivity will start a new one
    }

    // FIXED: Updated onPause method
    @Override
    protected void onPause() {
        super.onPause();

        if (isReadyCountdown && readyTimer != null && isTimerRunning) {
            readyTimer.cancel();
            isTimerRunning = false;
            btnPause.setText("RESUME");
            // Keep timer hidden during ready countdown pause
            tvExerciseTimer.setVisibility(View.INVISIBLE);
        } else if (isRepetitionBased && isTimerRunning) {
            pauseRepCounter();
            // Keep reps counter visible during pause
        } else if (!isRepetitionBased && timer != null && isTimerRunning) {
            timer.cancel();
            isTimerRunning = false;
            btnPause.setText("RESUME");
            // Keep time display visible during pause
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelAllTimers();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    private String calculateWorkoutDuration() {
        long durationMillis = System.currentTimeMillis() - workoutStartTime;
        long minutes = (durationMillis / 1000) / 60;
        long seconds = (durationMillis / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}