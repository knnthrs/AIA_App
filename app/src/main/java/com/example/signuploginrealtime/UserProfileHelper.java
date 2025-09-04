package com.example.signuploginrealtime;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.List;

// CREATE THIS AS A NEW FILE - UserProfileHelper.java

public class UserProfileHelper {

    public interface ProfileCheckListener {
        void onProfileExists(UserProfile profile);
        void onProfileNotExists();
        void onError(String error);
    }

    public static void checkUserProfile(ProfileCheckListener listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference profileRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                    .child("profile");

            profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        try {
                            UserProfile profile = dataSnapshot.getValue(UserProfile.class);
                            if (profile != null && profile.isProfileCompleted()) {
                                listener.onProfileExists(profile);
                            } else {
                                listener.onProfileNotExists();
                            }
                        } catch (Exception e) {
                            listener.onError("Error parsing profile data: " + e.getMessage());
                        }
                    } else {
                        listener.onProfileNotExists();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    listener.onError("Database error: " + databaseError.getMessage());
                }
            });
        } else {
            listener.onError("User not authenticated");
        }
    }

    // User Profile data class
    public static class UserProfile implements Serializable {
        private String gender;
        private int age;
        private float height;
        private float weight;
        private String fitnessLevel;
        private String fitnessGoal;
        private List<String> healthIssues;
        private boolean profileCompleted;
        private long lastUpdated;
        private float bmi;

        // Default constructor required for Firebase
        public UserProfile() {}

        public UserProfile(String gender, int age, float height, float weight,
                           String fitnessLevel, String fitnessGoal, List<String> healthIssues,
                           boolean profileCompleted, long lastUpdated, float bmi) {
            this.gender = gender;
            this.age = age;
            this.height = height;
            this.weight = weight;
            this.fitnessLevel = fitnessLevel;
            this.fitnessGoal = fitnessGoal;
            this.healthIssues = healthIssues;
            this.profileCompleted = profileCompleted;
            this.lastUpdated = lastUpdated;
            this.bmi = bmi;
        }

        // Getters and setters
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }

        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }

        public float getHeight() { return height; }
        public void setHeight(float height) { this.height = height; }

        public float getWeight() { return weight; }
        public void setWeight(float weight) { this.weight = weight; }

        public String getFitnessLevel() { return fitnessLevel; }
        public void setFitnessLevel(String fitnessLevel) { this.fitnessLevel = fitnessLevel; }

        public String getFitnessGoal() { return fitnessGoal; }
        public void setFitnessGoal(String fitnessGoal) { this.fitnessGoal = fitnessGoal; }

        public List<String> getHealthIssues() { return healthIssues; }
        public void setHealthIssues(List<String> healthIssues) { this.healthIssues = healthIssues; }

        public boolean isProfileCompleted() { return profileCompleted; }
        public void setProfileCompleted(boolean profileCompleted) { this.profileCompleted = profileCompleted; }

        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

        public float getBmi() { return bmi; }
        public void setBmi(float bmi) { this.bmi = bmi; }
    }
}
