package com.example.signuploginrealtime;

import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.signuploginrealtime.models.WgerExerciseResponse;
import com.example.signuploginrealtime.models.WgerExercise;
import com.example.signuploginrealtime.models.ExerciseInfo;
import com.example.signuploginrealtime.logic.AdvancedWorkoutDecisionMaker;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.signuploginrealtime.api.ApiClient;
import com.example.signuploginrealtime.api.WgerApiService;
import com.example.signuploginrealtime.logic.WorkoutDecisionMaker;
import com.example.signuploginrealtime.logic.WorkoutProgression;
import com.example.signuploginrealtime.models.ExerciseInfo;
import com.example.signuploginrealtime.models.UserProfile;
import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;
import java.util.ArrayList;


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

    private int currentDay = 1; // progression day counter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_list);

        exercisesContainer = findViewById(R.id.exercises_container);
        exerciseCount = findViewById(R.id.exercise_count);
        workoutDuration = findViewById(R.id.workout_duration);
        loadingIndicator = findViewById(R.id.loading_indicator);
        refreshButton = findViewById(R.id.start_button); // Make sure this matches your button ID

        apiService = ApiClient.getClient().create(WgerApiService.class);

        fetchExercises();

        // ✅ Replace old listener with this one
        refreshButton.setOnClickListener(v -> {
            ArrayList<String> names = new ArrayList<>();
            ArrayList<String> details = new ArrayList<>();
            ArrayList<Integer> rests = new ArrayList<>();

            for (int i = 0; i < exercisesContainer.getChildCount(); i++) {
                View card = exercisesContainer.getChildAt(i);
                TextView name = card.findViewById(R.id.tv_exercise_name);
                TextView detail = card.findViewById(R.id.tv_exercise_details);
                TextView rest = card.findViewById(R.id.tv_exercise_rest);

                names.add(name.getText().toString());
                details.add(detail.getText().toString());
                rests.add(Integer.parseInt(rest.getText().toString().replace("Rest: ", "").replace("s", "").trim()));
            }

            Intent intent = new Intent(WorkoutList.this, WorkoutSessionActivity.class);
            intent.putStringArrayListExtra("names", names);
            intent.putStringArrayListExtra("details", details);
            intent.putIntegerArrayListExtra("rests", rests);
            startActivity(intent);
        });
    }



    private void fetchExercises() {
        loadingIndicator.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Fetching exercises...", Toast.LENGTH_SHORT).show();

        // Example exercise IDs
        String exerciseIds = "9,12,20,31,41,43,46,48,50,51,56,57";

        apiService.getExercisesInfo(exerciseIds, 2).enqueue(new Callback<WgerExerciseResponse>() {
            @Override
            public void onResponse(Call<WgerExerciseResponse> call, Response<WgerExerciseResponse> response) {
                loadingIndicator.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<ExerciseInfo> exercises = new ArrayList<>();

                    // Convert each WgerExercise into ExerciseInfo
                    for (WgerExercise w : response.body().getResults()) {
                        ExerciseInfo e = new ExerciseInfo();

                        String name = "Unnamed Exercise";
                        String desc = "No description available";

                        if (w.getTranslations() != null) {
                            for (WgerExercise.Translation t : w.getTranslations()) {
                                if (t.getLanguage() == 2) { // English
                                    name = t.getName() != null ? t.getName() : name;
                                    desc = t.getDescription() != null ? Html.fromHtml(t.getDescription(), Html.FROM_HTML_MODE_LEGACY).toString() : desc;
                                    break;
                                }
                            }
                        }

                        e.setName(name);
                        e.setDescription(desc);
                        exercises.add(e);

                        Log.d("WorkoutList", "Exercise fetched: " + e.getName());
                    }



                    // Pass the converted list to generateWorkout
                    generateWorkout(exercises);

                } else {
                    Toast.makeText(WorkoutList.this, "Failed to fetch exercises", Toast.LENGTH_SHORT).show();
                    useDummyWorkout();
                }
            }

            @Override
            public void onFailure(Call<WgerExerciseResponse> call, Throwable t) {
                loadingIndicator.setVisibility(View.GONE);
                Toast.makeText(WorkoutList.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                useDummyWorkout();
            }
        });
    }




    private void generateWorkout(List<ExerciseInfo> availableExercises) {
        // Create user profile
        UserProfile userProfile = createEmptyProfileForProgression();
        userProfile.setFitnessGoal("lose weight"); // or "gain muscle", "increase endurance"
        userProfile.setAge(25); // example, adjust as needed

        // ✅ Use AdvancedWorkoutDecisionMaker instead of WorkoutDecisionMaker
        Workout baseWorkout = AdvancedWorkoutDecisionMaker.generatePersonalizedWorkout(
                availableExercises,
                userProfile
        );

        // Apply progression
        Workout finalWorkout = WorkoutProgression.generateProgressiveWorkout(
                baseWorkout,
                currentDay,
                userProfile
        );

        // ✅ Personalize sets/reps/rest
        if (finalWorkout != null && finalWorkout.getExercises() != null) {
            personalizeWorkout(finalWorkout.getExercises(), userProfile, currentDay);
        }

        if (finalWorkout == null || finalWorkout.getExercises() == null || finalWorkout.getExercises().isEmpty()) {
            Toast.makeText(this, "⚠️ No workout generated", Toast.LENGTH_SHORT).show();
            return;
        }

        showExercises(finalWorkout.getExercises());
    }



    private void personalizeWorkout(List<WorkoutExercise> exercises, UserProfile userProfile, int day) {
        for (WorkoutExercise we : exercises) {
            String name = we.getExerciseInfo() != null ? we.getExerciseInfo().getName().toLowerCase() : "";

            int sets = 3;
            int reps = 10;
            int rest = 60;

            // Determine exercise type (simplified)
            if (name.contains("push") || name.contains("bench") || name.contains("press") || name.contains("pushup")) {
                // Push exercises
                sets = 3 + (day % 2);            // 3-4 sets
                reps = 10 + (int)(Math.random() * 6); // 10-15 reps
                rest = 60 - (day * 2);           // slightly shorter rest as day increases
            } else if (name.contains("pull") || name.contains("row") || name.contains("pullup")) {
                // Pull exercises
                sets = 3 + (day % 2);
                reps = 8 + (int)(Math.random() * 5);  // 8-12 reps
                rest = 60;
            } else if (name.contains("squat") || name.contains("lunge") || name.contains("leg")) {
                // Legs
                sets = 4 + (day % 2);            // 4-5 sets
                reps = 12 + (int)(Math.random() * 4); // 12-15 reps
                rest = 75;
            } else if (name.contains("plank") || name.contains("hold")) {
                // Planks / timed exercises
                sets = 3;
                reps = 1;                        // reps = 1 for timed hold
                rest = 30 + day * 5;
            } else {
                // Default/general exercises
                sets = 2 + (day % 3);
                reps = 10 + (int)(Math.random() * 6);
                rest = 60;
            }

            // Adjust based on fitness level
            switch (userProfile.getFitnessLevel().toLowerCase()) {
                case "beginner":
                    sets = Math.max(2, sets - 1);
                    reps = Math.max(8, reps - 2);
                    rest += 10; // longer rest for beginners
                    break;
                case "intermediate":
                    sets = sets;
                    reps = reps;
                    rest -= 5; // slightly shorter rest
                    break;
                case "advanced":
                    sets += 1;  // more sets
                    reps += 2;  // more reps
                    rest -= 10; // shorter rest
                    break;
            }

            // Apply final values
            we.setSets(sets);
            we.setReps(reps);
            we.setRestSeconds(rest);
        }
    }


    private void showExercises(List<WorkoutExercise> workoutExercises) {
        exercisesContainer.removeAllViews();

        exerciseCount.setText("Exercises: " + workoutExercises.size());
        int duration = workoutExercises.size() * 5;
        workoutDuration.setText("Duration: " + duration + " mins");

        LayoutInflater inflater = LayoutInflater.from(this);
        int order = 1;
        for (WorkoutExercise we : workoutExercises) {
            View card = inflater.inflate(R.layout.item_exercise_card, exercisesContainer, false);

            TextView number = card.findViewById(R.id.tv_exercise_number);
            TextView name = card.findViewById(R.id.tv_exercise_name);
            TextView details = card.findViewById(R.id.tv_exercise_details);
            TextView rest = card.findViewById(R.id.tv_exercise_rest);

            number.setText(String.valueOf(order));

            if (we.getExerciseInfo() != null) {
                name.setText(we.getExerciseInfo().getName());
                String desc = we.getExerciseInfo().getDescription();
                details.setText(we.getSets() > 0 && we.getReps() > 0
                        ? we.getSets() + " sets x " + we.getReps() + " reps\n" + desc
                        : desc);
            } else {
                name.setText("Unknown");
                details.setText("No description");
            }

            rest.setText("Rest: " + (we.getRestSeconds() > 0 ? we.getRestSeconds() : 60) + "s");

            exercisesContainer.addView(card);
            order++;
        }
    }

    private void useDummyWorkout() {
        List<ExerciseInfo> dummy = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ExerciseInfo e = new ExerciseInfo();
            e.setName("Offline Exercise " + i);
            e.setDescription("Sample offline description.");
            dummy.add(e);
        }
        generateWorkout(dummy);
    }

    private UserProfile createEmptyProfileForProgression() {
        UserProfile p = new UserProfile();
        p.setFitnessGoal("general fitness");
        p.setFitnessLevel("beginner");
        p.setAge(25);
        p.setGender("not specified");
        p.setHealthIssues(new ArrayList<>());
        p.setAvailableEquipment(new ArrayList<>());
        p.setDislikedExercises(new ArrayList<>());
        p.setHasGymAccess(false);
        return p;
    }
}
