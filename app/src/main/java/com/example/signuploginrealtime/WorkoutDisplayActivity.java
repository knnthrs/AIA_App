package com.example.signuploginrealtime;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.signuploginrealtime.R;
import com.example.signuploginrealtime.Generator.WorkoutGenerator;
import com.example.signuploginrealtime.models.UserProfile;
import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.models.Exercise;

public class WorkoutDisplayActivity extends AppCompatActivity {

    // UI Components
    private LinearLayout exercisesContainer;
    private TextView tvWorkoutTitle;
    private TextView tvWorkoutSubtitle;
    private TextView tvUserGoal;
    private TextView tvFitnessLevel;
    private TextView tvWorkoutDuration;
    private TextView tvTotalExercises;
    private TextView tvEstimatedCalories;
    private TextView tvDifficulty;

    // Fixed: Changed from CardView to CardView (these are correctly CardView in XML)
    private CardView btnStartWorkout;
    private CardView btnGenerateNew;
    private CardView btnCustomize;

    // Fixed: Changed from CardView to ImageButton (these are ImageButton in XML)
    private ImageButton btnBack;
    private ImageButton btnRegenerate;

    private ProgressBar loadingProgress;

    // Data
    private WorkoutGenerator workoutGenerator;
    private UserProfile userProfile;
    private Workout currentWorkout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_display);

        // Initialize components
        initializeViews();
        initializeData();

        // Set click listeners
        setClickListeners();

        // Generate workout
        generateWorkout();
    }

    private void initializeViews() {
        exercisesContainer = findViewById(R.id.exercises_container);
        tvWorkoutTitle = findViewById(R.id.tv_workout_title);
        tvWorkoutSubtitle = findViewById(R.id.tv_workout_subtitle);
        tvUserGoal = findViewById(R.id.tv_user_goal);
        tvFitnessLevel = findViewById(R.id.tv_fitness_level);
        tvWorkoutDuration = findViewById(R.id.tv_workout_duration);
        tvTotalExercises = findViewById(R.id.tv_total_exercises);
        tvEstimatedCalories = findViewById(R.id.tv_estimated_calories);
        tvDifficulty = findViewById(R.id.tv_difficulty);

        // Fixed: Correct types
        btnStartWorkout = findViewById(R.id.btn_start_workout);
        btnGenerateNew = findViewById(R.id.btn_generate_new);
        btnCustomize = findViewById(R.id.btn_customize);
        btnBack = findViewById(R.id.btn_back);
        btnRegenerate = findViewById(R.id.btn_regenerate);

        // Add loading progress bar if you haven't added it to XML
        loadingProgress = new ProgressBar(this);
    }

    private void initializeData() {
        workoutGenerator = new WorkoutGenerator();

        // Get user profile from Intent (modify this based on how you pass data)
        userProfile = getUserProfileFromIntent();

        // Update UI with user info
        updateUserInfo();
    }

    private UserProfile getUserProfileFromIntent() {
        UserProfile profile = new UserProfile();

        // Get data from Intent (modify these keys based on your implementation)
        profile.setFitnessGoal(getIntent().getStringExtra("fitness_goal"));
        profile.setFitnessLevel(getIntent().getStringExtra("fitness_level"));
        profile.setGender(getIntent().getStringExtra("gender"));
        profile.setAge(getIntent().getIntExtra("age", 25));
        profile.setWeight(getIntent().getDoubleExtra("weight", 70.0));
        profile.setHeight(getIntent().getDoubleExtra("height", 170.0));

        // Set defaults if null
        if (profile.getFitnessGoal() == null) profile.setFitnessGoal("weight_loss");
        if (profile.getFitnessLevel() == null) profile.setFitnessLevel("beginner");
        if (profile.getGender() == null) profile.setGender("male");

        return profile;
    }

    private void updateUserInfo() {
        tvUserGoal.setText(capitalizeFirst(userProfile.getFitnessGoal().replace("_", " ")));
        tvFitnessLevel.setText(capitalizeFirst(userProfile.getFitnessLevel()));

        // Set workout title based on goal
        String title = "Your " + capitalizeFirst(userProfile.getFitnessGoal().replace("_", " ")) + " Plan";
        tvWorkoutTitle.setText(title);

        String subtitle = "Customized for " + userProfile.getFitnessLevel() + " level";
        tvWorkoutSubtitle.setText(subtitle);
    }

    private void setClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnStartWorkout.setOnClickListener(v -> startWorkout());

        // Fixed: Use btnRegenerate instead of btnRegenerate
        btnRegenerate.setOnClickListener(v -> {
            Toast.makeText(this, "Generating new workout...", Toast.LENGTH_SHORT).show();
            generateWorkout();
        });

        // Fixed: Use btnGenerateNew
        btnGenerateNew.setOnClickListener(v -> {
            Toast.makeText(this, "Generating new workout...", Toast.LENGTH_SHORT).show();
            generateWorkout();
        });

        btnCustomize.setOnClickListener(v -> {
            Toast.makeText(this, "Customize feature coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Open customization activity
        });
    }

    private void generateWorkout() {
        showLoading(true);

        // Run in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                Workout workout = workoutGenerator.generateWorkout(userProfile);

                // Update UI on main thread
                runOnUiThread(() -> {
                    showLoading(false);
                    if (workout != null && !workout.getExercises().isEmpty()) {
                        currentWorkout = workout;
                        displayWorkout(workout);
                    } else {
                        showError("Failed to generate workout. Please try again.");
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError("Error generating workout: " + e.getMessage());
                });
            }
        }).start();
    }

    private void displayWorkout(Workout workout) {
        // Update stats
        tvTotalExercises.setText(String.valueOf(workout.getExercises().size()));
        tvWorkoutDuration.setText(workout.getDuration() + " min");
        tvEstimatedCalories.setText(String.valueOf(workout.getDuration() * 7)); // Rough estimate
        tvDifficulty.setText(getDifficultyRating(userProfile) + "/10");

        // Clear existing exercises
        exercisesContainer.removeAllViews();

        // Add exercise cards
        for (WorkoutExercise workoutExercise : workout.getExercises()) {
            addExerciseCard(workoutExercise);
        }
    }

    private void addExerciseCard(WorkoutExercise workoutExercise) {
        // Create exercise card programmatically
        View exerciseCard = LayoutInflater.from(this).inflate(R.layout.exercise_card_layout, exercisesContainer, false);

        // Find views in the card
        TextView exerciseNumber = exerciseCard.findViewById(R.id.tv_exercise_number);
        TextView exerciseName = exerciseCard.findViewById(R.id.tv_exercise_name);
        TextView exerciseDetails = exerciseCard.findViewById(R.id.tv_exercise_details);
        TextView exerciseRest = exerciseCard.findViewById(R.id.tv_exercise_rest);
        ImageView exerciseInfo = exerciseCard.findViewById(R.id.iv_exercise_info);

        // Set data
        exerciseNumber.setText(String.valueOf(workoutExercise.getOrder()));
        exerciseName.setText(workoutExercise.getExercise().getName());
        exerciseDetails.setText(workoutExercise.getSets() + " sets × " + workoutExercise.getReps() + " reps");
        exerciseRest.setText("Rest: " + workoutExercise.getRestSeconds() + " seconds");

        // Set click listener for info button
        exerciseInfo.setOnClickListener(v -> {
            showExerciseInfo(workoutExercise.getExercise());
        });

        // Add to container
        exercisesContainer.addView(exerciseCard);
    }

    private void showExerciseInfo(Exercise exercise) {
        String info = exercise.getDescription() != null ? exercise.getDescription() : "No description available";
        Toast.makeText(this, exercise.getName() + ": " + info, Toast.LENGTH_LONG).show();
        // TODO: Create a proper dialog or new activity for exercise details
    }

    private void startWorkout() {
        if (currentWorkout != null) {
            Toast.makeText(this, "Starting workout with " + currentWorkout.getExercises().size() + " exercises!", Toast.LENGTH_SHORT).show();
            // TODO: Launch workout execution activity
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            // You can add a loading overlay or progress bar to your XML
            Toast.makeText(this, "Loading workout...", Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private int getDifficultyRating(UserProfile profile) {
        switch (profile.getFitnessLevel().toLowerCase()) {
            case "beginner": return 3;
            case "intermediate": return 6;
            case "advanced": return 8;
            default: return 5;
        }
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}