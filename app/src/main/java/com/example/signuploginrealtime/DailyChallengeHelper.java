package com.example.signuploginrealtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DailyChallengeHelper {

    public static class Challenge {
        public String emoji;
        public String description;
        public int targetCount;
        public String actionType; // "tap", "timer", "count"

        public Challenge(String emoji, String description, int targetCount, String actionType) {
            this.emoji = emoji;
            this.description = description;
            this.targetCount = targetCount;
            this.actionType = actionType;
        }
    }

    private static final List<Challenge> AVAILABLE_CHALLENGES = new ArrayList<>();

    static {
        // Simple tap/action challenges
        AVAILABLE_CHALLENGES.add(new Challenge("💧", "Drink 8 glasses of water", 8, "count"));
        AVAILABLE_CHALLENGES.add(new Challenge("🚶", "Take 10,000 steps today", 10000, "count"));
        AVAILABLE_CHALLENGES.add(new Challenge("🧘", "Do 10 minutes of stretching", 10, "timer"));
        AVAILABLE_CHALLENGES.add(new Challenge("🏃", "Run or walk for 30 minutes", 30, "timer"));
        AVAILABLE_CHALLENGES.add(new Challenge("🥗", "Eat 5 servings of fruits/vegetables", 5, "count"));
        AVAILABLE_CHALLENGES.add(new Challenge("😴", "Get 8 hours of sleep", 8, "count"));
        AVAILABLE_CHALLENGES.add(new Challenge("📱", "Spend less than 2 hours on phone", 2, "count"));
        AVAILABLE_CHALLENGES.add(new Challenge("🧘‍♀️", "Meditate for 15 minutes", 15, "timer"));
        AVAILABLE_CHALLENGES.add(new Challenge("💪", "Do 50 push-ups (any style)", 50, "count"));
        AVAILABLE_CHALLENGES.add(new Challenge("🦵", "Do 100 squats throughout the day", 100, "count"));
        AVAILABLE_CHALLENGES.add(new Challenge("🧊", "Take a cold shower", 1, "tap"));
        AVAILABLE_CHALLENGES.add(new Challenge("📖", "Read for 30 minutes", 30, "timer"));
        AVAILABLE_CHALLENGES.add(new Challenge("🎯", "Complete 3 small tasks", 3, "count"));
        AVAILABLE_CHALLENGES.add(new Challenge("🌅", "Wake up before 7 AM", 1, "tap"));
        AVAILABLE_CHALLENGES.add(new Challenge("🥤", "Avoid sugary drinks", 1, "tap"));
        AVAILABLE_CHALLENGES.add(new Challenge("🚫", "No junk food today", 1, "tap"));
        AVAILABLE_CHALLENGES.add(new Challenge("🏋️", "Do planks for 2 minutes total", 2, "timer"));
        AVAILABLE_CHALLENGES.add(new Challenge("🌟", "Do 20 jumping jacks", 20, "count"));
        AVAILABLE_CHALLENGES.add(new Challenge("🎨", "Spend 15 minutes on a hobby", 15, "timer"));
        AVAILABLE_CHALLENGES.add(new Challenge("☀️", "Get 15 minutes of sunlight", 15, "timer"));
    }

    public static Challenge getRandomChallenge() {
        Random random = new Random();
        int index = random.nextInt(AVAILABLE_CHALLENGES.size());
        return AVAILABLE_CHALLENGES.get(index);
    }

    public static Challenge getChallengeForDay(int dayOfYear) {
        // Consistent challenge for each day of the year
        int index = dayOfYear % AVAILABLE_CHALLENGES.size();
        return AVAILABLE_CHALLENGES.get(index);
    }
}

