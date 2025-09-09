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

            int newReps = baseReps + (dayNumber / 2);
            int newSets = baseSets + (dayNumber / 7);
            int newRest = Math.max(20, baseRest - (dayNumber / 5) * 5);

            String exerciseNameForAdjustment = "unknown"; // Default
            if (copyInfo.getName() != null) { // Use name from copyInfo which should be set
                exerciseNameForAdjustment = copyInfo.getName().toLowerCase();
            }

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
                case "beginner":
                    newSets = Math.max(2, newSets - 1);
                    newReps = Math.max(6, newReps - 2);
                    newRest += 10; break;
                case "intermediate":
                    newRest -= 5; break;
                case "advanced":
                    newSets += 1; newReps += 2; newRest -= 10; break;
            }

            // Health issues
            if (userProfile.getHealthIssues() != null) {
                for (String issue : userProfile.getHealthIssues()) {
                    issue = issue.toLowerCase();
                    if (issue.contains("joint") || issue.contains("back")) {
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

            newReps += (int) (Math.random() * 3) - 1;
            newSets += (int) (Math.random() * 2) - 1;
            newRest += ((int) (Math.random() * 11)) - 5;

            progressed.setReps(Math.max(1, newReps));
            progressed.setSets(Math.max(1, newSets));
            progressed.setRestSeconds(Math.max(15, newRest));

            newExercises.add(progressed);
        }

        Workout progressedWorkout = new Workout();
        progressedWorkout.setExercises(newExercises);

        return progressedWorkout;
    }
}
