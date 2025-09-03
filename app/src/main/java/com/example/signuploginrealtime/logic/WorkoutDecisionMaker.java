package com.example.signuploginrealtime.logic;

import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.models.ExerciseInfo;

import java.util.ArrayList;
import java.util.List;

public class WorkoutDecisionMaker {

    // Updated to accept List<ExerciseInfo>
    public static Workout generateBaseWorkout(
            List<ExerciseInfo> exercises,
            String goal,
            String level,
            List<String> disliked,
            int age,
            String gender,
            float weight
    ) {
        Workout workout = new Workout();
        List<WorkoutExercise> workoutExercises = new ArrayList<>();

        int order = 1;

        for (ExerciseInfo e : exercises) {
            // Skip disliked exercises
            if (disliked.contains(e.getName())) continue;

            WorkoutExercise we = new WorkoutExercise();
            we.setExerciseInfo(e);
            we.setOrder(order++);

            // Default beginner sets and reps (can adjust by level)
            switch (level.toLowerCase()) {
                case "beginner":
                    we.setSets(3);
                    we.setReps(10);
                    break;
                case "intermediate":
                    we.setSets(4);
                    we.setReps(12);
                    break;
                case "advanced":
                    we.setSets(5);
                    we.setReps(15);
                    break;
                default:
                    we.setSets(3);
                    we.setReps(10);
            }

            // Rest seconds default
            we.setRestSeconds(60);

            workoutExercises.add(we);
        }

        workout.setExercises(workoutExercises);
        return workout;
    }
}
