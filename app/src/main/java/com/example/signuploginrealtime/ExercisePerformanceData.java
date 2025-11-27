package com.example.signuploginrealtime;

import java.io.Serializable;

public class ExercisePerformanceData implements Serializable {

    private String exerciseName;
    private int targetReps;
    private int actualReps;
    private int targetDurationSeconds;
    private int actualDurationSeconds;
    private String status; // completed, skipped, partial
    private double weight; // weight used in kg (for weighted exercises)
    private String exerciseType; // cardio, strength, flexibility
    private int caloriesEstimate; // estimated calories for this exercise

    // Base constructor
    public ExercisePerformanceData(String exerciseName,
                                   int targetReps,
                                   int actualReps,
                                   int targetDurationSeconds,
                                   int actualDurationSeconds,
                                   String status) {
        this.exerciseName = exerciseName;
        this.targetReps = targetReps;
        this.actualReps = actualReps;
        this.targetDurationSeconds = targetDurationSeconds;
        this.actualDurationSeconds = actualDurationSeconds;
        this.status = status;
        this.weight = 0.0; // Default bodyweight
        this.exerciseType = "strength"; // Default type
        this.caloriesEstimate = 0;
    }

    // Enhanced constructor
    public ExercisePerformanceData(String exerciseName,
                                   int targetReps,
                                   int actualReps,
                                   int targetDurationSeconds,
                                   int actualDurationSeconds,
                                   String status,
                                   double weight,
                                   String exerciseType) {
        this(exerciseName, targetReps, actualReps, targetDurationSeconds, actualDurationSeconds, status);
        this.weight = weight;
        this.exerciseType = exerciseType;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public int getTargetReps() {
        return targetReps;
    }

    public int getActualReps() {
        return actualReps;
    }

    public int getTargetDurationSeconds() {
        return targetDurationSeconds;
    }

    public int getActualDurationSeconds() {
        return actualDurationSeconds;
    }

    public String getStatus() {
        return status;
    }

    public double getWeight() {
        return weight;
    }

    public String getExerciseType() {
        return exerciseType;
    }

    public int getCaloriesEstimate() {
        return caloriesEstimate;
    }

    // Setters (optional but useful for updating data)
    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public void setTargetReps(int targetReps) {
        this.targetReps = targetReps;
    }

    public void setActualReps(int actualReps) {
        this.actualReps = actualReps;
    }

    public void setTargetDurationSeconds(int targetDurationSeconds) {
        this.targetDurationSeconds = targetDurationSeconds;
    }

    public void setActualDurationSeconds(int actualDurationSeconds) {
        this.actualDurationSeconds = actualDurationSeconds;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void setExerciseType(String exerciseType) {
        this.exerciseType = exerciseType;
    }

    public void setCaloriesEstimate(int caloriesEstimate) {
        this.caloriesEstimate = caloriesEstimate;
    }

    @Override
    public String toString() {
        return "ExercisePerformanceData{" +
                "exerciseName='" + exerciseName + '\'' +
                ", targetReps=" + targetReps +
                ", actualReps=" + actualReps +
                ", targetDurationSeconds=" + targetDurationSeconds +
                ", actualDurationSeconds=" + actualDurationSeconds +
                ", status='" + status + '\'' +
                ", weight=" + weight +
                ", exerciseType='" + exerciseType + '\'' +
                ", caloriesEstimate=" + caloriesEstimate +
                '}';
    }
}
