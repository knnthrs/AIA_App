package com.example.signuploginrealtime.models;

import java.util.ArrayList;
import java.util.List;

public class Workout {
    private List<WorkoutExercise> exercises;
    private int totalDuration;

    // ✅ No-args constructor
    public Workout() {
        this.exercises = new ArrayList<>();
        this.totalDuration = 0;
    }

    // ✅ Existing constructor
    public Workout(List<WorkoutExercise> exercises, int totalDuration) {
        this.exercises = exercises;
        this.totalDuration = totalDuration;
    }

    public List<WorkoutExercise> getExercises() {
        return exercises;
    }

    public void setExercises(List<WorkoutExercise> exercises) {
        this.exercises = exercises;
        recalcDuration(); // auto update totalDuration
    }

    public int getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(int totalDuration) {
        this.totalDuration = totalDuration;
    }

    // ✅ Helper: recalc duration whenever exercises are set
    private void recalcDuration() {
        if (exercises == null || exercises.isEmpty()) {
            totalDuration = 0;
            return;
        }
        int duration = 0;
        for (WorkoutExercise we : exercises) {
            // Example formula: sets × reps × 5 sec + rest
            duration += (we.getSets() * we.getReps() * 5) + we.getRestSeconds();
        }
        this.totalDuration = duration / 60; // convert to minutes
    }
}
