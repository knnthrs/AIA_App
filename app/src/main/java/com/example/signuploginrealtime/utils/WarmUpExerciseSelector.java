package com.example.signuploginrealtime.utils;

import com.example.signuploginrealtime.models.ExerciseInfo;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.models.UserProfile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Selects appropriate warm-up exercises from exerciseDB based on:
 * - General movement (1 exercise): Light cardio
 * - Dynamic stretches (2-3 exercises): Leg swings, arm circles, torso rotations
 * - Activation exercises (1-2 exercises): Movements specific to the workout ahead
 */
public class WarmUpExerciseSelector {

    private static final String TAG = "WarmUpExerciseSelector";

    // Keywords for identifying cardio warm-up exercises (EXPANDED)
    private static final List<String> CARDIO_KEYWORDS = Arrays.asList(
            "jumping jack", "jog", "jogging", "march", "marching",
            "high knee", "butt kick", "spot jog", "jump rope", "jumping rope",
            "skipping", "shuffle", "side shuffle", "running in place",
            "mountain climber", "climber", "step", "stepping",
            "walk", "walking", "run", "running", "cardio"
    );

    // Keywords for dynamic stretches (EXPANDED)
    private static final List<String> DYNAMIC_STRETCH_KEYWORDS = Arrays.asList(
            "leg swing", "arm circle", "arm swing", "torso rotation",
            "hip circle", "shoulder circle", "trunk rotation",
            "windmill", "torso twist", "standing twist", "twist",
            "leg raise", "dynamic stretch", "arm rotation",
            "hip rotation", "ankle circle", "wrist circle",
            "neck rotation", "spinal rotation", "spine rotation",
            "side bend", "reach", "cross body", "overhead reach",
            "trunk twist", "hip opener", "hip flexor", "cat cow",
            "shoulder roll", "neck roll", "ankle roll"
    );

    // Keywords for activation exercises by body part (EXPANDED)
    private static final Map<String, List<String>> ACTIVATION_KEYWORDS = new HashMap<String, List<String>>() {{
        put("legs", Arrays.asList("bodyweight squat", "squat", "lunge", "glute bridge", "leg raise",
                "calf raise", "hip thrust", "step up", "lateral lunge", "reverse lunge",
                "walking lunge", "split squat", "bulgarian split", "single leg",
                "wall sit", "leg extension", "leg curl", "hamstring"));
        put("chest", Arrays.asList("push up", "pushup", "wall push up", "incline push up",
                "chest stretch", "arm circle", "chest opener", "chest fly",
                "pec", "decline push", "knee push", "modified push"));
        put("back", Arrays.asList("band pull", "scapular", "row", "lat stretch",
                "superman", "bird dog", "cat cow", "back extension",
                "pull up", "chin up", "inverted row", "prone", "y raise",
                "t raise", "reverse fly", "back fly"));
        put("shoulders", Arrays.asList("arm raise", "shoulder", "lateral raise",
                "front raise", "band pull apart", "shoulder press", "overhead",
                "shoulder roll", "shoulder circle", "shoulder shrug",
                "pike push", "handstand", "wall walk"));
        put("arms", Arrays.asList("arm curl", "tricep", "wrist", "forearm",
                "bicep", "triceps dip", "diamond push", "close grip",
                "arm extension", "tricep extension"));
        put("core", Arrays.asList("plank", "dead bug", "bird dog", "hollow",
                "mountain climber", "ab activation", "crunch", "sit up",
                "leg raise", "knee raise", "bicycle", "russian twist",
                "side plank", "ab wheel", "v up", "flutter kick"));
    }};

    /**
     * Selects a complete warm-up routine based on the main workout
     * HYBRID APPROACH: Try database first (has GIFs), fallback to universal
     * @param allExercises All available exercises from exerciseDB
     * @param mainWorkout The main workout exercises
     * @param userProfile User's fitness profile
     * @return List of warm-up exercises (4-6 exercises)
     */
    public static List<WorkoutExercise> selectWarmUpExercises(
            List<ExerciseInfo> allExercises,
            List<WorkoutExercise> mainWorkout,
            UserProfile userProfile) {

        // ✅ TRY DATABASE FIRST (with very lenient filtering)
        if (allExercises != null && !allExercises.isEmpty()) {
            android.util.Log.d(TAG, "Attempting database warm-up selection from " + allExercises.size() + " total exercises");

            List<WorkoutExercise> databaseWarmUp = tryDatabaseWarmUp(allExercises, mainWorkout, userProfile);

            android.util.Log.d(TAG, "Database search found " + databaseWarmUp.size() + " warm-up exercises");

            // If we got at least 1 exercise from database, use them (they have GIFs!)
            // Using any database exercises available, even if just 1
            if (databaseWarmUp.size() >= 1) {
                android.util.Log.d(TAG, "✅ Using DATABASE warm-up with " + databaseWarmUp.size() + " exercises (has GIFs!)");

                // If we only got 1 exercise, supplement with universal exercises
                if (databaseWarmUp.size() < 4) {
                    android.util.Log.d(TAG, "⚠️ Only " + databaseWarmUp.size() + " database exercise(s), supplementing with universal warm-up");
                    List<WorkoutExercise> universalWarmUp = createUniversalWarmUp(userProfile, mainWorkout);

                    // Combine: database exercise(s) first, then universal to fill out
                    List<WorkoutExercise> combined = new ArrayList<>();
                    combined.addAll(databaseWarmUp);

                    // Add universal exercises to reach at least 5 total
                    int neededCount = Math.max(5 - databaseWarmUp.size(), 0);
                    for (int i = 0; i < Math.min(neededCount, universalWarmUp.size()); i++) {
                        combined.add(universalWarmUp.get(i));
                    }

                    // Re-order all exercises
                    for (int i = 0; i < combined.size(); i++) {
                        combined.get(i).setOrder(i + 1);
                    }

                    android.util.Log.d(TAG, "✅ Using HYBRID warm-up: " + databaseWarmUp.size() + " from database + " + (combined.size() - databaseWarmUp.size()) + " universal");
                    return combined;
                }

                return databaseWarmUp;
            } else {
                android.util.Log.d(TAG, "⚠️ No database exercises found, falling back to universal");
            }
        } else {
            android.util.Log.d(TAG, "⚠️ No exercises in database, using universal warm-up");
        }

        // ✅ FALLBACK: Use universal warm-up (no GIFs but always works)
        android.util.Log.d(TAG, "Using UNIVERSAL warm-up (fallback)");
        return createUniversalWarmUp(userProfile, mainWorkout);
    }

    /**
     * Try to build warm-up from database with SMART filtering
     * Selects based on characteristics, not just keywords
     */
    private static List<WorkoutExercise> tryDatabaseWarmUp(
            List<ExerciseInfo> allExercises,
            List<WorkoutExercise> mainWorkout,
            UserProfile userProfile) {

        List<WorkoutExercise> warmUpExercises = new ArrayList<>();

        // STEP 1: Filter to get ONLY bodyweight exercises (no machines!)
        List<ExerciseInfo> bodyweightOnly = filterBodyweightExercises(allExercises);

        android.util.Log.d(TAG, "Filtered to " + bodyweightOnly.size() + " bodyweight exercises from " + allExercises.size() + " total");

        if (bodyweightOnly.isEmpty()) {
            android.util.Log.w(TAG, "❌ No bodyweight exercises found in database!");
            return warmUpExercises; // Database has no bodyweight exercises
        }

        // Log first few bodyweight exercises found
        for (int i = 0; i < Math.min(5, bodyweightOnly.size()); i++) {
            android.util.Log.d(TAG, "Bodyweight exercise " + (i+1) + ": " + bodyweightOnly.get(i).getName());
        }

        // STEP 2: Smart selection - look at characteristics, not just names
        List<ExerciseInfo> cardioLike = selectCardioLikeExercises(bodyweightOnly, userProfile);
        List<ExerciseInfo> stretchLike = selectStretchLikeExercises(bodyweightOnly, userProfile);
        List<ExerciseInfo> activationLike = selectActivationLikeExercises(bodyweightOnly, mainWorkout, userProfile);

        android.util.Log.d(TAG, "Found " + cardioLike.size() + " cardio-like, " + stretchLike.size() + " stretch-like, " + activationLike.size() + " activation-like exercises");

        // STEP 3: Build warm-up from selected exercises
        // Add 1 cardio-like exercise
        if (!cardioLike.isEmpty()) {
            ExerciseInfo cardio = cardioLike.get(0);
            android.util.Log.d(TAG, "✅ Cardio: " + cardio.getName());
            warmUpExercises.add(createWarmUpExercise(cardio, "cardio", userProfile));
        }

        // Add 2-3 stretch-like exercises
        int stretchCount = Math.min(3, stretchLike.size());
        for (int i = 0; i < stretchCount; i++) {
            android.util.Log.d(TAG, "✅ Stretch " + (i+1) + ": " + stretchLike.get(i).getName());
            warmUpExercises.add(createWarmUpExercise(stretchLike.get(i), "stretch", userProfile));
        }

        // Add 1-2 activation-like exercises
        int activationCount = Math.min(2, activationLike.size());
        for (int i = 0; i < activationCount; i++) {
            android.util.Log.d(TAG, "✅ Activation " + (i+1) + ": " + activationLike.get(i).getName());
            warmUpExercises.add(createWarmUpExercise(activationLike.get(i), "activation", userProfile));
        }

        // STEP 4: Set proper order
        for (int i = 0; i < warmUpExercises.size(); i++) {
            warmUpExercises.get(i).setOrder(i + 1);
        }

        return warmUpExercises;
    }

    /**
     * Select exercises that work well as cardio warm-up
     * CONSERVATIVE - only truly cardio/movement exercises
     */
    private static List<ExerciseInfo> selectCardioLikeExercises(List<ExerciseInfo> bodyweightExercises, UserProfile userProfile) {
        List<ExerciseInfo> candidates = new ArrayList<>();

        for (ExerciseInfo exercise : bodyweightExercises) {
            if (exercise == null || exercise.getName() == null) continue;

            String nameLower = exercise.getName().toLowerCase();

            // CONSERVATIVE - only clearly cardio movements
            boolean isCardioLike =
                nameLower.contains("jumping jack") || nameLower.contains("jump jack") ||
                nameLower.contains("high knee") || nameLower.contains("butt kick") ||
                nameLower.contains("mountain climber") || nameLower.contains("climber") ||
                nameLower.contains("burpee") || nameLower.contains("jump rope") ||
                nameLower.contains("jumping rope") || nameLower.contains("spot jog") ||
                nameLower.contains("jog") && !nameLower.contains("yoga") ||
                nameLower.contains("march") && !nameLower.contains("reverse") ||
                nameLower.contains("step up") && !nameLower.contains("weight") ||
                nameLower.contains("side shuffle") || nameLower.contains("shuffle");

            // Check target muscles for cardiovascular
            if (!isCardioLike) {
                List<String> targetMuscles = exercise.getTargetMuscles();
                if (targetMuscles != null) {
                    for (String muscle : targetMuscles) {
                        if (muscle.toLowerCase().contains("cardiovascular")) {
                            isCardioLike = true;
                            break;
                        }
                    }
                }
            }

            if (isCardioLike) {
                // Filter out advanced/dangerous moves for sedentary users
                if (userProfile != null && userProfile.getFitnessLevel() != null) {
                    String level = userProfile.getFitnessLevel().toLowerCase();
                    if (level.equals("sedentary") || level.contains("lightly")) {
                        if (nameLower.contains("burpee") || nameLower.contains("tuck jump") ||
                            nameLower.contains("box jump") || nameLower.contains("explosive")) {
                            continue; // Skip this one
                        }
                    }
                }
                android.util.Log.d(TAG, "🏃 CARDIO candidate: " + exercise.getName() +
                    " | bodyParts: " + exercise.getBodyParts() +
                    " | equipment: " + exercise.getEquipments());
                candidates.add(exercise);
            }
        }

        Collections.shuffle(candidates);
        return candidates;
    }

    /**
     * Select exercises that work well as dynamic stretches
     * CONSERVATIVE - only clearly stretching/mobility exercises
     */
    private static List<ExerciseInfo> selectStretchLikeExercises(List<ExerciseInfo> bodyweightExercises, UserProfile userProfile) {
        List<ExerciseInfo> candidates = new ArrayList<>();

        for (ExerciseInfo exercise : bodyweightExercises) {
            if (exercise == null || exercise.getName() == null) continue;

            String nameLower = exercise.getName().toLowerCase();

            // CONSERVATIVE - only clearly stretching movements
            boolean isStretchLike =
                nameLower.contains("stretch") && !nameLower.contains("strength") ||
                nameLower.contains("arm circle") || nameLower.contains("arm swing") ||
                nameLower.contains("leg swing") || nameLower.contains("hip circle") ||
                nameLower.contains("shoulder circle") || nameLower.contains("shoulder roll") ||
                nameLower.contains("torso twist") || nameLower.contains("trunk rotation") ||
                nameLower.contains("spinal rotation") || nameLower.contains("neck roll") ||
                nameLower.contains("ankle circle") || nameLower.contains("wrist circle") ||
                nameLower.contains("hip opener") || nameLower.contains("dynamic stretch") ||
                nameLower.contains("mobility") && !nameLower.contains("strength") ||
                nameLower.contains("flexibility") && !nameLower.contains("strength") ||
                nameLower.contains("windmill") && !nameLower.contains("weight");

            // Exclude static/passive stretches and strength moves
            if (isStretchLike) {
                if (nameLower.contains("static") || nameLower.contains("hold") ||
                    nameLower.contains("seated") || nameLower.contains("lying") ||
                    nameLower.contains("press") || nameLower.contains("pull") ||
                    nameLower.contains("row") || nameLower.contains("lift") ||
                    nameLower.contains("curl") || nameLower.contains("extension") ||
                    nameLower.contains("squat") || nameLower.contains("lunge") ||
                    nameLower.contains("push") || nameLower.contains("dip")) {
                    isStretchLike = false;
                }
            }

            if (isStretchLike) {
                android.util.Log.d(TAG, "🧘 STRETCH candidate: " + exercise.getName() +
                    " | bodyParts: " + exercise.getBodyParts() +
                    " | equipment: " + exercise.getEquipments());
                candidates.add(exercise);
            }
        }

        Collections.shuffle(candidates);
        return candidates.subList(0, Math.min(5, candidates.size()));
    }

    /**
     * Select exercises that work well as activation
     * CONSERVATIVE - only light bodyweight movements for activation
     */
    private static List<ExerciseInfo> selectActivationLikeExercises(
            List<ExerciseInfo> bodyweightExercises,
            List<WorkoutExercise> mainWorkout,
            UserProfile userProfile) {

        List<String> targetBodyParts = analyzeMainWorkoutBodyParts(mainWorkout);
        List<ExerciseInfo> candidates = new ArrayList<>();

        for (ExerciseInfo exercise : bodyweightExercises) {
            if (exercise == null || exercise.getName() == null) continue;

            String nameLower = exercise.getName().toLowerCase();
            List<String> bodyParts = exercise.getBodyParts();

            boolean isActivationLike = false;

            // CONSERVATIVE - only clearly appropriate activation exercises
            if (nameLower.contains("bodyweight squat") || nameLower.contains("air squat") ||
                nameLower.contains("wall push") || nameLower.contains("incline push") ||
                nameLower.contains("knee push") || nameLower.contains("modified push") ||
                nameLower.contains("glute bridge") || nameLower.contains("hip bridge") ||
                nameLower.contains("bird dog") || nameLower.contains("dead bug") ||
                nameLower.contains("plank") && !nameLower.contains("side") ||
                nameLower.contains("calf raise") || nameLower.contains("heel raise") ||
                nameLower.contains("leg raise") && !nameLower.contains("hanging") ||
                nameLower.contains("scapular") || nameLower.contains("wall sit") ||
                nameLower.contains("superman") || nameLower.contains("cat cow")) {
                isActivationLike = true;
            }

            // Check if matches main workout body parts for simple movements only
            if (!isActivationLike && bodyParts != null && !targetBodyParts.isEmpty()) {
                for (String targetPart : targetBodyParts) {
                    for (String exPart : bodyParts) {
                        if (exPart.toLowerCase().contains(targetPart.toLowerCase()) ||
                            targetPart.toLowerCase().contains(exPart.toLowerCase())) {
                            // Only if it's a simple movement
                            if (nameLower.contains("raise") || nameLower.contains("circle") ||
                                nameLower.contains("bridge") || nameLower.contains("squeeze")) {
                                isActivationLike = true;
                                break;
                            }
                        }
                    }
                    if (isActivationLike) break;
                }
            }

            // Exclude all advanced/heavy movements
            if (nameLower.contains("pistol") || nameLower.contains("one leg") ||
                nameLower.contains("single leg") && nameLower.contains("squat") ||
                nameLower.contains("jump") || nameLower.contains("plyometric") ||
                nameLower.contains("explosive") || nameLower.contains("handstand") ||
                nameLower.contains("muscle up") || nameLower.contains("pull up") ||
                nameLower.contains("chin up") || nameLower.contains("dip") ||
                nameLower.contains("decline") || nameLower.contains("pike") ||
                nameLower.contains("archer") || nameLower.contains("diamond")) {
                isActivationLike = false;
            }

            if (isActivationLike) {
                android.util.Log.d(TAG, "💪 ACTIVATION candidate: " + exercise.getName() +
                    " | bodyParts: " + exercise.getBodyParts() +
                    " | equipment: " + exercise.getEquipments());
                candidates.add(exercise);
            }
        }

        Collections.shuffle(candidates);
        return candidates.subList(0, Math.min(3, candidates.size()));
    }

    /**
     * Filter to get ONLY bodyweight exercises (excludes all machines)
     */
    private static List<ExerciseInfo> filterBodyweightExercises(List<ExerciseInfo> allExercises) {
        List<ExerciseInfo> bodyweight = new ArrayList<>();

        for (ExerciseInfo exercise : allExercises) {
            if (exercise == null || exercise.getName() == null) continue;

            List<String> equipments = exercise.getEquipments();

            // ONLY accept if equipment is "body weight" or null/empty
            if (equipments == null || equipments.isEmpty() ||
                equipments.contains("body weight") ||
                equipments.contains("bodyweight")) {

                // EXCLUDE exercises with machine keywords
                String nameLower = exercise.getName().toLowerCase();
                if (!nameLower.contains("machine") &&
                    !nameLower.contains("cable") &&
                    !nameLower.contains("smith") &&
                    !nameLower.contains("leverage") &&
                    !nameLower.contains("assisted") &&
                    !nameLower.contains("sled") &&
                    !nameLower.contains("barbell") &&
                    !nameLower.contains("dumbbell") &&
                    !nameLower.contains("kettlebell") &&
                    !nameLower.contains("rope") &&
                    !nameLower.contains("resistance band")) {
                    bodyweight.add(exercise);
                }
            }
        }

        return bodyweight;
    }

    /**
     * Create a universal warm-up that works for everyone
     * Uses hardcoded exercises that don't require equipment
     */
    private static List<WorkoutExercise> createUniversalWarmUp(UserProfile userProfile, List<WorkoutExercise> mainWorkout) {
        List<WorkoutExercise> warmUp = new ArrayList<>();

        String level = userProfile != null && userProfile.getFitnessLevel() != null
                ? userProfile.getFitnessLevel().toLowerCase() : "beginner";

        // Determine reps based on fitness level
        int cardioReps = level.equals("sedentary") ? 10 :
                        (level.contains("lightly") ? 15 : 20);
        int stretchReps = level.equals("sedentary") ? 8 : 10;
        int activationReps = level.equals("sedentary") ? 8 : 10;

        // 1. CARDIO - Marching in Place (safe for all levels)
        ExerciseInfo marching = new ExerciseInfo();
        marching.setExerciseId("warmup_marching");
        marching.setName("Marching in Place");
        marching.setBodyParts(Arrays.asList("cardio", "full body"));
        marching.setEquipments(Arrays.asList("body weight"));
        marching.setTargetMuscles(Arrays.asList("cardiovascular"));
        marching.setGifUrl("https://via.placeholder.com/150?text=March+in+Place");
        marching.setInstructions(Arrays.asList(
            "Stand tall with feet hip-width apart",
            "Lift your right knee to hip height",
            "Lower it and immediately lift your left knee",
            "Swing your arms naturally as you march",
            "Keep a steady, comfortable pace",
            "March for " + cardioReps + " repetitions per leg"
        ));
        WorkoutExercise marchingEx = new WorkoutExercise();
        marchingEx.setExerciseInfo(marching);
        marchingEx.setSets(1);
        marchingEx.setReps(cardioReps);
        marchingEx.setRestSeconds(20);
        marchingEx.setOrder(1);
        warmUp.add(marchingEx);

        // 2. STRETCH - Arm Circles
        ExerciseInfo armCircles = new ExerciseInfo();
        armCircles.setExerciseId("warmup_arm_circles");
        armCircles.setName("Arm Circles");
        armCircles.setBodyParts(Arrays.asList("shoulders", "arms"));
        armCircles.setEquipments(Arrays.asList("body weight"));
        armCircles.setTargetMuscles(Arrays.asList("shoulders", "rotator cuff"));
        armCircles.setGifUrl("https://via.placeholder.com/150?text=Arm+Circles");
        armCircles.setInstructions(Arrays.asList(
            "Stand with feet shoulder-width apart",
            "Extend arms out to the sides at shoulder height",
            "Make small circular motions forward for half the reps",
            "Reverse direction and circle backward",
            "Gradually increase the size of the circles",
            "Keep your core engaged and posture upright"
        ));
        WorkoutExercise armCirclesEx = new WorkoutExercise();
        armCirclesEx.setExerciseInfo(armCircles);
        armCirclesEx.setSets(1);
        armCirclesEx.setReps(stretchReps);
        armCirclesEx.setRestSeconds(20);
        armCirclesEx.setOrder(2);
        warmUp.add(armCirclesEx);

        // 3. STRETCH - Leg Swings
        ExerciseInfo legSwings = new ExerciseInfo();
        legSwings.setExerciseId("warmup_leg_swings");
        legSwings.setName("Leg Swings (Forward & Back)");
        legSwings.setBodyParts(Arrays.asList("legs", "hips"));
        legSwings.setEquipments(Arrays.asList("body weight"));
        legSwings.setTargetMuscles(Arrays.asList("hip flexors", "hamstrings"));
        legSwings.setGifUrl("https://via.placeholder.com/150?text=Leg+Swings");
        legSwings.setInstructions(Arrays.asList(
            "Stand next to a wall for balance",
            "Swing your outside leg forward and back in a controlled motion",
            "Keep your torso upright and core engaged",
            "Start with small swings and gradually increase range",
            "Complete reps on one leg, then switch sides",
            "Move smoothly without jerking motions"
        ));
        WorkoutExercise legSwingsEx = new WorkoutExercise();
        legSwingsEx.setExerciseInfo(legSwings);
        legSwingsEx.setSets(1);
        legSwingsEx.setReps(stretchReps);
        legSwingsEx.setRestSeconds(20);
        legSwingsEx.setOrder(3);
        warmUp.add(legSwingsEx);

        // 4. STRETCH - Torso Rotations
        ExerciseInfo torsoTwist = new ExerciseInfo();
        torsoTwist.setExerciseId("warmup_torso_twist");
        torsoTwist.setName("Standing Torso Rotations");
        torsoTwist.setBodyParts(Arrays.asList("core", "back"));
        torsoTwist.setEquipments(Arrays.asList("body weight"));
        torsoTwist.setTargetMuscles(Arrays.asList("obliques", "spine"));
        torsoTwist.setGifUrl("https://via.placeholder.com/150?text=Torso+Twist");
        torsoTwist.setInstructions(Arrays.asList(
            "Stand with feet shoulder-width apart",
            "Place hands on hips or clasp in front of chest",
            "Rotate your torso to the right, keeping hips forward",
            "Return to center, then rotate to the left",
            "Keep movements smooth and controlled",
            "Feel the gentle stretch in your core and back"
        ));
        WorkoutExercise torsoTwistEx = new WorkoutExercise();
        torsoTwistEx.setExerciseInfo(torsoTwist);
        torsoTwistEx.setSets(1);
        torsoTwistEx.setReps(stretchReps);
        torsoTwistEx.setRestSeconds(20);
        torsoTwistEx.setOrder(4);
        warmUp.add(torsoTwistEx);

        // 5. ACTIVATION - Analyze main workout and add appropriate activation
        List<String> targetBodyParts = analyzeMainWorkoutBodyParts(mainWorkout);
        WorkoutExercise activation = createActivationExercise(targetBodyParts, activationReps);
        activation.setOrder(5);
        warmUp.add(activation);

        // 6. OPTIONAL SECOND ACTIVATION - Add if main workout targets multiple body parts
        if (targetBodyParts.size() > 1) {
            String secondaryBodyPart = targetBodyParts.size() > 1 ? targetBodyParts.get(1) : targetBodyParts.get(0);
            WorkoutExercise secondActivation = createSecondaryActivation(secondaryBodyPart, activationReps);
            secondActivation.setOrder(6);
            warmUp.add(secondActivation);
        }

        return warmUp;
    }

    /**
     * Create primary activation exercise based on main workout focus
     */
    private static WorkoutExercise createActivationExercise(List<String> targetBodyParts, int reps) {
        String primaryTarget = targetBodyParts.isEmpty() ? "general" : targetBodyParts.get(0);

        ExerciseInfo activation = new ExerciseInfo();
        WorkoutExercise activationEx = new WorkoutExercise();

        // Choose activation based on primary body part
        if (primaryTarget.contains("leg") || primaryTarget.contains("quad") ||
            primaryTarget.contains("glute") || primaryTarget.contains("hamstring")) {
            // LEG ACTIVATION - Bodyweight Squats
            activation.setExerciseId("warmup_bodyweight_squats");
            activation.setName("Bodyweight Squats");
            activation.setBodyParts(Arrays.asList("legs", "glutes"));
            activation.setEquipments(Arrays.asList("body weight"));
            activation.setTargetMuscles(Arrays.asList("quadriceps", "glutes", "hamstrings"));
            activation.setGifUrl("https://via.placeholder.com/150?text=Bodyweight+Squats");
            activation.setInstructions(Arrays.asList(
                "Stand with feet shoulder-width apart, toes slightly out",
                "Keep your chest up and core engaged",
                "Lower down by bending knees and pushing hips back",
                "Go as low as comfortable while keeping heels down",
                "Push through heels to return to standing",
                "Keep knees tracking over toes throughout"
            ));
        } else if (primaryTarget.contains("chest") || primaryTarget.contains("push") || primaryTarget.contains("pec")) {
            // CHEST ACTIVATION - Wall Push-Ups
            activation.setExerciseId("warmup_wall_pushups");
            activation.setName("Wall Push-Ups");
            activation.setBodyParts(Arrays.asList("chest", "arms"));
            activation.setEquipments(Arrays.asList("body weight"));
            activation.setTargetMuscles(Arrays.asList("chest", "shoulders", "triceps"));
            activation.setGifUrl("https://via.placeholder.com/150?text=Wall+Pushups");
            activation.setInstructions(Arrays.asList(
                "Stand arm's length from a wall",
                "Place hands on wall at shoulder height and width",
                "Keep body straight from head to heels",
                "Bend elbows to bring chest toward wall",
                "Push back to starting position",
                "Control the movement both ways"
            ));
        } else if (primaryTarget.contains("back") || primaryTarget.contains("pull") || primaryTarget.contains("lat")) {
            // BACK ACTIVATION - Scapular Squeezes
            activation.setExerciseId("warmup_scapular_squeeze");
            activation.setName("Scapular Squeezes");
            activation.setBodyParts(Arrays.asList("back", "shoulders"));
            activation.setEquipments(Arrays.asList("body weight"));
            activation.setTargetMuscles(Arrays.asList("upper back", "rhomboids", "trapezius"));
            activation.setGifUrl("https://via.placeholder.com/150?text=Scapular+Squeeze");
            activation.setInstructions(Arrays.asList(
                "Stand or sit with good posture",
                "Arms at sides or bent at 90 degrees",
                "Squeeze shoulder blades together toward spine",
                "Hold for 2 seconds",
                "Release slowly",
                "Keep shoulders down, away from ears"
            ));
        } else if (primaryTarget.contains("shoulder") || primaryTarget.contains("delt")) {
            // SHOULDER ACTIVATION - Shoulder Rolls
            activation.setExerciseId("warmup_shoulder_rolls");
            activation.setName("Shoulder Rolls");
            activation.setBodyParts(Arrays.asList("shoulders"));
            activation.setEquipments(Arrays.asList("body weight"));
            activation.setTargetMuscles(Arrays.asList("shoulders", "upper back"));
            activation.setGifUrl("https://via.placeholder.com/150?text=Shoulder+Rolls");
            activation.setInstructions(Arrays.asList(
                "Stand with feet hip-width apart",
                "Roll shoulders up toward ears",
                "Roll them back and down in a circular motion",
                "Complete half the reps rolling backward",
                "Reverse direction and roll forward",
                "Move slowly and smoothly"
            ));
        } else {
            // GENERAL/CORE ACTIVATION - Standing Knee Raises
            activation.setExerciseId("warmup_knee_raises");
            activation.setName("Standing Knee Raises");
            activation.setBodyParts(Arrays.asList("core", "legs"));
            activation.setEquipments(Arrays.asList("body weight"));
            activation.setTargetMuscles(Arrays.asList("hip flexors", "core", "balance"));
            activation.setGifUrl("https://via.placeholder.com/150?text=Knee+Raises");
            activation.setInstructions(Arrays.asList(
                "Stand tall with feet together",
                "Lift right knee up toward chest",
                "Lower and immediately lift left knee",
                "Keep core engaged and back straight",
                "Use a wall for balance if needed",
                "Perform at a controlled pace"
            ));
        }

        activationEx.setExerciseInfo(activation);
        activationEx.setSets(1);
        activationEx.setReps(reps);
        activationEx.setRestSeconds(30);

        return activationEx;
    }

    /**
     * Create secondary activation for full-body preparation
     */
    private static WorkoutExercise createSecondaryActivation(String bodyPart, int reps) {
        ExerciseInfo activation = new ExerciseInfo();
        WorkoutExercise activationEx = new WorkoutExercise();

        // Always use hip circles as secondary (works for everything)
        activation.setExerciseId("warmup_hip_circles");
        activation.setName("Hip Circles");
        activation.setBodyParts(Arrays.asList("hips", "core"));
        activation.setEquipments(Arrays.asList("body weight"));
        activation.setTargetMuscles(Arrays.asList("hips", "core stability"));
        activation.setGifUrl("https://via.placeholder.com/150?text=Hip+Circles");
        activation.setInstructions(Arrays.asList(
            "Stand with hands on hips, feet shoulder-width apart",
            "Make circular motions with your hips",
            "Move in a clockwise direction for half the reps",
            "Reverse and go counterclockwise",
            "Keep upper body relatively still",
            "Make smooth, controlled circles"
        ));

        activationEx.setExerciseInfo(activation);
        activationEx.setSets(1);
        activationEx.setReps(reps);
        activationEx.setRestSeconds(30);

        return activationEx;
    }

    /**
     * Select a light cardio exercise (IMPROVED - more lenient)
     */
    private static ExerciseInfo selectCardioExercise(List<ExerciseInfo> allExercises, UserProfile userProfile) {
        List<ExerciseInfo> candidates = new ArrayList<>();

        for (ExerciseInfo exercise : allExercises) {
            if (exercise == null || exercise.getName() == null) continue;

            String nameLower = exercise.getName().toLowerCase();

            // Match cardio keywords (already filtered to bodyweight only)
            for (String keyword : CARDIO_KEYWORDS) {
                if (nameLower.contains(keyword)) {
                    candidates.add(exercise);
                    break;
                }
            }

            // Also accept exercises labeled as "cardio" in body parts
            List<String> bodyParts = exercise.getBodyParts();
            if (bodyParts != null) {
                for (String part : bodyParts) {
                    if (part.toLowerCase().contains("cardio")) {
                        candidates.add(exercise);
                        break;
                    }
                }
            }
        }

        // Filter by fitness level (avoid high-intensity for sedentary)
        if (userProfile != null && userProfile.getFitnessLevel() != null) {
            String level = userProfile.getFitnessLevel().toLowerCase();
            if (level.equals("sedentary") || level.contains("lightly")) {
                // Remove intense exercises
                candidates.removeIf(e -> {
                    String name = e.getName().toLowerCase();
                    return name.contains("burpee") || name.contains("tuck jump") ||
                           name.contains("sprint") || name.contains("box jump") ||
                           name.contains("explosive");
                });
            }
        }

        return candidates.isEmpty() ? null : candidates.get((int) (Math.random() * candidates.size()));
    }

    /**
     * Select dynamic stretches (IMPROVED - more lenient)
     */
    private static List<ExerciseInfo> selectDynamicStretches(
            List<ExerciseInfo> allExercises, UserProfile userProfile, int count) {

        List<ExerciseInfo> candidates = new ArrayList<>();

        for (ExerciseInfo exercise : allExercises) {
            if (exercise == null || exercise.getName() == null) continue;

            String nameLower = exercise.getName().toLowerCase();

            // Match dynamic stretch keywords (already filtered to bodyweight)
            for (String keyword : DYNAMIC_STRETCH_KEYWORDS) {
                if (nameLower.contains(keyword)) {
                    candidates.add(exercise);
                    break;
                }
            }

            // Also look for "stretch", "mobility", "flexibility" in name or body parts
            if (nameLower.contains("stretch") || nameLower.contains("mobility") ||
                nameLower.contains("flexibility")) {
                // Make sure it's not a static/passive stretch (we want dynamic)
                if (!nameLower.contains("static") && !nameLower.contains("hold") &&
                    !nameLower.contains("seated")) {
                    if (!candidates.contains(exercise)) {
                        candidates.add(exercise);
                    }
                }
            }
        }

        // Shuffle and return requested count
        Collections.shuffle(candidates);
        return candidates.subList(0, Math.min(count, candidates.size()));
    }

    /**
     * Analyze main workout to determine which body parts to activate
     */
    private static List<String> analyzeMainWorkoutBodyParts(List<WorkoutExercise> mainWorkout) {
        Map<String, Integer> bodyPartCount = new HashMap<>();

        for (WorkoutExercise we : mainWorkout) {
            ExerciseInfo info = we.getExerciseInfo();
            if (info == null) continue;

            List<String> bodyParts = info.getBodyParts();
            if (bodyParts != null) {
                for (String part : bodyParts) {
                    String partLower = part.toLowerCase();
                    bodyPartCount.put(partLower, bodyPartCount.getOrDefault(partLower, 0) + 1);
                }
            }

            List<String> targets = info.getTargetMuscles();
            if (targets != null) {
                for (String target : targets) {
                    String targetLower = target.toLowerCase();
                    // Map muscle groups to body parts
                    if (targetLower.contains("quad") || targetLower.contains("hamstring") ||
                        targetLower.contains("glute") || targetLower.contains("calves")) {
                        bodyPartCount.put("legs", bodyPartCount.getOrDefault("legs", 0) + 1);
                    } else if (targetLower.contains("chest") || targetLower.contains("pec")) {
                        bodyPartCount.put("chest", bodyPartCount.getOrDefault("chest", 0) + 1);
                    } else if (targetLower.contains("back") || targetLower.contains("lat")) {
                        bodyPartCount.put("back", bodyPartCount.getOrDefault("back", 0) + 1);
                    } else if (targetLower.contains("shoulder") || targetLower.contains("delt")) {
                        bodyPartCount.put("shoulders", bodyPartCount.getOrDefault("shoulders", 0) + 1);
                    } else if (targetLower.contains("bicep") || targetLower.contains("tricep")) {
                        bodyPartCount.put("arms", bodyPartCount.getOrDefault("arms", 0) + 1);
                    } else if (targetLower.contains("abs") || targetLower.contains("core")) {
                        bodyPartCount.put("core", bodyPartCount.getOrDefault("core", 0) + 1);
                    }
                }
            }
        }

        // Return top 2 body parts
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(bodyPartCount.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(2, sorted.size()); i++) {
            result.add(sorted.get(i).getKey());
        }

        return result;
    }

    /**
     * Select activation exercises for specific body parts (IMPROVED - more lenient)
     */
    private static List<ExerciseInfo> selectActivationExercises(
            List<ExerciseInfo> allExercises,
            List<String> targetBodyParts,
            UserProfile userProfile,
            int count) {

        List<ExerciseInfo> candidates = new ArrayList<>();

        for (String bodyPart : targetBodyParts) {
            List<String> keywords = ACTIVATION_KEYWORDS.get(bodyPart);
            if (keywords == null) continue;

            for (ExerciseInfo exercise : allExercises) {
                if (exercise == null || exercise.getName() == null) continue;

                String nameLower = exercise.getName().toLowerCase();

                // Match activation keywords (already filtered to bodyweight)
                for (String keyword : keywords) {
                    if (nameLower.contains(keyword)) {
                        if (!candidates.contains(exercise)) {
                            candidates.add(exercise);
                        }
                        break;
                    }
                }

                // Also check if exercise targets the right body part
                List<String> exerciseBodyParts = exercise.getBodyParts();
                if (exerciseBodyParts != null) {
                    for (String part : exerciseBodyParts) {
                        String partLower = part.toLowerCase();
                        if (partLower.contains(bodyPart.toLowerCase()) ||
                            bodyPart.toLowerCase().contains(partLower)) {
                            if (!candidates.contains(exercise)) {
                                candidates.add(exercise);
                            }
                            break;
                        }
                    }
                }
            }
        }

        // Filter by fitness level
        if (userProfile != null && userProfile.getFitnessLevel() != null) {
            String level = userProfile.getFitnessLevel().toLowerCase();
            if (level.equals("sedentary") || level.contains("lightly")) {
                // Prefer simpler variations
                candidates.removeIf(e -> {
                    String name = e.getName().toLowerCase();
                    return name.contains("jump") || name.contains("plyometric") ||
                           name.contains("explosive") || name.contains("pistol") ||
                           name.contains("one leg") || name.contains("single leg");
                });
            }
        }

        // Remove duplicates and shuffle
        List<ExerciseInfo> unique = new ArrayList<>();
        for (ExerciseInfo e : candidates) {
            if (!unique.contains(e)) {
                unique.add(e);
            }
        }

        Collections.shuffle(unique);
        return unique.subList(0, Math.min(count, unique.size()));
    }

    /**
     * Create a WorkoutExercise with warm-up specific parameters
     */
    private static WorkoutExercise createWarmUpExercise(
            ExerciseInfo exerciseInfo, String type, UserProfile userProfile) {

        WorkoutExercise we = new WorkoutExercise();
        we.setExerciseInfo(exerciseInfo);

        String level = userProfile != null && userProfile.getFitnessLevel() != null
                ? userProfile.getFitnessLevel().toLowerCase() : "beginner";

        switch (type) {
            case "cardio":
                // Light cardio: 30-60 seconds
                we.setSets(1);
                we.setReps(0); // Duration-based
                we.setRestSeconds(30);
                break;

            case "stretch":
                // Dynamic stretches: 10-15 reps per side
                we.setSets(1);
                we.setReps(level.equals("sedentary") ? 8 : 12);
                we.setRestSeconds(20);
                break;

            case "activation":
                // Activation: 1 set, 10-15 reps
                we.setSets(1);
                we.setReps(level.equals("sedentary") ? 8 :
                          (level.equals("lightly active") ? 10 : 12));
                we.setRestSeconds(30);
                break;
        }

        return we;
    }

    /**
     * Check if exercise matches any keyword list
     */
    private static boolean matchesKeywords(String exerciseName, List<String> keywords) {
        String nameLower = exerciseName.toLowerCase();
        for (String keyword : keywords) {
            if (nameLower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

