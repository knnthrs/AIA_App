package com.example.signuploginrealtime.models;

public class Translation {
    private int language;    // 2 = English
    private String name;
    private String description;

    public int getLanguage() { return language; }
    public String getName() { return name; }
    public String getDescription() { return description; }

    public void setLanguage(int language) { this.language = language; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
}
