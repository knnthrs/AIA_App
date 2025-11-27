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

import com.example.signuploginrealtime.adapters.MealPlanAdapter;
import com.example.signuploginrealtime.adapters.UserFoodAdapter;
import com.example.signuploginrealtime.models.FoodRecommendation;
import com.example.signuploginrealtime.models.UserMealPlan;
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
    private List<FoodRecommendation> originalFoodList; // For search filtering
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private androidx.appcompat.widget.SearchView searchView;

    // Meal Plan
    private RecyclerView recyclerViewMealPlan;
    private MealPlanAdapter mealPlanAdapter;
    private List<UserMealPlan> mealPlanList;
    private LinearLayout emptyStateMealPlan;

    // Common
    private ImageView btnBack;
    private TextView tvGoalInfo, tvMealPlanCount, tvRecommendationCount;

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
        loadMealPlan();
    }

    private void initializeViews() {
        // Recommendations
        recyclerViewRecommendations = findViewById(R.id.recyclerViewFoodRecommendations);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        tvRecommendationCount = findViewById(R.id.tvRecommendationCount);
        searchView = findViewById(R.id.searchView);

        // Meal Plan
        recyclerViewMealPlan = findViewById(R.id.recyclerViewMealPlan);
        emptyStateMealPlan = findViewById(R.id.emptyStateMealPlan);
        tvMealPlanCount = findViewById(R.id.tvMealPlanCount);

        // Common
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
        mealPlanList = new ArrayList<>();
    }

    private void loadUserProfile() {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        coachId = documentSnapshot.getString("coachId");

                        // Try to get fitnessGoal from the document directly
                        fitnessGoal = documentSnapshot.getString("fitnessGoal");

                        // If not found, try from fitnessProfile subcollection
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
        android.util.Log.d("FoodRecommendations", "Updated goal info: " + fitnessGoal);
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
        // Setup Meal Plan RecyclerView
        mealPlanAdapter = new MealPlanAdapter(mealPlanList, new MealPlanAdapter.OnMealPlanActionListener() {
            @Override
            public void onRemoveFromMealPlan(UserMealPlan mealPlan) {
                removeFromMealPlan(mealPlan);
            }
        });
        recyclerViewMealPlan.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMealPlan.setAdapter(mealPlanAdapter);

        // Setup Food Recommendations RecyclerView
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

        // Setup search functionality
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterFoods(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterFoods(newText);
                return false;
            }
        });
    }

    private void filterFoods(String query) {
        if (query == null || query.trim().isEmpty()) {
            // Show all foods
            foodList.clear();
            foodList.addAll(originalFoodList);
        } else {
            // Filter based on query
            String lowerQuery = query.toLowerCase().trim();
            foodList.clear();
            for (FoodRecommendation food : originalFoodList) {
                if (food.getName().toLowerCase().contains(lowerQuery) ||
                    (food.getTags() != null && food.getTags().toString().toLowerCase().contains(lowerQuery))) {
                    foodList.add(food);
                }
            }
        }
        recommendationsAdapter.notifyDataSetChanged();
        updateCounts();

        if (foodList.isEmpty() && !originalFoodList.isEmpty()) {
            // Search returned no results
            emptyState.setVisibility(View.VISIBLE);
        } else if (foodList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    private void updateCounts() {
        tvMealPlanCount.setText(mealPlanList.size() + " items");
        tvRecommendationCount.setText(foodList.size() + " items");
    }

    private void loadFoodRecommendations() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        foodList.clear();
        originalFoodList.clear();

        android.util.Log.d("FoodRecommendations", "Loading foods for userId: " + userId + ", coachId: " + coachId);

        // Load coach-recommended foods (both personalized and general from THIS coach)
        if (coachId != null) {
            android.util.Log.d("FoodRecommendations", "Querying coach foods...");
            db.collection("foods")
                    .whereEqualTo("coachId", coachId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        android.util.Log.d("FoodRecommendations", "Coach query returned " + queryDocumentSnapshots.size() + " foods");

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            FoodRecommendation food = document.toObject(FoodRecommendation.class);
                            food.setId(document.getId());

                            Boolean isVerified = document.getBoolean("isVerified");
                            Boolean verified = document.getBoolean("verified");
                            boolean foodIsVerified = (isVerified != null && isVerified) || (verified != null && verified);

                            if (!foodIsVerified) {
                                android.util.Log.d("FoodRecommendations", "Skipped unverified coach food: " + food.getName());
                                continue;
                            }

                            boolean isPersonalized = userId != null && userId.equals(food.getUserId());
                            boolean isGeneral = food.getUserId() == null;

                            if (!(isPersonalized || isGeneral)) {
                                android.util.Log.d("FoodRecommendations", "Skipped coach food for different user: " + food.getName());
                                continue;
                            }

                            // Avoid duplicates when later loading from DB/Firestore
                            if (isAlreadyInList(food)) {
                                android.util.Log.d("FoodRecommendations", "Coach food already in list (skipping duplicate): " + food.getName());
                                continue;
                            }

                            foodList.add(food);
                            originalFoodList.add(food);
                            String type = isPersonalized ? "personalized" : "general";
                            android.util.Log.d("FoodRecommendations", "Added " + type + " coach food: " + food.getName());
                        }

                        loadGeneralRecommendations();
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("FoodRecommendations", "Failed to load coach foods", e);
                        loadGeneralRecommendations();
                    });
        } else {
            android.util.Log.d("FoodRecommendations", "No coach assigned, loading general only");
            loadGeneralRecommendations();
        }
    }

    /**
     * Helper to check if a food with the same ID or name is already in the list
     */
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
        android.util.Log.d("FoodRecommendations", "Loading general recommendations...");
        
        // First try to load from Realtime Database (where our 500 foods are)
        realtimeDb.child("foods").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    android.util.Log.d("FoodRecommendations", "Loading from Realtime DB: " + dataSnapshot.getChildrenCount() + " foods found");
                    loadFromRealtimeDatabase(dataSnapshot);
                } else {
                    android.util.Log.d("FoodRecommendations", "No foods in Realtime DB, trying Firestore...");
                    loadFromFirestore(); // Fallback to Firestore
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                android.util.Log.e("FoodRecommendations", "Failed to load from Realtime DB: " + databaseError.getMessage());
                loadFromFirestore(); // Fallback to Firestore
            }
        });
    }

    private void loadFromRealtimeDatabase(DataSnapshot dataSnapshot) {
        int addedCount = 0;
        int filteredCount = 0;
        int goalMatchCount = 0;
        int duplicateCount = 0;

        for (DataSnapshot foodSnapshot : dataSnapshot.getChildren()) {
            try {
                FoodRecommendation food = foodSnapshot.getValue(FoodRecommendation.class);
                if (food != null) {
                    food.setId(foodSnapshot.getKey());
                    Boolean isVerified = food.isVerified();
                    if (isVerified != null && !isVerified) {
                        continue; // Skip unverified foods
                    }

                    // Check duplicates against anything already loaded (coach foods + others)
                    if (isAlreadyInList(food)) {
                        duplicateCount++;
                        android.util.Log.d("FoodRecommendations", "Skipped duplicate (RTDB): " + food.getName());
                        continue;
                    }

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
                        } else {
                            filteredCount++;
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("FoodRecommendations", "Error parsing food from Realtime DB: " + e.getMessage());
            }
        }

        android.util.Log.d("FoodRecommendations", "RTDB: added=" + addedCount + ", filtered=" + filteredCount + ", duplicates=" + duplicateCount);
        recommendationsAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
        updateCounts();

        if (foodList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            android.util.Log.w("FoodRecommendations", "No foods to display!");
        } else {
            updateGoalInfoWithResults(goalMatchCount, addedCount);
        }
    }

    private void loadFromFirestore() {
        android.util.Log.d("FoodRecommendations", "Loading from Firestore...");
        db.collection("foods")
                .limit(200)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int addedCount = 0;
                    int filteredCount = 0;
                    int goalMatchCount = 0;
                    int duplicateCount = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        FoodRecommendation food = document.toObject(FoodRecommendation.class);
                        food.setId(document.getId());

                        Boolean isVerifiedField = document.getBoolean("isVerified");
                        Boolean verifiedField = document.getBoolean("verified");
                        boolean foodIsVerified = (isVerifiedField != null && isVerifiedField) || (verifiedField != null && verifiedField);

                        boolean isGeneral = food.getUserId() == null;

                        if (isGeneral && foodIsVerified) {
                            // Check for duplicates across coach + RTDB + Firestore
                            if (isAlreadyInList(food)) {
                                duplicateCount++;
                                android.util.Log.d("FoodRecommendations", "Skipped duplicate (FS): " + food.getName());
                                continue;
                            }

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
                            } else {
                                filteredCount++;
                            }
                        }
                    }

                    android.util.Log.d("FoodRecommendations", "FS: added=" + addedCount + ", filtered=" + filteredCount + ", duplicates=" + duplicateCount);
                    recommendationsAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    updateCounts();

                    if (foodList.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        android.util.Log.w("FoodRecommendations", "No foods to display!");
                    } else {
                        updateGoalInfoWithResults(goalMatchCount, addedCount);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FoodRecommendations", "Failed to load from Firestore", e);
                    Toast.makeText(this, "Error loading foods", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                });
    }

    private void showMealTypeDialog(FoodRecommendation food) {
        String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snack"};

        new AlertDialog.Builder(this)
                .setTitle("Add to Meal Plan")
                .setItems(mealTypes, (dialog, which) -> {
                    addToMealPlan(food, mealTypes[which]); // Keep original case (Breakfast, Lunch, Dinner, Snack)
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addToMealPlan(FoodRecommendation food, String mealType) {
        // Format date as yyyy-MM-dd
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());

        android.util.Log.d("MealPlanAdd", "=== CHECKING FOR DUPLICATES ===");
        android.util.Log.d("MealPlanAdd", "Food: " + food.getName() + ", MealType: " + mealType + ", Date: " + today);

        // Check if this food already exists in the meal plan for this date and meal type
        db.collection("users")
                .document(userId)
                .collection("mealPlan")
                .whereEqualTo("date", today)
                .whereEqualTo("mealType", mealType)
                .whereEqualTo("foodName", food.getName()) // Check by name since foodId might be null
                .limit(1) // Only need to know if at least one exists
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Duplicate found
                        android.util.Log.w("MealPlanAdd", "⚠️ Duplicate found! Food already in " + mealType);
                        Toast.makeText(this, food.getName() + " is already in your " + mealType + " plan", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // No duplicate, proceed to add
                    android.util.Log.d("MealPlanAdd", "✅ No duplicate, adding food...");

                    UserMealPlan mealPlan = new UserMealPlan();
                    mealPlan.setUserId(userId);
                    mealPlan.setFoodId(food.getId());
                    mealPlan.setFoodName(food.getName());
                    mealPlan.setCalories(food.getCalories());
                    mealPlan.setProtein(food.getProtein());
                    mealPlan.setCarbs(food.getCarbs());
                    mealPlan.setFats(food.getFats());
                    mealPlan.setMealType(mealType);
                    mealPlan.setServingSize(food.getServingSize());
                    mealPlan.setAddedAt(Timestamp.now());
                    mealPlan.setDate(today);

                    db.collection("users")
                            .document(userId)
                            .collection("mealPlan")
                            .add(mealPlan)
                            .addOnSuccessListener(documentReference -> {
                                android.util.Log.d("MealPlanAdd", "✅ Successfully added! Doc ID: " + documentReference.getId());
                                Toast.makeText(this, "Added to " + mealType, Toast.LENGTH_SHORT).show();
                                // Reload the meal plan to show the newly added item
                                loadMealPlan();
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("MealPlanAdd", "❌ Failed to add to meal plan", e);
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MealPlanAdd", "❌ Failed to check for duplicates", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMealPlan() {
        // Format date as yyyy-MM-dd
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());

        android.util.Log.d("MealPlan", "Loading meal plan for date: " + today);

        db.collection("users")
                .document(userId)
                .collection("mealPlan")
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    mealPlanList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        UserMealPlan mealPlan = document.toObject(UserMealPlan.class);
                        mealPlan.setId(document.getId());
                        mealPlanList.add(mealPlan);
                        android.util.Log.d("MealPlan", "Loaded meal: " + mealPlan.getFoodName() + " - " + mealPlan.getMealType());
                    }

                    mealPlanAdapter.notifyDataSetChanged();
                    updateCounts();

                    // Show/hide empty state
                    if (mealPlanList.isEmpty()) {
                        emptyStateMealPlan.setVisibility(View.VISIBLE);
                        recyclerViewMealPlan.setVisibility(View.GONE);
                    } else {
                        emptyStateMealPlan.setVisibility(View.GONE);
                        recyclerViewMealPlan.setVisibility(View.VISIBLE);
                    }

                    android.util.Log.d("MealPlan", "Total meals loaded: " + mealPlanList.size());
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MealPlan", "Failed to load meal plan", e);
                    Toast.makeText(this, "Error loading meal plan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void removeFromMealPlan(UserMealPlan mealPlan) {
        new AlertDialog.Builder(this)
                .setTitle("Remove from Meal Plan")
                .setMessage("Remove " + mealPlan.getFoodName() + " from your meal plan?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    db.collection("users")
                            .document(userId)
                            .collection("mealPlan")
                            .document(mealPlan.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                mealPlanList.remove(mealPlan);
                                mealPlanAdapter.notifyDataSetChanged();
                                updateCounts();

                                // Show/hide empty state
                                if (mealPlanList.isEmpty()) {
                                    emptyStateMealPlan.setVisibility(View.VISIBLE);
                                    recyclerViewMealPlan.setVisibility(View.GONE);
                                } else {
                                    emptyStateMealPlan.setVisibility(View.GONE);
                                    recyclerViewMealPlan.setVisibility(View.VISIBLE);
                                }

                                Toast.makeText(this, "Removed from meal plan", Toast.LENGTH_SHORT).show();
                                android.util.Log.d("MealPlan", "Successfully removed: " + mealPlan.getFoodName());
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                android.util.Log.e("MealPlan", "Failed to remove meal", e);
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
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
