package com.example.signuploginrealtime.models;

public class WorkoutExercise {
    private Exercise exercise;
    private int order;
    private int sets;
    private int reps;
    private int restSeconds;

    public WorkoutExercise() {}

    // Getters and Setters
    public Exercise getExercise() { return exercise; }
    public void setExercise(Exercise exercise) { this.exercise = exercise; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public int getSets() { return sets; }
    public void setSets(int sets) { this.sets = sets; }

    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }

    public int getRestSeconds() { return restSeconds; }
    public void setRestSeconds(int restSeconds) { this.restSeconds = restSeconds; }
}
