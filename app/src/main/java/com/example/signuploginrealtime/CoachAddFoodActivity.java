package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.signuploginrealtime.models.FoodRecommendation;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CoachAddFoodActivity extends AppCompatActivity {

    private EditText etFoodName, etCalories, etProtein, etCarbs, etFats, etServingSize, etNotes;
    private LinearLayout tagsContainer;
    private Button btnSubmit;
    private ProgressBar progressBar;
    private ImageView btnBack;
    private TextView tvClientName;

    private FirebaseFirestore db;
    private String coachId;
    private String clientId; // null if general recommendation
    private String clientName;
    private String editFoodId; // null if adding new food

    private List<String> selectedTags = new ArrayList<>();
    private String[] availableTags = {
            "High Protein", "Low Carb", "Keto", "Vegan", "Vegetarian",
            "Gluten-Free", "Dairy-Free", "High Fiber", "Low Calorie", "Halal"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coach_add_food);

        initializeViews();
        setupData();
        setupTags();
        setupListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvClientName = findViewById(R.id.tvClientName);
        etFoodName = findViewById(R.id.etFoodName);
        etCalories = findViewById(R.id.etCalories);
        etProtein = findViewById(R.id.etProtein);
        etCarbs = findViewById(R.id.etCarbs);
        etFats = findViewById(R.id.etFats);
        etServingSize = findViewById(R.id.etServingSize);
        etNotes = findViewById(R.id.etNotes);
        tagsContainer = findViewById(R.id.tagsContainer);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();
        coachId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private void setupData() {
        Intent intent = getIntent();
        clientId = intent.getStringExtra("clientId");
        clientName = intent.getStringExtra("clientName");
        editFoodId = intent.getStringExtra("foodId");

        if (clientId != null && clientName != null) {
            tvClientName.setText("Personalized for: " + clientName);
            tvClientName.setVisibility(View.VISIBLE);
        } else {
            tvClientName.setText("General Recommendation");
            tvClientName.setVisibility(View.VISIBLE);
        }

        // If editing, load food data
        if (editFoodId != null) {
            loadFoodData();
            btnSubmit.setText("Update Food");
        }
    }

    private void setupTags() {
        tagsContainer.removeAllViews();

        for (String tag : availableTags) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(tag);
            checkBox.setTextSize(14);
            checkBox.setPadding(16, 8, 16, 8);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedTags.add(tag);
                } else {
                    selectedTags.remove(tag);
                }
            });

            tagsContainer.addView(checkBox);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            if (validateInputs()) {
                submitFood();
            }
        });
    }

    private boolean validateInputs() {
        if (etFoodName.getText().toString().trim().isEmpty()) {
            etFoodName.setError("Food name is required");
            return false;
        }

        if (etCalories.getText().toString().trim().isEmpty()) {
            etCalories.setError("Calories is required");
            return false;
        }

        if (etProtein.getText().toString().trim().isEmpty()) {
            etProtein.setError("Protein is required");
            return false;
        }

        if (etCarbs.getText().toString().trim().isEmpty()) {
            etCarbs.setError("Carbs is required");
            return false;
        }

        if (etFats.getText().toString().trim().isEmpty()) {
            etFats.setError("Fats is required");
            return false;
        }

        return true;
    }

    private void submitFood() {
        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        FoodRecommendation food = new FoodRecommendation();
        food.setName(etFoodName.getText().toString().trim());
        food.setCalories(Integer.parseInt(etCalories.getText().toString().trim()));
        food.setProtein(Double.parseDouble(etProtein.getText().toString().trim()));
        food.setCarbs(Double.parseDouble(etCarbs.getText().toString().trim()));
        food.setFats(Double.parseDouble(etFats.getText().toString().trim()));
        food.setServingSize(etServingSize.getText().toString().trim());
        food.setNotes(etNotes.getText().toString().trim());
        food.setTags(selectedTags);
        food.setCoachId(coachId);
        food.setUserId(clientId); // null if general
        food.setSource("Coach");
        food.setVerified(true); // Auto-approve coach recommendations
        food.setCreatedAt(Timestamp.now());

        // Add logging
        android.util.Log.d("CoachAddFood", "=== SUBMITTING FOOD ===");
        android.util.Log.d("CoachAddFood", "Name: " + food.getName());
        android.util.Log.d("CoachAddFood", "Calories: " + food.getCalories());
        android.util.Log.d("CoachAddFood", "coachId: " + coachId);
        android.util.Log.d("CoachAddFood", "userId (clientId): " + clientId);
        android.util.Log.d("CoachAddFood", "isVerified: " + food.isVerified());
        android.util.Log.d("CoachAddFood", "source: " + food.getSource());

        if (editFoodId != null) {
            // Update existing
            android.util.Log.d("CoachAddFood", "Mode: UPDATE (ID: " + editFoodId + ")");
            db.collection("foods")
                    .document(editFoodId)
                    .set(food)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("CoachAddFood", "✅ Food updated successfully!");
                        Toast.makeText(this, "Food updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("CoachAddFood", "❌ Failed to update food", e);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                    });
        } else {
            // Add new
            android.util.Log.d("CoachAddFood", "Mode: ADD NEW");
            db.collection("foods")
                    .add(food)
                    .addOnSuccessListener(documentReference -> {
                        android.util.Log.d("CoachAddFood", "✅ Food added successfully! Document ID: " + documentReference.getId());
                        Toast.makeText(this, "Food added successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("CoachAddFood", "❌ Failed to add food", e);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                    });
        }
    }

    private void loadFoodData() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("foods")
                .document(editFoodId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        FoodRecommendation food = documentSnapshot.toObject(FoodRecommendation.class);
                        if (food != null) {
                            etFoodName.setText(food.getName());
                            etCalories.setText(String.valueOf(food.getCalories()));
                            etProtein.setText(String.valueOf(food.getProtein()));
                            etCarbs.setText(String.valueOf(food.getCarbs()));
                            etFats.setText(String.valueOf(food.getFats()));
                            etServingSize.setText(food.getServingSize());
                            etNotes.setText(food.getNotes());

                            // Select tags
                            for (int i = 0; i < tagsContainer.getChildCount(); i++) {
                                CheckBox checkBox = (CheckBox) tagsContainer.getChildAt(i);
                                if (food.getTags().contains(checkBox.getText().toString())) {
                                    checkBox.setChecked(true);
                                }
                            }
                        }
                    }
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading food: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }
}
