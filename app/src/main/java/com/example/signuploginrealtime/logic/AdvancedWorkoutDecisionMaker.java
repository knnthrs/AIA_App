package com.example.signuploginrealtime.logic;

import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.models.ExerciseInfo;
import com.example.signuploginrealtime.models.UserProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class AdvancedWorkoutDecisionMaker {

    private static final Random random = new Random();

    public static Workout generatePersonalizedWorkout(List<ExerciseInfo> availableExercises, 
                                                    UserProfile userProfile) {
        
        // Step 1: Filter exercises based on user constraints
        List<ExerciseInfo> filteredExercises = filterExercisesByUserConstraints(availableExercises, userProfile);
        
        // Step 2: Determine workout structure based on goals
        WorkoutBlueprint blueprint = createWorkoutBlueprint(userProfile);
        
        // Step 3: Select exercises strategically
        List<ExerciseInfo> selectedExercises = selectExercisesForWorkout(filteredExercises, blueprint, userProfile);
        
        // Step 4: Create personalized workout exercises
        List<WorkoutExercise> workoutExercises = createPersonalizedWorkoutExercises(selectedExercises, userProfile, blueprint);
        
        // Step 5: Calculate total workout duration
        int totalDuration = calculateWorkoutDuration(workoutExercises, userProfile);
        
        return new Workout(workoutExercises, totalDuration);
    }

    private static List<ExerciseInfo> filterExercisesByUserConstraints(List<ExerciseInfo> exercises, UserProfile profile) {
        List<ExerciseInfo> filtered = new ArrayList<>();
        
        for (ExerciseInfo exercise : exercises) {
            if (exercise == null || exercise.getName() == null) continue;
            
            String exerciseName = exercise.getName().toLowerCase();
            
            // Filter based on health issues
            if (shouldSkipForHealthReasons(exerciseName, profile.getHealthIssues())) continue;
            
            // Filter based on equipment availability
            if (!hasRequiredEquipment(exercise, profile)) continue;
            
            // Filter based on user dislikes
            if (isUserDislikedExercise(exerciseName, profile.getDislikedExercises())) continue;
            
            // Filter based on fitness level appropriateness
            if (!isAppropriateForFitnessLevel(exerciseName, profile.getFitnessLevel())) continue;
            
            filtered.add(exercise);
        }
        
        return filtered;
    }

    private static boolean shouldSkipForHealthReasons(String exerciseName, List<String> healthIssues) {
        if (healthIssues == null) return false;
        
        for (String issue : healthIssues) {
            switch (issue.toLowerCase()) {
                case "back problems":
                    if (exerciseName.contains("deadlift") || exerciseName.contains("bent over") || 
                        exerciseName.contains("good morning")) return true;
                    break;
                case "knee problems":
                    if (exerciseName.contains("squat") || exerciseName.contains("lunge") || 
                        exerciseName.contains("jump")) return true;
                    break;
                case "shoulder problems":
                    if (exerciseName.contains("overhead") || exerciseName.contains("shoulder press") ||
                        exerciseName.contains("lateral raise")) return true;
                    break;
                case "heart condition":
                    if (exerciseName.contains("burpee") || exerciseName.contains("sprint") ||
                        exerciseName.contains("high intensity")) return true;
                    break;
                case "joint problems":
                    if (exerciseName.contains("jump") || exerciseName.contains("plyometric")) return true;
                    break;
            }
        }
        return false;
    }

    private static boolean hasRequiredEquipment(ExerciseInfo exercise, UserProfile profile) {
        // If user has gym access, assume all equipment is available
        if (profile.isHasGymAccess()) return true;
        
        String exerciseName = exercise.getName().toLowerCase();
        List<String> availableEquipment = profile.getAvailableEquipment();
        
        // Check if exercise requires equipment user doesn't have
        if (exerciseName.contains("barbell") && !hasEquipment(availableEquipment, "barbell")) return false;
        if (exerciseName.contains("dumbbell") && !hasEquipment(availableEquipment, "dumbbell")) return false;
        if (exerciseName.contains("cable") && !hasEquipment(availableEquipment, "cable machine")) return false;
        if (exerciseName.contains("machine") && !hasEquipment(availableEquipment, "gym machines")) return false;
        
        return true;
    }

    private static boolean hasEquipment(List<String> available, String required) {
        return available != null && available.contains(required);
    }

    private static boolean isUserDislikedExercise(String exerciseName, List<String> disliked) {
        if (disliked == null) return false;
        return disliked.stream().anyMatch(dislike -> exerciseName.contains(dislike.toLowerCase()));
    }

    private static boolean isAppropriateForFitnessLevel(String exerciseName, String level) {
        if ("beginner".equalsIgnoreCase(level)) {
            // Skip complex/dangerous exercises for beginners
            return !exerciseName.contains("deadlift") && 
                   !exerciseName.contains("clean") && 
                   !exerciseName.contains("snatch") &&
                   !exerciseName.contains("olympic");
        }
        return true;
    }

    private static WorkoutBlueprint createWorkoutBlueprint(UserProfile profile) {
        WorkoutBlueprint blueprint = new WorkoutBlueprint();
        
        String goal = profile.getFitnessGoal().toLowerCase();
        String level = profile.getFitnessLevel().toLowerCase();
        
        switch (goal) {
            case "lose weight":
                blueprint.targetExerciseCount = level.equals("beginner") ? 5 : 7;
                blueprint.focusOnCardio = true;
                blueprint.higherReps = true;
                blueprint.shorterRest = true;
                blueprint.muscleGroupPriority = Arrays.asList("legs", "core", "chest", "back");
                blueprint.targetSets = level.equals("beginner") ? 2 : 3;
                blueprint.targetRepsMin = 12;
                blueprint.targetRepsMax = 20;
                blueprint.restTimeSeconds = profile.getRecommendedRestTime() - 15;
                break;
                
            case "gain muscle":
                blueprint.targetExerciseCount = level.equals("beginner") ? 4 : 6;
                blueprint.focusOnStrength = true;
                blueprint.moderateReps = true;
                blueprint.longerRest = true;
                blueprint.muscleGroupPriority = Arrays.asList("chest", "back", "legs", "shoulders", "arms");
                blueprint.targetSets = level.equals("beginner") ? 3 : 4;
                blueprint.targetRepsMin = 6;
                blueprint.targetRepsMax = 12;
                blueprint.restTimeSeconds = profile.getRecommendedRestTime() + 30;
                break;
                
            case "increase endurance":
                blueprint.targetExerciseCount = level.equals("beginner") ? 6 : 8;
                blueprint.focusOnCardio = true;
                blueprint.veryHighReps = true;
                blueprint.shortRest = true;
                blueprint.muscleGroupPriority = Arrays.asList("legs", "core", "chest", "back", "arms");
                blueprint.targetSets = 2;
                blueprint.targetRepsMin = 15;
                blueprint.targetRepsMax = 25;
                blueprint.restTimeSeconds = Math.max(30, profile.getRecommendedRestTime() - 20);
                break;
                
            case "general fitness":
            default:
                blueprint.targetExerciseCount = level.equals("beginner") ? 5 : 6;
                blueprint.balanced = true;
                blueprint.muscleGroupPriority = Arrays.asList("legs", "chest", "back", "core", "shoulders");
                blueprint.targetSets = 3;
                blueprint.targetRepsMin = 8;
                blueprint.targetRepsMax = 15;
                blueprint.restTimeSeconds = profile.getRecommendedRestTime();
                break;
        }
        
        // Adjust for age and BMI
        adjustBlueprintForAge(blueprint, profile.getAge());
        adjustBlueprintForBMI(blueprint, profile.calculateBMI());
        
        return blueprint;
    }

    private static void adjustBlueprintForAge(WorkoutBlueprint blueprint, int age) {
        if (age > 50) {
            blueprint.restTimeSeconds += 15;
            blueprint.targetRepsMax = Math.max(10, blueprint.targetRepsMax - 3);
            blueprint.targetExerciseCount = Math.max(3, blueprint.targetExerciseCount - 1);
        }
        if (age > 65) {
            blueprint.restTimeSeconds += 20;
            blueprint.targetSets = Math.max(2, blueprint.targetSets - 1);
        }
    }

    private static void adjustBlueprintForBMI(WorkoutBlueprint blueprint, double bmi) {
        if (bmi > 30) {
            // Focus more on cardio for obese users
            blueprint.focusOnCardio = true;
            blueprint.targetRepsMin += 3;
            blueprint.targetRepsMax += 5;
            blueprint.restTimeSeconds = Math.max(45, blueprint.restTimeSeconds - 10);
        }
    }

    private static List<ExerciseInfo> selectExercisesForWorkout(List<ExerciseInfo> filteredExercises, 
                                                              WorkoutBlueprint blueprint, 
                                                              UserProfile profile) {
        
        // Categorize exercises by muscle groups and types
        Map<String, List<ExerciseInfo>> exercisesByCategory = categorizeExercises(filteredExercises);
        
        List<ExerciseInfo> selectedExercises = new ArrayList<>();
        
        // Select exercises based on muscle group priority
        for (String muscleGroup : blueprint.muscleGroupPriority) {
            List<ExerciseInfo> categoryExercises = exercisesByCategory.get(muscleGroup);
            if (categoryExercises != null && !categoryExercises.isEmpty()) {
                ExerciseInfo selected = selectBestExerciseFromCategory(categoryExercises, profile);
                if (selected != null && !selectedExercises.contains(selected)) {
                    selectedExercises.add(selected);
                }
                
                if (selectedExercises.size() >= blueprint.targetExerciseCount) break;
            }
        }
        
        // Fill remaining slots with variety
        while (selectedExercises.size() < blueprint.targetExerciseCount) {
            ExerciseInfo exercise = selectRandomFromFiltered(filteredExercises, selectedExercises);
            if (exercise != null) {
                selectedExercises.add(exercise);
            } else {
                break; // No more exercises available
            }
        }
        
        return selectedExercises;
    }

    private static Map<String, List<ExerciseInfo>> categorizeExercises(List<ExerciseInfo> exercises) {
        Map<String, List<ExerciseInfo>> categories = new HashMap<>();
        
        // Initialize categories
        categories.put("chest", new ArrayList<>());
        categories.put("back", new ArrayList<>());
        categories.put("legs", new ArrayList<>());
        categories.put("shoulders", new ArrayList<>());
        categories.put("arms", new ArrayList<>());
        categories.put("core", new ArrayList<>());
        categories.put("cardio", new ArrayList<>());
        categories.put("full_body", new ArrayList<>());
        
        for (ExerciseInfo exercise : exercises) {
            String name = exercise.getName().toLowerCase();
            String description = exercise.getDescription() != null ? exercise.getDescription().toLowerCase() : "";
            
            // Categorize based on exercise names and descriptions
            if (containsAny(name, Arrays.asList("push", "chest", "bench", "fly", "dip"))) {
                categories.get("chest").add(exercise);
            } else if (containsAny(name, Arrays.asList("pull", "row", "lat", "back", "chin"))) {
                categories.get("back").add(exercise);
            } else if (containsAny(name, Arrays.asList("squat", "lunge", "leg", "calf", "quad", "hamstring"))) {
                categories.get("legs").add(exercise);
            } else if (containsAny(name, Arrays.asList("shoulder", "deltoid", "raise", "shrug"))) {
                categories.get("shoulders").add(exercise);
            } else if (containsAny(name, Arrays.asList("curl", "tricep", "bicep", "arm", "extension"))) {
                categories.get("arms").add(exercise);
            } else if (containsAny(name, Arrays.asList("plank", "crunch", "abs", "core", "sit"))) {
                categories.get("core").add(exercise);
            } else if (containsAny(name, Arrays.asList("run", "jump", "cardio", "burpee", "mountain", "jack"))) {
                categories.get("cardio").add(exercise);
            } else if (containsAny(name, Arrays.asList("burpee", "thruster", "clean", "deadlift"))) {
                categories.get("full_body").add(exercise);
            }
        }
        
        return categories;
    }

    private static boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private static ExerciseInfo selectBestExerciseFromCategory(List<ExerciseInfo> categoryExercises, UserProfile profile) {
        if (categoryExercises.isEmpty()) return null;
        
        // Score exercises based on user preferences
        ExerciseInfo bestExercise = null;
        int bestScore = -1;
        
        for (ExerciseInfo exercise : categoryExercises) {
            int score = calculateExerciseScore(exercise, profile);
            if (score > bestScore) {
                bestScore = score;
                bestExercise = exercise;
            }
        }
        
        return bestExercise;
    }

    private static int calculateExerciseScore(ExerciseInfo exercise, UserProfile profile) {
        int score = 0;
        String name = exercise.getName().toLowerCase();
        
        // Prefer compound movements for efficiency
        if (isCompoundMovement(name)) score += 3;
        
        // Prefer bodyweight exercises for beginners or no gym access
        if (!profile.isHasGymAccess() && isBodyweightExercise(name)) score += 2;
        
        // Prefer simpler exercises for beginners
        if (profile.isBeginner() && isBeginnerFriendly(name)) score += 2;
        
        // Prefer exercises matching user's preferred muscle groups
        if (matchesPreferredMuscleGroups(exercise, profile.getPreferredMuscleGroups())) score += 1;
        
        return score;
    }

    private static boolean isCompoundMovement(String name) {
        return containsAny(name, Arrays.asList("squat", "deadlift", "press", "row", "pull", "push", "lunge"));
    }

    private static boolean isBodyweightExercise(String name) {
        return containsAny(name, Arrays.asList("push up", "pull up", "sit up", "plank", "burpee", "jumping", "mountain climber"));
    }

    private static boolean isBeginnerFriendly(String name) {
        return containsAny(name, Arrays.asList("push up", "squat", "plank", "march", "step", "basic"));
    }

    private static boolean matchesPreferredMuscleGroups(ExerciseInfo exercise, List<String> preferred) {
        if (preferred == null || preferred.isEmpty()) return false;
        String name = exercise.getName().toLowerCase();
        return preferred.stream().anyMatch(group -> name.contains(group.toLowerCase()));
    }

    private static ExerciseInfo selectRandomFromFiltered(List<ExerciseInfo> filtered, List<ExerciseInfo> alreadySelected) {
        List<ExerciseInfo> remaining = new ArrayList<>(filtered);
        remaining.removeAll(alreadySelected);
        
        if (remaining.isEmpty()) return null;
        return remaining.get(random.nextInt(remaining.size()));
    }

    private static List<WorkoutExercise> createPersonalizedWorkoutExercises(List<ExerciseInfo> exercises, 
                                                                          UserProfile profile, 
                                                                          WorkoutBlueprint blueprint) {
        List<WorkoutExercise> workoutExercises = new ArrayList<>();
        
        for (int i = 0; i < exercises.size(); i++) {
            ExerciseInfo exercise = exercises.get(i);
            WorkoutExercise we = new WorkoutExercise();
            
            we.setExerciseInfo(exercise);
            we.setOrder(i + 1);
            
            // Calculate personalized parameters
            PersonalizedParams params = calculatePersonalizedParameters(exercise, profile, blueprint);
            we.setSets(params.sets);
            we.setReps(params.reps);
            we.setRestSeconds(params.restSeconds);
            
            workoutExercises.add(we);
        }
        
        return workoutExercises;
    }

    private static PersonalizedParams calculatePersonalizedParameters(ExerciseInfo exercise, 
                                                                    UserProfile profile, 
                                                                    WorkoutBlueprint blueprint) {
        PersonalizedParams params = new PersonalizedParams();
        
        // Start with blueprint base values
        params.sets = blueprint.targetSets;
        params.reps = calculateRepsInRange(blueprint.targetRepsMin, blueprint.targetRepsMax, exercise, profile);
        params.restSeconds = blueprint.restTimeSeconds;
        
        // Fine-tune based on exercise type
        String exerciseName = exercise.getName().toLowerCase();
        
        if (isCompoundMovement(exerciseName)) {
            // Compound movements need more rest
            params.restSeconds += 15;
        }
        
        if (isCardioExercise(exerciseName)) {
            // Cardio exercises get shorter rest
            params.restSeconds = Math.max(30, params.restSeconds - 20);
            params.reps += 5; // More reps for cardio
        }
        
        if (isCoreExercise(exerciseName)) {
            // Core exercises often use time instead of reps
            params.reps = Math.min(params.reps, 15);
        }
        
        // Ensure reasonable ranges
        params.sets = Math.max(1, Math.min(5, params.sets));
        params.reps = Math.max(5, Math.min(30, params.reps));
        params.restSeconds = Math.max(30, Math.min(180, params.restSeconds));
        
        return params;
    }

    private static int calculateRepsInRange(int min, int max, ExerciseInfo exercise, UserProfile profile) {
        int baseReps = (min + max) / 2;
        
        // Adjust based on exercise difficulty
        String name = exercise.getName().toLowerCase();
        if (containsAny(name, Arrays.asList("push up", "pull up", "dip"))) {
            baseReps = Math.max(min, baseReps - 3); // Bodyweight exercises are harder
        }
        
        // Random variation within range
        int variation = (max - min) / 3;
        return Math.max(min, Math.min(max, baseReps + random.nextInt(variation * 2) - variation));
    }

    private static boolean isCardioExercise(String name) {
        return containsAny(name, Arrays.asList("run", "jump", "burpee", "mountain", "jack", "step"));
    }

    private static boolean isCoreExercise(String name) {
        return containsAny(name, Arrays.asList("plank", "crunch", "abs", "core", "sit"));
    }

    private static int calculateWorkoutDuration(List<WorkoutExercise> exercises, UserProfile profile) {
        int totalTime = 0;
        
        for (WorkoutExercise exercise : exercises) {
            // Estimate time per set (reps * 2 seconds + rest)
            int timePerSet = (exercise.getReps() * 2) + exercise.getRestSeconds();
            totalTime += timePerSet * exercise.getSets();
        }
        
        // Add warm-up and cool-down time
        totalTime += 10 * 60; // 10 minutes for warm-up/cool-down
        
        // Convert to minutes
        return totalTime / 60;
    }

    // Helper classes
    private static class WorkoutBlueprint {
        int targetExerciseCount = 5;
        boolean focusOnCardio = false;
        boolean focusOnStrength = false;
        boolean higherReps = false;
        boolean moderateReps = false;
        boolean veryHighReps = false;
        boolean shorterRest = false;
        boolean longerRest = false;
        boolean shortRest = false;
        boolean balanced = false;
        List<String> muscleGroupPriority = new ArrayList<>();
        int targetSets = 3;
        int targetRepsMin = 8;
        int targetRepsMax = 12;
        int restTimeSeconds = 60;
    }

    private static class PersonalizedParams {
        int sets, reps, restSeconds;
    }
}