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

            // Get previous sets/reps/rest
            int baseSets = we.getSets();
            int baseReps = we.getReps();
            int baseRest = we.getRestSeconds();

            // 1️⃣ Progressive scaling per day
            int newReps = baseReps + (dayNumber / 2);
            int newSets = baseSets + (dayNumber / 7);
            int newRest = Math.max(20, baseRest - (dayNumber / 5) * 5);

            // 2️⃣ Exercise type adjustment
            String name = originalInfo.getName().toLowerCase();
            if (name.contains("squat") || name.contains("deadlift") || name.contains("bench")) {
                newReps = Math.max(5, newReps - 1);
                newRest += 10;
            } else if (name.contains("curl") || name.contains("raise") || name.contains("fly")) {
                newReps += 1;
            }

            // -------------------- PERSONALIZATION --------------------
            // 3️⃣ Age
            int age = userProfile.getAge();
            if (age < 18) { newReps += 2; newRest += 5; }
            else if (age >= 40 && age <= 60) { newSets = Math.max(2, newSets - 1); newRest += 15; }
            else if (age > 60) { newSets = Math.max(2, newSets - 2); newReps = Math.max(5, newReps - 3); newRest += 30; }

            // 4️⃣ Gender
            String gender = userProfile.getGender().toLowerCase();
            if (gender.equals("female")) { newReps += 1; newRest = Math.max(30, newRest - 5); }
            else if (gender.equals("male")) { newSets += 1; }

            // 5️⃣ Height & Weight
            double height = userProfile.getHeight();
            double weight = userProfile.getWeight();
            if (height > 0 && weight > 0) {
                if (weight < 40) { newReps += 2; newSets = Math.max(1, newSets - 1); }
                if (height > 180) { newSets += 1; }
            }

            // 6️⃣ Fitness goal
            String goal = userProfile.getFitnessGoal().toLowerCase();
            switch (goal) {
                case "lose weight":
                case "weight loss": newReps += 3; newRest = Math.max(30, newRest - 10); break;
                case "gain muscle":
                case "muscle gain": newSets += 1; newReps = Math.max(6, newReps - 2); newRest += 15; break;
                case "increase endurance":
                case "endurance": newReps += 5; newRest = Math.max(20, newRest - 15); break;
            }

            // 7️⃣ Fitness level
            String level = userProfile.getFitnessLevel().toLowerCase();
            switch (level) {
                case "beginner": newSets = Math.max(2, newSets - 1); newReps = Math.max(6, newReps - 2); newRest += 10; break;
                case "intermediate": newRest -= 5; break;
                case "advanced": newSets += 1; newReps += 2; newRest -= 10; break;
            }

            // 8️⃣ Health issues
            if (userProfile.getHealthIssues() != null) {
                for (String issue : userProfile.getHealthIssues()) {
                    issue = issue.toLowerCase();
                    if (issue.contains("joint") || issue.contains("back")) { newReps = Math.max(8, newReps - 2); newSets = Math.max(2, newSets - 1); }
                    if (issue.contains("heart") || issue.contains("blood pressure")) { newRest += 20; newReps = Math.max(8, newReps - 2); }
                    if (issue.contains("respiratory")) { newRest += 30; }
                }
            }

            // Small random variation
            newReps += (int) (Math.random() * 3) - 1;
            newSets += (int) (Math.random() * 2) - 1;
            newRest += ((int) (Math.random() * 11)) - 5;

            progressed.setReps(Math.max(1, newReps));
            progressed.setSets(Math.max(1, newSets));
            progressed.setRestSeconds(Math.max(15, newRest));

            newExercises.add(progressed);
        }

        // ✅ Return workout; totalDuration auto-calculated
        Workout progressedWorkout = new Workout();
        progressedWorkout.setExercises(newExercises);

        return progressedWorkout;
    }
}
