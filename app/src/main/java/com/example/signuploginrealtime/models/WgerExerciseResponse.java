package com.example.signuploginrealtime.models;

import java.util.List;

public class WgerExerciseResponse {
    private int count;
    private String next;
    private String previous;
    private List<WgerExercise> results;

    public int getCount() { return count; }
    public String getNext() { return next; }
    public String getPrevious() { return previous; }
    public List<WgerExercise> getResults() { return results; }

    public void setCount(int count) { this.count = count; }
    public void setNext(String next) { this.next = next; }
    public void setPrevious(String previous) { this.previous = previous; }
    public void setResults(List<WgerExercise> results) { this.results = results; }
}
