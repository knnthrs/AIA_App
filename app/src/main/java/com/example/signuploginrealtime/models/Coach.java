package com.example.signuploginrealtime.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Coach implements Serializable {
    private String id;
    private String fullname;
    private String email;
    private String phoneNumber;
    private List<String> skills;
    private String specialization;
    private int yearsOfExperience;
    private String bio;
    private String profileImageUrl;

    public Coach() {
        this.skills = new ArrayList<>();
    }

    public Coach(String id, String fullname, String email) {
        this.id = id;
        this.fullname = fullname;
        this.email = email;
        this.skills = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public List<String> getSkills() {
        return skills != null ? skills : new ArrayList<>();
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(int yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    // Helper method to get skills as comma-separated string
    public String getSkillsAsString() {
        if (skills == null || skills.isEmpty()) {
            return "General Training";
        }
        return String.join(", ", skills);
    }
}

