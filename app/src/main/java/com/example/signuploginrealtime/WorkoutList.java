package com.example.signuploginrealtime;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.signuploginrealtime.api.ApiClient;
import com.example.signuploginrealtime.api.WgerApiService;
import com.example.signuploginrealtime.models.WgerExercise;
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

    private LinearLayout exercisesContainer;
    private TextView exerciseCount, workoutDuration;
    private ProgressBar loadingIndicator;
    private View refreshButton;

    private WgerApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_list);

        exercisesContainer = findViewById(R.id.exercises_container);
        exerciseCount = findViewById(R.id.exercise_count);
        workoutDuration = findViewById(R.id.workout_duration);
        loadingIndicator = findViewById(R.id.loading_indicator);
        refreshButton = findViewById(R.id.refresh_button);

        apiService = ApiClient.getClient().create(WgerApiService.class);

        // Fetch exercises from API
        fetchExercises();

        refreshButton.setOnClickListener(v -> fetchExercises());
    }

    private void fetchExercises() {
        loadingIndicator.setVisibility(View.VISIBLE);

        apiService.getExercises(2, 20, 0).enqueue(new Callback<WgerExerciseResponse>() {
            @Override
            public void onResponse(Call<WgerExerciseResponse> call, Response<WgerExerciseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<WgerExercise> wgerList = response.body().getResults();
                    List<ExerciseInfo> converted = new ArrayList<>();

                    for (WgerExercise w : wgerList) {
                        ExerciseInfo e = new ExerciseInfo();
                        e.setName(w.getName());
                        e.setDescription(w.getCleanDescription());
                        converted.add(e);
                    }

                    generateWorkout(converted);
                } else {
                    loadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(WorkoutList.this, "Failed to fetch exercises", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WgerExerciseResponse> call, Throwable t) {
                loadingIndicator.setVisibility(View.GONE);
                Toast.makeText(WorkoutList.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void generateWorkout(List<ExerciseInfo> availableExercises) {
        // Example profile data — replace later
        String userGoal = "Lose Weight";
        String userLevel = "Beginner";
        List<String> healthIssues = new ArrayList<>();
        int age = 25;
        String gender = "Male";
        float bmi = 23.0f;

        Workout workout = WorkoutDecisionMaker.generateBaseWorkout(
                availableExercises, userGoal, userLevel, healthIssues, age, gender, bmi
        );

        showExercises(workout.getExercises());
        loadingIndicator.setVisibility(View.GONE);
    }

    private void showExercises(List<WorkoutExercise> workoutExercises) {
        exercisesContainer.removeAllViews();

        exerciseCount.setText("Exercises: " + workoutExercises.size());
        workoutDuration.setText("Duration: " + (workoutExercises.size() * 5) + " mins");

        LayoutInflater inflater = LayoutInflater.from(this);
        int order = 1;
        for (WorkoutExercise we : workoutExercises) {
            View card = inflater.inflate(R.layout.item_exercise_card, exercisesContainer, false);

            TextView number = card.findViewById(R.id.tv_exercise_number);
            TextView name = card.findViewById(R.id.tv_exercise_name);
            TextView details = card.findViewById(R.id.tv_exercise_details);
            TextView rest = card.findViewById(R.id.tv_exercise_rest);

            number.setText(String.valueOf(order));

            if (we.getExerciseInfo() != null) {  // <-- updated
                ExerciseInfo ex = we.getExerciseInfo();
                name.setText(ex.getName() != null ? ex.getName() : "Unknown");
                details.setText(ex.getDescription() != null ? ex.getDescription() : "No description");
            } else {
                name.setText("Unknown");
                details.setText("No description");
            }

            rest.setText("Rest: " + we.getRestSeconds() + "s");

            exercisesContainer.addView(card);
            order++;
        }
    }

}
