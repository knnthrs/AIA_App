package com.example.signuploginrealtime.logic;

import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;

import java.util.ArrayList;
import java.util.List;

public class WorkoutAdjustmentHelper {

    /**
     * Adjusts workout difficulty based on user feedback
     * @param baseWorkout The current workout
     * @param multiplier Difficulty multiplier (0.75 = easier, 1.25 = harder)
     * @return Adjusted workout
     */
    public static Workout adjustWorkoutDifficulty(Workout baseWorkout, double multiplier) {
        if (baseWorkout == null || baseWorkout.getExercises() == null) {
            return baseWorkout;
        }

        List<WorkoutExercise> adjustedExercises = new ArrayList<>();

        for (WorkoutExercise we : baseWorkout.getExercises()) {
            WorkoutExercise newWe = new WorkoutExercise();
            newWe.setExerciseInfo(we.getExerciseInfo());
            newWe.setOrder(we.getOrder());

            int baseSets = we.getSets();
            int baseReps = we.getReps();
            int baseRest = we.getRestSeconds();

            // Apply multiplier
            int adjustedSets = (int) Math.round(baseSets * multiplier);
            int adjustedReps = (int) Math.round(baseReps * multiplier);
            // Rest is inverse - harder workout = less rest
            int adjustedRest = (int) Math.round(baseRest / multiplier);

            // Apply safety bounds
            newWe.setSets(Math.max(2, Math.min(adjustedSets, 5)));        // 2-5 sets
            newWe.setReps(Math.max(6, Math.min(adjustedReps, 20)));       // 6-20 reps
            newWe.setRestSeconds(Math.max(30, Math.min(adjustedRest, 120))); // 30-120 sec

            adjustedExercises.add(newWe);
        }

        return new Workout(adjustedExercises, adjustedExercises.size() * 5);
    }

    /**
     * Gets the stored difficulty multiplier from preferences
     * @param multiplierFromPrefs The multiplier from SharedPreferences
     * @return The multiplier to apply (default 1.0)
     */
    public static double getDifficultyMultiplier(float multiplierFromPrefs) {
        return multiplierFromPrefs > 0 ? multiplierFromPrefs : 1.0;
    }
}