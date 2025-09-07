package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class RestTimerActivity extends AppCompatActivity {

    private TextView tvRestTimer;
    private Button btnSkipRest, btnAddTime;

    private int nextExerciseIndex;
    private CountDownTimer timer;
    private long remainingRestMillis;

    private ArrayList<String> exerciseNames;
    private ArrayList<String> exerciseDetails;
    private ArrayList<String> exerciseImageUrls; // ✅ renamed
    private ArrayList<Integer> exerciseTimes;
    private ArrayList<Integer> exerciseRests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rest_timer);

        tvRestTimer = findViewById(R.id.tv_rest_timer);
        btnSkipRest = findViewById(R.id.btn_skip_rest);
        btnAddTime = findViewById(R.id.btn_add_time);

        // Get data from Intent
        nextExerciseIndex = getIntent().getIntExtra("nextIndex", 0);
        exerciseNames = getIntent().getStringArrayListExtra("exerciseNames");
        exerciseDetails = getIntent().getStringArrayListExtra("exerciseDetails");
        exerciseImageUrls = getIntent().getStringArrayListExtra("exerciseImageUrls"); // ✅ updated key
        exerciseTimes = getIntent().getIntegerArrayListExtra("exerciseTimes");
        exerciseRests = getIntent().getIntegerArrayListExtra("exerciseRests");

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
        tvRestTimer.setText("Rest: " + secondsLeft + "s");
    }

    private void goToNextExercise() {
        if (nextExerciseIndex >= exerciseNames.size()) {
            // All exercises done
            finish();
            return;
        }

        // Start next exercise with correct index
        Intent intent = new Intent(RestTimerActivity.this, WorkoutSessionActivity.class);
        intent.putExtra("currentIndex", nextExerciseIndex); // Integer value
        intent.putStringArrayListExtra("exerciseNames", exerciseNames); // ✅ String list
        intent.putStringArrayListExtra("exerciseDetails", exerciseDetails); // ✅ String list
        intent.putStringArrayListExtra("exerciseImageUrls", exerciseImageUrls); // ✅ String list
        intent.putIntegerArrayListExtra("exerciseTimes", exerciseTimes); // ✅ Integer list
        intent.putIntegerArrayListExtra("exerciseRests", exerciseRests); // ✅ Integer list

        startActivity(intent);
        finish();
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
}

