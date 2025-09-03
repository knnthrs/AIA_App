package com.example.signuploginrealtime.models;

public class WorkoutExercise {
    private ExerciseInfo exerciseInfo;
    private int sets;
    private int reps;
    private int restSeconds;
    private int order;

    public ExerciseInfo getExerciseInfo() { return exerciseInfo; }
    public void setExerciseInfo(ExerciseInfo exerciseInfo) { this.exerciseInfo = exerciseInfo; }

    public int getSets() { return sets; }
    public void setSets(int sets) { this.sets = sets; }

    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }

    public int getRestSeconds() { return restSeconds; }
    public void setRestSeconds(int restSeconds) { this.restSeconds = restSeconds; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
}
