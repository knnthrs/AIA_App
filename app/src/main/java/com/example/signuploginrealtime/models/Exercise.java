package com.example.signuploginrealtime.models;

public class Exercise {
    private int id;
    private String name;
    private String description;
    private int category;
    private String[] muscles;
    private String[] equipment;

    // Constructors
    public Exercise() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getCategory() { return category; }
    public void setCategory(int category) { this.category = category; }

    public String[] getMuscles() { return muscles; }
    public void setMuscles(String[] muscles) { this.muscles = muscles; }

    public String[] getEquipment() { return equipment; }
    public void setEquipment(String[] equipment) { this.equipment = equipment; }
}