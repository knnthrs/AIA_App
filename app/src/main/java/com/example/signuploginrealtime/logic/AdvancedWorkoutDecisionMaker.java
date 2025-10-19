package com.example.signuploginrealtime.logic;

import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.models.ExerciseInfo;
import com.example.signuploginrealtime.models.UserProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdvancedWorkoutDecisionMaker {

    private static List<ExerciseInfo> filterExercisesByFitnessLevel(
            List<ExerciseInfo> exercises,
            UserProfile userProfile) {

        List<ExerciseInfo> suitable = new ArrayList<>();
        String level = userProfile.getFitnessLevel().toLowerCase();

        for (ExerciseInfo exercise : exercises) {
            if (exercise == null || exercise.getName() == null) continue;

            String nameLower = exercise.getName().toLowerCase();
            List<String> equipments = exercise.getEquipments();

            boolean isSafe = true;

            // SEDENTARY: Very restrictive
            if (level.equals("sedentary")) {
                // Block all plyometric/explosive movements
                if (nameLower.contains("jump") || nameLower.contains("hop") ||
                        nameLower.contains("burpee") || nameLower.contains("plyometric") ||
                        nameLower.contains("explosive") || nameLower.contains("box") ||
                        nameLower.contains("sprint") || nameLower.contains("mountain climber") ||
                        nameLower.contains("high knee") || nameLower.contains("tuck")) {
                    isSafe = false;
                }

                // Block advanced bodyweight
                if (nameLower.contains("pull up") || nameLower.contains("chin up") ||
                        nameLower.contains("muscle up") || nameLower.contains("handstand") ||
                        nameLower.contains("pistol") || nameLower.contains("dragon flag")) {
                    isSafe = false;
                }

                // Block barbell exercises
                if (equipments != null && equipments.contains("barbell")) {
                    isSafe = false;
                }

                // Block loaded squats/lunges
                if ((nameLower.contains("squat") || nameLower.contains("lunge")) &&
                        equipments != null && !equipments.contains("body weight")) {
                    isSafe = false;
                }
            }

            // LIGHTLY ACTIVE: Moderate restrictions
            else if (level.equals("lightly active")) {
                if (nameLower.contains("burpee") || nameLower.contains("box jump") ||
                        nameLower.contains("plyometric") || nameLower.contains("explosive") ||
                        nameLower.contains("muscle up") || nameLower.contains("handstand") ||
                        nameLower.contains("pistol squat")) {
                    isSafe = false;
                }

                // Block olympic lifts
                if (nameLower.contains("clean") || nameLower.contains("snatch") ||
                        nameLower.contains("jerk")) {
                    isSafe = false;
                }
            }

            // HEALTH ISSUE FILTERING
            if (userProfile.getHealthIssues() != null) {
                for (String issue : userProfile.getHealthIssues()) {
                    issue = issue.toLowerCase();

                    if (issue.contains("knee")) {
                        if (nameLower.contains("squat") || nameLower.contains("lunge") ||
                                nameLower.contains("jump") || nameLower.contains("leg press")) {
                            isSafe = false;
                        }
                    }

                    if (issue.contains("back") || issue.contains("spine")) {
                        if (nameLower.contains("deadlift") || nameLower.contains("good morning") ||
                                nameLower.contains("bent over") || nameLower.contains("hyperextension")) {
                            isSafe = false;
                        }
                    }

                    if (issue.contains("shoulder")) {
                        if (nameLower.contains("overhead press") || nameLower.contains("snatch") ||
                                nameLower.contains("handstand")) {
                            isSafe = false;
                        }
                    }

                    if (issue.contains("wrist")) {
                        if (nameLower.contains("push up") || nameLower.contains("plank") ||
                                nameLower.contains("handstand")) {
                            isSafe = false;
                        }
                    }
                }
            }

            if (isSafe) {
                suitable.add(exercise);
            }
        }

        return suitable.isEmpty() ? exercises : suitable;
    }

    public static Workout generatePersonalizedWorkout(List<ExerciseInfo> availableExercises,
                                                      UserProfile userProfile) {

        // Filter exercises first based on fitness level
        List<ExerciseInfo> suitableExercises = filterExercisesByFitnessLevel(
                availableExercises,
                userProfile
        );

// Prioritize equipment for sedentary users
        if (userProfile.getFitnessLevel().toLowerCase().equals("sedentary") &&
                suitableExercises.size() > 10) {

            List<ExerciseInfo> prioritized = new ArrayList<>();

            // Priority 1: Bodyweight
            for (ExerciseInfo e : suitableExercises) {
                if (e.getEquipments() != null &&
                        e.getEquipments().contains("body weight")) {
                    prioritized.add(e);
                }
            }

            // Priority 2: Bands
            for (ExerciseInfo e : suitableExercises) {
                if (e.getEquipments() != null &&
                        e.getEquipments().contains("band") &&
                        !prioritized.contains(e)) {
                    prioritized.add(e);
                }
            }

            // Priority 3: Cables
            for (ExerciseInfo e : suitableExercises) {
                if (e.getEquipments() != null &&
                        e.getEquipments().contains("cable") &&
                        !prioritized.contains(e)) {
                    prioritized.add(e);
                }
            }

            // Priority 4: Dumbbells
            for (ExerciseInfo e : suitableExercises) {
                if (e.getEquipments() != null &&
                        e.getEquipments().contains("dumbbell") &&
                        !prioritized.contains(e)) {
                    prioritized.add(e);
                }
            }

            // Add remaining exercises
            for (ExerciseInfo e : suitableExercises) {
                if (!prioritized.contains(e)) {
                    prioritized.add(e);
                }
            }

            suitableExercises = prioritized;
        }

        List<WorkoutExercise> exercises = new ArrayList<>();
        Collections.shuffle(suitableExercises);

        for (ExerciseInfo exInfo : suitableExercises) {
            if (exInfo == null || exInfo.getName() == null) continue;

            // Skip disliked exercises
            if (userProfile.getDislikedExercises() != null &&
                    userProfile.getDislikedExercises().contains(exInfo.getName())) {
                continue;
            }

            WorkoutExercise we = new WorkoutExercise();
            we.setExerciseInfo(exInfo);
            we.setOrder(exercises.size() + 1);

            // ====================
            // STEP 1: Set base values from FITNESS LEVEL and GOAL combination
            // ====================
            int sets, reps, rest;
            String level = userProfile.getFitnessLevel().toLowerCase();
            String goal = userProfile.getFitnessGoal().toLowerCase();

            // Base on fitness level first
            switch (level) {
                case "sedentary":
                    sets = 2; reps = 10; rest = 90; break;
                case "lightly active":
                    sets = 3; reps = 10; rest = 75; break;
                case "moderately active":
                    sets = 3; reps = 12; rest = 60; break;
                case "very active":
                    sets = 4; reps = 12; rest = 45; break;
                default:
                    sets = 2; reps = 10; rest = 75; break;
            }

            // Adjust for goals (modify reps and rest, not sets)
            switch (goal) {
                case "lose weight":
                case "weight loss":
                    // Higher reps, lower rest for fat burning
                    reps = Math.min(reps + 3, 15); // Cap at 15 reps
                    rest = Math.max(rest - 15, 30);
                    break;
                case "gain muscle":
                case "muscle gain":
                    // Moderate reps for hypertrophy, longer rest
                    reps = Math.max(8, Math.min(reps, 12)); // Keep in 8-12 range
                    rest = Math.min(rest + 15, 120);
                    break;
                case "increase endurance":
                case "endurance":
                    // Higher reps, shorter rest
                    reps = Math.min(reps + 5, 20); // Cap at 20 reps
                    rest = Math.max(rest - 15, 30);
                    break;
                case "general fitness":
                default:
                    // Keep base values
                    break;
            }

            // ====================
            // STEP 2: Adjust for WORKOUT FREQUENCY (volume distribution)
            // ====================
            int frequency = userProfile.getWorkoutDaysPerWeek();
            if (frequency <= 2) {
                // Low frequency = slightly higher volume per session
                sets = Math.min(sets + 1, 4); // Cap at 4 sets
            } else if (frequency >= 5) {
                // High frequency = lower volume per session
                sets = Math.max(sets - 1, 2); // Minimum 2 sets
                rest += 10;
            }
            // 3-4 days = no adjustment (optimal frequency)

            // ====================
            // STEP 3: Age adjustments (for safety and recovery)
            // ====================
            int age = userProfile.getAge();
            if (age < 18) {
                // Younger = focus on form, moderate volume
                rest += 10;
            } else if (age >= 40 && age <= 60) {
                // Middle age = need more recovery
                rest += 15;
            } else if (age > 60) {
                // Older adults = lower volume, much more rest
                sets = Math.max(2, sets - 1);
                reps = Math.max(8, reps - 2);
                rest += 30;
            }

            // ====================
            // STEP 4: Health issues (SAFETY REDUCTIONS)
            // ====================
            if (userProfile.getHealthIssues() != null) {
                for (String issue : userProfile.getHealthIssues()) {
                    issue = issue.toLowerCase();
                    if (issue.contains("joint") || issue.contains("back")) {
                        // Reduce volume for joint/back issues
                        sets = Math.max(2, sets - 1);
                        reps = Math.max(8, reps - 2);
                        rest += 15;
                    }
                    if (issue.contains("heart") || issue.contains("blood pressure")) {
                        // Cardiovascular concerns = lower intensity
                        reps = Math.max(8, reps - 2);
                        rest += 30;
                    }
                    if (issue.contains("respiratory")) {
                        // Breathing issues = longer rest
                        rest += 30;
                    }
                }
            }

            // ====================
            // STEP 5: Final safety bounds
            // ====================
            we.setSets(Math.max(2, Math.min(sets, 5)));      // 2-5 sets
            we.setReps(Math.max(6, Math.min(reps, 20)));     // 6-20 reps
            we.setRestSeconds(Math.max(30, Math.min(rest, 120))); // 30-120 seconds

            exercises.add(we);

            if (exercises.size() >= 6) break;
        }

        return new Workout(exercises, exercises.size() * 5);
    }
}