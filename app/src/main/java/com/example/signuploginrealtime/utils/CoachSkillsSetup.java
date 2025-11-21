package com.example.signuploginrealtime.utils;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to add skills to coach profiles
 * This is a one-time setup utility to populate coach skills in Firestore
 */
public class CoachSkillsSetup {

    private static final String TAG = "CoachSkillsSetup";

    // Common fitness skills
    public static final List<String> WEIGHT_LOSS_SKILLS = Arrays.asList(
            "Weight Loss", "Cardio Training", "Nutrition Guidance"
    );

    public static final List<String> STRENGTH_SKILLS = Arrays.asList(
            "Strength Training", "Muscle Building", "Powerlifting"
    );

    public static final List<String> BODYBUILDING_SKILLS = Arrays.asList(
            "Bodybuilding", "Muscle Building", "Nutrition Guidance"
    );

    public static final List<String> CROSSFIT_SKILLS = Arrays.asList(
            "CrossFit", "HIIT", "Functional Training"
    );

    public static final List<String> GENERAL_SKILLS = Arrays.asList(
            "General Fitness", "Weight Loss", "Cardio Training"
    );

    public static final List<String> SPORTS_SKILLS = Arrays.asList(
            "Sports Training", "Agility", "Speed Training"
    );

    public static final List<String> REHABILITATION_SKILLS = Arrays.asList(
            "Rehabilitation", "Injury Prevention", "Flexibility"
    );

    /**
     * Add skills to a specific coach
     * @param coachId The coach's Firestore document ID
     * @param skills List of skill strings
     * @param yearsOfExperience Years of coaching experience
     */
    public static void addSkillsToCoach(String coachId, List<String> skills, int yearsOfExperience) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> updates = new HashMap<>();
        updates.put("skills", skills);
        updates.put("yearsOfExperience", yearsOfExperience);

        db.collection("coaches")
                .document(coachId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Skills added successfully to coach: " + coachId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding skills to coach: " + coachId, e);
                });
    }

    /**
     * Example: Add skills to all coaches at once
     * Call this method once from an Activity (e.g., from a debug button)
     */
    public static void setupAllCoaches() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get all coaches and add default skills
        db.collection("coaches")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        String coachId = doc.getId();

                        // Assign different skills to different coaches (rotate through skill sets)
                        List<String> skills;
                        int experience;

                        switch (count % 7) {
                            case 0:
                                skills = WEIGHT_LOSS_SKILLS;
                                experience = 5;
                                break;
                            case 1:
                                skills = STRENGTH_SKILLS;
                                experience = 3;
                                break;
                            case 2:
                                skills = BODYBUILDING_SKILLS;
                                experience = 7;
                                break;
                            case 3:
                                skills = CROSSFIT_SKILLS;
                                experience = 4;
                                break;
                            case 4:
                                skills = SPORTS_SKILLS;
                                experience = 6;
                                break;
                            case 5:
                                skills = REHABILITATION_SKILLS;
                                experience = 8;
                                break;
                            default:
                                skills = GENERAL_SKILLS;
                                experience = 4;
                                break;
                        }

                        addSkillsToCoach(coachId, skills, experience);
                        count++;
                    }

                    Log.d(TAG, "Setup complete for " + count + " coaches");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching coaches", e);
                });
    }

    /**
     * All available skill options
     */
    public static final List<String> ALL_AVAILABLE_SKILLS = Arrays.asList(
            // Weight Management
            "Weight Loss",
            "Weight Gain",
            "Body Transformation",

            // Strength & Muscle
            "Strength Training",
            "Muscle Building",
            "Powerlifting",
            "Bodybuilding",

            // Cardio & Endurance
            "Cardio Training",
            "HIIT",
            "Endurance Training",
            "Marathon Training",

            // Functional & CrossFit
            "Functional Training",
            "CrossFit",
            "Circuit Training",

            // Sports Specific
            "Sports Training",
            "Athletic Performance",
            "Speed Training",
            "Agility Training",

            // Flexibility & Recovery
            "Flexibility",
            "Mobility Training",
            "Yoga",
            "Pilates",
            "Stretching",

            // Specialized
            "Rehabilitation",
            "Injury Prevention",
            "Post-Injury Training",
            "Senior Fitness",
            "Youth Training",
            "Prenatal Fitness",
            "Postnatal Fitness",

            // Nutrition & Wellness
            "Nutrition Guidance",
            "Meal Planning",
            "Supplement Advice",
            "Lifestyle Coaching",

            // General
            "General Fitness",
            "Personal Training",
            "Group Training"
    );
}

