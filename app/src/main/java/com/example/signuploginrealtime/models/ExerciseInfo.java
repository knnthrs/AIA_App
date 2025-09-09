package com.example.signuploginrealtime.models;

import com.google.firebase.database.PropertyName;
import java.util.List;
import java.util.ArrayList;

public class ExerciseInfo {

    @PropertyName("exerciseId")
    private String exerciseId;

    @PropertyName("name")
    private String name;

    @PropertyName("bodyParts")
    private List<String> bodyParts;

    @PropertyName("equipments")
    private List<String> equipments;

    @PropertyName("equipments")  // fallback in case JSON uses singular
    private List<String> equipmentLegacy;

    @PropertyName("gifUrl")
    private String gifUrl;

    @PropertyName("targetMuscles")
    private List<String> targetMuscles;

    @PropertyName("secondaryMuscles")
    private List<String> secondaryMuscles;

    // ✅ Now it's a List, not a Map
    @PropertyName("instructions")
    private List<String> instructions;

    public ExerciseInfo() {
        // Default constructor required for Firebase
    }

    // --- Getters & Setters ---
    public String getExerciseId() { return exerciseId; }
    public void setExerciseId(String exerciseId) { this.exerciseId = exerciseId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getBodyParts() { return bodyParts; }
    public void setBodyParts(List<String> bodyParts) { this.bodyParts = bodyParts; }

    public List<String> getEquipments() {
        if (equipments != null && !equipments.isEmpty()) {
            return equipments;
        }
        if (equipmentLegacy != null && !equipmentLegacy.isEmpty()) {
            return equipmentLegacy;
        }
        return new ArrayList<>();
    }

    public void setEquipments(List<String> equipments) {
        this.equipments = equipments;
    }

    public void setEquipmentLegacy(List<String> equipmentLegacy) {
        this.equipmentLegacy = equipmentLegacy;
    }

    public String getGifUrl() { return gifUrl; }
    public void setGifUrl(String gifUrl) { this.gifUrl = gifUrl; }

    public List<String> getTargetMuscles() { return targetMuscles; }
    public void setTargetMuscles(List<String> targetMuscles) { this.targetMuscles = targetMuscles; }

    public List<String> getSecondaryMuscles() { return secondaryMuscles; }
    public void setSecondaryMuscles(List<String> secondaryMuscles) { this.secondaryMuscles = secondaryMuscles; }

    public List<String> getInstructions() {
        return instructions != null ? instructions : new ArrayList<>();
    }
    public void setInstructions(List<String> instructions) { this.instructions = instructions; }

    // Convenience methods
    public String getPrimaryBodyPart() {
        return (bodyParts != null && !bodyParts.isEmpty()) ? bodyParts.get(0) : null;
    }

    public String getPrimaryEquipment() {
        return (equipments != null && !equipments.isEmpty()) ? equipments.get(0) : null;
    }

    public String getPrimaryTargetMuscle() {
        return (targetMuscles != null && !targetMuscles.isEmpty()) ? targetMuscles.get(0) : null;
    }
}
