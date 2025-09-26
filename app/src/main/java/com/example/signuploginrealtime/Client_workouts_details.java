package com.example.signuploginrealtime;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.adapters.SearchResultsAdapter;
import com.example.signuploginrealtime.adapters.WorkoutExerciseAdapter;
import com.example.signuploginrealtime.models.ExerciseInfo;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import android.text.Editable;
import android.text.TextWatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Client_workouts_details extends AppCompatActivity {

    private TextView clientName, clientWeight, clientHeight, clientGoal, workoutFrequency;
    private Set<String> alreadyAddedNames = new HashSet<>();
    private SearchResultsAdapter searchAdapter;
    private EditText searchWorkouts;
    private RecyclerView searchResultsRecycler;

    private WorkoutExerciseAdapter workoutAdapter;
    private List<WorkoutExercise> workoutExercises = new ArrayList<>();

    private String clientUid;
    private DocumentReference workoutRef;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_client_workouts_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- Bind views
        clientName = findViewById(R.id.client_name);
        clientWeight = findViewById(R.id.client_weight);
        clientHeight = findViewById(R.id.client_height);
        clientGoal = findViewById(R.id.client_goal);
        workoutFrequency = findViewById(R.id.workout_frequency);
        saveButton = findViewById(R.id.btn_save_changes);
        saveButton.setVisibility(View.GONE);

        RecyclerView recyclerView = findViewById(R.id.workouts_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        clientUid = getIntent().getStringExtra("client_uid");

        if (clientUid == null) {
            Toast.makeText(this, "No client selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 🔹 Reference to workout doc
        workoutRef = db.collection("users")
                .document(clientUid)
                .collection("currentWorkout")
                .document("week_1");

        // --- 1) Load client info ---
        db.collection("users").document(clientUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        clientName.setText(snapshot.getString("fullname"));
                        clientWeight.setText(String.valueOf(snapshot.get("weight")));
                        clientHeight.setText(String.valueOf(snapshot.get("height")));
                        clientGoal.setText(snapshot.getString("fitnessGoal"));
                        workoutFrequency.setText(snapshot.getString("fitnessLevel"));
                    }
                });

        // --- 2) Load workout exercises ---
        workoutRef.get().addOnSuccessListener(documentSnapshot -> {
            workoutExercises.clear();
            alreadyAddedNames.clear();

            if (documentSnapshot.exists()) {
                List<Object> exercises = (List<Object>) documentSnapshot.get("exercises");
                if (exercises != null) {
                    for (Object obj : exercises) {
                        if (obj instanceof Map) {
                            Map<String, Object> exerciseMap = (Map<String, Object>) obj;
                            Map<String, Object> exerciseInfoMap =
                                    (Map<String, Object>) exerciseMap.get("exerciseInfo");

                            ExerciseInfo info = new ExerciseInfo();
                            if (exerciseInfoMap != null) {
                                info.setName((String) exerciseInfoMap.get("name"));
                                info.setGifUrl((String) exerciseInfoMap.get("gifUrl"));
                            }

                            WorkoutExercise workoutExercise = new WorkoutExercise();
                            workoutExercise.setExerciseInfo(info);
                            workoutExercise.setReps(((Long) exerciseMap.get("reps")).intValue());
                            workoutExercise.setSets(((Long) exerciseMap.get("sets")).intValue());
                            workoutExercise.setRestSeconds(((Long) exerciseMap.get("restSeconds")).intValue());

                            workoutExercises.add(workoutExercise);

                            if (info.getName() != null) {
                                alreadyAddedNames.add(info.getName().toLowerCase());
                            }
                        }
                    }
                }
            }

            workoutAdapter = new WorkoutExerciseAdapter(this, workoutExercises, true);
            workoutAdapter.setOnWorkoutChangedListener(() -> onWorkoutChanged());
            recyclerView.setAdapter(workoutAdapter);

            if (searchAdapter != null) {
                searchAdapter.setAlreadyAdded(alreadyAddedNames);
            }
        });

        // --- 3) Setup search bar (Realtime DB) ---
        searchWorkouts = findViewById(R.id.search_workouts);
        searchResultsRecycler = findViewById(R.id.search_results_recycler);

        List<Map<String, Object>> searchResults = new ArrayList<>();
        searchAdapter = new SearchResultsAdapter(
                this,
                searchResults,
                exercise -> addExerciseToList(exercise) // Local only
        );

        searchResultsRecycler.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecycler.setAdapter(searchAdapter);

        DatabaseReference workoutsRef = FirebaseDatabase.getInstance().getReference();
        final String TAG = "WorkoutSearch";

        // ✅ Updated search functionality with new adapter methods
        searchWorkouts.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s == null ? "" : s.toString().trim();

                if (query.isEmpty()) {
                    // ✅ Use the new clearResults() method instead of manual clearing
                    searchAdapter.clearResults();
                    searchResultsRecycler.setVisibility(View.GONE);
                    return;
                }

                String qLower = query.toLowerCase();

                workoutsRef.orderByChild("name")
                        .startAt(query)
                        .endAt(query + "\uf8ff")
                        .limitToFirst(50)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            List<Map<String, Object>> newResults = new ArrayList<>();

                            for (DataSnapshot child : snapshot.getChildren()) {
                                Object val = child.getValue();
                                if (!(val instanceof Map)) continue;
                                Map<String, Object> exercise = (Map<String, Object>) val;
                                String name = exercise.get("name") != null ? exercise.get("name").toString() : "";
                                if (!name.isEmpty() && name.toLowerCase().contains(qLower)) {
                                    newResults.add(exercise);
                                }
                            }

                            // ✅ Use the new updateResults() method
                            searchAdapter.updateResults(newResults);

                            // ✅ Use the new isEmpty() method for cleaner code
                            searchResultsRecycler.setVisibility(
                                    searchAdapter.isEmpty() ? View.GONE : View.VISIBLE
                            );
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "search error", e);
                            Toast.makeText(Client_workouts_details.this, "Search failed", Toast.LENGTH_SHORT).show();
                            // ✅ Also clear results on error
                            searchAdapter.clearResults();
                            searchResultsRecycler.setVisibility(View.GONE);
                        });
            }
        });

        // --- 4) Save changes button ---
        saveButton.setOnClickListener(v -> saveWorkoutToFirestore());
    }

    // ✅ NEW: Method to handle clearing search when user taps elsewhere or wants to dismiss
    private void clearSearch() {
        searchWorkouts.setText("");
        searchAdapter.clearResults();
        searchResultsRecycler.setVisibility(View.GONE);

        // Also clear focus from the EditText
        searchWorkouts.clearFocus();
    }

    // ✅ NEW: Override back button to clear search first
    @Override
    public void onBackPressed() {
        // If search results are visible, clear them first
        if (searchResultsRecycler.getVisibility() == View.VISIBLE) {
            clearSearch();
        } else {
            super.onBackPressed();
        }
    }

    // ✅ Add new exercise locally
    private void addExerciseToList(Map<String, Object> exercise) {
        String exerciseName = exercise.get("name") != null ? exercise.get("name").toString() : "";

        if (alreadyAddedNames.contains(exerciseName.toLowerCase())) {
            Toast.makeText(this, "Exercise already added!", Toast.LENGTH_SHORT).show();
            return;
        }

        ExerciseInfo info = new ExerciseInfo();
        info.setName(exerciseName);
        info.setGifUrl((String) exercise.get("gifUrl"));

        WorkoutExercise newExercise = new WorkoutExercise();
        newExercise.setExerciseInfo(info);
        newExercise.setSets(0);
        newExercise.setReps(0);
        newExercise.setRestSeconds(60);

        workoutExercises.add(newExercise);
        workoutAdapter.notifyItemInserted(workoutExercises.size() - 1);
        alreadyAddedNames.add(exerciseName.toLowerCase());

        if (searchAdapter != null) {
            searchAdapter.setAlreadyAdded(alreadyAddedNames);
        }

        // ✅ Optional: Clear search after adding exercise
        clearSearch();
    }

    // ✅ Save all changes to Firestore
    private void saveWorkoutToFirestore() {
        List<Map<String, Object>> exerciseList = new ArrayList<>();

        for (WorkoutExercise we : workoutExercises) {
            Map<String, Object> map = new HashMap<>();
            Map<String, Object> info = new HashMap<>();
            if (we.getExerciseInfo() != null) {
                info.put("name", we.getExerciseInfo().getName());
                info.put("gifUrl", we.getExerciseInfo().getGifUrl());
            }
            map.put("exerciseInfo", info);
            map.put("sets", we.getSets());
            map.put("reps", we.getReps());
            map.put("restSeconds", we.getRestSeconds());

            exerciseList.add(map);
        }

        workoutRef.update("exercises", exerciseList)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Workout saved!", Toast.LENGTH_SHORT).show();
                    hasChanges = false;
                    saveButton.setVisibility(View.GONE); // 👈 hide button again after saving
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private boolean hasChanges = false;

    private void onWorkoutChanged() {
        if (!hasChanges) {
            hasChanges = true;
            saveButton.setVisibility(View.VISIBLE); // show the save button
        }
    }
}