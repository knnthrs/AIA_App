package com.example.signuploginrealtime.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WgerExerciseResponse {
    @SerializedName("results")
    private List<ExerciseInfo> results; // Changed from WgerExercise to ExerciseInfo

    @SerializedName("count")
    private int count;

    @SerializedName("next")
    private String next;

    @SerializedName("previous")
    private String previous;

    public List<ExerciseInfo> getResults() {
        return results;
    }

    public void setResults(List<ExerciseInfo> results) {
        this.results = results;
    }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public String getNext() { return next; }
    public void setNext(String next) { this.next = next; }

    public String getPrevious() { return previous; }
    public void setPrevious(String previous) { this.previous = previous; }
}