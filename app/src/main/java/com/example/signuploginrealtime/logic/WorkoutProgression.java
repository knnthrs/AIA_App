package com.example.signuploginrealtime.logic;

import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.models.ExerciseInfo;
import com.example.signuploginrealtime.models.UserProfile;

import java.util.ArrayList;
import java.util.List;

public class WorkoutProgression {

    public static Workout generateProgressiveWorkout(Workout baseWorkout, int dayNumber, UserProfile userProfile) {
        List<WorkoutExercise> newExercises = new ArrayList<>();

        for (WorkoutExercise we : baseWorkout.getExercises()) {
            WorkoutExercise progressed = new WorkoutExercise();

            // Copy ExerciseInfo
            ExerciseInfo originalInfo = we.getExerciseInfo();
            ExerciseInfo copyInfo = new ExerciseInfo();
            copyInfo.setName(originalInfo.getName());
            copyInfo.setDescription(originalInfo.getDescription());
            progressed.setExerciseInfo(copyInfo);

            progressed.setOrder(we.getOrder());

            // Determine base sets/reps from fitness goal
            int baseSets = we.getSets();
            int baseReps = we.getReps();
            int baseRest = we.getRestSeconds();

            String goal = userProfile.getFitnessGoal().toLowerCase();
            switch (goal) {
                case "lose weight":
                    baseSets = 3;
                    baseReps = 15;
                    baseRest = 45;
                    break;
                case "gain muscle":
                    baseSets = 4;
                    baseReps = 8;
                    baseRest = 90;
                    break;
                case "increase endurance":
                    baseSets = 2;
                    baseReps = 20;
                    baseRest = 30;
                    break;
                default:
                    baseSets = we.getSets();
                    baseReps = we.getReps();
                    baseRest = we.getRestSeconds();
            }

            // Progressive scaling per day
            int newReps = baseReps + (dayNumber / 2);
            int newSets = baseSets + (dayNumber / 7);
            int newRest = Math.max(30, baseRest - (dayNumber / 5) * 5);

            // Adjust for age
            if (userProfile.getAge() > 50) {
                newRest += 15;
            }

            progressed.setReps(newReps);
            progressed.setSets(newSets);
            progressed.setRestSeconds(newRest);

            newExercises.add(progressed);
        }

        Workout progressedWorkout = new Workout();
        progressedWorkout.setExercises(newExercises);

        return progressedWorkout;
    }
}
