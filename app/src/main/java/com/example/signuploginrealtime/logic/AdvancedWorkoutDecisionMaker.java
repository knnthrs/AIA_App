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

    private static List<ExerciseInfo> prioritizeByBodyFocus(
            List<ExerciseInfo> exercises,
            UserProfile userProfile) {

        List<String> bodyFocus = userProfile.getBodyFocus();

        // If no body focus is set, just shuffle and return all exercises
        if (bodyFocus == null || bodyFocus.isEmpty()) {
            Collections.shuffle(exercises);
            return exercises;
        }

        List<ExerciseInfo> prioritized = new ArrayList<>();
        List<ExerciseInfo> others = new ArrayList<>();

        // Log for debugging
        System.out.println("🎯 Body Focus Filter - Selected: " + bodyFocus);
        System.out.println("🎯 Total exercises to filter: " + exercises.size());

        // Separate exercises based on body focus
        for (ExerciseInfo exercise : exercises) {
            if (exercise == null || exercise.getName() == null) continue;

            String nameLower = exercise.getName().toLowerCase();
            List<String> targets = exercise.getTargetMuscles();
            boolean matchesBodyFocus = false;

            // Check if exercise targets any of the focused body parts
            for (String focus : bodyFocus) {
                String focusLower = focus.toLowerCase();

                // ✅ SPECIAL: Check exercise name for common patterns FIRST
                // This helps when targetMuscles are missing or wrong
                if (focusLower.equals("arms") || focusLower.equals("arm")) {
                    // Common arm exercise names
                    if (nameLower.contains("curl") || nameLower.contains("extension") ||
                        nameLower.contains("tricep") || nameLower.contains("bicep") ||
                        nameLower.contains("preacher") || nameLower.contains("hammer") ||
                        nameLower.contains("concentration") || nameLower.contains("overhead extension") ||
                        nameLower.contains("skull crusher") || nameLower.contains("dip") ||
                        nameLower.contains("close grip")) {
                        matchesBodyFocus = true;
                        break;
                    }
                }

                if (focusLower.equals("legs") || focusLower.equals("leg")) {
                    // Common leg exercise names
                    if (nameLower.contains("squat") || nameLower.contains("lunge") ||
                        nameLower.contains("leg press") || nameLower.contains("leg curl") ||
                        nameLower.contains("leg extension") || nameLower.contains("calf raise") ||
                        nameLower.contains("romanian deadlift") || nameLower.contains("step up")) {
                        matchesBodyFocus = true;
                        break;
                    }
                }

                if (focusLower.equals("abs") || focusLower.equals("core") || focusLower.equals("ab")) {
                    // Common abs exercise names
                    if (nameLower.contains("crunch") || nameLower.contains("sit up") ||
                        nameLower.contains("plank") || nameLower.contains("leg raise") ||
                        nameLower.contains("russian twist") || nameLower.contains("bicycle") ||
                        nameLower.contains("mountain climber") || nameLower.contains("v-up")) {
                        matchesBodyFocus = true;
                        break;
                    }
                }

                if (focusLower.equals("chest") || focusLower.equals("pecs")) {
                    // Common chest exercise names
                    if (nameLower.contains("bench press") || nameLower.contains("push up") ||
                        nameLower.contains("chest fly") || nameLower.contains("cable crossover") ||
                        nameLower.contains("dumbbell press") || nameLower.contains("incline") ||
                        nameLower.contains("decline")) {
                        matchesBodyFocus = true;
                        break;
                    }
                }

                if (focusLower.equals("back")) {
                    // Common back exercise names
                    if (nameLower.contains("row") || nameLower.contains("pull up") ||
                        nameLower.contains("lat pulldown") || nameLower.contains("deadlift") ||
                        nameLower.contains("pull down") || nameLower.contains("face pull")) {
                        matchesBodyFocus = true;
                        break;
                    }
                }

                if (focusLower.equals("shoulders") || focusLower.equals("shoulder")) {
                    // Common shoulder exercise names
                    if (nameLower.contains("shoulder press") || nameLower.contains("lateral raise") ||
                        nameLower.contains("front raise") || nameLower.contains("overhead press") ||
                        nameLower.contains("arnold press") || nameLower.contains("military press")) {
                        matchesBodyFocus = true;
                        break;
                    }
                }

                // Check exercise name directly for focus word
                if (nameLower.contains(focusLower)) {
                    matchesBodyFocus = true;
                    break;
                }

                // Check target muscles with VERY LENIENT matching
                if (targets != null && !targets.isEmpty()) {
                    for (String target : targets) {
                        String targetLower = target.toLowerCase();

                        // ✅ CHEST - match any chest variation
                        if ((focusLower.equals("chest") || focusLower.equals("pecs")) &&
                            (targetLower.contains("chest") || targetLower.contains("pec"))) {
                            matchesBodyFocus = true;
                            break;
                        }

                        // ✅ BACK - match any back variation
                        else if (focusLower.equals("back") &&
                            (targetLower.contains("back") || targetLower.contains("lat") ||
                             targetLower.contains("spine") || targetLower.contains("rhomboid") ||
                             targetLower.contains("trapezius") || targetLower.contains("trap"))) {
                            matchesBodyFocus = true;
                            break;
                        }

                        // ✅ SHOULDERS - match any shoulder variation
                        else if ((focusLower.equals("shoulders") || focusLower.equals("shoulder")) &&
                            (targetLower.contains("shoulder") || targetLower.contains("deltoid") ||
                             targetLower.contains("delt"))) {
                            matchesBodyFocus = true;
                            break;
                        }

                        // ✅ ARMS - match any arm variation
                        else if ((focusLower.equals("arms") || focusLower.equals("arm")) &&
                            (targetLower.contains("bicep") || targetLower.contains("tricep") ||
                             targetLower.contains("forearm") || targetLower.contains("arm"))) {
                            matchesBodyFocus = true;
                            break;
                        }

                        // ✅ LEGS - match ANY leg-related term
                        else if ((focusLower.equals("legs") || focusLower.equals("leg")) &&
                            (targetLower.contains("quad") || targetLower.contains("hamstring") ||
                             targetLower.contains("calf") || targetLower.contains("calves") ||
                             targetLower.contains("leg") || targetLower.contains("glute") ||
                             targetLower.contains("thigh") || targetLower.contains("lower body"))) {
                            matchesBodyFocus = true;
                            break;
                        }

                        // ✅ ABS - match ANY abs-related term
                        else if ((focusLower.equals("abs") || focusLower.equals("core") || focusLower.equals("ab")) &&
                            (targetLower.contains("ab") || targetLower.contains("core") ||
                             targetLower.contains("oblique") || targetLower.contains("stomach") ||
                             targetLower.contains("waist"))) {
                            matchesBodyFocus = true;
                            break;
                        }
                    }
                }

                if (matchesBodyFocus) break;
            }

            if (matchesBodyFocus) {
                prioritized.add(exercise);
                System.out.println("  ✅ MATCH: " + exercise.getName() + " | Targets: " + targets);
            } else {
                others.add(exercise);
            }
        }

        System.out.println("🎯 Matched exercises: " + prioritized.size());
        System.out.println("🎯 Skipped exercises: " + others.size());

        // Shuffle focused exercises
        Collections.shuffle(prioritized);

        // ✅ STRICT MODE: only body-focus-matching exercises
        List<ExerciseInfo> result = new ArrayList<>();
        for (int i = 0; i < Math.min(6, prioritized.size()); i++) {
            result.add(prioritized.get(i));
        }

        // ⚠️ FALLBACK: If body focus found 0 exercises, return a few general exercises
        // so the user gets SOMETHING instead of a complete failure
        if (result.isEmpty() && !others.isEmpty()) {
            System.out.println("⚠️ WARNING: No exercises matched body focus! Using general exercises as fallback.");
            Collections.shuffle(others);
            for (int i = 0; i < Math.min(3, others.size()); i++) {
                result.add(others.get(i));
            }
        }

        System.out.println("🎯 Final result: " + result.size() + " exercises for main workout");

        return result;
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

        // ✅ PRIORITIZE EXERCISES BASED ON BODY FOCUS
        List<ExerciseInfo> prioritizedExercises = prioritizeByBodyFocus(suitableExercises, userProfile);

        List<WorkoutExercise> exercises = new ArrayList<>();

        for (ExerciseInfo exInfo : prioritizedExercises) {
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


