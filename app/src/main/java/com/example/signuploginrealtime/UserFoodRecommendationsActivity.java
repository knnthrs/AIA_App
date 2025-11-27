package com.example.signuploginrealtime;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.adapters.UserFoodAdapter;
import com.example.signuploginrealtime.models.FoodRecommendation;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserFoodRecommendationsActivity extends AppCompatActivity {

    // Food Recommendations
    private RecyclerView recyclerViewRecommendations;
    private UserFoodAdapter recommendationsAdapter;
    private List<FoodRecommendation> foodList;
    private List<FoodRecommendation> originalFoodList;
    private ProgressBar progressBar;
    private LinearLayout emptyState;

    // Common
    private ImageView btnBack;
    private TextView tvGoalInfo, tvRecommendationCount;

    private FirebaseFirestore db;
    private DatabaseReference realtimeDb;
    private String userId;
    private String coachId;
    private String fitnessGoal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_food_recommendations);

        initializeViews();
        loadUserProfile();
        setupRecyclerViews();
        setupListeners();
    }

    private void initializeViews() {
        recyclerViewRecommendations = findViewById(R.id.recyclerViewFoodRecommendations);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        tvRecommendationCount = findViewById(R.id.tvRecommendationCount);
        btnBack = findViewById(R.id.btnBack);
        tvGoalInfo = findViewById(R.id.tvGoalInfo);

        db = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance().getReference();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            userId = null;
        }
        foodList = new ArrayList<>();
        originalFoodList = new ArrayList<>();
    }

    private void loadUserProfile() {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        coachId = documentSnapshot.getString("coachId");
                        fitnessGoal = documentSnapshot.getString("fitnessGoal");

                        if (fitnessGoal == null) {
                            db.collection("users")
                                    .document(userId)
                                    .collection("fitnessProfile")
                                    .document("profile")
                                    .get()
                                    .addOnSuccessListener(profileDoc -> {
                                        if (profileDoc.exists()) {
                                            fitnessGoal = profileDoc.getString("fitnessGoal");
                                        }
                                        updateGoalInfoText();
                                        loadFoodRecommendations();
                                    })
                                    .addOnFailureListener(e -> {
                                        updateGoalInfoText();
                                        loadFoodRecommendations();
                                    });
                        } else {
                            updateGoalInfoText();
                            loadFoodRecommendations();
                        }
                    } else {
                        updateGoalInfoText();
                        loadFoodRecommendations();
                    }
                })
                .addOnFailureListener(e -> {
                    updateGoalInfoText();
                    loadFoodRecommendations();
                });
    }

    private void updateGoalInfoText() {
        if (tvGoalInfo == null) return;

        String message;
        if (fitnessGoal == null) {
            message = "Foods recommended by your coach and general nutrition database";
        } else {
            switch (fitnessGoal.toLowerCase()) {
                case "weight loss":
                    message = "🎯 Goal: Weight Loss\nShowing low-calorie foods (<250 cal) and high-protein options to help you lose weight";
                    break;
                case "muscle gain":
                case "muscle building":
                    message = "🎯 Goal: Muscle Gain\nShowing high-protein foods (≥12g protein) to build muscle mass";
                    break;
                case "general fitness":
                case "fitness":
                    message = "🎯 Goal: General Fitness\nShowing balanced nutrition and quality foods for overall health";
                    break;
                case "endurance":
                case "cardio":
                    message = "🎯 Goal: Endurance\nShowing carb-rich and lean protein foods for sustained energy";
                    break;
                case "strength training":
                case "powerlifting":
                    message = "🎯 Goal: Strength\nShowing protein-rich and calorie-dense foods for strength gains";
                    break;
                default:
                    message = "🎯 Goal: " + fitnessGoal + "\nFoods curated for your fitness goals from our 500+ food database";
            }
        }

        tvGoalInfo.setText(message);
    }

    private void updateGoalInfoWithResults(int goalMatches, int totalFoods) {
        if (tvGoalInfo == null) return;

        String baseMessage = tvGoalInfo.getText().toString();
        String resultsMessage = baseMessage + "\n\n✅ Found " + goalMatches + " perfect matches out of " + totalFoods + " foods shown";

        if (goalMatches > 0) {
            resultsMessage += "\n🎯 " + Math.round((float)goalMatches/totalFoods * 100) + "% match your " + fitnessGoal + " goal";
        }

        tvGoalInfo.setText(resultsMessage);
    }

    private void setupRecyclerViews() {
        recommendationsAdapter = new UserFoodAdapter(foodList, new UserFoodAdapter.OnFoodActionListener() {
            @Override
            public void onAddToMealPlan(FoodRecommendation food) {
                showMealTypeDialog(food);
            }

            @Override
            public void onViewDetails(FoodRecommendation food) {
                showFoodDetailsDialog(food);
            }
        });
        recyclerViewRecommendations.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRecommendations.setAdapter(recommendationsAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void updateCounts() {
        tvRecommendationCount.setText(foodList.size() + " items");
    }

    private void loadFoodRecommendations() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        foodList.clear();
        originalFoodList.clear();

        if (coachId != null) {
            db.collection("foods")
                    .whereEqualTo("coachId", coachId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            FoodRecommendation food = document.toObject(FoodRecommendation.class);
                            food.setId(document.getId());

                            Boolean isVerified = document.getBoolean("isVerified");
                            Boolean verified = document.getBoolean("verified");
                            boolean foodIsVerified = (isVerified != null && isVerified) || (verified != null && verified);

                            if (!foodIsVerified) continue;

                            boolean isPersonalized = userId != null && userId.equals(food.getUserId());
                            boolean isGeneral = food.getUserId() == null;

                            if (!(isPersonalized || isGeneral)) continue;
                            if (isAlreadyInList(food)) continue;

                            foodList.add(food);
                            originalFoodList.add(food);
                        }

                        loadGeneralRecommendations();
                    })
                    .addOnFailureListener(e -> loadGeneralRecommendations());
        } else {
            loadGeneralRecommendations();
        }
    }

    private boolean isAlreadyInList(FoodRecommendation candidate) {
        if (candidate == null || candidate.getName() == null) return false;
        String name = candidate.getName().trim().toLowerCase();
        String id = candidate.getId();

        for (FoodRecommendation existing : foodList) {
            if (existing == null) continue;
            if (id != null && id.equals(existing.getId())) return true;
            if (existing.getName() != null && existing.getName().trim().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private void loadGeneralRecommendations() {
        realtimeDb.child("foods").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    loadFromRealtimeDatabase(dataSnapshot);
                } else {
                    loadFromFirestore();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                loadFromFirestore();
            }
        });
    }

    private void loadFromRealtimeDatabase(DataSnapshot dataSnapshot) {
        int addedCount = 0;
        int goalMatchCount = 0;

        for (DataSnapshot foodSnapshot : dataSnapshot.getChildren()) {
            try {
                FoodRecommendation food = foodSnapshot.getValue(FoodRecommendation.class);
                if (food != null) {
                    food.setId(foodSnapshot.getKey());
                    Boolean isVerified = food.isVerified();
                    if (isVerified != null && !isVerified) continue;
                    if (isAlreadyInList(food)) continue;

                    boolean isGeneral = food.getUserId() == null;
                    if (isGeneral) {
                        boolean matchesGoal = (fitnessGoal == null || food.isGoodForGoal(fitnessGoal));
                        boolean isFromCoach = (coachId != null && coachId.equals(food.getCoachId()));

                        boolean shouldInclude = isFromCoach || matchesGoal ||
                                              (food.getProtein() >= 20) ||
                                              (food.getCalories() <= 50);

                        if (shouldInclude) {
                            foodList.add(food);
                            originalFoodList.add(food);
                            addedCount++;
                            if (matchesGoal) goalMatchCount++;
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("FoodRecommendations", "Error parsing food: " + e.getMessage());
            }
        }

        recommendationsAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
        updateCounts();

        if (foodList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
        } else {
            updateGoalInfoWithResults(goalMatchCount, addedCount);
        }
    }

    private void loadFromFirestore() {
        db.collection("foods")
                .limit(200)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int addedCount = 0;
                    int goalMatchCount = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        FoodRecommendation food = document.toObject(FoodRecommendation.class);
                        food.setId(document.getId());

                        Boolean isVerifiedField = document.getBoolean("isVerified");
                        Boolean verifiedField = document.getBoolean("verified");
                        boolean foodIsVerified = (isVerifiedField != null && isVerifiedField) || (verifiedField != null && verifiedField);

                        boolean isGeneral = food.getUserId() == null;

                        if (isGeneral && foodIsVerified) {
                            if (isAlreadyInList(food)) continue;

                            boolean matchesGoal = (fitnessGoal == null || food.isGoodForGoal(fitnessGoal));
                            boolean isFromCoach = (coachId != null && coachId.equals(food.getCoachId()));

                            boolean shouldInclude = isFromCoach || matchesGoal ||
                                                  (food.getProtein() >= 20) ||
                                                  (food.getCalories() <= 50);

                            if (shouldInclude) {
                                foodList.add(food);
                                originalFoodList.add(food);
                                addedCount++;
                                if (matchesGoal) goalMatchCount++;
                            }
                        }
                    }

                    recommendationsAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    updateCounts();

                    if (foodList.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                    } else {
                        updateGoalInfoWithResults(goalMatchCount, addedCount);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading foods", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                });
    }

    private void showMealTypeDialog(FoodRecommendation food) {
        String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snack"};

        new AlertDialog.Builder(this)
                .setTitle("Add to Meal Plan")
                .setItems(mealTypes, (dialog, which) -> addToMealPlan(food, mealTypes[which]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addToMealPlan(FoodRecommendation food, String mealType) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());

        db.collection("users")
                .document(userId)
                .collection("mealPlan")
                .whereEqualTo("date", today)
                .whereEqualTo("mealType", mealType)
                .whereEqualTo("foodName", food.getName())
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, food.getName() + " is already in your " + mealType + " plan", Toast.LENGTH_LONG).show();
                        return;
                    }

                    java.util.Map<String, Object> mealPlanData = new java.util.HashMap<>();
                    mealPlanData.put("userId", userId);
                    mealPlanData.put("foodId", food.getId());
                    mealPlanData.put("foodName", food.getName());
                    mealPlanData.put("calories", food.getCalories());
                    mealPlanData.put("protein", food.getProtein());
                    mealPlanData.put("carbs", food.getCarbs());
                    mealPlanData.put("fats", food.getFats());
                    mealPlanData.put("mealType", mealType);
                    mealPlanData.put("servingSize", food.getServingSize());
                    mealPlanData.put("addedAt", Timestamp.now());
                    mealPlanData.put("date", today);

                    db.collection("users")
                            .document(userId)
                            .collection("mealPlan")
                            .add(mealPlanData)
                            .addOnSuccessListener(documentReference -> Toast.makeText(this, "Added to " + mealType, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showFoodDetailsDialog(FoodRecommendation food) {
        String details = "Food: " + food.getName() + "\n\n" +
                "Calories: " + food.getCalories() + " kcal\n" +
                "Protein: " + food.getProtein() + "g (" + food.getProteinPercentage() + "%)\n" +
                "Carbs: " + food.getCarbs() + "g (" + food.getCarbsPercentage() + "%)\n" +
                "Fats: " + food.getFats() + "g (" + food.getFatsPercentage() + "%)\n\n" +
                "Serving Size: " + (food.getServingSize() != null ? food.getServingSize() : "N/A") + "\n\n";

        if (food.getNotes() != null && !food.getNotes().isEmpty()) {
            details += "Coach Notes:\n" + food.getNotes() + "\n\n";
        }

        if (food.getTags() != null && !food.getTags().isEmpty()) {
            details += "Tags: " + String.join(", ", food.getTags());
        }

        new AlertDialog.Builder(this)
                .setTitle("Food Details")
                .setMessage(details)
                .setPositiveButton("Add to Meal Plan", (dialog, which) -> showMealTypeDialog(food))
                .setNegativeButton("Close", null)
                .show();
    }
}

