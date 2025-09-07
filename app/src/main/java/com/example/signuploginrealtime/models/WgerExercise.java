package com.example.signuploginrealtime.models;

import java.util.List;

public class WgerExercise {

    private int id;
    private List<Translation> translations;

    // NEW: List of images
    private List<ExerciseImage> images;

    // getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public List<Translation> getTranslations() { return translations; }
    public void setTranslations(List<Translation> translations) { this.translations = translations; }

    public List<ExerciseImage> getImages() { return images; }
    public void setImages(List<ExerciseImage> images) { this.images = images; }

    // Inner class for translations
    public static class Translation {
        private int id;
        private int language;
        private String name;
        private String description;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getLanguage() { return language; }
        public void setLanguage(int language) { this.language = language; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    // NEW Inner class for images
    public static class ExerciseImage {
        private String image; // URL
        private int exercise; // Exercise ID

        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }

        public int getExercise() { return exercise; }
        public void setExercise(int exercise) { this.exercise = exercise; }
    }
}
