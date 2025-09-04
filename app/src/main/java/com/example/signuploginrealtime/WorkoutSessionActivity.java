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

public class WorkoutSessionActivity extends AppCompatActivity {

    private TextView tvName, tvDetails, tvProgress;
    private Button btnSkip;

    private ArrayList<String> exerciseNames;
    private ArrayList<String> exerciseDetails;
    private ArrayList<Integer> exerciseRests;
    private ArrayList<Integer> exerciseReps;

    private int currentIndex = 0;
    private int currentRep = 1;
    private CountDownTimer timer;

    private TextToSpeech tts; // TTS engine

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_session);

        tvName = findViewById(R.id.tv_exercise_name);
        tvDetails = findViewById(R.id.tv_exercise_details);
        tvProgress = findViewById(R.id.tv_exercise_progress);
        btnSkip = findViewById(R.id.btn_skip);

        // Initialize TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
        });

        // Get exercises from Intent
        exerciseNames = getIntent().getStringArrayListExtra("names");
        exerciseDetails = getIntent().getStringArrayListExtra("details");
        exerciseRests = getIntent().getIntegerArrayListExtra("rests");
        exerciseReps = getIntent().getIntegerArrayListExtra("reps");

        if (exerciseNames == null || exerciseNames.isEmpty()) {
            tvName.setText("No exercises available");
            btnSkip.setVisibility(Button.GONE);
            return;
        }

        showExercise(currentIndex);

        btnSkip.setOnClickListener(v -> moveToNextExercise());
    }

    private void showExercise(int index) {
        if (index >= exerciseNames.size()) {
            tvName.setText("Workout Complete!");
            tvDetails.setText("");
            tvProgress.setText("");
            btnSkip.setVisibility(Button.GONE);
            return;
        }

        btnSkip.setVisibility(Button.VISIBLE);

        tvName.setText(exerciseNames.get(index));
        tvDetails.setText(exerciseDetails.get(index));
        currentRep = 1;
        startRepTimer(index);
    }

    private void startRepTimer(int index) {
        int totalReps = exerciseReps.get(index);
        int repDuration = 5000; // 5s per rep (adjust to your logic)
        long durationMillis = repDuration;

        tvProgress.setText(currentRep + "/" + totalReps);

        if (timer != null) timer.cancel();

        // Speak current rep
        if (tts != null) {
            tts.speak(String.valueOf(currentRep), TextToSpeech.QUEUE_FLUSH, null, null);
        }

        timer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                if (currentRep < totalReps) {
                    currentRep++;
                    tvProgress.setText(currentRep + "/" + totalReps);
                    startRepTimer(index); // next rep
                } else {
                    if (tts != null) {
                        tts.speak("Rest time", TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                    goToRest(exerciseRests.get(index));
                }
            }
        }.start();
    }

    private void goToRest(int restSeconds) {
        if (timer != null) timer.cancel();
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
        currentIndex++;
        showExercise(currentIndex);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
