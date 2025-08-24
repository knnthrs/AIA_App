package com.example.signuploginrealtime.models;

import java.util.List;

public class Workout {
    private List<WorkoutExercise> exercises;
    private int duration;

    public Workout(List<WorkoutExercise> exercises, int duration) {
        this.exercises = exercises;
        this.duration = duration;
    }

    // Getters and Setters
    public List<WorkoutExercise> getExercises() { return exercises; }
    public void setExercises(List<WorkoutExercise> exercises) { this.exercises = exercises; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
}
