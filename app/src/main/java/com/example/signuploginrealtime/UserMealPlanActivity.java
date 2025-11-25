package com.example.signuploginrealtime;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.models.UserMealPlan;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserMealPlanActivity extends AppCompatActivity {

    private ImageView btnBack, btnCalendar;
    private TextView tvSelectedDate, tvTotalCalories, tvTotalProtein, tvTotalCarbs, tvTotalFats;
    private RecyclerView recyclerBreakfast, recyclerLunch, recyclerDinner, recyclerSnacks;
    private TextView tvEmptyBreakfast, tvEmptyLunch, tvEmptyDinner, tvEmptySnacks;
    private LinearLayout emptyStateAll;
    private Button btnGoToRecommendations;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String userId;
    private String selectedDate;

    private List<UserMealPlan> breakfastList = new ArrayList<>();
    private List<UserMealPlan> lunchList = new ArrayList<>();
    private List<UserMealPlan> dinnerList = new ArrayList<>();
    private List<UserMealPlan> snacksList = new ArrayList<>();

    private MealPlanAdapter breakfastAdapter, lunchAdapter, dinnerAdapter, snacksAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_meal_plan);

        initializeViews();
        setupRecyclerViews();
        setupListeners();
        setTodayDate();

        // Load meal plan directly (duplicate prevention now happens at add-time)
        loadMealPlan();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnCalendar = findViewById(R.id.btnCalendar);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvTotalCalories = findViewById(R.id.tvTotalCalories);
        tvTotalProtein = findViewById(R.id.tvTotalProtein);
        tvTotalCarbs = findViewById(R.id.tvTotalCarbs);
        tvTotalFats = findViewById(R.id.tvTotalFats);
        recyclerBreakfast = findViewById(R.id.recyclerBreakfast);
        recyclerLunch = findViewById(R.id.recyclerLunch);
        recyclerDinner = findViewById(R.id.recyclerDinner);
        recyclerSnacks = findViewById(R.id.recyclerSnacks);
        tvEmptyBreakfast = findViewById(R.id.tvEmptyBreakfast);
        tvEmptyLunch = findViewById(R.id.tvEmptyLunch);
        tvEmptyDinner = findViewById(R.id.tvEmptyDinner);
        tvEmptySnacks = findViewById(R.id.tvEmptySnacks);
        emptyStateAll = findViewById(R.id.emptyStateAll);
        btnGoToRecommendations = findViewById(R.id.btnGoToRecommendations);
        progressBar = findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private void setupRecyclerViews() {
        breakfastAdapter = new MealPlanAdapter(breakfastList, this::deleteMealItem);
        lunchAdapter = new MealPlanAdapter(lunchList, this::deleteMealItem);
        dinnerAdapter = new MealPlanAdapter(dinnerList, this::deleteMealItem);
        snacksAdapter = new MealPlanAdapter(snacksList, this::deleteMealItem);

        recyclerBreakfast.setLayoutManager(new LinearLayoutManager(this));
        recyclerBreakfast.setAdapter(breakfastAdapter);

        recyclerLunch.setLayoutManager(new LinearLayoutManager(this));
        recyclerLunch.setAdapter(lunchAdapter);

        recyclerDinner.setLayoutManager(new LinearLayoutManager(this));
        recyclerDinner.setAdapter(dinnerAdapter);

        recyclerSnacks.setLayoutManager(new LinearLayoutManager(this));
        recyclerSnacks.setAdapter(snacksAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnCalendar.setOnClickListener(v -> showDatePicker());

        btnGoToRecommendations.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserFoodRecommendationsActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void setTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(new Date());
        updateDateDisplay();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    selectedDate = sdf.format(calendar.getTime());
                    updateDateDisplay();
                    loadMealPlan();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void updateDateDisplay() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(selectedDate);

            Calendar today = Calendar.getInstance();
            Calendar selected = Calendar.getInstance();
            selected.setTime(date);

            if (isSameDay(today, selected)) {
                tvSelectedDate.setText("Today");
            } else {
                SimpleDateFormat displayFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
                tvSelectedDate.setText(displayFormat.format(date));
            }
        } catch (Exception e) {
            tvSelectedDate.setText(selectedDate);
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void loadMealPlan() {
        progressBar.setVisibility(View.VISIBLE);
        emptyStateAll.setVisibility(View.GONE);

        breakfastList.clear();
        lunchList.clear();
        dinnerList.clear();
        snacksList.clear();

        android.util.Log.d("MealPlanLoad", "=== LOADING MEAL PLAN ===");
        android.util.Log.d("MealPlanLoad", "UserId: " + userId);
        android.util.Log.d("MealPlanLoad", "Selected Date: " + selectedDate);

        db.collection("users")
                .document(userId)
                .collection("mealPlan")
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d("MealPlanLoad", "Query returned " + queryDocumentSnapshots.size() + " documents");

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        UserMealPlan mealPlan = document.toObject(UserMealPlan.class);
                        mealPlan.setId(document.getId());

                        android.util.Log.d("MealPlanLoad", "Found: " + mealPlan.getFoodName() +
                            " - MealType: " + mealPlan.getMealType() +
                            " - Date: " + mealPlan.getDate());

                        String mealType = mealPlan.getMealType();
                        if ("Breakfast".equals(mealType)) {
                            breakfastList.add(mealPlan);
                            android.util.Log.d("MealPlanLoad", "✅ Added to Breakfast");
                        } else if ("Lunch".equals(mealType)) {
                            lunchList.add(mealPlan);
                            android.util.Log.d("MealPlanLoad", "✅ Added to Lunch");
                        } else if ("Dinner".equals(mealType)) {
                            dinnerList.add(mealPlan);
                            android.util.Log.d("MealPlanLoad", "✅ Added to Dinner");
                        } else if ("Snack".equals(mealType)) {
                            snacksList.add(mealPlan);
                            android.util.Log.d("MealPlanLoad", "✅ Added to Snacks");
                        } else {
                            android.util.Log.w("MealPlanLoad", "⚠️ Unknown meal type: '" + mealType + "'");
                        }
                    }

                    android.util.Log.d("MealPlanLoad", "Final counts - Breakfast: " + breakfastList.size() +
                        ", Lunch: " + lunchList.size() +
                        ", Dinner: " + dinnerList.size() +
                        ", Snacks: " + snacksList.size());

                    updateUI();
                    calculateTotals();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MealPlanLoad", "❌ Failed to load meal plan", e);
                    Toast.makeText(this, "Error loading meal plan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void updateUI() {
        // Update breakfast
        if (breakfastList.isEmpty()) {
            tvEmptyBreakfast.setVisibility(View.VISIBLE);
            recyclerBreakfast.setVisibility(View.GONE);
        } else {
            tvEmptyBreakfast.setVisibility(View.GONE);
            recyclerBreakfast.setVisibility(View.VISIBLE);
            breakfastAdapter.notifyDataSetChanged();
        }

        // Update lunch
        if (lunchList.isEmpty()) {
            tvEmptyLunch.setVisibility(View.VISIBLE);
            recyclerLunch.setVisibility(View.GONE);
        } else {
            tvEmptyLunch.setVisibility(View.GONE);
            recyclerLunch.setVisibility(View.VISIBLE);
            lunchAdapter.notifyDataSetChanged();
        }

        // Update dinner
        if (dinnerList.isEmpty()) {
            tvEmptyDinner.setVisibility(View.VISIBLE);
            recyclerDinner.setVisibility(View.GONE);
        } else {
            tvEmptyDinner.setVisibility(View.GONE);
            recyclerDinner.setVisibility(View.VISIBLE);
            dinnerAdapter.notifyDataSetChanged();
        }

        // Update snacks
        if (snacksList.isEmpty()) {
            tvEmptySnacks.setVisibility(View.VISIBLE);
            recyclerSnacks.setVisibility(View.GONE);
        } else {
            tvEmptySnacks.setVisibility(View.GONE);
            recyclerSnacks.setVisibility(View.VISIBLE);
            snacksAdapter.notifyDataSetChanged();
        }

        // Show empty state if all lists are empty
        int totalCount = breakfastList.size() + lunchList.size() + dinnerList.size() + snacksList.size();
        if (totalCount == 0) {
            emptyStateAll.setVisibility(View.VISIBLE);
        }
    }

    private void calculateTotals() {
        int totalCalories = 0;
        double totalProtein = 0;
        double totalCarbs = 0;
        double totalFats = 0;

        List<UserMealPlan> allMeals = new ArrayList<>();
        allMeals.addAll(breakfastList);
        allMeals.addAll(lunchList);
        allMeals.addAll(dinnerList);
        allMeals.addAll(snacksList);

        for (UserMealPlan meal : allMeals) {
            totalCalories += meal.getCalories();
            totalProtein += meal.getProtein();
            totalCarbs += meal.getCarbs();
            totalFats += meal.getFats();
        }

        tvTotalCalories.setText(String.valueOf(totalCalories));
        tvTotalProtein.setText(String.format("%.1fg", totalProtein));
        tvTotalCarbs.setText(String.format("%.1fg", totalCarbs));
        tvTotalFats.setText(String.format("%.1fg", totalFats));
    }

    private void deleteMealItem(UserMealPlan mealPlan) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Food")
                .setMessage("Remove " + mealPlan.getFoodName() + " from your meal plan?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    db.collection("users")
                            .document(userId)
                            .collection("mealPlan")
                            .document(mealPlan.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Removed from meal plan", Toast.LENGTH_SHORT).show();
                                loadMealPlan(); // Reload to update UI
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Don't reload here - it causes constant reloading
        // Data is loaded in onCreate and after delete
    }

    // Manual cleanup utility - only call if user reports duplicate issues
    private void removeDuplicates() {
        android.util.Log.d("MealPlanCleanup", "=== MANUAL DUPLICATE CLEANUP ===");
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users")
                .document(userId)
                .collection("mealPlan")
                .whereEqualTo("date", selectedDate) // Only check current date
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Group by mealType + foodName
                    java.util.Map<String, java.util.List<QueryDocumentSnapshot>> groupedDocs = new java.util.HashMap<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        UserMealPlan meal = doc.toObject(UserMealPlan.class);
                        // Group by meal type and food name
                        String key = meal.getMealType() + "|" + meal.getFoodName();

                        if (!groupedDocs.containsKey(key)) {
                            groupedDocs.put(key, new java.util.ArrayList<>());
                        }
                        groupedDocs.get(key).add(doc);
                    }

                    // Delete duplicates (keep only the first one)
                    int duplicatesFound = 0;
                    for (java.util.List<QueryDocumentSnapshot> docs : groupedDocs.values()) {
                        if (docs.size() > 1) {
                            android.util.Log.d("MealPlanCleanup", "Found " + docs.size() + " copies of same food");
                            // Keep first, delete rest
                            for (int i = 1; i < docs.size(); i++) {
                                QueryDocumentSnapshot duplicate = docs.get(i);
                                android.util.Log.d("MealPlanCleanup", "Removing duplicate: " + duplicate.getId());
                                db.collection("users")
                                        .document(userId)
                                        .collection("mealPlan")
                                        .document(duplicate.getId())
                                        .delete();
                                duplicatesFound++;
                            }
                        }
                    }

                    progressBar.setVisibility(View.GONE);

                    if (duplicatesFound > 0) {
                        android.util.Log.d("MealPlanCleanup", "✅ Removed " + duplicatesFound + " duplicates");
                        Toast.makeText(this, "Removed " + duplicatesFound + " duplicates", Toast.LENGTH_SHORT).show();
                        // Reload after a short delay
                        new android.os.Handler().postDelayed(this::loadMealPlan, 800);
                    } else {
                        android.util.Log.d("MealPlanCleanup", "✅ No duplicates found");
                        Toast.makeText(this, "No duplicates found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MealPlanCleanup", "❌ Failed to check duplicates", e);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
