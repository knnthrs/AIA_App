package com.example.signuploginrealtime.models;

import com.google.gson.annotations.SerializedName;

public class WgerExercise {
    private int id;
    private String name;

    @SerializedName("description")
    private String descriptionHtml;

    public int getId() { return id; }
    public String getName() { return name; }

    public String getDescriptionHtml() {
        return descriptionHtml;
    }

    // Convert HTML from API → plain text
    public String getCleanDescription() {
        return descriptionHtml != null ? descriptionHtml.replaceAll("<.*?>", "") : "";
    }
}
