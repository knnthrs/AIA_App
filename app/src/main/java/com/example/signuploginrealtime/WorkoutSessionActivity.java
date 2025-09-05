package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import java.util.ArrayList;
import java.util.Locale;

public class WorkoutSessionActivity extends AppCompatActivity {

    private TextView tvName, tvDetails, tvProgress;
    private ImageView ivExercise;
    private AppCompatButton btnPause, btnDone, btnPrevious, btnSkip;
    private ProgressBar progressBar;

    private ArrayList<String> exerciseNames;
    private ArrayList<String> exerciseDetails;
    private ArrayList<Integer> exerciseRests;
    private ArrayList<Integer> exerciseReps;

    private int currentIndex = 0;
    private int currentRep = 1;
    private boolean isPaused = false;
    private boolean ttsReady = false;
    private boolean isTransitioning = false; // Prevent multiple transitions
    private boolean isActivityFinished = false; // Track if activity is being finished

    private CountDownTimer timer, prepTimer;
    private long timeLeftMillis;
    private int repDuration = 3000;

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_session);

        ivExercise = findViewById(R.id.iv_exercise_animation);
        tvName = findViewById(R.id.tv_exercise_name);
        tvDetails = findViewById(R.id.tv_exercise_details);
        tvProgress = findViewById(R.id.tv_exercise_progress);
        btnPause = findViewById(R.id.btn_pause);
        btnDone = findViewById(R.id.btn_done);
        btnPrevious = findViewById(R.id.btn_previous);
        btnSkip = findViewById(R.id.btn_skip);
        progressBar = findViewById(R.id.progress_bar);

        exerciseNames = getIntent().getStringArrayListExtra("names");
        exerciseDetails = getIntent().getStringArrayListExtra("details");
        exerciseRests = getIntent().getIntegerArrayListExtra("rests");
        exerciseReps = getIntent().getIntegerArrayListExtra("reps");
        currentIndex = getIntent().getIntExtra("currentIndex", 0);

        // Clear placeholder UI immediately
        tvName.setText("");
        tvDetails.setText("");
        tvProgress.setText("");

        // Show loading state or first exercise info without starting timers
        if (exerciseNames != null && !exerciseNames.isEmpty()) {
            tvName.setText(exerciseNames.get(currentIndex));
            tvDetails.setText(exerciseDetails.get(currentIndex));
            tvProgress.setText("0/" + exerciseReps.get(currentIndex));
            progressBar.setMax(exerciseNames.size());
            progressBar.setProgress(currentIndex);
        }

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                ttsReady = true;

                // Add a small delay to ensure TTS is fully ready
                tvName.postDelayed(() -> {
                    if (exerciseNames != null && !exerciseNames.isEmpty() && !isActivityFinished) {
                        startPrepCountdown(currentIndex);
                    }
                }, 500);
            }
        });

        btnPause.setOnClickListener(v -> {
            if (!isTransitioning) {
                togglePause();
            }
        });

        btnDone.setOnClickListener(v -> {
            if (!isTransitioning) {
                moveToNextExercise();
            }
        });

        btnSkip.setOnClickListener(v -> {
            if (!isTransitioning) {
                moveToNextExercise();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (!isTransitioning) {
                moveToPreviousExercise();
            }
        });
    }

    private void showExercise(int index) {
        if (isActivityFinished || isTransitioning) {
            return;
        }

        if (index >= exerciseNames.size()) {
            // Mark as transitioning to prevent further operations
            isTransitioning = true;
            cleanupAndFinish();

            Intent intent = new Intent(this, activity_workout_complete.class);
            intent.putExtra("workout_name", "Full Body Workout");
            intent.putExtra("total_exercises", exerciseNames.size());
            intent.putExtra("workout_duration", calculateWorkoutDuration());
            startActivity(intent);
            finish();
            return;
        }

        tvName.setText(exerciseNames.get(index));
        tvDetails.setText(exerciseDetails.get(index));
        currentRep = 1;

        progressBar.setMax(exerciseNames.size());
        progressBar.setProgress(currentIndex);

        tvProgress.setText("0/" + exerciseReps.get(index));

        // Only start prep countdown if TTS is ready and not transitioning
        if (ttsReady && !isTransitioning && !isActivityFinished) {
            startPrepCountdown(index);
        }
    }

    private String calculateWorkoutDuration() {
        return "30 minutes";
    }

    private void startPrepCountdown(int index) {
        if (isActivityFinished || isTransitioning) {
            return;
        }

        // Cancel any existing timers first
        cancelAllTimers();

        String[] prep = {"Ready", "1", "2", "3", "Go"};

        prepTimer = new CountDownTimer(prep.length * 1000 + 1000, 1000) {
            int step = 0;

            @Override
            public void onTick(long millisUntilFinished) {
                if (isPaused || isActivityFinished || isTransitioning) return;

                if (step < prep.length && ttsReady) {
                    if (tts != null && !tts.isSpeaking()) {
                        tts.speak(prep[step], TextToSpeech.QUEUE_FLUSH, null, "prep_" + step);
                    }
                    step++;
                }
            }

            @Override
            public void onFinish() {
                if (!isPaused && !isActivityFinished && !isTransitioning) {
                    startRepTimer(index);
                }
            }
        }.start();
    }

    private void startRepTimer(int index) {
        if (isActivityFinished || isTransitioning) {
            return;
        }

        int totalReps = exerciseReps.get(index);

        if (timeLeftMillis == 0) timeLeftMillis = repDuration;

        timer = new CountDownTimer(timeLeftMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isActivityFinished || isTransitioning) {
                    cancel();
                    return;
                }
                timeLeftMillis = millisUntilFinished;
            }

            @Override
            public void onFinish() {
                if (isPaused || isActivityFinished || isTransitioning) return;

                if (currentRep <= totalReps) {
                    if (ttsReady && tts != null) {
                        tts.speak(String.valueOf(currentRep), TextToSpeech.QUEUE_FLUSH, null, "rep_" + currentRep);
                    }
                    tvProgress.setText(currentRep + "/" + totalReps);
                    currentRep++;
                    timeLeftMillis = 0;
                    startRepTimer(index);
                } else {
                    progressBar.setProgress(currentIndex + 1);
                    // Check if this is the last exercise
                    if (currentIndex + 1 >= exerciseNames.size()) {
                        // Mark as transitioning and go to completion
                        isTransitioning = true;
                        cleanupAndFinish();

                        Intent intent = new Intent(WorkoutSessionActivity.this, activity_workout_complete.class);
                        intent.putExtra("workout_name", "Full Body Workout");
                        intent.putExtra("total_exercises", exerciseNames.size());
                        intent.putExtra("workout_duration", calculateWorkoutDuration());
                        startActivity(intent);
                        finish();
                    } else {
                        goToRest(exerciseRests.get(index));
                    }
                }
            }
        }.start();
    }

    private void togglePause() {
        if (!isPaused) {
            cancelAllTimers();
            if (tts != null) tts.stop();
            btnPause.setText("RESUME");
            isPaused = true;
        } else {
            btnPause.setText("PAUSE");
            isPaused = false;
            resumeTimer();
        }
    }

    private void resumeTimer() {
        if (timeLeftMillis > 0) {
            startRepTimer(currentIndex);
        } else {
            startRepTimer(currentIndex);
        }
    }

    private void goToRest(int restSeconds) {
        if (isTransitioning || isActivityFinished) {
            return;
        }

        // Mark as transitioning to prevent multiple calls
        isTransitioning = true;

        cleanupAndFinish();

        Intent intent = new Intent(this, activity_rest.class);
        intent.putExtra("restDuration", restSeconds);
        intent.putExtra("nextIndex", currentIndex + 1);
        intent.putStringArrayListExtra("names", exerciseNames);
        intent.putStringArrayListExtra("details", exerciseDetails);
        intent.putIntegerArrayListExtra("rests", exerciseRests);
        intent.putIntegerArrayListExtra("reps", exerciseReps);
        startActivity(intent);
        finish();
    }

    private void moveToNextExercise() {
        if (isTransitioning || isActivityFinished) {
            return;
        }

        cleanupTimersAndTTS();
        currentIndex++;
        timeLeftMillis = 0;
        showExercise(currentIndex);
    }

    private void moveToPreviousExercise() {
        if (isTransitioning || isActivityFinished) {
            return;
        }

        cleanupTimersAndTTS();
        if (currentIndex > 0) currentIndex--;
        timeLeftMillis = 0;
        showExercise(currentIndex);
    }

    private void cancelAllTimers() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (prepTimer != null) {
            prepTimer.cancel();
            prepTimer = null;
        }
    }

    private void cleanupTimersAndTTS() {
        cancelAllTimers();
        if (tts != null) tts.stop();
    }

    private void cleanupAndFinish() {
        isActivityFinished = true;
        cleanupTimersAndTTS();
    }

    @Override
    protected void onDestroy() {
        cleanupAndFinish();
        if (tts != null) {
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Don't mark as finished, just cleanup timers
        cleanupTimersAndTTS();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityFinished = true;
        cleanupTimersAndTTS();
    }
}