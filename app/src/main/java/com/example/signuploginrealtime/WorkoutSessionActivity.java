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

    private CountDownTimer timer, prepTimer;
    private long timeLeftMillis;
    private int repDuration = 3000; // faster rep timing

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

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);

                // ✅ Only show first exercise after TTS is ready
                if (exerciseNames != null && !exerciseNames.isEmpty()) {
                    // Clear placeholder UI
                    tvName.setText("");
                    tvDetails.setText("");
                    tvProgress.setText("");
                    // Start first exercise
                    showExercise(currentIndex);
                }
            }
        });

        btnPause.setOnClickListener(v -> togglePause());
        btnDone.setOnClickListener(v -> moveToNextExercise());
        btnSkip.setOnClickListener(v -> moveToNextExercise());
        btnPrevious.setOnClickListener(v -> moveToPreviousExercise());
    }

    private void showExercise(int index) {
        if (index >= exerciseNames.size()) {
            startActivity(new Intent(this, activity_workout_complete.class));
            finish();
            return;
        }

        tvName.setText(exerciseNames.get(index));
        tvDetails.setText(exerciseDetails.get(index));
        currentRep = 1;

        progressBar.setMax(exerciseNames.size());
        progressBar.setProgress(currentIndex);

        tvProgress.setText("0/" + exerciseReps.get(index));

        startPrepCountdown(index);
    }

    private void startPrepCountdown(int index) {
        String[] prep = {"Ready", "1", "2", "3", "Go"};

        prepTimer = new CountDownTimer(prep.length * 1000, 1000) {
            int step = 0;

            @Override
            public void onTick(long millisUntilFinished) {
                if (isPaused) return;

                if (step < prep.length) {
                    tts.speak(prep[step], TextToSpeech.QUEUE_FLUSH, null, "prep_" + step);
                    step++;
                }
            }

            @Override
            public void onFinish() {
                startRepTimer(index);
            }
        }.start();
    }

    private void startRepTimer(int index) {
        int totalReps = exerciseReps.get(index);

        if (timeLeftMillis == 0) timeLeftMillis = repDuration;

        timer = new CountDownTimer(timeLeftMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMillis = millisUntilFinished;
            }

            @Override
            public void onFinish() {
                if (isPaused) return;

                if (currentRep <= totalReps) {
                    tts.speak(String.valueOf(currentRep), TextToSpeech.QUEUE_FLUSH, null, "rep_" + currentRep);
                    tvProgress.setText(currentRep + "/" + totalReps);
                    currentRep++;
                    timeLeftMillis = 0;
                    startRepTimer(index);
                } else {
                    progressBar.setProgress(currentIndex + 1);
                    goToRest(exerciseRests.get(index));
                }
            }
        }.start();
    }

    private void togglePause() {
        if (!isPaused) {
            if (timer != null) timer.cancel();
            if (prepTimer != null) prepTimer.cancel();
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
        if (timer != null) timer.cancel();
        if (prepTimer != null) prepTimer.cancel();

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
        if (timer != null) timer.cancel();
        if (prepTimer != null) prepTimer.cancel();
        currentIndex++;
        timeLeftMillis = 0;
        showExercise(currentIndex);
    }

    private void moveToPreviousExercise() {
        if (timer != null) timer.cancel();
        if (prepTimer != null) prepTimer.cancel();
        if (currentIndex > 0) currentIndex--;
        timeLeftMillis = 0;
        showExercise(currentIndex);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (timer != null) timer.cancel();
        if (prepTimer != null) prepTimer.cancel();
        super.onDestroy();
    }
}
