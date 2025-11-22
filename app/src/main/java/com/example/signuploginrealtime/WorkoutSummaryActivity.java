package com.example.signuploginrealtime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.signuploginrealtime.models.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Locale;

public class WorkoutSummaryActivity extends AppCompatActivity {

    private static final String TAG = "WorkoutSummary";

    // UI Elements
    private TextView tvSummaryTitle;
    private LinearLayout summaryContainer;
    private Button btnContinue;

    // Before/After Comparison UI (Side by Side)
    private TextView tvWeightBefore, tvWeightAfter;
    private TextView tvBMIBefore, tvBMIAfter;
    private TextView tvCaloriesBefore, tvCaloriesAfter;
    private TextView tvExercisesBefore, tvExercisesAfter;
    private TextView tvDurationBefore, tvDurationAfter;
    private TextView tvMusclesBefore, tvMusclesAfter;

    // Data
    private ArrayList<ExercisePerformanceData> performanceDataList;
    private UserProfile userProfile;
    private int workoutDurationMinutes;

    // Before workout stats (for comparison)
    private double beforeWeight;
    private double beforeBMI;
    private int beforeCaloriesToday;
    private int beforeDurationToday;
    private int beforeMuscleGroupsToday;
    private int beforeExercisesToday;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_summary);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI
        initializeViews();

        // Get data from intent
        performanceDataList = (ArrayList<ExercisePerformanceData>) getIntent()
                .getSerializableExtra("performanceData");
        workoutDurationMinutes = getIntent().getIntExtra("workoutDuration", 0);

        if (performanceDataList == null) performanceDataList = new ArrayList<>();

        // 🆕 DEBUG: Log what data we received
        Log.d(TAG, "📥 Received workout data:");
        Log.d(TAG, "📥 Duration from intent: " + workoutDurationMinutes + " minutes");
        Log.d(TAG, "📥 Performance data count: " + performanceDataList.size());

        for (int i = 0; i < performanceDataList.size(); i++) {
            ExercisePerformanceData data = performanceDataList.get(i);
            Log.d(TAG, "📥 Exercise " + (i+1) + ": " + data.getExerciseName() +
                  " | Status: " + data.getStatus() +
                  " | Duration: " + data.getActualDurationSeconds() + "s" +
                  " | Reps: " + data.getActualReps());
        }

        // Load user profile and calculate metrics
        loadUserProfileAndCalculateMetrics();

        // Set up continue button
        btnContinue.setOnClickListener(v -> {
            // Go to MainActivity with celebration flags
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("workout_just_completed", true); // Signal that a workout was just completed
            startActivity(intent);
            finish();
        });
    }

    private void initializeViews() {
        tvSummaryTitle = findViewById(R.id.tv_summary_title);
        summaryContainer = findViewById(R.id.summary_container);
        btnContinue = findViewById(R.id.btn_continue);

        // Before/After Comparison (Side by Side)
        tvWeightBefore = findViewById(R.id.tv_weight_before);
        tvWeightAfter = findViewById(R.id.tv_weight_after);
        tvBMIBefore = findViewById(R.id.tv_bmi_before);
        tvBMIAfter = findViewById(R.id.tv_bmi_after);
        tvCaloriesBefore = findViewById(R.id.tv_calories_before);
        tvCaloriesAfter = findViewById(R.id.tv_calories_after);
        tvExercisesBefore = findViewById(R.id.tv_exercises_before);
        tvExercisesAfter = findViewById(R.id.tv_exercises_after);
        tvDurationBefore = findViewById(R.id.tv_duration_before);
        tvDurationAfter = findViewById(R.id.tv_duration_after);
        tvMusclesBefore = findViewById(R.id.tv_muscles_before);
        tvMusclesAfter = findViewById(R.id.tv_muscles_after);
    }

    private void loadUserProfileAndCalculateMetrics() {
        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User not authenticated");
            showDefaultMetrics();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(this::processUserProfileAndCalculate)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user profile", e);
                    showDefaultMetrics();
                });
    }

    private void processUserProfileAndCalculate(DocumentSnapshot document) {
        Log.d(TAG, "👤 Processing user profile...");

        if (document.exists()) {
            Log.d(TAG, "👤 User document exists");

            // Parse user profile
            userProfile = new UserProfile();

            Double weight = document.getDouble("weight");
            Double height = document.getDouble("height");
            Long age = document.getLong("age");
            String gender = document.getString("gender");
            String fitnessLevel = document.getString("fitnessLevel");

            userProfile.setWeight(weight != null ? weight : 70.0);
            userProfile.setHeight(height != null ? height : 170.0);
            userProfile.setAge(age != null ? age.intValue() : 25);
            userProfile.setGender(gender != null ? gender : "Male");
            userProfile.setFitnessLevel(fitnessLevel != null ? fitnessLevel : "Moderately Active");

            // Store "before workout" stats for comparison
            beforeWeight = userProfile.getWeight();
            beforeBMI = userProfile.calculateBMI();

            Log.d(TAG, "👤 Profile loaded - Weight: " + userProfile.getWeight() + "kg, Height: " + userProfile.getHeight() + "cm, Age: " + userProfile.getAge());
            Log.d(TAG, "👤 Profile loaded - Gender: " + userProfile.getGender() + ", Fitness Level: " + userProfile.getFitnessLevel());

        } else {
            Log.w(TAG, "👤 User document doesn't exist, using defaults");
            // Use default profile
            userProfile = createDefaultProfile();
            beforeWeight = userProfile.getWeight();
            beforeBMI = userProfile.calculateBMI();
        }

        // Load today's workout history for comparison
        loadTodayWorkoutHistory();
    }

    private UserProfile createDefaultProfile() {
        UserProfile profile = new UserProfile();
        profile.setWeight(70.0);
        profile.setHeight(170.0);
        profile.setAge(25);
        profile.setGender("Male");
        profile.setFitnessLevel("Moderately Active");
        return profile;
    }

    private void calculateAndDisplayMetrics() {
        // Calculate metrics
        WorkoutMetrics metrics = calculateWorkoutMetrics();

        // Display simple side-by-side comparison
        displaySideBySideComparison(metrics);
    }

    private WorkoutMetrics calculateWorkoutMetrics() {
        WorkoutMetrics metrics = new WorkoutMetrics();

        Log.d(TAG, "📊 Starting metrics calculation");
        Log.d(TAG, "📊 Workout duration from intent: " + workoutDurationMinutes + " minutes");
        Log.d(TAG, "📊 Performance data list size: " + (performanceDataList != null ? performanceDataList.size() : "null"));

        // 1. Workout Duration (from intent or calculate from performance data)
        if (workoutDurationMinutes > 0) {
            metrics.durationMinutes = workoutDurationMinutes;
            Log.d(TAG, "📊 Using provided duration: " + workoutDurationMinutes + " minutes");
        } else {
            // Calculate from performance data
            int totalSeconds = 0;
            for (ExercisePerformanceData data : performanceDataList) {
                totalSeconds += data.getActualDurationSeconds();
                // Add estimated rest time between exercises (30 seconds average)
                totalSeconds += 30;
            }
            metrics.durationMinutes = Math.max(1, totalSeconds / 60);
            Log.d(TAG, "📊 Calculated duration from data: " + metrics.durationMinutes + " minutes");
        }

        // 🆕 FALLBACK: If still no duration, estimate based on number of exercises
        if (metrics.durationMinutes <= 0) {
            int exerciseCount = performanceDataList.size();
            metrics.durationMinutes = Math.max(5, exerciseCount * 3); // 3 minutes per exercise minimum
            Log.d(TAG, "📊 Fallback duration estimate: " + metrics.durationMinutes + " minutes for " + exerciseCount + " exercises");
        }

        // 2. Advanced Calories Calculation
        metrics.caloriesBurned = calculateAdvancedCaloriesBurned();

        // 3. Weight Loss Potential (more accurate calculation)
        // 1 pound of fat = 3500 calories, 1kg = 2.2 pounds
        // So 1kg fat = 7700 calories
        double fatCaloriesRatio = 7700.0; // calories per kg of fat
        double fatBurnEfficiency = 0.7; // 70% of calories from fat during exercise
        metrics.weightLossGrams = Math.max(1, (int) ((metrics.caloriesBurned * fatBurnEfficiency / fatCaloriesRatio) * 1000));

        // 4. Exercises Completed Analysis
        metrics.exercisesCompleted = performanceDataList.size();
        metrics.exercisesCompletedSuccessfully = 0;
        metrics.exercisesSkipped = 0;

        for (ExercisePerformanceData data : performanceDataList) {
            String status = data.getStatus();
            if ("completed".equals(status)) {
                metrics.exercisesCompletedSuccessfully++;
            } else if ("skipped".equals(status)) {
                metrics.exercisesSkipped++;
            }
            Log.d(TAG, "📊 Exercise: " + data.getExerciseName() + " | Status: " + status + " | Duration: " + data.getActualDurationSeconds() + "s");
        }

        Log.d(TAG, "📊 Exercises: " + metrics.exercisesCompletedSuccessfully + "/" + metrics.exercisesCompleted + " completed");

        // 5. Total Reps and Volume
        metrics.totalReps = 0;
        metrics.totalVolume = 0.0; // weight x reps

        for (ExercisePerformanceData data : performanceDataList) {
            metrics.totalReps += data.getActualReps();

            // Calculate volume (weight x reps)
            if (data.getWeight() > 0) {
                metrics.totalVolume += data.getWeight() * data.getActualReps();
            } else {
                // For bodyweight exercises, use user's body weight
                metrics.totalVolume += userProfile.getWeight() * data.getActualReps();
            }
        }

        // 6. Heart Rate Zones (estimated)
        metrics.avgHeartRate = estimateAverageHeartRate();
        metrics.maxHeartRate = 220 - userProfile.getAge();
        metrics.heartRateZone = calculateHeartRateZone(metrics.avgHeartRate, metrics.maxHeartRate);

        // 7. BMI and Health Metrics
        metrics.currentBMI = userProfile.calculateBMI();
        metrics.bmiCategory = userProfile.getBMICategory();

        // 8. Progressive Metrics (compared to user's average)
        metrics.intensityRating = calculateWorkoutIntensity();

        // 9. Estimated Recovery Time
        metrics.estimatedRecoveryHours = calculateRecoveryTime();

        Log.d(TAG, "📊 Final metrics - Duration: " + metrics.durationMinutes + "min, Calories: " + metrics.caloriesBurned + ", HR: " + metrics.avgHeartRate + " bpm");

        return metrics;
    }

    private int calculateAdvancedCaloriesBurned() {
        double totalCalories = 0;
        double weight = userProfile.getWeight();
        String fitnessLevel = userProfile.getFitnessLevel();
        String gender = userProfile.getGender();
        int age = userProfile.getAge();

        Log.d(TAG, "🔥 Calorie calculation - Weight: " + weight + "kg, Age: " + age + ", Duration: " + workoutDurationMinutes + "min");
        Log.d(TAG, "🔥 Performance data size: " + performanceDataList.size());

        // Base metabolic rate factor
        double bmrFactor = calculateBMRFactor(weight, userProfile.getHeight(), age, gender);
        Log.d(TAG, "🔥 BMR Factor: " + bmrFactor + " cal/hour");

        // Calculate from individual exercises
        for (ExercisePerformanceData exercise : performanceDataList) {
            double exerciseDurationHours = exercise.getActualDurationSeconds() / 3600.0;
            double metValue = getExerciseMETValue(exercise.getExerciseName(), exercise.getExerciseType());

            // Adjust MET based on fitness level
            metValue = adjustMETForFitnessLevel(metValue, fitnessLevel);

            // Calculate calories: MET x weight(kg) x time(hours)
            double exerciseCalories = metValue * weight * exerciseDurationHours;
            totalCalories += exerciseCalories;

            Log.d(TAG, "🔥 Exercise: " + exercise.getExerciseName() +
                  " | Duration: " + exercise.getActualDurationSeconds() + "s" +
                  " | MET: " + metValue + " | Calories: " + exerciseCalories);
        }

        // 🆕 FALLBACK: If no exercise calories (e.g., all skipped), use workout duration
        if (totalCalories == 0 && workoutDurationMinutes > 0) {
            Log.d(TAG, "⚠️ No exercise calories found, using fallback calculation");

            // Estimate based on general workout MET value
            double generalWorkoutMET = 5.0; // Moderate general exercise
            double workoutHours = workoutDurationMinutes / 60.0;
            totalCalories = generalWorkoutMET * weight * workoutHours;

            Log.d(TAG, "🔥 Fallback: " + generalWorkoutMET + " MET × " + weight + "kg × " + workoutHours + "h = " + totalCalories + " calories");
        }

        // Add base calories burned during workout duration
        double workoutHours = workoutDurationMinutes / 60.0;
        double baseCalories = bmrFactor * workoutHours;

        Log.d(TAG, "🔥 Base calories (BMR): " + baseCalories);
        Log.d(TAG, "🔥 Total calories: " + (totalCalories + baseCalories));

        return Math.max(1, (int) (totalCalories + baseCalories)); // Ensure at least 1 calorie
    }

    private double calculateBMRFactor(double weight, double height, int age, String gender) {
        // Mifflin-St Jeor Equation (calories per hour)
        double bmr;
        if ("Male".equalsIgnoreCase(gender)) {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) + 5;
        } else {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) - 161;
        }
        return bmr / 24.0; // Convert to calories per hour
    }

    private double getExerciseMETValue(String exerciseName, String exerciseType) {
        String nameLower = exerciseName.toLowerCase();

        // High intensity exercises
        if (nameLower.contains("burpee") || nameLower.contains("jump") ||
            nameLower.contains("sprint") || nameLower.contains("hiit")) {
            return 12.0;
        }

        // Moderate-high intensity
        if (nameLower.contains("squat") || nameLower.contains("deadlift") ||
            nameLower.contains("bench press") || nameLower.contains("pull up")) {
            return 8.0;
        }

        // Moderate intensity
        if (nameLower.contains("push up") || nameLower.contains("lunge") ||
            nameLower.contains("plank") || nameLower.contains("row")) {
            return 6.0;
        }

        // Lower intensity
        if (nameLower.contains("stretch") || nameLower.contains("walk") ||
            nameLower.contains("yoga")) {
            return 3.5;
        }

        // Default based on exercise type
        switch (exerciseType.toLowerCase()) {
            case "cardio": return 7.0;
            case "strength": return 6.0;
            case "flexibility": return 2.5;
            default: return 5.0;
        }
    }

    private double adjustMETForFitnessLevel(double baseMET, String fitnessLevel) {
        if (fitnessLevel == null) return baseMET;

        switch (fitnessLevel.toLowerCase()) {
            case "sedentary":
                return baseMET * 0.8;
            case "lightly active":
                return baseMET * 0.9;
            case "moderately active":
                return baseMET;
            case "very active":
                return baseMET * 1.1;
            case "extremely active":
                return baseMET * 1.2;
            default:
                return baseMET;
        }
    }

    private String calculateHeartRateZone(int avgHR, int maxHR) {
        double percentage = (double) avgHR / maxHR;

        if (percentage < 0.5) return "Very Light (50-60%)";
        else if (percentage < 0.6) return "Light (60-70%)";
        else if (percentage < 0.7) return "Moderate (70-80%)";
        else if (percentage < 0.8) return "Vigorous (80-90%)";
        else return "Maximum (90-100%)";
    }

    private String calculateWorkoutIntensity() {
        if (performanceDataList.isEmpty()) return "Light";

        double completionRate = (double) performanceDataList.size() / performanceDataList.size();
        int avgDuration = performanceDataList.stream()
                .mapToInt(ExercisePerformanceData::getActualDurationSeconds)
                .sum() / performanceDataList.size();

        if (completionRate >= 0.9 && avgDuration >= 45) return "High";
        else if (completionRate >= 0.7 && avgDuration >= 30) return "Moderate";
        else return "Light";
    }

    private int calculateRecoveryTime() {
        String intensity = calculateWorkoutIntensity();
        int baseRecovery = 24; // hours

        switch (intensity) {
            case "High": return baseRecovery + 24;
            case "Moderate": return baseRecovery + 12;
            default: return baseRecovery;
        }
    }

    private int calculateCaloriesBurned(int durationMinutes) {
        // Estimated calories based on workout intensity and user profile
        double weight = userProfile.getWeight();
        String fitnessLevel = userProfile.getFitnessLevel();

        // MET values for different workout intensities
        double metValue = 6.0; // Moderate intensity strength training

        // Adjust based on fitness level
        if ("Sedentary".equals(fitnessLevel)) {
            metValue = 4.0; // Lower intensity
        } else if ("Very Active".equals(fitnessLevel)) {
            metValue = 8.0; // Higher intensity
        }

        // Calculate: METs x weight(kg) x time(hours)
        double hours = durationMinutes / 60.0;
        return (int) (metValue * weight * hours);
    }

    private int estimateAverageHeartRate() {
        int age = userProfile.getAge();

        Log.d(TAG, "❤️ Heart rate calculation - Age: " + age);

        // Fallback if age is 0 or invalid
        if (age <= 0 || age > 100) {
            Log.w(TAG, "❤️ Invalid age (" + age + "), using default age 30");
            age = 30; // Default fallback age
        }

        int maxHeartRate = 220 - age;
        int estimatedHR = (int) (maxHeartRate * 0.675); // 67.5% for moderate exercise

        Log.d(TAG, "❤️ Max HR: " + maxHeartRate + ", Estimated avg HR: " + estimatedHR);

        // Ensure reasonable range
        if (estimatedHR < 60 || estimatedHR > 200) {
            Log.w(TAG, "❤️ Unreasonable HR (" + estimatedHR + "), using default 130");
            estimatedHR = 130; // Safe fallback
        }

        return estimatedHR;
    }

    /**
     * Display simple side-by-side before/after comparison
     */
    private void displaySideBySideComparison(WorkoutMetrics metrics) {
        int delay = 150;

        // Calculate "after" stats
        double afterWeight = beforeWeight - (metrics.weightLossGrams / 1000.0);
        double afterBMI = calculateBMI(afterWeight, userProfile.getHeight());
        int afterCaloriesToday = beforeCaloriesToday + metrics.caloriesBurned;
        int afterExercisesToday = beforeExercisesToday + metrics.exercisesCompletedSuccessfully;
        int afterDurationToday = beforeDurationToday + metrics.durationMinutes;

        // Count unique muscle groups from current workout
        java.util.Set<String> currentMuscleGroups = new java.util.HashSet<>();
        for (ExercisePerformanceData exercise : performanceDataList) {
            String type = exercise.getExerciseType();
            if (type != null && !type.isEmpty()) {
                currentMuscleGroups.add(type);
            }
            // Also check exercise name for muscle group hints
            String name = exercise.getExerciseName().toLowerCase();
            if (name.contains("chest") || name.contains("bench")) currentMuscleGroups.add("Chest");
            if (name.contains("back") || name.contains("row") || name.contains("pull")) currentMuscleGroups.add("Back");
            if (name.contains("shoulder") || name.contains("press")) currentMuscleGroups.add("Shoulders");
            if (name.contains("bicep") || name.contains("curl")) currentMuscleGroups.add("Biceps");
            if (name.contains("tricep") || name.contains("dip")) currentMuscleGroups.add("Triceps");
            if (name.contains("leg") || name.contains("squat") || name.contains("lunge")) currentMuscleGroups.add("Legs");
            if (name.contains("core") || name.contains("ab") || name.contains("plank")) currentMuscleGroups.add("Core");
        }
        int afterMuscleGroupsToday = beforeMuscleGroupsToday + currentMuscleGroups.size();

        // Weight
        animateTextView(tvWeightBefore, String.format(Locale.US, "%.1fkg", beforeWeight), delay);
        animateTextView(tvWeightAfter, String.format(Locale.US, "%.1fkg", afterWeight), delay * 2);

        // BMI
        animateTextView(tvBMIBefore, String.format(Locale.US, "%.1f", beforeBMI), delay * 3);
        animateTextView(tvBMIAfter, String.format(Locale.US, "%.1f", afterBMI), delay * 4);

        // Calories
        animateTextView(tvCaloriesBefore, String.format(Locale.US, "%d", beforeCaloriesToday), delay * 5);
        animateTextView(tvCaloriesAfter, String.format(Locale.US, "%d", afterCaloriesToday), delay * 6);

        // Exercises
        animateTextView(tvExercisesBefore, String.format(Locale.US, "%d", beforeExercisesToday), delay * 7);
        animateTextView(tvExercisesAfter, String.format(Locale.US, "%d", afterExercisesToday), delay * 8);

        // Duration
        animateTextView(tvDurationBefore, String.format(Locale.US, "%dmin", beforeDurationToday), delay * 9);
        animateTextView(tvDurationAfter, String.format(Locale.US, "%dmin", afterDurationToday), delay * 10);

        // Muscle Groups
        animateTextView(tvMusclesBefore, String.format(Locale.US, "%d", beforeMuscleGroupsToday), delay * 11);
        animateTextView(tvMusclesAfter, String.format(Locale.US, "%d", afterMuscleGroupsToday), delay * 12);

        // Summary title with celebration
        tvSummaryTitle.postDelayed(() -> {
            String message = getSimpleMotivationalMessage(metrics, afterCaloriesToday);
            tvSummaryTitle.setText(message);
            tvSummaryTitle.setTextColor(Color.parseColor("#2E7D32")); // Green
        }, delay * 13);

        Log.d(TAG, "📈 Displayed side-by-side comparison - Before: " + beforeWeight + "kg, " + beforeCaloriesToday + "cal, " + beforeExercisesToday + " exercises | After: " + afterWeight + "kg, " + afterCaloriesToday + "cal, " + afterExercisesToday + " exercises");
    }

    /**
     * Simple motivational message
     */
    private String getSimpleMotivationalMessage(WorkoutMetrics metrics, int totalCalories) {
        if (totalCalories > 500) {
            return "🔥 Amazing! " + totalCalories + " calories burned today!";
        } else if (totalCalories > 300) {
            return "💪 Great job! " + totalCalories + " calories burned!";
        } else if (totalCalories > 100) {
            return "👍 Good work! Keep it up!";
        } else {
            return "🌟 Every workout counts!";
        }
    }

    private void animateTextView(TextView textView, String text, int delay) {
        textView.postDelayed(() -> {
            textView.setText(text);
            textView.setAlpha(0f);
            textView.setVisibility(TextView.VISIBLE);
            textView.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .start();
        }, delay);
    }

    private void showDefaultMetrics() {
        // Not needed anymore with simplified layout
    }

    /**
     * Calculate BMI from weight and height
     */
    private double calculateBMI(double weightKg, double heightCm) {
        double heightM = heightCm / 100.0;
        return weightKg / (heightM * heightM);
    }

    /**
     * Load today's workout history to compare before/after stats
     */
    private void loadTodayWorkoutHistory() {
        if (mAuth.getCurrentUser() == null) {
            beforeCaloriesToday = 0;
            beforeDurationToday = 0;
            beforeMuscleGroupsToday = 0;
            beforeExercisesToday = 0;
            calculateAndDisplayMetrics();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        // Get today's date at start of day
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        com.google.firebase.Timestamp startOfDay = new com.google.firebase.Timestamp(calendar.getTime());

        Log.d(TAG, "📅 Loading today's workout history...");

        // Query history collection for today's workouts (excluding current one)
        db.collection("history")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    beforeCaloriesToday = 0;
                    beforeDurationToday = 0;
                    beforeExercisesToday = 0;
                    java.util.Set<String> muscleGroupsSet = new java.util.HashSet<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Long calories = doc.getLong("caloriesBurned");
                        Long duration = doc.getLong("durationMinutes");
                        Long exercises = doc.getLong("exercisesCompleted");
                        @SuppressWarnings("unchecked")
                        java.util.List<String> muscles = (java.util.List<String>) doc.get("muscleGroups");

                        if (calories != null) beforeCaloriesToday += calories.intValue();
                        if (duration != null) beforeDurationToday += duration.intValue();
                        if (exercises != null) beforeExercisesToday += exercises.intValue();
                        if (muscles != null) muscleGroupsSet.addAll(muscles);
                    }

                    beforeMuscleGroupsToday = muscleGroupsSet.size();

                    Log.d(TAG, "📅 Before stats - Calories: " + beforeCaloriesToday + ", Duration: " + beforeDurationToday + "min, Exercises: " + beforeExercisesToday + ", Muscle groups: " + beforeMuscleGroupsToday);

                    // Now calculate and display metrics
                    calculateAndDisplayMetrics();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading today's history", e);
                    beforeCaloriesToday = 0;
                    beforeDurationToday = 0;
                    beforeMuscleGroupsToday = 0;
                    beforeExercisesToday = 0;
                    calculateAndDisplayMetrics();
                });
    }



    private static class WorkoutMetrics {
        int durationMinutes;
        int caloriesBurned;
        int weightLossGrams;
        int exercisesCompleted;
        int exercisesCompletedSuccessfully;
        int exercisesSkipped;
        int totalReps;
        double totalVolume; // weight x reps in kg
        int avgHeartRate;
        int maxHeartRate;
        String heartRateZone;
        double currentBMI;
        String bmiCategory;
        String intensityRating; // High, Moderate, Light
        int estimatedRecoveryHours;
    }
}
