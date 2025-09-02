package com.example.signuploginrealtime.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WgerExerciseResponse {
    @SerializedName("results")
    private List<WgerExercise> results;

    public List<WgerExercise> getResults() {
        return results;
    }
}
