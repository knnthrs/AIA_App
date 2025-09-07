package com.example.signuploginrealtime.models;

import java.util.List;

public class ApiResponse<T> {
    private int count;
    private String next;
    private String previous;
    private List<T> results;

    // Getters
    public int getCount() {
        return count;
    }

    public String getNext() {
        return next;
    }

    public String getPrevious() {
        return previous;
    }

    public List<T> getResults() {
        return results;
    }
}
