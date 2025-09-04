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

    private ArrayList<String> exerciseNames;
    private ArrayList<String> exerciseDetails;
    private ArrayList<Integer> exerciseRests;
    private ArrayList<Integer> exerciseReps;

    private TextToSpeech tts; // TTS engine

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rest);

        tvRestTimer = findViewById(R.id.tv_rest_timer);
        btnSkip = findViewById(R.id.btn_skip_rest);
        btnAddTime = findViewById(R.id.btn_add_time);

        // Initialize TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
        });

        // Get data
        restDuration = getIntent().getIntExtra("restDuration", 60);
        nextIndex = getIntent().getIntExtra("nextIndex", 0);
        exerciseNames = getIntent().getStringArrayListExtra("names");
        exerciseDetails = getIntent().getStringArrayListExtra("details");
        exerciseRests = getIntent().getIntegerArrayListExtra("rests");
        exerciseReps = getIntent().getIntegerArrayListExtra("reps");

        startRestTimer(restDuration);

        btnSkip.setOnClickListener(v -> goToNextExercise());
        btnAddTime.setOnClickListener(v -> {
            if (restTimer != null) restTimer.cancel();
            restDuration += 20;
            startRestTimer(restDuration);
        });
    }

    private void startRestTimer(int seconds) {
        if (restTimer != null) restTimer.cancel();

        restTimer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secs = (int) (millisUntilFinished / 1000);
                tvRestTimer.setText("Rest: " + secs + "s");

                // Announce at milestones
                if (tts != null) {
                    if (secs == 10 || secs == 5) {
                        tts.speak(secs + " seconds left", TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
            }

            @Override
            public void onFinish() {
                if (tts != null) {
                    tts.speak("Go!", TextToSpeech.QUEUE_FLUSH, null, null);
                }
                goToNextExercise();
            }
        }.start();
    }

    private void goToNextExercise() {
        if (restTimer != null) restTimer.cancel();
        Intent intent = new Intent(this, WorkoutSessionActivity.class);
        intent.putStringArrayListExtra("names", exerciseNames);
        intent.putStringArrayListExtra("details", exerciseDetails);
        intent.putIntegerArrayListExtra("rests", exerciseRests);
        intent.putIntegerArrayListExtra("reps", exerciseReps);
        intent.putExtra("currentIndex", nextIndex);
        startActivity(intent);
        finish();
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
