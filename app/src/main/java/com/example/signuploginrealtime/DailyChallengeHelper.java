package com.example.signuploginrealtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DailyChallengeHelper {

    public static class Challenge {
        public String id; // Unique identifier
        public String emoji;
        public String description;
        public int targetCount;
        public String actionType; // "tap", "timer", "count"

        public Challenge(String id, String emoji, String description, int targetCount, String actionType) {
            this.id = id;
            this.emoji = emoji;
            this.description = description;
            this.targetCount = targetCount;
            this.actionType = actionType;
        }
    }

    // Fitness goal categories
    private static final Map<String, List<Challenge>> GOAL_CHALLENGES = new HashMap<>();

    static {
        // Weight Loss Challenges
        List<Challenge> weightLossChallenges = new ArrayList<>();
        weightLossChallenges.add(new Challenge("wl_1", "🏃", "Run or jog for 30 minutes", 1, "tap"));
        weightLossChallenges.add(new Challenge("wl_2", "🚶", "Take 12,000 steps today", 1, "tap"));
        weightLossChallenges.add(new Challenge("wl_3", "🥗", "Eat a healthy salad for lunch", 1, "tap"));
        weightLossChallenges.add(new Challenge("wl_4", "💧", "Drink 10 glasses of water", 1, "tap"));
        weightLossChallenges.add(new Challenge("wl_5", "🚫", "No sugary drinks today", 1, "tap"));
        weightLossChallenges.add(new Challenge("wl_6", "🏊", "30 minutes of cardio", 1, "tap"));
        weightLossChallenges.add(new Challenge("wl_7", "🥤", "Skip dessert today", 1, "tap"));
        weightLossChallenges.add(new Challenge("wl_8", "🧘", "15 minutes of HIIT training", 1, "tap"));
        weightLossChallenges.add(new Challenge("wl_9", "🍎", "Eat 5 fruits today", 1, "tap"));
        weightLossChallenges.add(new Challenge("wl_10", "⏰", "Intermittent fasting (16 hours)", 1, "tap"));
        weightLossChallenges.add(new Challenge("wl_11", "🚴", "30 minutes cycling", 1, "tap"));
        weightLossChallenges.add(new Challenge("wl_12", "🏃‍♀️", "Do 100 burpees throughout day", 1, "tap"));
        GOAL_CHALLENGES.put("weight loss", weightLossChallenges);

        // Muscle Gain Challenges
        List<Challenge> muscleGainChallenges = new ArrayList<>();
        muscleGainChallenges.add(new Challenge("mg_1", "💪", "Do 50 push-ups", 1, "tap"));
        muscleGainChallenges.add(new Challenge("mg_2", "🏋️", "Complete a strength workout", 1, "tap"));
        muscleGainChallenges.add(new Challenge("mg_3", "🥚", "Eat 150g of protein today", 1, "tap"));
        muscleGainChallenges.add(new Challenge("mg_4", "🦵", "Do 100 squats", 1, "tap"));
        muscleGainChallenges.add(new Challenge("mg_5", "💪", "Train your weakest muscle group", 1, "tap"));
        muscleGainChallenges.add(new Challenge("mg_6", "🏋️‍♂️", "Do 5 sets of deadlifts", 1, "tap"));
        muscleGainChallenges.add(new Challenge("mg_7", "🥩", "Consume 6 protein-rich meals", 1, "tap"));
        muscleGainChallenges.add(new Challenge("mg_8", "💪", "50 pull-ups throughout the day", 1, "tap"));
        muscleGainChallenges.add(new Challenge("mg_9", "🏋️", "Do 30 minutes of weight training", 1, "tap"));
        muscleGainChallenges.add(new Challenge("mg_10", "🦾", "100 bicep curls", 1, "tap"));
        muscleGainChallenges.add(new Challenge("mg_11", "😴", "Get 8+ hours of sleep for recovery", 1, "tap"));
        muscleGainChallenges.add(new Challenge("mg_12", "🏋️‍♀️", "Train with progressive overload", 1, "tap"));
        GOAL_CHALLENGES.put("muscle gain", muscleGainChallenges);

        // Endurance Challenges
        List<Challenge> enduranceChallenges = new ArrayList<>();
        enduranceChallenges.add(new Challenge("en_1", "🏃", "Run 5 kilometers", 1, "tap"));
        enduranceChallenges.add(new Challenge("en_2", "🚴", "Cycle for 45 minutes", 1, "tap"));
        enduranceChallenges.add(new Challenge("en_3", "🏊", "Swim 30 laps", 1, "tap"));
        enduranceChallenges.add(new Challenge("en_4", "🧘", "30 minutes of yoga", 1, "tap"));
        enduranceChallenges.add(new Challenge("en_5", "⛰️", "Climb stairs for 15 minutes", 1, "tap"));
        enduranceChallenges.add(new Challenge("en_6", "🏃‍♀️", "Do interval training for 25 minutes", 1, "tap"));
        enduranceChallenges.add(new Challenge("en_7", "🚶", "Walk 15,000 steps", 1, "tap"));
        enduranceChallenges.add(new Challenge("en_8", "🏋️", "Circuit training for 40 minutes", 1, "tap"));
        enduranceChallenges.add(new Challenge("en_9", "🏃", "Sprint intervals - 10 rounds", 1, "tap"));
        enduranceChallenges.add(new Challenge("en_10", "🧘‍♂️", "Hold plank for 3 minutes total", 1, "tap"));
        enduranceChallenges.add(new Challenge("en_11", "🏊‍♂️", "40 minutes continuous cardio", 1, "tap"));
        enduranceChallenges.add(new Challenge("en_12", "⏱️", "Beat your personal running time", 1, "tap"));
        GOAL_CHALLENGES.put("endurance", enduranceChallenges);

        // General Fitness Challenges
        List<Challenge> generalFitnessChallenges = new ArrayList<>();
        generalFitnessChallenges.add(new Challenge("gf_1", "🧘", "15 minutes of stretching", 1, "tap"));
        generalFitnessChallenges.add(new Challenge("gf_2", "💧", "Drink 8 glasses of water", 1, "tap"));
        generalFitnessChallenges.add(new Challenge("gf_3", "🚶", "Take 10,000 steps", 1, "tap"));
        generalFitnessChallenges.add(new Challenge("gf_4", "🥗", "Eat 5 servings of vegetables", 1, "tap"));
        generalFitnessChallenges.add(new Challenge("gf_5", "😴", "Get 7-8 hours of sleep", 1, "tap"));
        generalFitnessChallenges.add(new Challenge("gf_6", "🧘‍♀️", "Meditate for 10 minutes", 1, "tap"));
        generalFitnessChallenges.add(new Challenge("gf_7", "☀️", "Get 20 minutes of sunlight", 1, "tap"));
        generalFitnessChallenges.add(new Challenge("gf_8", "🏃", "Do 30 minutes of any exercise", 1, "tap"));
        generalFitnessChallenges.add(new Challenge("gf_9", "🚫", "No junk food today", 1, "tap"));
        generalFitnessChallenges.add(new Challenge("gf_10", "💪", "Do 30 push-ups and 50 squats", 1, "tap"));
        generalFitnessChallenges.add(new Challenge("gf_11", "🧊", "Take a cold shower", 1, "tap"));
        generalFitnessChallenges.add(new Challenge("gf_12", "📱", "Limit screen time to 2 hours", 1, "tap"));
        GOAL_CHALLENGES.put("general fitness", generalFitnessChallenges);
    }

    /**
     * Get 3 challenges for the day based on fitness goal
     */
    public static List<Challenge> getDailyChallenges(String fitnessGoal, int dayOfYear) {
        List<Challenge> result = new ArrayList<>();
        String goalKey = fitnessGoal != null ? fitnessGoal.toLowerCase() : "general fitness";

        List<Challenge> challenges = GOAL_CHALLENGES.get(goalKey);
        if (challenges == null) {
            challenges = GOAL_CHALLENGES.get("general fitness");
        }

        // Select 3 challenges based on day of year (consistent per day)
        int size = challenges.size();
        for (int i = 0; i < 3; i++) {
            int index = (dayOfYear + i * 7) % size; // Offset each challenge
            result.add(challenges.get(index));
        }

        return result;
    }

    /**
     * Get single challenge for backward compatibility
     */
    public static Challenge getChallengeForDay(int dayOfYear) {
        List<Challenge> generalChallenges = GOAL_CHALLENGES.get("general fitness");
        int index = dayOfYear % generalChallenges.size();
        return generalChallenges.get(index);
    }
}

