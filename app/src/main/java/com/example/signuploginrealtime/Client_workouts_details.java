package com.example.signuploginrealtime;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Client_workouts_details extends AppCompatActivity {

    private TextView clientName, clientWeight, clientHeight, clientGoal, workoutFrequency;

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

        // 🔹 Bind client info header
        clientName = findViewById(R.id.client_name);
        clientWeight = findViewById(R.id.client_weight);
        clientHeight = findViewById(R.id.client_height);
        clientGoal = findViewById(R.id.client_goal);
        workoutFrequency = findViewById(R.id.workout_frequency);

        // 🔹 RecyclerView for exercises
        RecyclerView recyclerView = findViewById(R.id.workouts_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 🔹 Firestore refs
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // ✅ Get client UID passed from adapter
        String clientUid = getIntent().getStringExtra("client_uid");

        if (clientUid == null) {
            Toast.makeText(this, "No client selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // --- 1) Load client info ---
        DocumentReference userRef = db.collection("users").document(clientUid);
        userRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                clientName.setText(snapshot.getString("fullname")); // your users collection uses "fullname"
                clientWeight.setText(String.valueOf(snapshot.get("weight")));
                clientHeight.setText(String.valueOf(snapshot.get("height")));
                clientGoal.setText(snapshot.getString("fitnessGoal"));
                workoutFrequency.setText(snapshot.getString("fitnessLevel"));
            }
        });

        // --- 2) Load current workout ---
        DocumentReference workoutRef = userRef.collection("currentWorkout").document("week_1");
        workoutRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<WorkoutExercise> workoutExercises = new ArrayList<>();
                List<Object> exercises = (List<Object>) documentSnapshot.get("exercises");

                if (exercises != null) {
                    for (Object obj : exercises) {
                        if (obj instanceof java.util.Map) {
                            java.util.Map<String, Object> exerciseMap = (java.util.Map<String, Object>) obj;

                            java.util.Map<String, Object> exerciseInfoMap =
                                    (java.util.Map<String, Object>) exerciseMap.get("exerciseInfo");

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
                        }
                    }
                }

                WorkoutExerciseAdapter adapter = new WorkoutExerciseAdapter(this, workoutExercises, false);
                recyclerView.setAdapter(adapter);
            }
        }).addOnFailureListener(Throwable::printStackTrace);

        // --- 3) Setup search bar for global workouts (RealtimeDB) ---
        EditText searchWorkouts = findViewById(R.id.search_workouts);
        RecyclerView searchResultsRecycler = findViewById(R.id.search_results_recycler);

        List<Map<String, Object>> searchResults = new ArrayList<>();
        SearchResultsAdapter searchAdapter = new SearchResultsAdapter(this, searchResults, exercise -> {
            Toast.makeText(this, "Selected: " + exercise.get("name"), Toast.LENGTH_SHORT).show();
            // Later: add to client's workout in Firestore
        });

        searchResultsRecycler.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecycler.setAdapter(searchAdapter);

        DatabaseReference workoutsRef = FirebaseDatabase.getInstance().getReference();

        final String TAG = "WorkoutSearch"; // add near top of onCreate() or as class field

        searchWorkouts.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s == null ? "" : s.toString().trim();
                Log.d(TAG, "onTextChanged: query='" + query + "'");

                // hide when empty
                if (query.isEmpty()) {
                    searchResults.clear();
                    searchAdapter.notifyDataSetChanged();
                    searchResultsRecycler.setVisibility(View.GONE);
                    return;
                }

                final String qLower = query.toLowerCase();

                // Primary: prefix search (efficient if names stored in searchable form)
                workoutsRef.orderByChild("name")
                        .startAt(query)
                        .endAt(query + "\uf8ff")
                        .limitToFirst(50)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            Log.d(TAG, "primary query returned children=" + snapshot.getChildrenCount());
                            searchResults.clear();

                            // Add results returned by query (may be empty)
                            for (DataSnapshot child : snapshot.getChildren()) {
                                Object val = child.getValue();
                                if (!(val instanceof Map)) continue;
                                @SuppressWarnings("unchecked")
                                Map<String, Object> exercise = (Map<String, Object>) val;

                                // defensive read of name
                                String name = exercise.get("name") != null ? exercise.get("name").toString() : "";
                                // Include if case-insensitive contains (extra safety) or just add the node
                                if (!name.isEmpty() && name.toLowerCase().contains(qLower)) {
                                    searchResults.add(exercise);
                                } else {
                                    // include anyway so results show (primary query may match prefix differently due to case)
                                    searchResults.add(exercise);
                                }
                            }

                            // If primary query returns nothing, do a small fallback client-side filter
                            if (searchResults.isEmpty()) {
                                Log.d(TAG, "primary query empty — running fallback (limited scan)");
                                // fetch a small slice and filter locally case-insensitive
                                workoutsRef.orderByChild("name")
                                        .limitToFirst(200) // reasonable small batch
                                        .get()
                                        .addOnSuccessListener(snapshot2 -> {
                                            Log.d(TAG, "fallback returned children=" + snapshot2.getChildrenCount());
                                            for (DataSnapshot child2 : snapshot2.getChildren()) {
                                                Object val2 = child2.getValue();
                                                if (!(val2 instanceof Map)) continue;
                                                @SuppressWarnings("unchecked")
                                                Map<String, Object> ex = (Map<String, Object>) val2;
                                                String nm = ex.get("name") != null ? ex.get("name").toString() : "";
                                                if (!nm.isEmpty() && nm.toLowerCase().contains(qLower)) {
                                                    searchResults.add(ex);
                                                }
                                            }
                                            searchAdapter.notifyDataSetChanged();
                                            searchResultsRecycler.setVisibility(searchResults.isEmpty() ? View.GONE : View.VISIBLE);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "fallback query error", e);
                                            Toast.makeText(Client_workouts_details.this, "Search error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                searchAdapter.notifyDataSetChanged();
                                searchResultsRecycler.setVisibility(View.VISIBLE);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "primary query error", e);
                            Toast.makeText(Client_workouts_details.this, "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

    }
}
