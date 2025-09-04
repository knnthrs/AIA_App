package com.example.signuploginrealtime;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class WorkoutSessionActivity extends AppCompatActivity {

    private TextView tvName, tvDetails, tvRest;
    private Button btnSkip;

    private ArrayList<String> exerciseNames;
    private ArrayList<String> exerciseDetails;
    private ArrayList<Integer> exerciseRests;

    private int currentIndex = 0;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_session);

        tvName = findViewById(R.id.tv_exercise_name);
        tvDetails = findViewById(R.id.tv_exercise_details);
        tvRest = findViewById(R.id.tv_exercise_rest);
        btnSkip = findViewById(R.id.btn_skip);

        // ✅ Get exercises using keys sent from WorkoutList
        exerciseNames = getIntent().getStringArrayListExtra("exerciseNames");
        exerciseDetails = getIntent().getStringArrayListExtra("exerciseDetails");
        exerciseRests = getIntent().getIntegerArrayListExtra("exerciseRests");

        if (exerciseNames == null || exerciseNames.isEmpty()) {
            tvName.setText("No exercises available");
            btnSkip.setVisibility(View.GONE);
            return;
        }

        // Show first exercise
        showExercise(currentIndex);

        // Skip button logic
        btnSkip.setOnClickListener(v -> moveToNextExercise());
    }

    private void showExercise(int index) {
        if (index >= exerciseNames.size()) {
            // Finished all exercises
            tvName.setText("Workout Complete!");
            tvDetails.setText("");
            tvRest.setText("");
            btnSkip.setVisibility(View.GONE);
            return;
        }

        btnSkip.setVisibility(View.VISIBLE);

        tvName.setText(exerciseNames.get(index));
        tvDetails.setText(exerciseDetails.get(index));

        int durationSeconds = exerciseRests.get(index);
        if (durationSeconds <= 0) durationSeconds = 60; // fallback
        long durationMillis = durationSeconds * 1000L;

        if (timer != null) timer.cancel();

        timer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvRest.setText("Time left: " + millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                moveToNextExercise();
            }
        }.start();
    }

    private void moveToNextExercise() {
        if (timer != null) timer.cancel();
        currentIndex++;
        showExercise(currentIndex);
    }
}
