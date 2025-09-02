package com.example.signuploginrealtime.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ExerciseInfo {
    @SerializedName("id")
    private int id;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("exercise")
    private Integer exerciseId; // some endpoints use this; keep if you need it

    @SerializedName("language")
    private int language;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("created")
    private String created;

    @SerializedName("license_author")
    private String licenseAuthor;

    // ✅ New fields from wger /exercise/
    @SerializedName("category")
    private Integer category; // can be null

    @SerializedName("muscles")
    private List<Integer> muscles;

    // There is also "muscles_secondary" in wger, add if needed
    // @SerializedName("muscles_secondary")
    // private List<Integer> musclesSecondary;

    @SerializedName("equipment")
    private List<Integer> equipment;

    public ExerciseInfo() {}

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Integer getExerciseId() { return exerciseId; }
    public void setExerciseId(Integer exerciseId) { this.exerciseId = exerciseId; }

    public int getLanguage() { return language; }
    public void setLanguage(int language) { this.language = language; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreated() { return created; }
    public void setCreated(String created) { this.created = created; }

    public String getLicenseAuthor() { return licenseAuthor; }
    public void setLicenseAuthor(String licenseAuthor) { this.licenseAuthor = licenseAuthor; }

    // ✅ New getters
    public Integer getCategory() { return category; }
    public void setCategory(Integer category) { this.category = category; }

    public List<Integer> getMuscles() { return muscles; }
    public void setMuscles(List<Integer> muscles) { this.muscles = muscles; }

    public List<Integer> getEquipment() { return equipment; }
    public void setEquipment(List<Integer> equipment) { this.equipment = equipment; }

    @Override
    public String toString() {
        return "ExerciseInfo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
