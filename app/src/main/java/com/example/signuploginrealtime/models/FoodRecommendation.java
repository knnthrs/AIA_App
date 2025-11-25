package com.example.signuploginrealtime.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FoodRecommendation implements Serializable {
    private String id;
    private String name;
    private int calories;
    private double protein; // grams
    private double carbs; // grams
    private double fats; // grams
    private List<String> tags; // e.g., "protein", "vegan", "keto"
    private String coachId; // null if from base database
    private String userId; // null if general recommendation, specific if personalized
    private String notes; // Coach's recommendation reason

    @PropertyName("isVerified")
    private boolean isVerified; // Admin approval

    private Timestamp createdAt;
    private String source; // "USDA" or "Coach"
    private String servingSize; // e.g., "100g", "1 cup"

    public FoodRecommendation() {
        this.tags = new ArrayList<>();
    }

    public FoodRecommendation(String name, int calories, double protein, double carbs, double fats) {
        this.name = name;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
        this.tags = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public double getProtein() {
        return protein;
    }

    public void setProtein(double protein) {
        this.protein = protein;
    }

    public double getCarbs() {
        return carbs;
    }

    public void setCarbs(double carbs) {
        this.carbs = carbs;
    }

    public double getFats() {
        return fats;
    }

    public void setFats(double fats) {
        this.fats = fats;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getCoachId() {
        return coachId;
    }

    public void setCoachId(String coachId) {
        this.coachId = coachId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @PropertyName("isVerified")
    public boolean isVerified() {
        return isVerified;
    }

    @PropertyName("isVerified")
    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getServingSize() {
        return servingSize;
    }

    public void setServingSize(String servingSize) {
        this.servingSize = servingSize;
    }

    // Enhanced helper method to calculate if food fits user's goal
    public boolean isGoodForGoal(String goal) {
        if (goal == null) return true;

        switch (goal.toLowerCase()) {
            case "weight loss":
                // More comprehensive weight loss criteria
                return calories < 250 &&
                       (protein >= 8 || // High protein OR
                        (calories < 100 && fats < 5) || // Very low calorie OR
                        (protein >= 5 && carbs <= 15)); // Moderate protein, low carb

            case "muscle gain":
                // Enhanced muscle gain criteria
                return protein >= 12 && // Lower threshold for more variety
                       (calories >= 80 || // Not too low calorie OR
                        protein >= 20); // Very high protein

            case "muscle building":
                // Alternative spelling
                return protein >= 12 && calories >= 80;

            case "general fitness":
            case "fitness":
                // More inclusive balanced nutrition
                return (calories < 350 && protein >= 5) || // Balanced OR
                       (protein >= 15) || // High protein OR
                       (calories < 200); // Low calorie

            case "endurance":
            case "cardio":
                // Good for endurance athletes
                return (carbs >= 15 && calories < 300) || // Good carbs OR
                       (protein >= 10 && calories < 250); // Lean protein

            case "strength training":
            case "powerlifting":
                // For strength athletes
                return protein >= 12 || calories >= 200; // Protein or calorie dense

            default:
                return true; // Show all foods for unknown goals
        }
    }

    // Calculate macros percentage
    public int getProteinPercentage() {
        double totalCals = (protein * 4) + (carbs * 4) + (fats * 9);
        if (totalCals == 0) return 0;
        return (int) ((protein * 4 / totalCals) * 100);
    }

    public int getCarbsPercentage() {
        double totalCals = (protein * 4) + (carbs * 4) + (fats * 9);
        if (totalCals == 0) return 0;
        return (int) ((carbs * 4 / totalCals) * 100);
    }

    public int getFatsPercentage() {
        double totalCals = (protein * 4) + (carbs * 4) + (fats * 9);
        if (totalCals == 0) return 0;
        return (int) ((fats * 9 / totalCals) * 100);
    }
}

