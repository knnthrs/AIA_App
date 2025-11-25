package com.example.signuploginrealtime;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.signuploginrealtime.models.WorkoutHistory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WorkoutHistoryDetailActivity extends AppCompatActivity {

    private static final String TAG = "WorkoutHistoryDetail";

    private TextView tvDate, tvTime, tvDuration, tvExercisesCount, tvCalories, tvWeight, tvBmi, tvBmiCategory, tvBodyFocus;
    private LinearLayout exercisesContainer, layoutBodyFocus;

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private String workoutIdFromIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_history_detail);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize views
        initViews();

        // Get workout ID from intent and store it
        workoutIdFromIntent = getIntent().getStringExtra("workoutId");
        long timestamp = getIntent().getLongExtra("timestamp", 0);

        Log.d(TAG, "📝 onCreate - workoutId from intent: " + workoutIdFromIntent);
        Log.d(TAG, "📝 onCreate - timestamp from intent: " + timestamp);
        Log.d(TAG, "📝 onCreate - currentUser: " + (currentUser != null ? currentUser.getUid() : "null"));

        if (workoutIdFromIntent != null && !workoutIdFromIntent.isEmpty()) {
            loadWorkoutDetails(workoutIdFromIntent);
        } else {
            Log.e(TAG, "❌ workoutId is null or empty! Cannot load workout details.");
            Toast.makeText(this, "Error: Cannot load workout details", Toast.LENGTH_SHORT).show();
            // Show empty state or finish activity
            TextView errorView = new TextView(this);
            errorView.setText("Error loading workout details. Please try again.");
            errorView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            errorView.setPadding(32, 32, 32, 32);
            errorView.setTextSize(16);
            errorView.setGravity(android.view.Gravity.CENTER);

            if (exercisesContainer != null) {
                exercisesContainer.removeAllViews();
                exercisesContainer.addView(errorView);
            }
        }

        // Back button
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });
    }

    private void initViews() {
        tvDate = findViewById(R.id.tv_date);
        tvTime = findViewById(R.id.tv_time);
        tvDuration = findViewById(R.id.tv_duration);
        tvExercisesCount = findViewById(R.id.tv_exercises_count);
        tvCalories = findViewById(R.id.tv_calories);
        tvWeight = findViewById(R.id.tv_weight);
        tvBmi = findViewById(R.id.tv_bmi);
        tvBmiCategory = findViewById(R.id.tv_bmi_category);
        tvBodyFocus = findViewById(R.id.tv_body_focus);
        layoutBodyFocus = findViewById(R.id.layout_body_focus);
        exercisesContainer = findViewById(R.id.exercises_container);
    }

    private void loadWorkoutDetails(String workoutId) {
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        firestore.collection("users")
                .document(userId)
                .collection("workoutHistory")
                .document(workoutId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        WorkoutHistory workout = snapshot.toObject(WorkoutHistory.class);
                        if (workout != null) {
                            displayWorkoutDetails(workout);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading workout details", e));
    }

    private void displayWorkoutDetails(WorkoutHistory workout) {
        // Date and Time
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        Date date = new Date(workout.getTimestamp());
        tvDate.setText(dateFormat.format(date));
        tvTime.setText(timeFormat.format(date));

        // Stats
        tvDuration.setText(workout.getDuration() + " mins");
        tvExercisesCount.setText(String.valueOf(workout.getExercisesCount()));
        tvCalories.setText(String.valueOf(workout.getCaloriesBurned()));
        tvWeight.setText(String.format("%.1f kg", workout.getWeight()));
        tvBmi.setText(String.format("%.1f", workout.getBmi()));

        // BMI Category with color
        String bmiCategory = WorkoutHistory.getBMICategory(workout.getBmi());
        tvBmiCategory.setText(bmiCategory);
        int bmiColor = getBmiColor(workout.getBmi());
        tvBmiCategory.setTextColor(bmiColor);

        // Body Focus
        if (workout.getBodyFocus() != null && !workout.getBodyFocus().isEmpty()) {
            layoutBodyFocus.setVisibility(View.VISIBLE);
            tvBodyFocus.setText(String.join(", ", workout.getBodyFocus()));
        } else {
            layoutBodyFocus.setVisibility(View.GONE);
        }

        // ✅ Use the workoutId that came from the intent (the Firestore document ID)
        if (workoutIdFromIntent != null && !workoutIdFromIntent.isEmpty()) {
            loadExercisesFromFirestore(workoutIdFromIntent);
        } else {
            Log.e(TAG, "❌ workoutIdFromIntent is null when trying to load exercises");
            loadExercisesFromFirestore(null);
        }
    }

    private void loadExercisesFromFirestore(String workoutId) {
        if (currentUser == null || workoutId == null) {
            Log.e(TAG, "Cannot load exercises: user or workoutId is null");
            return;
        }

        String userId = currentUser.getUid();

        Log.d(TAG, "📝 Loading exercises for workout: " + workoutId);

        firestore.collection("users")
                .document(userId)
                .collection("workoutHistory")
                .document(workoutId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Log.d(TAG, "✅ Workout document found");

                        // Try to get exercises data
                        Object exercisesObj = snapshot.get("exercises");
                        Log.d(TAG, "📊 Exercises object type: " + (exercisesObj != null ? exercisesObj.getClass().getName() : "null"));
                        Log.d(TAG, "📊 Exercises object: " + exercisesObj);

                        if (exercisesObj instanceof List) {
                            List<Map<String, Object>> exercisesList = (List<Map<String, Object>>) exercisesObj;
                            Log.d(TAG, "✅ Exercises list size: " + exercisesList.size());
                            displayExercisesFromMaps(exercisesList);
                        } else {
                            Log.e(TAG, "❌ Exercises is not a List! Type: " + (exercisesObj != null ? exercisesObj.getClass() : "null"));
                            displayExercisesFromMaps(null);
                        }
                    } else {
                        Log.e(TAG, "❌ Workout document not found!");
                        displayExercisesFromMaps(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error loading exercises", e);
                    displayExercisesFromMaps(null);
                });
    }

    private void displayExercisesFromMaps(List<Map<String, Object>> exercises) {
        exercisesContainer.removeAllViews();

        Log.d(TAG, "📝 displayExercisesFromMaps called");
        Log.d(TAG, "📊 Exercises list: " + (exercises != null ? "size=" + exercises.size() : "null"));

        if (exercises == null || exercises.isEmpty()) {
            Log.w(TAG, "⚠️ No exercises to display");
            TextView emptyView = new TextView(this);
            emptyView.setText("No exercise data available");
            emptyView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            emptyView.setPadding(16, 16, 16, 16);
            emptyView.setTextSize(16);
            exercisesContainer.addView(emptyView);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < exercises.size(); i++) {
            Map<String, Object> exerciseData = exercises.get(i);

            Log.d(TAG, "📝 Exercise " + (i+1) + ": " + exerciseData);

            View exerciseView = inflater.inflate(R.layout.item_exercise_history, exercisesContainer, false);

            TextView tvExerciseNumber = exerciseView.findViewById(R.id.tv_exercise_number);
            TextView tvExerciseName = exerciseView.findViewById(R.id.tv_exercise_name);
            TextView tvExerciseDetails = exerciseView.findViewById(R.id.tv_exercise_details);

            tvExerciseNumber.setText(String.valueOf(i + 1));

            // Get exercise name
            String exerciseName = "Unknown Exercise";
            if (exerciseData.get("name") != null) {
                exerciseName = exerciseData.get("name").toString();
            }
            Log.d(TAG, "  📝 Name: " + exerciseName);
            tvExerciseName.setText(exerciseName);

            // Get reps (use actualReps if available, fallback to targetReps)
            int reps = 0;
            if (exerciseData.get("actualReps") != null) {
                Object repsObj = exerciseData.get("actualReps");
                reps = repsObj instanceof Long ? ((Long) repsObj).intValue() : (Integer) repsObj;
                Log.d(TAG, "  📝 Using actualReps: " + reps);
            } else if (exerciseData.get("targetReps") != null) {
                Object repsObj = exerciseData.get("targetReps");
                reps = repsObj instanceof Long ? ((Long) repsObj).intValue() : (Integer) repsObj;
                Log.d(TAG, "  📝 Using targetReps: " + reps);
            }

            // Get sets
            int sets = 3; // Default assumption
            if (exerciseData.get("sets") != null) {
                Object setsObj = exerciseData.get("sets");
                sets = setsObj instanceof Long ? ((Long) setsObj).intValue() : (Integer) setsObj;
                Log.d(TAG, "  📝 Sets: " + sets);
            }

            String details = sets + " sets × " + reps + " reps";
            Log.d(TAG, "  📝 Details: " + details);
            tvExerciseDetails.setText(details);

            exercisesContainer.addView(exerciseView);
            Log.d(TAG, "  ✅ Exercise view added to container");
        }

        Log.d(TAG, "✅ All exercises displayed. Total: " + exercises.size());
    }

    private int getBmiColor(double bmi) {
        if (bmi < 18.5) {
            return getResources().getColor(android.R.color.holo_orange_dark);
        } else if (bmi < 25) {
            return getResources().getColor(android.R.color.holo_green_dark);
        } else if (bmi < 30) {
            return getResources().getColor(android.R.color.holo_orange_dark);
        } else {
            return getResources().getColor(android.R.color.holo_red_dark);
        }
    }
}
