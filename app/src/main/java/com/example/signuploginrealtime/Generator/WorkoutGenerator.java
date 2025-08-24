package com.example.signuploginrealtime.Generator;

import com.example.signuploginrealtime.api.WgerApiService;
import com.example.signuploginrealtime.models.Exercise;
import com.example.signuploginrealtime.models.UserProfile;
import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WorkoutGenerator {
    private WgerApiService apiService;

    // Wger Category IDs
    private static final int CATEGORY_ARMS = 8;
    private static final int CATEGORY_LEGS = 9;
    private static final int CATEGORY_ABS = 10;
    private static final int CATEGORY_CHEST = 11;
    private static final int CATEGORY_BACK = 12;
    private static final int CATEGORY_SHOULDERS = 13;

    // Equipment IDs
    private static final int EQUIPMENT_BODYWEIGHT = 7;
    private static final int EQUIPMENT_DUMBBELL = 3;
    private static final int EQUIPMENT_BARBELL = 1;

    public WorkoutGenerator() {
        this.apiService = new WgerApiService();
    }

    public Workout generateWorkout(UserProfile user) {
        List<Exercise> allExercises = new ArrayList<>();

        try {
            // Get exercises based on fitness goal
            switch (user.getFitnessGoal().toLowerCase()) {
                case "weight_loss":
                case "weight loss":
                    // Full body workout for weight loss
                    allExercises.addAll(apiService.getExercisesByCategory(CATEGORY_CHEST));
                    allExercises.addAll(apiService.getExercisesByCategory(CATEGORY_LEGS));
                    allExercises.addAll(apiService.getExercisesByCategory(CATEGORY_ABS));
                    allExercises.addAll(apiService.getExercisesByCategory(CATEGORY_BACK));
                    break;

                case "muscle_gain":
                case "muscle gain":
                    // Strength training focus
                    allExercises.addAll(apiService.getExercisesByCategory(CATEGORY_CHEST));
                    allExercises.addAll(apiService.getExercisesByCategory(CATEGORY_BACK));
                    allExercises.addAll(apiService.getExercisesByCategory(CATEGORY_ARMS));
                    allExercises.addAll(apiService.getExercisesByCategory(CATEGORY_SHOULDERS));
                    allExercises.addAll(apiService.getExercisesByCategory(CATEGORY_LEGS));
                    break;

                case "endurance":
                    // Bodyweight and endurance focus
                    allExercises.addAll(apiService.getExercisesByEquipment(EQUIPMENT_BODYWEIGHT));
                    allExercises.addAll(apiService.getExercisesByCategory(CATEGORY_ABS));
                    allExercises.addAll(apiService.getExercisesByCategory(CATEGORY_LEGS));
                    break;

                default:
                    // General fitness
                    allExercises.addAll(apiService.getExercisesByCategory(CATEGORY_CHEST));
                    allExercises.addAll(apiService.getExercisesByCategory(CATEGORY_LEGS));
                    allExercises.addAll(apiService.getExercisesByCategory(CATEGORY_ABS));
                    break;
            }

            // Filter and select exercises
            List<Exercise> selectedExercises = selectAppropriateExercises(allExercises, user);

            // Create workout plan
            return createWorkoutPlan(selectedExercises, user);

        } catch (Exception e) {
            e.printStackTrace();
            // Return fallback workout if API fails
            return createFallbackWorkout(user);
        }
    }

    private List<Exercise> selectAppropriateExercises(List<Exercise> exercises, UserProfile user) {
        // Remove duplicates and filter empty names
        Set<String> uniqueNames = new HashSet<>();
        List<Exercise> filtered = new ArrayList<>();

        for (Exercise exercise : exercises) {
            if (exercise.getName() != null && !exercise.getName().trim().isEmpty()
                    && !uniqueNames.contains(exercise.getName().toLowerCase())) {
                uniqueNames.add(exercise.getName().toLowerCase());
                filtered.add(exercise);
            }
        }

        // Determine number of exercises based on fitness level
        int numberOfExercises;
        switch (user.getFitnessLevel().toLowerCase()) {
            case "beginner":
                numberOfExercises = 6;
                break;
            case "intermediate":
                numberOfExercises = 8;
                break;
            case "advanced":
                numberOfExercises = 10;
                break;
            default:
                numberOfExercises = 6;
        }

        // Shuffle and select
        Collections.shuffle(filtered);
        return filtered.stream()
                .limit(Math.min(numberOfExercises, filtered.size()))
                .collect(Collectors.toList());
    }

    private Workout createWorkoutPlan(List<Exercise> exercises, UserProfile user) {
        List<WorkoutExercise> workoutExercises = new ArrayList<>();

        for (int i = 0; i < exercises.size(); i++) {
            Exercise exercise = exercises.get(i);
            WorkoutExercise workoutExercise = new WorkoutExercise();

            workoutExercise.setExercise(exercise);
            workoutExercise.setOrder(i + 1);

            // Set reps and sets based on goal and level
            setSetsAndReps(workoutExercise, user);

            workoutExercises.add(workoutExercise);
        }

        return new Workout(workoutExercises, estimateDuration(workoutExercises));
    }

    private void setSetsAndReps(WorkoutExercise workoutExercise, UserProfile user) {
        String goal = user.getFitnessGoal().toLowerCase();
        String level = user.getFitnessLevel().toLowerCase();

        switch (goal) {
            case "weight_loss":
            case "weight loss":
                workoutExercise.setSets(3);
                workoutExercise.setReps(level.equals("beginner") ? 12 : 15);
                workoutExercise.setRestSeconds(60);
                break;

            case "muscle_gain":
            case "muscle gain":
                workoutExercise.setSets(4);
                workoutExercise.setReps(level.equals("beginner") ? 8 : 10);
                workoutExercise.setRestSeconds(90);
                break;

            case "endurance":
                workoutExercise.setSets(2);
                workoutExercise.setReps(20);
                workoutExercise.setRestSeconds(45);
                break;

            default:
                workoutExercise.setSets(3);
                workoutExercise.setReps(12);
                workoutExercise.setRestSeconds(60);
        }
    }

    private int estimateDuration(List<WorkoutExercise> exercises) {
        int totalSeconds = 0;
        for (WorkoutExercise exercise : exercises) {
            // Estimate: 30 seconds per set + rest time
            totalSeconds += exercise.getSets() * (30 + exercise.getRestSeconds());
        }
        return Math.max(totalSeconds / 60, 20); // Convert to minutes, minimum 20
    }

    // Fallback workout if API fails
    private Workout createFallbackWorkout(UserProfile user) {
        List<WorkoutExercise> fallbackExercises = new ArrayList<>();

        // Basic bodyweight exercises
        String[] basicExercises = {"Push-ups", "Squats", "Plank", "Lunges", "Burpees", "Mountain Climbers"};

        for (int i = 0; i < basicExercises.length; i++) {
            Exercise exercise = new Exercise();
            exercise.setId(1000 + i);
            exercise.setName(basicExercises[i]);
            exercise.setDescription("Basic bodyweight exercise");

            WorkoutExercise workoutExercise = new WorkoutExercise();
            workoutExercise.setExercise(exercise);
            workoutExercise.setOrder(i + 1);
            setSetsAndReps(workoutExercise, user);

            fallbackExercises.add(workoutExercise);
        }

        return new Workout(fallbackExercises, 30);
    }
}
