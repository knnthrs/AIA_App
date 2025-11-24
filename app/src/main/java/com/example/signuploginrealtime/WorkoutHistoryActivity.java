package com.example.signuploginrealtime;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.models.WorkoutHistory;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WorkoutHistoryActivity extends AppCompatActivity {

    private static final String TAG = "WorkoutHistoryActivity";

    private RecyclerView rvWorkoutHistory;
    private LinearLayout emptyState;
    private TextView tvTotalWorkouts, tvTotalCalories, tvCurrentWeight, tvCurrentBMI, tvBmiCategory;
    private MaterialButton btnAll, btnThisWeek, btnThisMonth;

    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private WorkoutHistoryAdapter adapter;
    private List<WorkoutHistory> allWorkouts = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_history);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize views
        initViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup filter buttons
        setupFilterButtons();

        // Load data
        loadWorkoutHistory();
        loadUserStats();

        // Back button
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });
    }

    private void initViews() {
        rvWorkoutHistory = findViewById(R.id.rv_workout_history);
        emptyState = findViewById(R.id.empty_state);
        tvTotalWorkouts = findViewById(R.id.tv_total_workouts);
        tvTotalCalories = findViewById(R.id.tv_total_calories);
        tvCurrentWeight = findViewById(R.id.tv_current_weight);
        tvCurrentBMI = findViewById(R.id.tv_current_bmi);
        tvBmiCategory = findViewById(R.id.tv_bmi_category);
        btnAll = findViewById(R.id.btn_all);
        btnThisWeek = findViewById(R.id.btn_this_week);
        btnThisMonth = findViewById(R.id.btn_this_month);
    }

    private void setupRecyclerView() {
        adapter = new WorkoutHistoryAdapter(this, new ArrayList<>());
        rvWorkoutHistory.setLayoutManager(new LinearLayoutManager(this));
        rvWorkoutHistory.setAdapter(adapter);
    }

    private void setupFilterButtons() {
        btnAll.setOnClickListener(v -> applyFilter("all"));
        btnThisWeek.setOnClickListener(v -> applyFilter("week"));
        btnThisMonth.setOnClickListener(v -> applyFilter("month"));

        // Set initial selection
        updateFilterButtonStyles("all");
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        updateFilterButtonStyles(filter);
        filterWorkouts(filter);
    }

    private void updateFilterButtonStyles(String selectedFilter) {
        // Reset all buttons
        btnAll.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        btnThisWeek.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        btnThisMonth.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        // Highlight selected
        MaterialButton selectedButton = null;
        switch (selectedFilter) {
            case "all":
                selectedButton = btnAll;
                break;
            case "week":
                selectedButton = btnThisWeek;
                break;
            case "month":
                selectedButton = btnThisMonth;
                break;
        }

        if (selectedButton != null) {
            selectedButton.setBackgroundColor(getResources().getColor(android.R.color.black));
            selectedButton.setTextColor(getResources().getColor(android.R.color.white));
        }
    }

    private void filterWorkouts(String filter) {
        List<WorkoutHistory> filtered = new ArrayList<>();
        long now = System.currentTimeMillis();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);

        long weekStart = getWeekStart(calendar);
        long monthStart = getMonthStart(calendar);

        for (WorkoutHistory workout : allWorkouts) {
            switch (filter) {
                case "all":
                    filtered.add(workout);
                    break;
                case "week":
                    if (workout.getTimestamp() >= weekStart) {
                        filtered.add(workout);
                    }
                    break;
                case "month":
                    if (workout.getTimestamp() >= monthStart) {
                        filtered.add(workout);
                    }
                    break;
            }
        }

        adapter.updateData(filtered);
        updateEmptyState(filtered.isEmpty());
    }

    private long getWeekStart(Calendar calendar) {
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getMonthStart(Calendar calendar) {
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void loadWorkoutHistory() {
        if (currentUser == null) {
            Log.e(TAG, "User not logged in");
            return;
        }

        String userId = currentUser.getUid();

        firestore.collection("users")
                .document(userId)
                .collection("workoutHistory")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allWorkouts.clear();

                    Log.d(TAG, "📝 Loading workouts from Firestore...");

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        WorkoutHistory workout = document.toObject(WorkoutHistory.class);
                        String docId = document.getId();
                        workout.setWorkoutId(docId);
                        allWorkouts.add(workout);

                        Log.d(TAG, "  ✅ Loaded workout: ID=" + docId +
                              ", Duration=" + workout.getDuration() +
                              ", Calories=" + workout.getCaloriesBurned());
                    }

                    Log.d(TAG, "✅ Loaded " + allWorkouts.size() + " workouts total");

                    // Apply current filter
                    filterWorkouts(currentFilter);

                    // Update overall stats
                    updateOverallStats();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading workout history", e);
                    updateEmptyState(true);
                });
    }

    private void loadUserStats() {
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Double weight = snapshot.getDouble("weight");
                        Double height = snapshot.getDouble("height");

                        if (weight != null && height != null) {
                            tvCurrentWeight.setText(String.format("%.1f", weight));

                            double bmi = WorkoutHistory.calculateBMI(weight, height);
                            tvCurrentBMI.setText(String.format("%.1f", bmi));
                            tvBmiCategory.setText(WorkoutHistory.getBMICategory(bmi));
                        } else {
                            tvCurrentWeight.setText("--");
                            tvCurrentBMI.setText("--");
                            tvBmiCategory.setText("BMI");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading user stats", e));
    }

    private void updateOverallStats() {
        int totalWorkouts = allWorkouts.size();
        int totalCalories = 0;

        for (WorkoutHistory workout : allWorkouts) {
            totalCalories += workout.getCaloriesBurned();
        }

        tvTotalWorkouts.setText(String.valueOf(totalWorkouts));
        tvTotalCalories.setText(String.valueOf(totalCalories));
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            rvWorkoutHistory.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvWorkoutHistory.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }
}

