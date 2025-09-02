package com.example.signuploginrealtime.logic;

import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.models.ExerciseInfo;

import java.util.ArrayList;
import java.util.List;

public class WorkoutDecisionMaker {

    public static Workout generateBaseWorkout(List<ExerciseInfo> availableExercises,
                                              String fitnessGoal,
                                              String fitnessLevel,
                                              List<String> healthIssues,
                                              int age, String gender, float bmi) {

        List<WorkoutExercise> exercises = new ArrayList<>();

        for (ExerciseInfo exInfo : availableExercises) {
            if (exInfo == null || exInfo.getName() == null) continue;

            boolean skip = false;

            // Skip exercises based on health issues
            for (String issue : healthIssues) {
                if ("Back Problems".equalsIgnoreCase(issue)
                        && exInfo.getName().toLowerCase().contains("deadlift")) {
                    skip = true;
                    break;
                }
            }

            if (!skip) {
                WorkoutExercise we = new WorkoutExercise();

                // Directly use ExerciseInfo instead of Exercise
                we.setExerciseInfo(exInfo); // Make sure WorkoutExercise has setExerciseInfo method

                we.setOrder(exercises.size() + 1);
                we.setSets(3);      // default sets
                we.setReps(10);     // default reps
                we.setRestSeconds(60); // default rest

                exercises.add(we);

                // Limit to 5 exercises
                if (exercises.size() >= 5) break;
            }
        }

        // Create workout with total duration or default value
        return new Workout(exercises, 15);
    }
}
