package com.example.signuploginrealtime;

import android.content.Intent;
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

import com.example.signuploginrealtime.adapters.CoachFoodAdapter;
import com.example.signuploginrealtime.models.FoodRecommendation;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CoachFoodManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CoachFoodAdapter adapter;
    private List<FoodRecommendation> foodList;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private FloatingActionButton fabAddFood;
    private ImageView btnBack;
    private TextView tvClientName;

    private FirebaseFirestore db;
    private String coachId;
    private String clientId; // null for general recommendations
    private String clientName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coach_food_management);

        initializeViews();
        setupRecyclerView();
        setupListeners();
        loadFoods();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewFoods);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
        fabAddFood = findViewById(R.id.fabAddFood);
        btnBack = findViewById(R.id.btnBack);
        tvClientName = findViewById(R.id.tvClientName);

        db = FirebaseFirestore.getInstance();
        coachId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        foodList = new ArrayList<>();

        // Get client info if passed
        Intent intent = getIntent();
        clientId = intent.getStringExtra("clientId");
        clientName = intent.getStringExtra("clientName");

        if (clientName != null) {
            tvClientName.setText("Food Recommendations for " + clientName);
        } else {
            tvClientName.setText("General Food Recommendations");
        }
    }

    private void setupRecyclerView() {
        adapter = new CoachFoodAdapter(foodList, new CoachFoodAdapter.OnFoodActionListener() {
            @Override
            public void onEdit(FoodRecommendation food) {
                editFood(food);
            }

            @Override
            public void onDelete(FoodRecommendation food) {
                confirmDelete(food);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        fabAddFood.setOnClickListener(v -> {
            Intent intent = new Intent(CoachFoodManagementActivity.this, CoachAddFoodActivity.class);
            intent.putExtra("clientId", clientId);
            intent.putExtra("clientName", clientName);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFoods();
    }

    private void loadFoods() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        foodList.clear();

        android.util.Log.d("CoachFoodMgmt", "=== LOADING COACH FOODS ===");
        android.util.Log.d("CoachFoodMgmt", "coachId: " + coachId);
        android.util.Log.d("CoachFoodMgmt", "clientId: " + clientId);

        // Build query based on whether we're filtering by client
        if (clientId != null) {
            // Load foods for specific client
            android.util.Log.d("CoachFoodMgmt", "Mode: Loading foods for specific client");
            db.collection("foods")
                    .whereEqualTo("coachId", coachId)
                    .whereEqualTo("userId", clientId)
                    .get()
                    .addOnSuccessListener(this::processFoodResults)
                    .addOnFailureListener(this::handleLoadError);
        } else {
            // Load general recommendations (userId is null)
            // Firestore doesn't support direct null queries, so we filter manually
            android.util.Log.d("CoachFoodMgmt", "Mode: Loading GENERAL foods (userId == null)");
            db.collection("foods")
                    .whereEqualTo("coachId", coachId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        android.util.Log.d("CoachFoodMgmt", "Query returned " + queryDocumentSnapshots.size() + " foods from coach");
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            android.util.Log.d("CoachFoodMgmt", "Doc ID: " + document.getId() + ", data: " + document.getData());

                            FoodRecommendation food = document.toObject(FoodRecommendation.class);
                            // Only include foods with null userId (general recommendations)
                            if (food.getUserId() == null) {
                                food.setId(document.getId());
                                foodList.add(food);
                                android.util.Log.d("CoachFoodMgmt", "✅ Added general food: " + food.getName());
                            } else {
                                android.util.Log.d("CoachFoodMgmt", "⏭️ Skipped personalized food: " + food.getName() + " (userId: " + food.getUserId() + ")");
                            }
                        }

                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);

                        if (foodList.isEmpty()) {
                            emptyState.setVisibility(View.VISIBLE);
                        }

                        android.util.Log.d("CoachFoodMgmt", "Final list size: " + foodList.size());
                    })
                    .addOnFailureListener(this::handleLoadError);
        }
    }

    private void processFoodResults(com.google.firebase.firestore.QuerySnapshot queryDocumentSnapshots) {
        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
            FoodRecommendation food = document.toObject(FoodRecommendation.class);
            food.setId(document.getId());
            foodList.add(food);
        }

        adapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);

        if (foodList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
        }
    }

    private void handleLoadError(Exception e) {
        Toast.makeText(this, "Error loading foods: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }

    private void editFood(FoodRecommendation food) {
        Intent intent = new Intent(this, CoachAddFoodActivity.class);
        intent.putExtra("clientId", clientId);
        intent.putExtra("clientName", clientName);
        intent.putExtra("foodId", food.getId());
        startActivity(intent);
    }

    private void confirmDelete(FoodRecommendation food) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Food")
                .setMessage("Are you sure you want to delete \"" + food.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteFood(food))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteFood(FoodRecommendation food) {
        db.collection("foods")
                .document(food.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Food deleted", Toast.LENGTH_SHORT).show();
                    loadFoods();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error deleting food: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

