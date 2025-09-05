package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class activity_rest extends AppCompatActivity {

    private TextView tvRestTimer;
    private Button btnSkip, btnAddTime;

    private CountDownTimer restTimer;
    private int restDuration;
    private int nextIndex;
    private boolean isTransitioning = false; // Prevent multiple transitions
    private boolean isActivityFinished = false; // Track if activity is being finished

    private ArrayList<String> exerciseNames;
    private ArrayList<String> exerciseDetails;
    private ArrayList<Integer> exerciseRests;
    private ArrayList<Integer> exerciseReps;

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rest);

        tvRestTimer = findViewById(R.id.tv_rest_timer);
        btnSkip = findViewById(R.id.btn_skip_rest);
        btnAddTime = findViewById(R.id.btn_add_time);

        // Get data
        restDuration = getIntent().getIntExtra("restDuration", 30);
        nextIndex = getIntent().getIntExtra("nextIndex", 0);
        exerciseNames = getIntent().getStringArrayListExtra("names");
        exerciseDetails = getIntent().getStringArrayListExtra("details");
        exerciseRests = getIntent().getIntegerArrayListExtra("rests");
        exerciseReps = getIntent().getIntegerArrayListExtra("reps");

        // Init TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS && !isActivityFinished) {
                tts.setLanguage(Locale.US);
                startRestTimer(restDuration);
            }
        });

        btnSkip.setOnClickListener(v -> {
            if (!isTransitioning) {
                skipRest();
            }
        });

        btnAddTime.setOnClickListener(v -> {
            if (!isTransitioning) {
                addTime();
            }
        });
    }

    private void skipRest() {
        if (isTransitioning || isActivityFinished) {
            return;
        }

        isTransitioning = true;
        stopRest();
        goToNextExercise();
    }

    private void addTime() {
        if (isTransitioning || isActivityFinished) {
            return;
        }

        stopRest();
        restDuration += 20;
        startRestTimer(restDuration);
    }

    private void startRestTimer(int seconds) {
        if (isActivityFinished || isTransitioning) {
            return;
        }

        if (restTimer != null) restTimer.cancel();

        restTimer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isActivityFinished || isTransitioning) {
                    cancel();
                    return;
                }

                int secs = (int) (millisUntilFinished / 1000);
                tvRestTimer.setText("Rest: " + secs + "s");

                if (tts != null) {
                    if (secs == 10 || secs == 5) {
                        tts.speak(secs + " seconds left", TextToSpeech.QUEUE_FLUSH, null, "rest_" + secs);
                    }
                }
            }

            @Override
            public void onFinish() {
                if (isActivityFinished || isTransitioning) {
                    return;
                }

                if (tts != null) {
                    tts.speak("Go!", TextToSpeech.QUEUE_FLUSH, null, "rest_go");
                }

                // Add small delay for "Go!" announcement
                tvRestTimer.postDelayed(() -> {
                    if (!isActivityFinished && !isTransitioning) {
                        isTransitioning = true;
                        goToNextExercise();
                    }
                }, 500);
            }
        }.start();
    }

    private void goToNextExercise() {
        if (isTransitioning && !isActivityFinished) {
            // Check if we've reached the end of exercises
            if (nextIndex >= exerciseNames.size()) {
                // Go to workout completion instead of next exercise
                Intent intent = new Intent(this, activity_workout_complete.class);
                intent.putExtra("workout_name", "Full Body Workout");
                intent.putExtra("total_exercises", exerciseNames.size());
                intent.putExtra("workout_duration", "30 minutes");
                startActivity(intent);
                finish();
                return;
            }

            Intent intent = new Intent(this, WorkoutSessionActivity.class);
            intent.putStringArrayListExtra("names", exerciseNames);
            intent.putStringArrayListExtra("details", exerciseDetails);
            intent.putIntegerArrayListExtra("rests", exerciseRests);
            intent.putIntegerArrayListExtra("reps", exerciseReps);
            intent.putExtra("currentIndex", nextIndex);
            startActivity(intent);
            finish();
        }
    }

    private void stopRest() {
        if (restTimer != null) {
            restTimer.cancel();
            restTimer = null;
        }
        if (tts != null) tts.stop();
    }

    @Override
    protected void onDestroy() {
        isActivityFinished = true;
        stopRest();
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Don't stop completely, just pause timers
        if (restTimer != null) {
            restTimer.cancel();
        }
        if (tts != null) tts.stop();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityFinished = true;
        stopRest();
    }
}