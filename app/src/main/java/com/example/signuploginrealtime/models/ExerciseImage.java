package com.example.signuploginrealtime.models;

import com.google.gson.annotations.SerializedName;

public class ExerciseImage {

    @SerializedName("id")
    private int id;

    @SerializedName("exercise")
    private int exerciseId;

    @SerializedName("image")
    private String imageUrl;

    @SerializedName("is_main")
    private boolean isMain;

    public int getId() { return id; }
    public int getExerciseId() { return exerciseId; }
    public String getImageUrl() { return imageUrl; }
    public boolean isMain() { return isMain; }
}
