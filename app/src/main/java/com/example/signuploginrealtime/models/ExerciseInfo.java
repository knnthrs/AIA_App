package com.example.signuploginrealtime.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ExerciseInfo {

    @SerializedName("id")
    private int id;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("category")
    private Integer category; // nullable

    @SerializedName("muscles")
    private List<Integer> muscles;

    @SerializedName("muscles_secondary")
    private List<Integer> musclesSecondary;

    @SerializedName("equipment")
    private List<Integer> equipment;

    @SerializedName("license_author")
    private String licenseAuthor;

    @SerializedName("variations")
    private Integer variations; // nullable

    @SerializedName("language")
    private Integer language; // optional


    // ✅ Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getCategory() { return category; }
    public void setCategory(int category) { this.category = category; }

    public List<Integer> getMuscles() { return muscles; }
    public void setMuscles(List<Integer> muscles) { this.muscles = muscles; }

    public List<Integer> getMusclesSecondary() { return musclesSecondary; }
    public void setMusclesSecondary(List<Integer> musclesSecondary) { this.musclesSecondary = musclesSecondary; }

    public List<Integer> getEquipment() { return equipment; }
    public void setEquipment(List<Integer> equipment) { this.equipment = equipment; }

    public String getLicenseAuthor() { return licenseAuthor; }
    public void setLicenseAuthor(String licenseAuthor) { this.licenseAuthor = licenseAuthor; }

    public int getVariations() { return variations; }
    public void setVariations(int variations) { this.variations = variations; }
}
