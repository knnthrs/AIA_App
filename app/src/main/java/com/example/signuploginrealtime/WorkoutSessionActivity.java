package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class WorkoutSessionActivity extends AppCompatActivity {

    private TextView tvExerciseName, tvExerciseDetails, tvExerciseTimer, tvExerciseProgress, tvNoImage;
    private ImageView ivExerciseImage;
    private Button btnPause, btnNext;

    private ArrayList<String> exerciseNames;
    private ArrayList<String> exerciseDetails;
    private ArrayList<String> exerciseImageUrls;
    private ArrayList<Integer> exerciseDurations;
    private ArrayList<Integer> exerciseRests;

    private int currentIndex = 0;
    private CountDownTimer timer;
    private boolean isTimerRunning = false;
    private long timeLeftMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_session);

        tvExerciseName = findViewById(R.id.tv_exercise_name);
        tvExerciseDetails = findViewById(R.id.tv_exercise_details);
        tvExerciseTimer = findViewById(R.id.tv_exercise_timer);
        tvExerciseProgress = findViewById(R.id.tv_exercise_progress);
        ivExerciseImage = findViewById(R.id.iv_exercise_image);
        tvNoImage = findViewById(R.id.tvNoImage);
        btnPause = findViewById(R.id.btn_pause);
        btnNext = findViewById(R.id.btn_done);

        // ✅ Use "exerciseImageUrls" instead of "exerciseVideoUrls"
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

        // Get current index if coming from rest
        currentIndex = getIntent().getIntExtra("currentIndex", 0);

        showExercise(currentIndex);

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
    }

    private void showExercise(int index) {
        tvExerciseName.setText(exerciseNames.get(index));
        tvExerciseDetails.setText(exerciseDetails.get(index));
        tvExerciseProgress.setText((index + 1) + "/" + exerciseNames.size());

        String imageUrl = exerciseImageUrls.get(index);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            tvNoImage.setVisibility(View.GONE);
            ivExerciseImage.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .load(imageUrl)
                    .into(ivExerciseImage);

        } else {
            ivExerciseImage.setVisibility(View.GONE);
            tvNoImage.setVisibility(View.VISIBLE);
        }

        startTimer(exerciseDurations.get(index));
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
            Toast.makeText(this, "Workout complete!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Go to RestTimerActivity with nextIndex
        int nextIndex = currentIndex + 1;
        Intent intent = new Intent(WorkoutSessionActivity.this, RestTimerActivity.class);
        intent.putExtra("nextIndex", nextIndex); // Integer
        intent.putStringArrayListExtra("exerciseNames", exerciseNames); // String list
        intent.putStringArrayListExtra("exerciseDetails", exerciseDetails); // String list
        intent.putStringArrayListExtra("exerciseImageUrls", exerciseImageUrls); // String list
        intent.putIntegerArrayListExtra("exerciseTimes", exerciseDurations); // Integer list
        intent.putIntegerArrayListExtra("exerciseRests", exerciseRests); // Integer list

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
}
