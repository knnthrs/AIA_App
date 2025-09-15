package com.example.signuploginrealtime.logic;

import com.example.signuploginrealtime.models.UserProfile;
import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.models.ExerciseInfo;

import java.util.ArrayList;
import java.util.List;

public class WorkoutDecisionMaker {

    // Updated to accept List<ExerciseInfo>
    public static Workout generateProgressiveWorkout(
            List<ExerciseInfo> exercises,
            UserProfile userProfile
    ) {
        Workout workout = new Workout();
        List<WorkoutExercise> workoutExercises = new ArrayList<>();

        int order = 1;
        int week = userProfile.getCurrentWeek();
        String level = userProfile.getFitnessLevel();

        for (ExerciseInfo e : exercises) {
            WorkoutExercise we = new WorkoutExercise();
            we.setExerciseInfo(e);
            we.setOrder(order++);

            int sets = 2;
            int reps = 8;
            int rest = 30; // default baseline

            switch (level) {
                case "Sedentary":
                    sets = (week >= 3) ? 3 : 2;
                    reps = Math.min(12, 8 + (week - 1) * 2);
                    rest = Math.max(15, 30 - (week - 1) * 5);
                    break;

                case "Lightly Active":
                    sets = (week >= 3) ? 4 : 3;
                    reps = Math.min(14, 10 + (week - 1) * 2);
                    rest = Math.max(10, 25 - (week - 1) * 5);
                    break;

                case "Moderately Active":
                    sets = (week >= 3) ? 4 : 3;
                    reps = Math.min(16, 12 + (week - 1) * 2);
                    rest = Math.max(5, 20 - (week - 1) * 5);
                    break;

                case "Very Active":
                    sets = (week >= 3) ? 5 : 4;
                    reps = Math.min(18, 14 + (week - 1) * 2);
                    rest = Math.max(5, 15 - (week - 1) * 3);
                    break;
            }

            we.setSets(sets);
            we.setReps(reps);
            we.setRestSeconds(rest);

            workoutExercises.add(we);
        }

        workout.setExercises(workoutExercises);
        return workout;
    }
}
