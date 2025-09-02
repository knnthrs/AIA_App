package com.example.signuploginrealtime.models;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class UserProfile implements Serializable {
    private String fitnessGoal;
    private String fitnessLevel;
    private String gender;
    private int age;
    private double weight;
    private double height;

    // Enhanced fields for better personalization
    private List<String> healthIssues;
    private List<String> availableEquipment;
    private List<String> preferredMuscleGroups;
    private List<String> dislikedExercises;
    private int workoutDaysPerWeek;
    private int preferredWorkoutDuration; // in minutes
    private String workoutTimePreference; // morning, afternoon, evening
    private boolean hasGymAccess;
    private String experienceWithWeights; // none, basic, experienced

    public UserProfile() {
        this.healthIssues = new ArrayList<>();
        this.availableEquipment = new ArrayList<>();
        this.preferredMuscleGroups = new ArrayList<>();
        this.dislikedExercises = new ArrayList<>();
    }

    // BMI and health calculations
    public double calculateBMI() {
        if (height > 0) {
            double heightInMeters = height / 100.0; // assuming height in cm
            return weight / (heightInMeters * heightInMeters);
        }
        return 0.0;
    }

    public String getBMICategory() {
        double bmi = calculateBMI();
        if (bmi < 18.5) return "Underweight";
        else if (bmi < 25) return "Normal";
        else if (bmi < 30) return "Overweight";
        else return "Obese";
    }

    public boolean isOverweight() {
        return calculateBMI() >= 25;
    }

    public boolean isBeginner() {
        return "beginner".equalsIgnoreCase(fitnessLevel);
    }

    public boolean hasHealthIssue(String issue) {
        return healthIssues != null && healthIssues.contains(issue);
    }

    // Age-based recommendations
    public boolean needsLowImpactExercises() {
        return age > 50 || hasHealthIssue("Joint Problems") || hasHealthIssue("Knee Problems");
    }

    public int getRecommendedRestTime() {
        int baseRest = 60;
        if (age > 50) baseRest += 20;
        if (age > 65) baseRest += 30;
        if (hasHealthIssue("Heart Condition")) baseRest += 15;
        return baseRest;
    }

    // All getters and setters...
    public String getFitnessGoal() { return fitnessGoal; }
    public void setFitnessGoal(String fitnessGoal) { this.fitnessGoal = fitnessGoal; }

    public String getFitnessLevel() { return fitnessLevel; }
    public void setFitnessLevel(String fitnessLevel) { this.fitnessLevel = fitnessLevel; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }

    public List<String> getHealthIssues() { return healthIssues; }
    public void setHealthIssues(List<String> healthIssues) { this.healthIssues = healthIssues; }

    public List<String> getAvailableEquipment() { return availableEquipment; }
    public void setAvailableEquipment(List<String> availableEquipment) { this.availableEquipment = availableEquipment; }

    public List<String> getPreferredMuscleGroups() { return preferredMuscleGroups; }
    public void setPreferredMuscleGroups(List<String> preferredMuscleGroups) { this.preferredMuscleGroups = preferredMuscleGroups; }

    public List<String> getDislikedExercises() { return dislikedExercises; }
    public void setDislikedExercises(List<String> dislikedExercises) { this.dislikedExercises = dislikedExercises; }

    public int getWorkoutDaysPerWeek() { return workoutDaysPerWeek; }
    public void setWorkoutDaysPerWeek(int workoutDaysPerWeek) { this.workoutDaysPerWeek = workoutDaysPerWeek; }

    public int getPreferredWorkoutDuration() { return preferredWorkoutDuration; }
    public void setPreferredWorkoutDuration(int preferredWorkoutDuration) { this.preferredWorkoutDuration = preferredWorkoutDuration; }

    public String getWorkoutTimePreference() { return workoutTimePreference; }
    public void setWorkoutTimePreference(String workoutTimePreference) { this.workoutTimePreference = workoutTimePreference; }

    public boolean isHasGymAccess() { return hasGymAccess; }
    public void setHasGymAccess(boolean hasGymAccess) { this.hasGymAccess = hasGymAccess; }

    public String getExperienceWithWeights() { return experienceWithWeights; }
    public void setExperienceWithWeights(String experienceWithWeights) { this.experienceWithWeights = experienceWithWeights; }
}