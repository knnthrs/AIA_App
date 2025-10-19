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
import com.example.signuploginrealtime.models.UserProfile;
import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.app.AlertDialog;
import android.content.SharedPreferences;
import com.example.signuploginrealtime.logic.WorkoutAdjustmentHelper;

import java.util.ArrayList;
import java.util.List;



public class WorkoutList extends AppCompatActivity {

    private static final String TAG = "WorkoutList";
    private SharedPreferences workoutPrefs;
    private LinearLayout exercisesContainer;
    private TextView exerciseCount, workoutDuration;
    private ProgressBar loadingIndicator;
    private View startWorkoutButton;

    private UserProfile userProfile;

    private List<WorkoutExercise> currentWorkoutExercises;
    private List<ExerciseInfo> allExercises = new ArrayList<>();
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;


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
        overridePendingTransition(0, 0);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            workoutPrefs = getSharedPreferences("workout_prefs_" + userId, MODE_PRIVATE);
        } else {
            workoutPrefs = getSharedPreferences("workout_prefs_default", MODE_PRIVATE);
        }


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

                // ✅ Keep exercise name clean - sets/reps will be extracted from details
                exerciseNames.add(currentExerciseName);

                // ✅ Create detailed instructions with proper formatting for parsing
                StringBuilder detailsBuilder = new StringBuilder();

                // Add sets/reps information in the expected format
                if (we.getSets() > 0 && we.getReps() > 0) {
                    detailsBuilder.append("Sets: ").append(we.getSets()).append("\n");
                    detailsBuilder.append("Reps: ").append(we.getReps()).append("\n\n");
                }

                // Add original instructions
                if (info != null && info.getInstructions() != null && !info.getInstructions().isEmpty()) {
                    detailsBuilder.append("Instructions:\n");
                    detailsBuilder.append(String.join("\n", info.getInstructions()));
                } else {
                    detailsBuilder.append("Follow proper form and technique.");
                }

                exerciseDetails.add(detailsBuilder.toString());

                int baseRest = we.getRestSeconds() > 0 ? we.getRestSeconds() : 20;
                exerciseRests.add(adaptRestTime(baseRest, userProfile.getFitnessLevel()));

                // ✅ Set appropriate time based on exercise type
                if (we.getSets() > 0 && we.getReps() > 0) {
                    // For rep-based exercises, calculate estimated time
                    int estimatedTime = we.getSets() * we.getReps() * 3; // 3 seconds per rep
                    exerciseTimes.add(Math.max(estimatedTime, 60)); // Minimum 1 minute
                } else {
                    // For time-based exercises
                    exerciseTimes.add(30);
                }

                exerciseImageUrls.add(info != null && info.getGifUrl() != null && !info.getGifUrl().isEmpty()
                        ? info.getGifUrl()
                        : "https://via.placeholder.com/150");
            }

            Intent intent = new Intent(WorkoutList.this, WorkoutSessionActivity.class);
            intent.putExtra("userProfile", userProfile);
            intent.putExtra("workout_name", "Personalized Workout");
            intent.putStringArrayListExtra("exerciseNames", exerciseNames);
            intent.putStringArrayListExtra("exerciseDetails", exerciseDetails);
            intent.putStringArrayListExtra("exerciseImageUrls", exerciseImageUrls);
            intent.putIntegerArrayListExtra("exerciseTimes", exerciseTimes);
            intent.putIntegerArrayListExtra("exerciseRests", exerciseRests);

            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }

    private int adaptRestTime(int baseRest, String fitnessLevel) {
        if (fitnessLevel == null) return baseRest;

        String level = fitnessLevel.toLowerCase().trim();

        if (level.contains("sedentary")) {
            return baseRest; // Full rest time
        } else if (level.contains("lightly active") || level.contains("light")) {
            return (int) (baseRest * 0.9);
        } else if (level.contains("moderately active") || level.contains("moderate")) {
            return (int) (baseRest * 0.8);
        } else if (level.contains("very active") || level.contains("active")) {
            return (int) (baseRest * 0.7);
        }

        return baseRest; // Default
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

    private void useDummyWorkout() {
        Log.d(TAG, "Using dummy workout.");
        List<ExerciseInfo> dummy = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ExerciseInfo e = new ExerciseInfo();
            e.setName("Offline Exercise " + i);
            e.setGifUrl("https://via.placeholder.com/150");
            e.setExerciseId("dummy_id_" + i);

            List<String> dummyInstructions = new ArrayList<>();
            dummyInstructions.add("Sample step 1 for exercise " + i);
            dummyInstructions.add("Sample step 2 for exercise " + i);
            e.setInstructions(dummyInstructions);

            dummy.add(e);
        }
        generateWorkout(dummy);
    }

    private void generateWorkout(List<ExerciseInfo> availableExercises) {
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        DocumentReference userDocRef = firestore.collection("users").document(uid);

        // ✅ GET DIFFICULTY MULTIPLIER FROM PREFERENCES
        float savedMultiplier = workoutPrefs.getFloat("workout_difficulty_multiplier", 1.0f);
        final double difficultyMultiplier = WorkoutAdjustmentHelper.getDifficultyMultiplier(savedMultiplier);

        Log.d(TAG, "Using difficulty multiplier: " + difficultyMultiplier);

        userDocRef.get().addOnSuccessListener(userSnapshot -> {
            if (!userSnapshot.exists()) {
                Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                return;
            }

            updateUserProfileFromFirestore(userSnapshot);

            // ✅ ALSO CHECK FOR MULTIPLIER IN FIRESTORE (if user adjusted on another device)
            Double firestoreMultiplier = userSnapshot.getDouble("workoutDifficultyMultiplier");
            final double finalMultiplier = (firestoreMultiplier != null) ? firestoreMultiplier : difficultyMultiplier;

            if (firestoreMultiplier != null && firestoreMultiplier != savedMultiplier) {
                // Update local preference to match Firestore
                workoutPrefs.edit().putFloat("workout_difficulty_multiplier", firestoreMultiplier.floatValue()).apply();
            }

            Long profileLastModified = userSnapshot.getLong("profileLastModified");

            DocumentReference workoutRef = firestore.collection("users")
                    .document(uid)
                    .collection("currentWorkout")
                    .document("week_" + userProfile.getCurrentWeek());

            workoutRef.get().addOnSuccessListener(workoutSnapshot -> {
                boolean shouldRegenerate = false;

                if (!workoutSnapshot.exists()) {
                    Log.d(TAG, "No existing workout found. Generating new workout.");
                    shouldRegenerate = true;
                } else if (Boolean.TRUE.equals(workoutSnapshot.getBoolean("completed"))) {
                    Log.d(TAG, "Previous workout completed. Generating new workout.");
                    shouldRegenerate = true;
                } else {
                    Long workoutCreatedAt = workoutSnapshot.getLong("createdAt");

                    if (profileLastModified != null && workoutCreatedAt != null
                            && profileLastModified > workoutCreatedAt) {
                        Log.d(TAG, "Profile changed after workout creation. Regenerating workout.");
                        Toast.makeText(this, "Your profile has changed. Generating new personalized workout...",
                                Toast.LENGTH_LONG).show();
                        shouldRegenerate = true;
                    } else {
                        // ✅ CHECK IF DIFFICULTY WAS ADJUSTED AFTER WORKOUT CREATION
                        Long lastAdjustmentTime = workoutPrefs.getLong("last_adjustment_timestamp", 0);
                        if (lastAdjustmentTime > 0 && workoutCreatedAt != null && lastAdjustmentTime > workoutCreatedAt) {
                            Log.d(TAG, "Difficulty adjusted after workout creation. Regenerating workout.");
                            Toast.makeText(this, "Workout difficulty adjusted. Generating new workout...",
                                    Toast.LENGTH_LONG).show();
                            shouldRegenerate = true;
                        } else {
                            Log.d(TAG, "Loading existing workout (profile unchanged).");
                            currentWorkoutExercises = workoutSnapshot.toObject(WorkoutWrapper.class).toWorkoutExercises();
                            showExercises(currentWorkoutExercises);
                            startWorkoutButton.setEnabled(true);
                            return;
                        }
                    }
                }

                if (shouldRegenerate) {
                    com.example.signuploginrealtime.models.UserProfile modelProfile = convertToModel(userProfile);

                    // ✅ GENERATE BASE WORKOUT
                    Workout baseWorkout = AdvancedWorkoutDecisionMaker.generatePersonalizedWorkout(
                            availableExercises, modelProfile);

                    // ✅ APPLY PROGRESSION
                    Workout progressedWorkout = WorkoutProgression.generateProgressiveWorkout(
                            baseWorkout,
                            userProfile.getCurrentWeek(),
                            modelProfile
                    );

                    // ✅ APPLY DIFFICULTY ADJUSTMENT
                    Workout finalWorkout = progressedWorkout;
                    if (finalMultiplier != 1.0) {
                        Log.d(TAG, "Applying difficulty adjustment: " + finalMultiplier);
                        finalWorkout = WorkoutAdjustmentHelper.adjustWorkoutDifficulty(
                                progressedWorkout,
                                finalMultiplier
                        );
                    }

                    if (finalWorkout != null && finalWorkout.getExercises() != null
                            && !finalWorkout.getExercises().isEmpty()) {
                        currentWorkoutExercises = finalWorkout.getExercises();
                        showExercises(currentWorkoutExercises);
                        startWorkoutButton.setEnabled(true);

                        WorkoutWrapper wrapper = new WorkoutWrapper(currentWorkoutExercises, false);
                        wrapper.createdAt = System.currentTimeMillis();

                        workoutRef.set(wrapper)
                                .addOnSuccessListener(aVoid ->
                                        Log.d(TAG, "New workout saved to Firestore with timestamp and difficulty adjustment."))
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "Error saving workout", e));
                    } else {
                        Toast.makeText(this, "Could not generate a valid workout.",
                                Toast.LENGTH_SHORT).show();
                        startWorkoutButton.setEnabled(false);
                        useDummyWorkout();
                    }
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching workout", e);
                Toast.makeText(this, "Error loading workout", Toast.LENGTH_SHORT).show();
                useDummyWorkout();
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching user profile", e);
            Toast.makeText(this, "Error loading profile data", Toast.LENGTH_SHORT).show();
        });
    }
    // ✅ ADD THIS NEW METHOD to update the UserProfile object with fresh Firestore data
    private void updateUserProfileFromFirestore(com.google.firebase.firestore.DocumentSnapshot snapshot) {
        if (snapshot != null && snapshot.exists()) {
            // Update fitness level
            String fitnessLevel = snapshot.getString("fitnessLevel");
            if (fitnessLevel != null && !fitnessLevel.isEmpty()) {
                userProfile.setFitnessLevel(fitnessLevel);
                Log.d(TAG, "Updated fitnessLevel: " + fitnessLevel);
            }

            // Update fitness goal
            String fitnessGoal = snapshot.getString("fitnessGoal");
            if (fitnessGoal != null && !fitnessGoal.isEmpty()) {
                userProfile.setFitnessGoal(fitnessGoal);
                Log.d(TAG, "Updated fitnessGoal: " + fitnessGoal);
            }

            // Update workout frequency
            Long workoutDays = snapshot.getLong("workoutDaysPerWeek");
            if (workoutDays != null) {
                userProfile.setWorkoutDaysPerWeek(workoutDays.intValue());
                Log.d(TAG, "Updated workoutDaysPerWeek: " + workoutDays);
            }

            // Update age if available
            Long age = snapshot.getLong("age");
            if (age != null) {
                userProfile.setAge(age.intValue());
            }

            // Update gender if available
            String gender = snapshot.getString("gender");
            if (gender != null) {
                userProfile.setGender(gender);
            }
        }
    }private com.example.signuploginrealtime.models.UserProfile convertToModel(UserProfile firebaseProfile) {
        com.example.signuploginrealtime.models.UserProfile modelProfile =
                new com.example.signuploginrealtime.models.UserProfile();

        if (firebaseProfile != null) {
            modelProfile.setAge(firebaseProfile.getAge());
            modelProfile.setGender(firebaseProfile.getGender());
            modelProfile.setFitnessGoal(firebaseProfile.getFitnessGoal());
            modelProfile.setFitnessLevel(firebaseProfile.getFitnessLevel()); // ✅ Pass original value directly
            modelProfile.setHealthIssues(firebaseProfile.getHealthIssues());
            modelProfile.setHeight(firebaseProfile.getHeight());
            modelProfile.setWeight(firebaseProfile.getWeight());
            modelProfile.setDislikedExercises(firebaseProfile.getDislikedExercises());
            modelProfile.setWorkoutDaysPerWeek(firebaseProfile.getWorkoutDaysPerWeek());
        }
        return modelProfile;
    }

    private void showExercises(List<WorkoutExercise> workoutExercises) {
        // Lifecycle check
        if (isDestroyed() || isFinishing()) {
            Log.d(TAG, "Activity is being destroyed, skipping showExercises");
            return;
        }

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
        workoutDuration.setText("Duration: " + Math.max(1, totalDurationSeconds / 60) + " mins");

        LayoutInflater inflater = LayoutInflater.from(this);
        int order = 1;

        for (int i = 0; i < workoutExercises.size(); i++) {
            WorkoutExercise we = workoutExercises.get(i);
            View card = inflater.inflate(R.layout.item_exercise_card, exercisesContainer, false);

            TextView number = card.findViewById(R.id.tv_exercise_number);
            TextView name = card.findViewById(R.id.tv_exercise_name);
            ImageView image = card.findViewById(R.id.iv_exercise_gif);
            TextView setsReps = card.findViewById(R.id.tv_exercise_sets_reps);
            TextView targetMuscles = card.findViewById(R.id.tv_exercise_target_muscles);
            TextView equipment = card.findViewById(R.id.tv_exercise_equipment);
            ImageButton deleteButton = card.findViewById(R.id.btn_delete_exercise);

            number.setText(String.valueOf(order));

            if (we.getExerciseInfo() != null) {
                ExerciseInfo info = we.getExerciseInfo();
                String exerciseNameStr = info.getName() != null ? info.getName() : "Unknown Exercise";
                name.setText(exerciseNameStr);

                if (info.getTargetMuscles() != null && !info.getTargetMuscles().isEmpty()) {
                    targetMuscles.setText("Target: " + String.join(", ", info.getTargetMuscles()));
                } else {
                    targetMuscles.setText("Target: N/A");
                }

                if (info.getEquipments() != null && !info.getEquipments().isEmpty()) {
                    equipment.setText("Equipment: " + String.join(", ", info.getEquipments()));
                } else {
                    equipment.setText("Equipment: None");
                }

                if (we.getSets() > 0 && we.getReps() > 0) {
                    setsReps.setText(we.getSets() + " sets x " + we.getReps() + " reps");
                } else {
                    setsReps.setText("Time-based exercise");
                }

                String gifUrl = info.getGifUrl() != null && !info.getGifUrl().isEmpty()
                        ? info.getGifUrl()
                        : "https://via.placeholder.com/150";

                if (!isDestroyed() && !isFinishing()) {
                    Glide.with(this)
                            .asGif()
                            .load(gifUrl)
                            .placeholder(R.drawable.loading_placeholder)
                            .error(R.drawable.no_image_placeholder)
                            .into(image);
                }
            }

            // ✅ SET UP DELETE BUTTON CLICK LISTENER
            final int position = i;
            deleteButton.setOnClickListener(v -> deleteExercise(position));

            exercisesContainer.addView(card);
            order++;
        }
    }
    private void deleteExercise(int position) {
        if (currentWorkoutExercises == null || position < 0 || position >= currentWorkoutExercises.size()) {
            return;
        }

        // Get exercise name for confirmation message
        String exerciseName = "this exercise";
        if (currentWorkoutExercises.get(position).getExerciseInfo() != null &&
                currentWorkoutExercises.get(position).getExerciseInfo().getName() != null) {
            exerciseName = currentWorkoutExercises.get(position).getExerciseInfo().getName();
        }

        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Delete Exercise")
                .setMessage("Are you sure you want to remove '" + exerciseName + "' from your workout?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Remove from list
                    WorkoutExercise removedExercise = currentWorkoutExercises.remove(position);

                    // Check if workout is now empty
                    if (currentWorkoutExercises.isEmpty()) {
                        Toast.makeText(this, "All exercises removed. Generating new workout...",
                                Toast.LENGTH_SHORT).show();
                        startWorkoutButton.setEnabled(false);
                        fetchAllExercises();
                        return;
                    }

                    // Update UI
                    showExercises(currentWorkoutExercises);
                    Toast.makeText(this, "Exercise removed", Toast.LENGTH_SHORT).show();

                    // Save changes to Firestore
                    saveWorkoutToFirestore();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Saves the current workout to Firestore
     */
    private void saveWorkoutToFirestore() {
        if (currentUser == null) {
            Log.w(TAG, "Cannot save workout: user not logged in");
            return;
        }

        String uid = currentUser.getUid();
        DocumentReference workoutRef = firestore.collection("users")
                .document(uid)
                .collection("currentWorkout")
                .document("week_" + userProfile.getCurrentWeek());

        WorkoutWrapper wrapper = new WorkoutWrapper(currentWorkoutExercises, false);
        wrapper.createdAt = System.currentTimeMillis();

        workoutRef.set(wrapper)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Workout updated in Firestore after deletion");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating workout in Firestore", e);
                    Toast.makeText(this, "Failed to save changes", Toast.LENGTH_SHORT).show();
                });
    }
    public static class WorkoutWrapper {
        public List<WorkoutExercise> exercises;
        public boolean completed;
        public Long createdAt; // ✅ ADD THIS FIELD

        public WorkoutWrapper() {}

        public WorkoutWrapper(List<WorkoutExercise> exercises, boolean completed) {
            this.exercises = exercises;
            this.completed = completed;
            this.createdAt = System.currentTimeMillis(); // ✅ Initialize timestamp
        }

        public List<WorkoutExercise> toWorkoutExercises() {
            return exercises != null ? exercises : new ArrayList<>();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }


}