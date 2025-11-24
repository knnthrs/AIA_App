package com.example.signuploginrealtime.models;

import java.util.List;

public class WorkoutHistory {
    private String workoutId;
    private long timestamp;
    private int duration; // in minutes
    private int exercisesCount;
    private int caloriesBurned;
    private double weight; // in kg
    private double height; // in cm
    private double bmi;
    private List<String> bodyFocus;
    private List<WorkoutExercise> exercises;
    private String fitnessGoal;
    private String fitnessLevel;

    public WorkoutHistory() {
        // Required empty constructor for Firestore
    }

    public WorkoutHistory(String workoutId, long timestamp, int duration, int exercisesCount,
                          int caloriesBurned, double weight, double height, double bmi,
                          List<String> bodyFocus, List<WorkoutExercise> exercises,
                          String fitnessGoal, String fitnessLevel) {
        this.workoutId = workoutId;
        this.timestamp = timestamp;
        this.duration = duration;
        this.exercisesCount = exercisesCount;
        this.caloriesBurned = caloriesBurned;
        this.weight = weight;
        this.height = height;
        this.bmi = bmi;
        this.bodyFocus = bodyFocus;
        this.exercises = exercises;
        this.fitnessGoal = fitnessGoal;
        this.fitnessLevel = fitnessLevel;
    }

    // Getters and Setters
    public String getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(String workoutId) {
        this.workoutId = workoutId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getExercisesCount() {
        return exercisesCount;
    }

    public void setExercisesCount(int exercisesCount) {
        this.exercisesCount = exercisesCount;
    }

    public int getCaloriesBurned() {
        return caloriesBurned;
    }

    public void setCaloriesBurned(int caloriesBurned) {
        this.caloriesBurned = caloriesBurned;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getBmi() {
        return bmi;
    }

    public void setBmi(double bmi) {
        this.bmi = bmi;
    }

    public List<String> getBodyFocus() {
        return bodyFocus;
    }

    public void setBodyFocus(List<String> bodyFocus) {
        this.bodyFocus = bodyFocus;
    }

    public List<WorkoutExercise> getExercises() {
        return exercises;
    }

    public void setExercises(List<WorkoutExercise> exercises) {
        this.exercises = exercises;
    }

    public String getFitnessGoal() {
        return fitnessGoal;
    }

    public void setFitnessGoal(String fitnessGoal) {
        this.fitnessGoal = fitnessGoal;
    }

    public String getFitnessLevel() {
        return fitnessLevel;
    }

    public void setFitnessLevel(String fitnessLevel) {
        this.fitnessLevel = fitnessLevel;
    }

    // Utility method to calculate BMI
    public static double calculateBMI(double weight, double height) {
        if (height <= 0) return 0;
        double heightInMeters = height / 100.0;
        return weight / (heightInMeters * heightInMeters);
    }

    // Utility method to get BMI category
    public static String getBMICategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        else if (bmi < 25) return "Normal";
        else if (bmi < 30) return "Overweight";
        else return "Obese";
    }
}

