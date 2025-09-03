package com.example.signuploginrealtime;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.api.ApiClient;
import com.example.signuploginrealtime.api.WgerApiService;
import com.example.signuploginrealtime.models.WgerExerciseResponse;
import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.logic.WorkoutDecisionMaker;
import com.example.signuploginrealtime.models.ExerciseInfo;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkoutList extends AppCompatActivity {

    private static final String TAG = "WorkoutList";

    private RecyclerView exercisesRecycler;
    private WorkoutAdapter workoutAdapter;
    private TextView exerciseCount, workoutDuration;
    private ProgressBar loadingIndicator;
    private View refreshButton;

    private WgerApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_list);

        Log.d(TAG, "onCreate started");

        exercisesRecycler = findViewById(R.id.exercises_recycler);
        exercisesRecycler.setLayoutManager(new LinearLayoutManager(this));

        // ✅ Initialize adapter correctly
        workoutAdapter = new WorkoutAdapter(this, new ArrayList<WorkoutExercise>());
        exercisesRecycler.setAdapter(workoutAdapter);

        exerciseCount = findViewById(R.id.exercise_count);
        workoutDuration = findViewById(R.id.workout_duration);
        loadingIndicator = findViewById(R.id.loading_indicator);
        refreshButton = findViewById(R.id.refresh_button);

        apiService = ApiClient.getClient().create(WgerApiService.class);

        // Fetch exercises from API
        fetchExercises();

        refreshButton.setOnClickListener(v -> {
            Log.d(TAG, "Refresh button clicked");
            fetchExercises();
        });
    }

    private void fetchExercises() {
        Log.d(TAG, "fetchExercises() called");
        loadingIndicator.setVisibility(View.VISIBLE);

        apiService.getExercises(2, 20, 0).enqueue(new Callback<WgerExerciseResponse>() {
            @Override
            public void onResponse(Call<WgerExerciseResponse> call, Response<WgerExerciseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ExerciseInfo> exercises = response.body().getResults();

                    Log.d(TAG, "API returned " + exercises.size() + " exercises");

                    for (int i = 0; i < Math.min(5, exercises.size()); i++) {
                        ExerciseInfo ex = exercises.get(i);
                        Log.d(TAG, "Exercise " + i + " details:");
                        Log.d(TAG, "  - ID: " + (ex != null ? ex.getId() : "null"));
                        Log.d(TAG, "  - Name: " + (ex != null ? ex.getName() : "null"));
                        Log.d(TAG, "  - UUID: " + (ex != null ? ex.getUuid() : "null"));
                        Log.d(TAG, "  - Description: " + (ex != null ? ex.getCleanDescription() : "null"));
                        Log.d(TAG, "  - Category: " + (ex != null ? ex.getCategoryName() : "null"));
                        Log.d(TAG, "  - Language: " + (ex != null ? ex.getLanguage() : "null"));
                    }

                    generateWorkout(exercises);
                }
            }

            @Override
            public void onFailure(Call<WgerExerciseResponse> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
                loadingIndicator.setVisibility(View.GONE);
                Toast.makeText(WorkoutList.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void generateWorkout(List<ExerciseInfo> availableExercises) {
        Log.d(TAG, "generateWorkout() called with " + availableExercises.size() + " exercises");

        // Example profile data — replace later
        String userGoal = "Lose Weight";
        String userLevel = "Beginner";
        List<String> healthIssues = new ArrayList<>();
        int age = 25;
        String gender = "Male";
        float bmi = 23.0f;

        Log.d(TAG, "User profile - Goal: " + userGoal + ", Level: " + userLevel);

        Workout workout = WorkoutDecisionMaker.generateBaseWorkout(
                availableExercises, userGoal, userLevel, healthIssues, age, gender, bmi
        );

        Log.d(TAG, "Generated workout with " + workout.getExercises().size() + " exercises");

        for (int i = 0; i < workout.getExercises().size(); i++) {
            WorkoutExercise we = workout.getExercises().get(i);
            Log.d(TAG, "Workout exercise " + i + ": " +
                    (we.getExerciseInfo() != null ? we.getExerciseInfo().getName() : "NULL"));
        }

        showExercises(workout.getExercises());
        loadingIndicator.setVisibility(View.GONE);
    }

    private void showExercises(List<WorkoutExercise> workoutExercises) {
        Log.d(TAG, "showExercises() called with " + workoutExercises.size() + " exercises");

        exerciseCount.setText("Exercises: " + workoutExercises.size());
        workoutDuration.setText("Duration: " + (workoutExercises.size() * 5) + " mins");

        // ✅ Update adapter data
        workoutAdapter.setExercises(workoutExercises);
    }
}
