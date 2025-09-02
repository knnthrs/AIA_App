package com.example.signuploginrealtime.logic;

import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.models.UserProfile;

import java.util.ArrayList;
import java.util.List;

public class WorkoutProgression {

    public static Workout generateProgressiveWorkout(Workout baseWorkout, int dayNumber, UserProfile userProfile) {
        List<WorkoutExercise> newExercises = new ArrayList<>();

        for (WorkoutExercise we : baseWorkout.getExercises()) {
            WorkoutExercise progressed = new WorkoutExercise();
            progressed.setExerciseInfo(we.getExerciseInfo()); // <-- updated
            progressed.setOrder(we.getOrder());

            // Progressive scaling
            int newReps = we.getReps() + (dayNumber / 2);
            int newSets = we.getSets() + (dayNumber / 7);
            int newRest = Math.max(30, we.getRestSeconds() - (dayNumber / 5) * 5);

            // Adjust based on user profile
            if (userProfile.getAge() > 50) {
                newRest += 15; // older users get more rest
            }

            // Example: Adjust intensity based on fitness goal
            String goal = userProfile.getFitnessGoal().toLowerCase();
            switch (goal) {
                case "lose weight":
                    newReps += 2;   // slightly higher reps
                    newRest = Math.min(newRest, 50); // shorter rest
                    break;
                case "gain muscle":
                    newSets += 1;   // slightly more sets
                    newRest = Math.max(newRest, 90); // longer rest
                    break;
                case "increase endurance":
                    newReps += 5;   // more reps
                    newRest = Math.min(newRest, 40);
                    break;
            }

            progressed.setReps(newReps);
            progressed.setSets(newSets);
            progressed.setRestSeconds(newRest);

            newExercises.add(progressed);
        }

        int newDuration = baseWorkout.getDuration() + (dayNumber * 2);

        return new Workout(newExercises, newDuration);
    }
}
