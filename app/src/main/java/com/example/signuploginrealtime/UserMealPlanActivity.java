package com.example.signuploginrealtime;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.adapters.MealPlanAdapter;
import com.example.signuploginrealtime.models.UserMealPlan;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserMealPlanActivity extends AppCompatActivity {

    private RecyclerView recyclerViewMealPlan;
    private MealPlanAdapter mealPlanAdapter;
    private List<UserMealPlan> mealPlanList;
    private LinearLayout emptyStateMealPlan;
    private ImageView btnBack;
    private TextView tvMealPlanCount;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_meal_plan);

        initializeViews();
        setupRecyclerView();
        setupListeners();
        loadMealPlan();
    }

    private void initializeViews() {
        recyclerViewMealPlan = findViewById(R.id.recyclerViewMealPlan);
        emptyStateMealPlan = findViewById(R.id.emptyStateMealPlan);
        btnBack = findViewById(R.id.btnBack);
        tvMealPlanCount = findViewById(R.id.tvMealPlanCount);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mealPlanList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        mealPlanAdapter = new MealPlanAdapter(mealPlanList, this::removeFromMealPlan);
        recyclerViewMealPlan.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMealPlan.setAdapter(mealPlanAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadMealPlan() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());

        db.collection("users")
                .document(userId)
                .collection("mealPlan")
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    mealPlanList.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : querySnapshot) {
                        UserMealPlan meal = document.toObject(UserMealPlan.class);
                        meal.setId(document.getId());
                        mealPlanList.add(meal);
                    }
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading meal plan", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI() {
        mealPlanAdapter.notifyDataSetChanged();
        tvMealPlanCount.setText(mealPlanList.size() + " items");

        if (mealPlanList.isEmpty()) {
            emptyStateMealPlan.setVisibility(View.VISIBLE);
            recyclerViewMealPlan.setVisibility(View.GONE);
        } else {
            emptyStateMealPlan.setVisibility(View.GONE);
            recyclerViewMealPlan.setVisibility(View.VISIBLE);
        }
    }

    private void removeFromMealPlan(UserMealPlan meal) {
        new AlertDialog.Builder(this)
                .setTitle("Remove from Meal Plan")
                .setMessage("Remove " + meal.getFoodName() + " from your meal plan?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    db.collection("users")
                            .document(userId)
                            .collection("mealPlan")
                            .document(meal.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Removed from meal plan", Toast.LENGTH_SHORT).show();
                                loadMealPlan();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error removing from meal plan", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

