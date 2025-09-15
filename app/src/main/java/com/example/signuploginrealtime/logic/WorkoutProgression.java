package com.example.signuploginrealtime.logic;

import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.models.ExerciseInfo;
import com.example.signuploginrealtime.models.UserProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkoutProgression {

    public static Workout generateProgressiveWorkout(Workout baseWorkout, int dayNumber, UserProfile userProfile) {
        List<WorkoutExercise> newExercises = new ArrayList<>();

        for (WorkoutExercise we : baseWorkout.getExercises()) {
            WorkoutExercise progressed = new WorkoutExercise();
            ExerciseInfo originalInfo = we.getExerciseInfo();
            ExerciseInfo copyInfo = new ExerciseInfo();

            if (originalInfo != null) {
                copyInfo.setExerciseId(originalInfo.getExerciseId());
                copyInfo.setName(originalInfo.getName());
                copyInfo.setGifUrl(originalInfo.getGifUrl());

                // Copy instructions as a list
                if (originalInfo.getInstructions() != null) {
                    copyInfo.setInstructions(new ArrayList<>(originalInfo.getInstructions()));
                } else {
                    copyInfo.setInstructions(new ArrayList<>());
                }

                // Copy lists
                copyInfo.setBodyParts(originalInfo.getBodyParts() != null ? new ArrayList<>(originalInfo.getBodyParts()) : new ArrayList<>());
                copyInfo.setEquipments(originalInfo.getEquipments() != null ? new ArrayList<>(originalInfo.getEquipments()) : new ArrayList<>());
                copyInfo.setTargetMuscles(originalInfo.getTargetMuscles() != null ? new ArrayList<>(originalInfo.getTargetMuscles()) : new ArrayList<>());
                copyInfo.setSecondaryMuscles(originalInfo.getSecondaryMuscles() != null ? new ArrayList<>(originalInfo.getSecondaryMuscles()) : new ArrayList<>());
            } else {
                // Defaults when original is null
                copyInfo.setExerciseId("default_id");
                copyInfo.setName("Default Exercise");
                copyInfo.setGifUrl("");
                copyInfo.setInstructions(new ArrayList<>()); // empty list instead of HashMap
                copyInfo.setBodyParts(new ArrayList<>());
                copyInfo.setEquipments(new ArrayList<>());
                copyInfo.setTargetMuscles(new ArrayList<>());
                copyInfo.setSecondaryMuscles(new ArrayList<>());
            }


            progressed.setExerciseInfo(copyInfo);
            progressed.setOrder(we.getOrder());

            int baseSets = we.getSets();
            int baseReps = we.getReps();
            int baseRest = we.getRestSeconds();

            // 🔹 Week-based progression
            int week = userProfile.getCurrentWeek();

            // ✅ 1️⃣ ADD: workout frequency factor
            int workoutFrequency = userProfile.getWorkoutDaysPerWeek(); // 1–7 days
            double frequencyFactor = 1.0 + (workoutFrequency - 3) * 0.1; // 3 days/week = factor 1

            int maxExtraSets = 3;   // cap progression to +3 sets
            int maxExtraReps = 10;  // cap progression to +10 reps

            int newReps = baseReps + Math.min(maxExtraReps, week);
            int newSets = baseSets + Math.min(maxExtraSets, week / 2);
            int newRest = Math.max(20, baseRest - (week * 2)); // reduce rest slightly each week


            // 🔹 Exercise-specific tweaks
            String exerciseNameForAdjustment = copyInfo.getName() != null ? copyInfo.getName().toLowerCase() : "unknown";
            if (exerciseNameForAdjustment.contains("squat") || exerciseNameForAdjustment.contains("deadlift") || exerciseNameForAdjustment.contains("bench")) {
                newReps = Math.max(5, newReps - 1);
                newRest += 10;
            } else if (exerciseNameForAdjustment.contains("curl") || exerciseNameForAdjustment.contains("raise") || exerciseNameForAdjustment.contains("fly")) {
                newReps += 1;
            }

            // Age
            int age = userProfile.getAge();
            if (age < 18) { newReps += 2; newRest += 5; }
            else if (age >= 40 && age <= 60) { newSets = Math.max(2, newSets - 1); newRest += 15; }
            else if (age > 60) { newSets = Math.max(2, newSets - 2); newReps = Math.max(5, newReps - 3); newRest += 30; }

            // Gender
            String gender = userProfile.getGender().toLowerCase();
            if (gender.equals("female")) { newReps += 1; newRest = Math.max(30, newRest - 5); }
            else if (gender.equals("male")) { newSets += 1; }

            // Height & Weight
            double height = userProfile.getHeight();
            double weight = userProfile.getWeight();
            if (height > 0 && weight > 0) {
                if (weight < 40) { newReps += 2; newSets = Math.max(1, newSets - 1); }
                if (height > 180) { newSets += 1; }
            }

            // Fitness goal
            String goal = userProfile.getFitnessGoal().toLowerCase();
            switch (goal) {
                case "lose weight":
                case "weight loss":
                    newReps += 3; newRest = Math.max(30, newRest - 10); break;
                case "gain muscle":
                case "muscle gain":
                    newSets += 1; newReps = Math.max(6, newReps - 2); newRest += 15; break;
                case "increase endurance":
                case "endurance":
                    newReps += 5; newRest = Math.max(20, newRest - 15); break;
            }

            // Fitness level
            String level = userProfile.getFitnessLevel().toLowerCase();
            switch (level) {
                case "sedentary":
                    newSets = 2;
                    newReps = 8;
                    newRest = 90;
                    break;
                case "lightly active":
                    newSets = 3;
                    newReps = 10;
                    newRest = 75;
                    break;
                case "moderately active":
                    newSets = 4;
                    newReps = 12;
                    newRest = 60;
                    break;
                case "very active":
                    newSets = 5;
                    newReps = 15;
                    newRest = 45;
                    break;
            }

            // ✅ 2️⃣ Health issues loop (add knee check)
            if (userProfile.getHealthIssues() != null) {
                for (String issue : userProfile.getHealthIssues()) {
                    issue = issue.toLowerCase();
                    if (issue.contains("joint") || issue.contains("back") || issue.contains("knee")) {
                        newReps = Math.max(8, newReps - 2);
                        newSets = Math.max(2, newSets - 1);
                    }
                    if (issue.contains("heart") || issue.contains("blood pressure")) {
                        newRest += 20;
                        newReps = Math.max(8, newReps - 2);
                    }
                    if (issue.contains("respiratory")) { newRest += 30; }
                }
            }

            // ✅ 3️⃣ Optional: handle otherHealthIssue
            String other = userProfile.getOtherHealthIssue();
            if (other != null && !other.isEmpty()) {
                String o = other.toLowerCase();
                if (o.contains("shoulder") || o.contains("knee")) {
                    newReps = Math.max(8, newReps - 1);
                    newRest += 10;
                }
            }

            // ✅ 1️⃣ APPLY frequency scaling here (after all other adjustments, before randomness)
            newReps = (int)(newReps * frequencyFactor);
            newSets = (int)(newSets * frequencyFactor);
            newRest = (int)(newRest / frequencyFactor);

            // Random adjustments
            newReps += (int) (Math.random() * 3) - 1;
            newSets += (int) (Math.random() * 2) - 1;
            newRest += ((int) (Math.random() * 11)) - 5;

            // ✅ 4️⃣ Optional: cap sets/reps/rest
            newReps = Math.min(newReps, 20);
            newSets = Math.min(newSets, 6);
            newRest = Math.min(newRest, 180);

            progressed.setReps(Math.max(1, newReps));
            progressed.setSets(Math.max(1, newSets));
            progressed.setRestSeconds(Math.max(15, newRest));

            newExercises.add(progressed);
        }


        // Shuffle exercises to vary order each day
        Collections.shuffle(newExercises);

        Workout progressedWorkout = new Workout();
        progressedWorkout.setExercises(newExercises);

        return progressedWorkout;
    }
}
