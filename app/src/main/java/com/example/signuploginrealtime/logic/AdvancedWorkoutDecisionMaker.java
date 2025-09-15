package com.example.signuploginrealtime.logic;

import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.models.ExerciseInfo;
import com.example.signuploginrealtime.models.UserProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdvancedWorkoutDecisionMaker {

    public static Workout generatePersonalizedWorkout(List<ExerciseInfo> availableExercises,
                                                      UserProfile userProfile) {

        List<WorkoutExercise> exercises = new ArrayList<>();

        Collections.shuffle(availableExercises);


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

            // Base sets/reps/rest defaults
            int sets = 3, reps = 10, rest = 60;

            // -------------------- SEQUENCE START --------------------
            // 1️⃣ Age adjustment
            int age = userProfile.getAge();
            if (age < 18) { reps += 2; rest += 5; }
            else if (age >= 40 && age <= 60) { sets = Math.max(2, sets - 1); rest += 15; }
            else if (age > 60) { sets = Math.max(2, sets - 2); reps = Math.max(5, reps - 3); rest += 30; }

            // 2️⃣ Gender adjustment
            String gender = userProfile.getGender().toLowerCase();
            if (gender.equals("female")) { reps += 1; rest = Math.max(30, rest - 5); }
            else if (gender.equals("male")) { sets += 1; }

            // 3️⃣ Height & Weight adjustment
            float height = (float) userProfile.getHeight(); // cast double → float
            float weight = (float) userProfile.getWeight(); // cast double → float
            if (height > 0 && weight > 0) {
                if (weight < 40) { reps += 2; sets = Math.max(1, sets - 1); }
                if (height > 180) { sets += 1; }
            }

            // 4️⃣ Fitness goal adjustment
            String goal = userProfile.getFitnessGoal().toLowerCase();
            switch (goal) {
                case "lose weight":
                case "weight loss":
                    sets = Math.max(2, sets); reps += 3; rest = Math.max(30, rest - 10);
                    break;
                case "gain muscle":
                case "muscle gain":
                    sets += 1; reps = Math.max(6, reps - 2); rest += 15;
                    break;
                case "increase endurance":
                case "endurance":
                    reps += 5; rest = Math.max(20, rest - 15);
                    break;
            }

            // 5️⃣ Fitness level adjustment
            String level = userProfile.getFitnessLevel().toLowerCase();
            switch (level) {
                case "sedentary":
                    sets = 2; reps = 8; rest = 90;
                    break;
                case "lightly active":
                    sets = 3; reps = 10; rest = 75;
                    break;
                case "moderately active":
                    sets = 4; reps = 12; rest = 60;
                    break;
                case "very active":
                    sets = 5; reps = 15; rest = 45;
                    break;
            }


            // 6️⃣ Health issues adjustment
            if (userProfile.getHealthIssues() != null) {
                for (String issue : userProfile.getHealthIssues()) {
                    issue = issue.toLowerCase();
                    if (issue.contains("joint") || issue.contains("back")) {
                        reps = Math.max(8, reps - 2);
                        sets = Math.max(2, sets - 1);
                    }
                    if (issue.contains("heart") || issue.contains("blood pressure")) {
                        rest += 20;
                        reps = Math.max(8, reps - 2);
                    }
                    if (issue.contains("respiratory")) { rest += 30; }
                }
            }

            // 7️⃣ Progressive overload based on current week
            int week = userProfile.getCurrentWeek();
            int maxExtraSets = 3;  // cap progression to +3 sets max
            int maxExtraReps = 10; // cap progression to +10 reps max

            if (week > 1) {
                sets += Math.min(maxExtraSets, week / 2);
                reps += Math.min(maxExtraReps, week);
                rest = Math.max(20, rest - (week * 2));
            }



            // Tiny variation without reducing progression
            reps += (int) (Math.random() * 2);  // +0 or +1
            sets += (int) (Math.random() * 2);  // +0 or +1
            rest -= (int) (Math.random() * 3);  // -0 to -2 sec (never adds rest)


            // Set final values
            we.setSets(Math.max(1, sets));
            we.setReps(Math.max(1, reps));
            we.setRestSeconds(Math.max(15, rest));

            exercises.add(we);

            if (exercises.size() >= 6) break; // limit to 6 exercises
        }

        // Return workout with total duration (default: 5 min per exercise)
        return new Workout(exercises, exercises.size() * 5);
    }
}
