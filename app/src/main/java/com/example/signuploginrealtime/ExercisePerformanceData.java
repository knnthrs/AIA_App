package com.example.signuploginrealtime;

import java.io.Serializable;

public class ExercisePerformanceData implements Serializable {
    private String exerciseName;
    private int targetReps;
    private int actualReps;
    private int targetDurationSeconds;
    private int actualDurationSeconds;
    private String status; // <<< NEW FIELD
    // private double weight; // We can add this later

    public ExercisePerformanceData(String exerciseName, int targetReps, int actualReps, int targetDurationSeconds, int actualDurationSeconds, String status) {
        this.exerciseName = exerciseName;
        this.targetReps = targetReps;
        this.actualReps = actualReps;
        this.targetDurationSeconds = targetDurationSeconds;
        this.actualDurationSeconds = actualDurationSeconds;
        this.status = status; // <<< ASSIGN STATUS
    }

    // Getters
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

    public String getStatus() { // <<< NEW GETTER
        return status;
    }

    // Setters (optional, but can be useful)
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

    public void setStatus(String status) { // <<< NEW SETTER (optional but good to have)
        this.status = status;
    }

    @Override
    public String toString() {
        return "ExercisePerformanceData{" +
                "exerciseName='" + exerciseName + '\'' +
                ", targetReps=" + targetReps +
                ", actualReps=" + actualReps +
                ", targetDurationSeconds=" + targetDurationSeconds +
                ", actualDurationSeconds=" + actualDurationSeconds +
                ", status='" + status + '\'' + // <<< ADD STATUS TO TOSTRING
                '}';
    }
}
