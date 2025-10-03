package com.example.signuploginrealtime.logic;

import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.models.UserProfile;

import java.util.ArrayList;
import java.util.List;

public class WorkoutProgression {

    public static Workout generateProgressiveWorkout(Workout baseWorkout,
                                                     int completedWeeks,
                                                     UserProfile userProfile) {

        if (baseWorkout == null || baseWorkout.getExercises() == null) {
            return baseWorkout;
        }

        List<WorkoutExercise> progressedExercises = new ArrayList<>();
        String level = userProfile != null ? userProfile.getFitnessLevel().toLowerCase() : "beginner";
        int frequency = userProfile != null ? userProfile.getWorkoutDaysPerWeek() : 3;

        for (WorkoutExercise we : baseWorkout.getExercises()) {
            WorkoutExercise newWe = new WorkoutExercise();
            newWe.setExerciseInfo(we.getExerciseInfo());
            newWe.setOrder(we.getOrder());

            int baseSets = we.getSets();
            int baseReps = we.getReps();
            int baseRest = we.getRestSeconds();

            // ====================
            // Progressive Overload - Realistic Approach
            // ====================

            // Progression rate based on frequency
            // More frequent training = faster adaptation
            int weeksPerSetIncrease = (frequency >= 5) ? 4 : 6;  // +1 set every 4-6 weeks
            int weeksPerRepIncrease = (frequency >= 5) ? 2 : 3;  // +1 rep every 2-3 weeks

            int extraSets = completedWeeks / weeksPerSetIncrease;
            int extraReps = completedWeeks / weeksPerRepIncrease;
            int restReduction = completedWeeks / 2; // Reduce rest by 1 sec every 2 weeks

            int sets = baseSets + extraSets;
            int reps = baseReps + extraReps;
            int rest = baseRest - restReduction;

            // ====================
            // Apply fitness-level caps (realistic limits)
            // ====================
            int maxTotalSets, maxTotalReps, minRest;

            switch (level) {
                case "sedentary":
                    maxTotalSets = 3;      // Never exceed 3 sets
                    maxTotalReps = 12;     // Cap at 12 reps
                    minRest = 60;
                    break;
                case "lightly active":
                    maxTotalSets = 4;
                    maxTotalReps = 15;
                    minRest = 50;
                    break;
                case "moderately active":
                    maxTotalSets = 4;
                    maxTotalReps = 15;
                    minRest = 45;
                    break;
                case "very active":
                    maxTotalSets = 5;
                    maxTotalReps = 18;
                    minRest = 40;
                    break;
                default: // beginner
                    maxTotalSets = 3;
                    maxTotalReps = 12;
                    minRest = 60;
                    break;
            }

            // Apply absolute caps (don't let progression go crazy)
            sets = Math.min(sets, maxTotalSets);
            reps = Math.min(reps, maxTotalReps);
            rest = Math.max(rest, minRest);

            // Final safety bounds
            newWe.setSets(Math.max(2, Math.min(sets, 5)));        // 2-5 sets always
            newWe.setReps(Math.max(6, Math.min(reps, 20)));       // 6-20 reps always
            newWe.setRestSeconds(Math.max(30, Math.min(rest, 120))); // 30-120 sec always

            progressedExercises.add(newWe);
        }

        return new Workout(progressedExercises, progressedExercises.size() * 5);
    }
}