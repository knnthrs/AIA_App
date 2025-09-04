package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.signuploginrealtime.api.ApiClient;
import com.example.signuploginrealtime.api.WgerApiService;
import com.example.signuploginrealtime.logic.AdvancedWorkoutDecisionMaker;
import com.example.signuploginrealtime.logic.WorkoutProgression;
import com.example.signuploginrealtime.models.ExerciseInfo;
import com.example.signuploginrealtime.models.WgerExercise;
import com.example.signuploginrealtime.models.WgerExerciseResponse;
import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.UserProfileHelper.UserProfile;

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

    private int currentDay = 1;
    private UserProfile userProfile;
    private List<WorkoutExercise> currentWorkoutExercises;
    private List<ExerciseInfo> allExercises = new ArrayList<>();
    private int batchesCompleted = 0;
    private int totalBatches = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_list);

        exercisesContainer = findViewById(R.id.exercises_container);
        exerciseCount = findViewById(R.id.exercise_count);
        workoutDuration = findViewById(R.id.workout_duration);
        loadingIndicator = findViewById(R.id.loading_indicator);
        refreshButton = findViewById(R.id.start_button);

        apiService = ApiClient.getClient().create(WgerApiService.class);

        userProfile = (UserProfile) getIntent().getSerializableExtra("userProfile");
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setAge(25);
            userProfile.setGender("not specified");
            userProfile.setFitnessGoal("general fitness");
            userProfile.setFitnessLevel("beginner");
            userProfile.setHealthIssues(new ArrayList<>());
        }

        fetchAllExercises(); // Start API fetch

        refreshButton.setOnClickListener(v -> {
            if (currentWorkoutExercises == null || currentWorkoutExercises.isEmpty()) return;

            ArrayList<String> exerciseNames = new ArrayList<>();
            ArrayList<String> exerciseDetails = new ArrayList<>();
            ArrayList<Integer> exerciseRests = new ArrayList<>();

            for (WorkoutExercise we : currentWorkoutExercises) {
                exerciseNames.add(we.getExerciseInfo() != null ? we.getExerciseInfo().getName() : "Unknown");
                String desc = we.getExerciseInfo() != null ? we.getExerciseInfo().getDescription() : "No description";
                exerciseDetails.add(we.getSets() > 0 && we.getReps() > 0
                        ? we.getSets() + " sets x " + we.getReps() + " reps\n" + desc
                        : desc);
                exerciseRests.add(we.getRestSeconds() > 0 ? we.getRestSeconds() : 60);
            }

            Intent intent = new Intent(WorkoutList.this, WorkoutSessionActivity.class);
            intent.putExtra("userProfile", userProfile);
            intent.putStringArrayListExtra("exerciseNames", exerciseNames);
            intent.putStringArrayListExtra("exerciseDetails", exerciseDetails);
            intent.putIntegerArrayListExtra("exerciseRests", exerciseRests);
            startActivity(intent);
        });
    }

    // --- Step 1: Fetch all exercise IDs recursively ---
    private void fetchAllExercises() {
        loadingIndicator.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Fetching exercises...", Toast.LENGTH_SHORT).show();
        fetchExercisesPage(0, 100); // start at offset 0 with limit 100
    }

    private void fetchExercisesPage(int offset, int limit) {
        apiService.getExercises(2, limit, offset).enqueue(new Callback<WgerExerciseResponse>() {
            @Override
            public void onResponse(Call<WgerExerciseResponse> call, Response<WgerExerciseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Integer> exerciseIds = new ArrayList<>();
                    for (WgerExercise w : response.body().getResults()) {
                        exerciseIds.add(w.getId());
                    }
                    fetchExerciseDetailsInBatches(exerciseIds);

                    // If there's a next page, fetch it recursively
                    if (response.body().getNext() != null) {
                        fetchExercisesPage(offset + limit, limit);
                    }
                } else {
                    useDummyWorkout();
                }
            }

            @Override
            public void onFailure(Call<WgerExerciseResponse> call, Throwable t) {
                useDummyWorkout();
            }
        });
    }

    // --- Step 2: Batch fetch full exercise info ---
    private void fetchExerciseDetailsInBatches(List<Integer> allIds) {
        int batchSize = 50;
        totalBatches = (int) Math.ceil(allIds.size() / (double) batchSize);
        batchesCompleted = 0;

        for (int i = 0; i < allIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allIds.size());
            List<Integer> batch = allIds.subList(i, end);
            fetchExerciseInfo(batch);
        }
    }

    private void fetchExerciseInfo(List<Integer> batchIds) {
        String idParam = TextUtils.join(",", batchIds);
        apiService.getExercisesInfo(idParam, 2).enqueue(new Callback<WgerExerciseResponse>() {
            @Override
            public void onResponse(Call<WgerExerciseResponse> call, Response<WgerExerciseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (WgerExercise w : response.body().getResults()) {
                        if (w.getTranslations() != null) {
                            WgerExercise.Translation englishTranslation = null;
                            for (WgerExercise.Translation t : w.getTranslations()) {
                                if (t.getLanguage() == 2) {
                                    englishTranslation = t;
                                    break;
                                }
                            }

                            if (englishTranslation != null) {
                                ExerciseInfo e = new ExerciseInfo();
                                e.setId(w.getId());
                                e.setName(englishTranslation.getName() != null ? englishTranslation.getName() : "Exercise #" + w.getId());
                                e.setDescription(englishTranslation.getDescription() != null
                                        ? Html.fromHtml(englishTranslation.getDescription(), Html.FROM_HTML_MODE_LEGACY).toString()
                                        : "No description available");

                                // Avoid duplicates by checking ID
                                boolean alreadyExists = false;
                                for (ExerciseInfo ex : allExercises) {
                                    if (ex.getId() == e.getId()) {
                                        alreadyExists = true;
                                        break;
                                    }
                                }
                                if (!alreadyExists) {
                                    allExercises.add(e);
                                }
                            }
                        }
                    }

                    batchesCompleted++;
                    if (batchesCompleted >= totalBatches) {
                        pickRandomExercises(allExercises);
                    }
                } else {
                    useDummyWorkout();
                }
            }

            @Override
            public void onFailure(Call<WgerExerciseResponse> call, Throwable t) {
                useDummyWorkout();
            }
        });
    }

    // --- Pick 6 random exercises without duplicates ---
    private void pickRandomExercises(List<ExerciseInfo> exercises) {
        if (exercises.isEmpty()) {
            useDummyWorkout();
            return;
        }

        List<ExerciseInfo> random6 = new ArrayList<>();
        List<Integer> pickedIds = new ArrayList<>();

        while (random6.size() < 6 && random6.size() < exercises.size()) {
            int index = (int) (Math.random() * exercises.size());
            ExerciseInfo candidate = exercises.get(index);
            if (!pickedIds.contains(candidate.getId())) {
                random6.add(candidate);
                pickedIds.add(candidate.getId());
            }
        }

        loadingIndicator.setVisibility(View.GONE);
        generateWorkout(random6);
    }

    // --- Workout generation and display ---
    private void generateWorkout(List<ExerciseInfo> availableExercises) {
        com.example.signuploginrealtime.models.UserProfile modelProfile = convertToModel(userProfile);

        Workout baseWorkout = AdvancedWorkoutDecisionMaker.generatePersonalizedWorkout(
                availableExercises, modelProfile
        );

        Workout finalWorkout = WorkoutProgression.generateProgressiveWorkout(
                baseWorkout, currentDay, modelProfile
        );

        if (finalWorkout != null && finalWorkout.getExercises() != null) {
            personalizeWorkout(finalWorkout.getExercises(), modelProfile, currentDay);
        }

        if (finalWorkout == null || finalWorkout.getExercises() == null || finalWorkout.getExercises().isEmpty()) {
            Toast.makeText(this, "⚠️ No workout generated", Toast.LENGTH_SHORT).show();
            return;
        }

        showExercises(finalWorkout.getExercises());
    }

    private com.example.signuploginrealtime.models.UserProfile convertToModel(UserProfile firebaseProfile) {
        if (firebaseProfile == null) {
            com.example.signuploginrealtime.models.UserProfile defaultProfile = new com.example.signuploginrealtime.models.UserProfile();
            defaultProfile.setAge(25);
            defaultProfile.setGender("not specified");
            defaultProfile.setFitnessGoal("general fitness");
            defaultProfile.setFitnessLevel("beginner");
            defaultProfile.setHealthIssues(new ArrayList<>());
            defaultProfile.setAvailableEquipment(new ArrayList<>());
            defaultProfile.setDislikedExercises(new ArrayList<>());
            defaultProfile.setHasGymAccess(false);
            defaultProfile.setHeight(170);
            defaultProfile.setWeight(70);
            return defaultProfile;
        }

        com.example.signuploginrealtime.models.UserProfile modelProfile = new com.example.signuploginrealtime.models.UserProfile();
        modelProfile.setAge(firebaseProfile.getAge());
        modelProfile.setGender(firebaseProfile.getGender());
        modelProfile.setFitnessGoal(firebaseProfile.getFitnessGoal());
        modelProfile.setFitnessLevel(firebaseProfile.getFitnessLevel());
        modelProfile.setHealthIssues(firebaseProfile.getHealthIssues());
        modelProfile.setAvailableEquipment(new ArrayList<>());
        modelProfile.setDislikedExercises(new ArrayList<>());
        modelProfile.setHasGymAccess(false);
        modelProfile.setHeight(firebaseProfile.getHeight());
        modelProfile.setWeight(firebaseProfile.getWeight());
        return modelProfile;
    }

    private void personalizeWorkout(List<WorkoutExercise> exercises, com.example.signuploginrealtime.models.UserProfile profile, int day) {
        // Already handled in AdvancedWorkoutDecisionMaker
    }

    private void showExercises(List<WorkoutExercise> workoutExercises) {
        exercisesContainer.removeAllViews();
        currentWorkoutExercises = workoutExercises;
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
}
