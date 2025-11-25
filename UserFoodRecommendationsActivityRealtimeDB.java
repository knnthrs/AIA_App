package com.example.signuploginrealtime;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserFoodRecommendationsActivityRealtimeDB extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UserFoodAdapter adapter;
    private List<FoodRecommendation> foodList;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private ImageView btnBack;
    private TextView tvGoalInfo;

    private DatabaseReference database; // Realtime Database
    private FirebaseFirestore firestore; // Still need Firestore for user data
    private String userId;
    private String coachId;
    private String fitnessGoal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_food_recommendations);

        initializeViews();
        loadUserProfile();
        setupRecyclerView();
        setupListeners();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewFoodRecommendations);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        btnBack = findViewById(R.id.btnBack);
        tvGoalInfo = findViewById(R.id.tvGoalInfo);

        // Initialize both databases
        database = FirebaseDatabase.getInstance().getReference();
        firestore = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        foodList = new ArrayList<>();
    }

    private void loadUserProfile() {
        // Still use Firestore for user profile data
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        coachId = documentSnapshot.getString("coachId");
                        fitnessGoal = documentSnapshot.getString("fitnessGoal");

                        if (fitnessGoal == null) {
                            // Try fitness profile subcollection
                            firestore.collection("users")
                                    .document(userId)
                                    .collection("fitnessProfile")
                                    .document("profile")
                                    .get()
                                    .addOnSuccessListener(profileDoc -> {
                                        if (profileDoc.exists()) {
                                            fitnessGoal = profileDoc.getString("fitnessGoal");
                                        }
                                        updateGoalInfoText();
                                        loadFoodRecommendationsFromRealtimeDB();
                                    })
                                    .addOnFailureListener(e -> {
                                        updateGoalInfoText();
                                        loadFoodRecommendationsFromRealtimeDB();
                                    });
                        } else {
                            updateGoalInfoText();
                            loadFoodRecommendationsFromRealtimeDB();
                        }
                    } else {
                        updateGoalInfoText();
                        loadFoodRecommendationsFromRealtimeDB();
                    }
                })
                .addOnFailureListener(e -> {
                    updateGoalInfoText();
                    loadFoodRecommendationsFromRealtimeDB();
                });
    }

    private void loadFoodRecommendationsFromRealtimeDB() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        foodList.clear();

        android.util.Log.d("FoodRecommendations", "Loading foods from Realtime DB for userId: " + userId + ", goal: " + fitnessGoal);

        // Load foods from Realtime Database
        database.child("foods").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                android.util.Log.d("FoodRecommendations", "Realtime DB returned " + dataSnapshot.getChildrenCount() + " foods");

                int addedCount = 0;
                int filteredCount = 0;
                int goalMatchCount = 0;

                for (DataSnapshot foodSnapshot : dataSnapshot.getChildren()) {
                    try {
                        FoodRecommendation food = foodSnapshot.getValue(FoodRecommendation.class);
                        if (food != null) {
                            food.setId(foodSnapshot.getKey());

                            // Check if food is verified
                            Boolean isVerified = food.isVerified();
                            if (isVerified == null || !isVerified) {
                                continue; // Skip unverified foods
                            }

                            // Check if it's a general food (not user-specific)
                            boolean isGeneral = food.getUserId() == null;

                            if (isGeneral) {
                                // Apply goal-based filtering
                                boolean matchesGoal = (fitnessGoal == null || food.isGoodForGoal(fitnessGoal));
                                boolean isFromCoach = (coachId != null && coachId.equals(food.getCoachId()));

                                // Include if: coach food OR matches goal OR high-quality food
                                boolean shouldInclude = isFromCoach || matchesGoal ||
                                                      (food.getProtein() >= 20) ||
                                                      (food.getCalories() <= 50);

                                if (shouldInclude) {
                                    foodList.add(food);
                                    addedCount++;

                                    if (matchesGoal) goalMatchCount++;

                                    String goalStatus = matchesGoal ? "✅ PERFECT MATCH" :
                                                      isFromCoach ? "👨‍⚕️ COACH PRIORITY" :
                                                      "⭐ HIGH QUALITY";
                                    android.util.Log.d("FoodRecommendations", "Added: " + food.getName() +
                                        " (" + goalStatus + ", cal: " + food.getCalories() + ", protein: " + food.getProtein() + "g)");
                                } else {
                                    filteredCount++;
                                    android.util.Log.d("FoodRecommendations", "Filtered out: " + food.getName() +
                                        " (goal: " + fitnessGoal + ", cal: " + food.getCalories() + ", protein: " + food.getProtein() + "g)");
                                }
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("FoodRecommendations", "Error parsing food: " + e.getMessage());
                    }
                }

                android.util.Log.d("FoodRecommendations", "=== FINAL RESULTS ===");
                android.util.Log.d("FoodRecommendations", "Total foods loaded: " + foodList.size());
                android.util.Log.d("FoodRecommendations", "Goal matches: " + goalMatchCount + "/" + addedCount);
                android.util.Log.d("FoodRecommendations", "Foods filtered out: " + filteredCount);

                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);

                if (foodList.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    android.util.Log.w("FoodRecommendations", "No foods to display!");
                } else {
                    updateGoalInfoWithResults(goalMatchCount, addedCount);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                android.util.Log.e("FoodRecommendations", "Failed to load foods: " + databaseError.getMessage());
                Toast.makeText(UserFoodRecommendationsActivityRealtimeDB.this, "Error loading recommendations: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                emptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateGoalInfoText() {
        if (tvGoalInfo == null) return;

        String message;
        if (fitnessGoal == null) {
            message = "Foods recommended from our comprehensive nutrition database";
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

    private void setupRecyclerView() {
        adapter = new UserFoodAdapter(foodList, new UserFoodAdapter.OnFoodActionListener() {
            @Override
            public void onAddToMealPlan(FoodRecommendation food) {
                showMealTypeDialog(food);
            }

            @Override
            public void onViewDetails(FoodRecommendation food) {
                showFoodDetailsDialog(food);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void showMealTypeDialog(FoodRecommendation food) {
        String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snack"};

        new AlertDialog.Builder(this)
                .setTitle("Add to Meal Plan")
                .setItems(mealTypes, (dialog, which) -> {
                    addToMealPlan(food, mealTypes[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addToMealPlan(FoodRecommendation food, String mealType) {
        // Still use Firestore for meal plan storage
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());

        // Check for duplicates in Firestore
        firestore.collection("users")
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

                    // Add to meal plan
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

                    firestore.collection("users")
                            .document(userId)
                            .collection("mealPlan")
                            .add(mealPlan)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, "Added to " + mealType, Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
