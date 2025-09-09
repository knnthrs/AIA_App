package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.signuploginrealtime.logic.AdvancedWorkoutDecisionMaker;
import com.example.signuploginrealtime.logic.WorkoutProgression;
import com.example.signuploginrealtime.models.ExerciseInfo;
import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.UserProfileHelper.UserProfile;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map; // Added for useDummyWorkout
import java.util.HashMap; // Added for useDummyWorkout

public class WorkoutList extends AppCompatActivity {

    private static final String TAG = "WorkoutList"; // Tag for logging

    private LinearLayout exercisesContainer;
    private TextView exerciseCount, workoutDuration;
    private ProgressBar loadingIndicator;
    private View startWorkoutButton;

    private UserProfile userProfile;

    private List<WorkoutExercise> currentWorkoutExercises;
    private List<ExerciseInfo> allExercises = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_list);

        exercisesContainer = findViewById(R.id.exercises_container);
        exerciseCount = findViewById(R.id.exercise_count);
        workoutDuration = findViewById(R.id.workout_duration);
        loadingIndicator = findViewById(R.id.loading_indicator);
        startWorkoutButton = findViewById(R.id.start_button);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        userProfile = (UserProfile) getIntent().getSerializableExtra("userProfile");

        if (userProfile == null) {
            Log.w(TAG, "UserProfile not passed via Intent, using default.");
            userProfile = new UserProfile();
            userProfile.setAge(25);
            userProfile.setGender("not specified");
            userProfile.setFitnessGoal("general fitness");
            userProfile.setFitnessLevel("beginner");
            userProfile.setHealthIssues(new ArrayList<>());
        }

        startWorkoutButton.setEnabled(false);
        fetchAllExercises();

        startWorkoutButton.setOnClickListener(v -> {
            if (currentWorkoutExercises == null || currentWorkoutExercises.isEmpty()) {
                Toast.makeText(this, "No workout exercises available to start.", Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<String> exerciseNames = new ArrayList<>();
            ArrayList<String> exerciseDetails = new ArrayList<>();
            ArrayList<Integer> exerciseRests = new ArrayList<>();
            ArrayList<Integer> exerciseTimes = new ArrayList<>();
            ArrayList<String> exerciseImageUrls = new ArrayList<>();

            for (WorkoutExercise we : currentWorkoutExercises) {
                ExerciseInfo info = we.getExerciseInfo();
                String currentExerciseName = (info != null && info.getName() != null) ? info.getName() : "Unknown Exercise";
                exerciseNames.add(currentExerciseName);

                // Updated based on user feedback for instructions in showExercises
                String desc = "No instructions available.";
                if (info != null && info.getInstructions() != null && !info.getInstructions().isEmpty()) {
                    desc = String.join("\n\n", info.getInstructions());
                }

                exerciseDetails.add(desc);

                int baseRest = we.getRestSeconds() > 0 ? we.getRestSeconds() : 60;
                exerciseRests.add(adaptRestTime(baseRest, userProfile.getFitnessLevel()));

                exerciseTimes.add(30); // Placeholder duration, consider making this dynamic

                exerciseImageUrls.add(info != null && info.getGifUrl() != null && !info.getGifUrl().isEmpty()
                        ? info.getGifUrl()
                        : "https://via.placeholder.com/150"); // Placeholder GIF
            }

            Intent intent = new Intent(WorkoutList.this, WorkoutSessionActivity.class);
            intent.putExtra("userProfile", userProfile); // Make sure UserProfile is Serializable
            intent.putStringArrayListExtra("exerciseNames", exerciseNames);
            intent.putStringArrayListExtra("exerciseDetails", exerciseDetails);
            intent.putStringArrayListExtra("exerciseImageUrls", exerciseImageUrls);
            intent.putIntegerArrayListExtra("exerciseTimes", exerciseTimes);
            intent.putIntegerArrayListExtra("exerciseRests", exerciseRests);
            startActivity(intent);
        });
    }

    private int adaptRestTime(int baseRest, String fitnessLevel) {
        if (fitnessLevel == null) fitnessLevel = "beginner"; // Default if null
        switch (fitnessLevel.toLowerCase()) {
            case "intermediate":
                return (int) (baseRest * 0.9);
            case "advanced":
                return (int) (baseRest * 0.75);
            default: // beginner or other
                return baseRest;
        }
    }

    private void fetchAllExercises() {
        loadingIndicator.setVisibility(View.VISIBLE);
        Log.d(TAG, "Fetching exercises from Firebase root...");

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(); 
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Firebase data load successful.");
                allExercises.clear();
                if (!snapshot.exists()) {
                    Log.w(TAG, "Firebase snapshot does not exist at path: " + snapshot.getRef().toString());
                } else if (snapshot.getChildrenCount() == 0) {
                    Log.w(TAG, "Firebase snapshot exists but has no children at path: " + snapshot.getRef().toString());
                } else {
                    Log.d(TAG, "Firebase snapshot exists and has " + snapshot.getChildrenCount() + " children.");
                    for (DataSnapshot exerciseSnap : snapshot.getChildren()) {
                        if (exerciseSnap.getKey() != null && exerciseSnap.getKey().matches("^[0-9]+$")) {
                            ExerciseInfo e = exerciseSnap.getValue(ExerciseInfo.class);

                            // ---- START: USER PROVIDED SNIPPET for logging (from a previous step) ----
                            Log.d(TAG, "Parsed ExerciseInfo: " + (e != null ? "ID: " + e.getExerciseId() + ", Name: " + e.getName() : "NULL ExerciseInfo object"));

                            if (e != null) {
                                if (e.getGifUrl() == null || e.getGifUrl().isEmpty()) {
                                    Log.w(TAG, "Exercise '" + e.getName() + "' (ID: " + e.getExerciseId() + ") has null or empty gifUrl, using placeholder.");
                                    e.setGifUrl("https://via.placeholder.com/150");
                                }
                                if (e.getName() == null || e.getName().isEmpty()) {
                                    Log.w(TAG, "Exercise (ID: " + e.getExerciseId() + ") has null or empty name.");
                                }
                                allExercises.add(e);
                                if (allExercises.size() <= 3) {
                                    Log.d(TAG, "Equipments for " + e.getName() + ": " + e.getEquipments());

                                    Log.d(TAG, "Parsed Exercise: Name=" + e.getName() + ", GifUrl=" + e.getGifUrl() + ", ID=" + e.getExerciseId());
                                }
                            } else {
                                Log.w(TAG, "Parsed null ExerciseInfo from snapshot key: " + exerciseSnap.getKey());
                            }
                            // ---- END: USER PROVIDED SNIPPET ----
                        } else {
                            Log.d(TAG, "Skipping non-numeric child key at root: " + exerciseSnap.getKey());
                        }
                    }
                }

                loadingIndicator.setVisibility(View.GONE);

                if (!allExercises.isEmpty()) {
                    Log.d(TAG, "Successfully populated allExercises with " + allExercises.size() + " items.");
                    pickRandomExercises(allExercises);
                } else {
                    Log.w(TAG, "allExercises is empty after Firebase fetch attempt. Using dummy workout.");
                    Toast.makeText(WorkoutList.this, "No exercises found in database.", Toast.LENGTH_LONG).show();
                    useDummyWorkout();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadingIndicator.setVisibility(View.GONE);
                Log.e(TAG, "Firebase data load cancelled or failed: " + error.getMessage(), error.toException());
                Toast.makeText(WorkoutList.this, "Failed to load exercises. Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                useDummyWorkout();
            }
        });
    }

    private void pickRandomExercises(List<ExerciseInfo> exercises) {
        if (exercises.isEmpty()) {
            Log.w(TAG, "pickRandomExercises called with empty list. Using dummy workout.");
            useDummyWorkout();
            return;
        }

        List<ExerciseInfo> randomExercises = new ArrayList<>();
        List<String> pickedIds = new ArrayList<>();
        int numberOfExercisesToPick = Math.min(6, exercises.size());

        int attempts = 0;
        int maxAttempts = exercises.size() * 3; 

        while (randomExercises.size() < numberOfExercisesToPick && attempts < maxAttempts && !exercises.isEmpty()) {
            int index = (int) (Math.random() * exercises.size());
            ExerciseInfo candidate = exercises.get(index);
            // In pickRandomExercises (USER PROVIDED)
            if (candidate != null && candidate.getExerciseId() != null && !pickedIds.contains(candidate.getExerciseId())) {
                randomExercises.add(candidate);
                pickedIds.add(candidate.getExerciseId());
            } else if (candidate == null || candidate.getExerciseId() == null) {
                Log.w(TAG, "Skipping candidate with null info or ID in pickRandomExercises.");
            }
            attempts++;
        }
        Log.d(TAG, "Picked " + randomExercises.size() + " random exercises.");
        generateWorkout(randomExercises);
    }

    // useDummyWorkout() (USER PROVIDED)
    private void useDummyWorkout() {
        Log.d(TAG, "Using dummy workout.");
        List<ExerciseInfo> dummy = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ExerciseInfo e = new ExerciseInfo();
            e.setName("Offline Exercise " + i);
            e.setGifUrl("https://via.placeholder.com/150");
            e.setExerciseId("dummy_id_" + i);

            // Add some fake instructions
            // ✅ correct
            List<String> dummyInstructions = new ArrayList<>();
            dummyInstructions.add("Sample step 1 for exercise " + i);
            dummyInstructions.add("Sample step 2 for exercise " + i);
            e.setInstructions(dummyInstructions);


            dummy.add(e);
        }
        generateWorkout(dummy);
    }

    private void generateWorkout(List<ExerciseInfo> availableExercises) {
        if (availableExercises == null || availableExercises.isEmpty()) {
            Log.e(TAG, "generateWorkout called with null or empty availableExercises. Cannot generate workout.");
            Toast.makeText(this, "Cannot generate workout: No exercises available.", Toast.LENGTH_SHORT).show();
            startWorkoutButton.setEnabled(false);
            exercisesContainer.removeAllViews();
            exerciseCount.setText("Exercises: 0");
            workoutDuration.setText("Duration: 0 mins");
            return;
        }
        Log.d(TAG, "Generating workout with " + availableExercises.size() + " available exercises.");
        com.example.signuploginrealtime.models.UserProfile modelProfile = convertToModel(userProfile);

        Workout baseWorkout = AdvancedWorkoutDecisionMaker.generatePersonalizedWorkout(availableExercises, modelProfile);
        Workout finalWorkout = WorkoutProgression.generateProgressiveWorkout(baseWorkout, 1, modelProfile);

        if (finalWorkout != null && finalWorkout.getExercises() != null && !finalWorkout.getExercises().isEmpty()) {
            currentWorkoutExercises = finalWorkout.getExercises();
            Log.d(TAG, "Generated workout with " + currentWorkoutExercises.size() + " exercises.");
            showExercises(currentWorkoutExercises);
            startWorkoutButton.setEnabled(true);
        } else {
            Log.e(TAG, "Workout generation resulted in null or empty workout/exercises.");
            Toast.makeText(this, "⚠️ Could not generate a valid workout.", Toast.LENGTH_SHORT).show();
            startWorkoutButton.setEnabled(false);
            useDummyWorkout(); 
        }
    }

    private com.example.signuploginrealtime.models.UserProfile convertToModel(UserProfile firebaseProfile) {
        com.example.signuploginrealtime.models.UserProfile modelProfile =
                new com.example.signuploginrealtime.models.UserProfile();

        if (firebaseProfile != null) {
            modelProfile.setAge(firebaseProfile.getAge());
            modelProfile.setGender(firebaseProfile.getGender());
            modelProfile.setFitnessGoal(firebaseProfile.getFitnessGoal());
            modelProfile.setFitnessLevel(firebaseProfile.getFitnessLevel());
            modelProfile.setHealthIssues(firebaseProfile.getHealthIssues());
            modelProfile.setHeight(firebaseProfile.getHeight());
            modelProfile.setWeight(firebaseProfile.getWeight());
        }
        return modelProfile;
    }

    private void showExercises(List<WorkoutExercise> workoutExercises) {
        exercisesContainer.removeAllViews();

        if (workoutExercises == null || workoutExercises.isEmpty()) {
            Log.w(TAG, "showExercises called with no exercises to display.");
            exerciseCount.setText("Exercises: 0");
            workoutDuration.setText("Duration: 0 mins");
            TextView emptyView = new TextView(this);
            emptyView.setText("No exercises in this workout.");
            emptyView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            exercisesContainer.addView(emptyView);
            return;
        }

        Log.d(TAG, "Displaying " + workoutExercises.size() + " exercises.");
        exerciseCount.setText("Exercises: " + workoutExercises.size());
        int totalDurationSeconds = 0;
        for(WorkoutExercise we : workoutExercises) {
            totalDurationSeconds += (we.getSets() * we.getReps() * 3); 
            totalDurationSeconds += we.getSets() * (we.getRestSeconds() > 0 ? we.getRestSeconds() : 60); 
        }
        workoutDuration.setText("Duration: " + (totalDurationSeconds / 60) + " mins");

        LayoutInflater inflater = LayoutInflater.from(this);
        int order = 1;

        for (WorkoutExercise we : workoutExercises) {
            View card = inflater.inflate(R.layout.item_exercise_card, exercisesContainer, false);

            TextView number = card.findViewById(R.id.tv_exercise_number);
            TextView name = card.findViewById(R.id.tv_exercise_name);
            ImageView image = card.findViewById(R.id.iv_exercise_gif);
            TextView setsReps = card.findViewById(R.id.tv_exercise_sets_reps);
            TextView targetMuscles = card.findViewById(R.id.tv_exercise_target_muscles);
            TextView equipment = card.findViewById(R.id.tv_exercise_equipment);


            number.setText(String.valueOf(order));
            if (we.getExerciseInfo() != null) {
                ExerciseInfo info = we.getExerciseInfo();
                String exerciseNameStr = info.getName() != null ? info.getName() : "Unknown Exercise";
                name.setText(exerciseNameStr);

                // ✅ Show target muscles
                if (info.getTargetMuscles() != null && !info.getTargetMuscles().isEmpty()) {
                    targetMuscles.setText("Target: " + String.join(", ", info.getTargetMuscles()));
                } else {
                    targetMuscles.setText("Target: N/A");
                }

                // ✅ Show equipment
                if (info.getEquipments() != null && !info.getEquipments().isEmpty()) {
                    equipment.setText("Equipment: " + String.join(", ", info.getEquipments()));
                } else {
                    equipment.setText("Equipment: None");
                }


                // ✅ Show sets and reps
                if (we.getSets() > 0 && we.getReps() > 0) {
                    setsReps.setText(we.getSets() + " sets x " + we.getReps() + " reps");
                } else {
                    setsReps.setText("");
                }


                // Updated with user provided snippet (info.getInstructionsList())
                String desc = "No instructions available.";
                if (info != null && info.getInstructions() != null && !info.getInstructions().isEmpty()) {
                    desc = String.join("\n\n", info.getInstructions());
                }


                String setsRepsInfo = (we.getSets() > 0 && we.getReps() > 0)
                                ? we.getSets() + " sets x " + we.getReps() + " reps\n"
                                : "";

                String gifUrl = info.getGifUrl() != null && !info.getGifUrl().isEmpty()
                                ? info.getGifUrl()
                                : "https://via.placeholder.com/150";
                Glide.with(this)
                        .asGif()
                        .load(gifUrl)
                        .placeholder(R.drawable.loading_placeholder) 
                        .error(R.drawable.no_image_placeholder) 
                        .into(image);
            }

            exercisesContainer.addView(card);
            order++;
        }
    }
}
