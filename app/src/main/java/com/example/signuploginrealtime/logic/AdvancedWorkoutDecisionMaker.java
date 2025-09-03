package com.example.signuploginrealtime.logic;

import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.models.ExerciseInfo;
import com.example.signuploginrealtime.models.UserProfile;

import java.util.ArrayList;
import java.util.List;

public class AdvancedWorkoutDecisionMaker {

    public static Workout generatePersonalizedWorkout(List<ExerciseInfo> availableExercises,
                                                      UserProfile userProfile) {

        List<WorkoutExercise> exercises = new ArrayList<>();

        for (ExerciseInfo exInfo : availableExercises) {
            if (exInfo == null || exInfo.getName() == null) continue;

            // Skip disliked exercises
            if (userProfile.getDislikedExercises() != null &&
                    userProfile.getDislikedExercises().contains(exInfo.getName())) {
                continue;
            }

            WorkoutExercise we = new WorkoutExercise();
            we.setExerciseInfo(exInfo);
            we.setOrder(exercises.size() + 1);

            // Sets/reps based on fitness goal
            String goal = userProfile.getFitnessGoal().toLowerCase();
            switch (goal) {
                case "lose weight":
                    we.setSets(3);
                    we.setReps(15);
                    we.setRestSeconds(45);
                    break;
                case "gain muscle":
                    we.setSets(4);
                    we.setReps(8);
                    we.setRestSeconds(90);
                    break;
                case "increase endurance":
                    we.setSets(2);
                    we.setReps(20);
                    we.setRestSeconds(30);
                    break;
                default:
                    we.setSets(3);
                    we.setReps(12);
                    we.setRestSeconds(60);
            }

            exercises.add(we);

            if (exercises.size() >= 6) break; // limit to 6 exercises
        }

        return new Workout(exercises, exercises.size() * 5);
    }
}
