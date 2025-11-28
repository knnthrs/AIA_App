package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

    private static final String TAG = "ClientWorkoutDetails";

    private TextView clientName, clientWeight, clientHeight, clientGoal, workoutFrequency;
    private TextView clientRemainingSessions;
    private TextView clientFitnessLevel, clientAge, clientHealthIssues, clientBodyFocus;
    private Button markSessionCompleteButton;
    private Set<String> alreadyAddedNames = new HashSet<>();
    private SearchResultsAdapter searchAdapter;
    private EditText searchWorkouts;
    private RecyclerView searchResultsRecycler;
    private ImageView searchBackButton;
    private View workoutsSection;
    private View searchOverlayContainer;
    private View mainContent;

    private WorkoutExerciseAdapter workoutAdapter;
    private List<WorkoutExercise> workoutExercises = new ArrayList<>();

    private String clientUid;
    private DocumentReference workoutRef;
    private Button saveButton;
    private int currentSessions = 0;
    private boolean hasChanges = false;
    private FirebaseFirestore db;

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

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // --- Bind views
        clientName = findViewById(R.id.client_name);
        clientWeight = findViewById(R.id.client_weight);
        clientHeight = findViewById(R.id.client_height);
        clientGoal = findViewById(R.id.client_goal);
        workoutFrequency = findViewById(R.id.workout_frequency);
        clientRemainingSessions = findViewById(R.id.client_remaining_sessions);
        clientFitnessLevel = findViewById(R.id.client_fitness_level);
        clientAge = findViewById(R.id.client_age);
        clientHealthIssues = findViewById(R.id.client_health_issues);
        clientBodyFocus = findViewById(R.id.client_body_focus);
        markSessionCompleteButton = findViewById(R.id.btn_mark_session_complete);
        saveButton = findViewById(R.id.btn_save_changes);
        saveButton.setVisibility(View.GONE);

        RecyclerView recyclerView = findViewById(R.id.workouts_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        clientUid = getIntent().getStringExtra("client_uid");

        if (clientUid == null) {
            Toast.makeText(this, "No client selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup mark session button click listener
        markSessionCompleteButton.setOnClickListener(v -> markSessionComplete(db));

        // ✅ NEW: Setup Manage Food Recommendations click listener
        androidx.cardview.widget.CardView manageFoodCard = findViewById(R.id.manage_food_card);
        manageFoodCard.setOnClickListener(v -> openFoodManagement());

        // Initialize search overlay and main content
        searchOverlayContainer = findViewById(R.id.search_overlay_container);
        mainContent = findViewById(R.id.main_content);

        // Initialize search trigger in main content
        View searchTrigger = findViewById(R.id.search_trigger);
        searchTrigger.setOnClickListener(v -> showSearchOverlay());

        // Initialize search back button
        searchBackButton = findViewById(R.id.search_back_button);
        searchBackButton.setOnClickListener(v -> clearSearch());

        // Initialize workouts section
        workoutsSection = findViewById(R.id.workouts_section);

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

                        // ✅ height / weight can be number or string
                        Object wObj = snapshot.get("weight");
                        Object hObj = snapshot.get("height");
                        clientWeight.setText(formatNumberField(wObj, "kg"));
                        clientHeight.setText(formatNumberField(hObj, "cm"));

                        clientGoal.setText(snapshot.getString("fitnessGoal"));
                        workoutFrequency.setText(snapshot.getString("fitnessLevel"));

                        // Fitness Level
                        String fitnessLevel = snapshot.getString("fitnessLevel");
                        clientFitnessLevel.setText(
                                fitnessLevel != null && !fitnessLevel.isEmpty() ? fitnessLevel : "Not set"
                        );

                        // ✅ age can be number or string
                        Object ageObj = snapshot.get("age");
                        String ageText = formatNumberField(ageObj, "years");
                        clientAge.setText(!ageText.isEmpty() ? ageText : "Not set");

                        // ✅ healthIssues may be List<String> or String
                        Object healthIssuesObj = snapshot.get("healthIssues");
                        String healthDisplay = formatFlexibleStringOrList(healthIssuesObj, "None");
                        clientHealthIssues.setText(healthDisplay);

                        // ✅ bodyFocus may be List<String> or String
                        Object bodyFocusObj = snapshot.get("bodyFocus");
                        String bodyFocusDisplay = formatFlexibleStringOrList(bodyFocusObj, "Not set");
                        clientBodyFocus.setText(bodyFocusDisplay);
                    } else {
                        Log.w(TAG, "Client document does not exist: " + clientUid);
                        Toast.makeText(this, "Client data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading client data: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to load client data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

        // --- Load PT Sessions ---
        loadPTSessions(db);

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
                                info.setTargetMuscles((List<String>) exerciseInfoMap.get("targetMuscles"));
                                info.setEquipments((List<String>) exerciseInfoMap.get("equipments"));
                            }

                            WorkoutExercise workoutExercise = new WorkoutExercise();
                            workoutExercise.setExerciseInfo(info);
                            workoutExercise.setReps(safeInt(exerciseMap.get("reps"), 0));
                            workoutExercise.setSets(safeInt(exerciseMap.get("sets"), 0));
                            workoutExercise.setRestSeconds(safeInt(exerciseMap.get("restSeconds"), 60));

                            workoutExercises.add(workoutExercise);

                            if (info.getName() != null) {
                                alreadyAddedNames.add(info.getName().toLowerCase());
                            }
                        }
                    }
                }
            } else {
                Log.d(TAG, "No workout document found for client, showing empty state");
            }

            workoutAdapter = new WorkoutExerciseAdapter(this, workoutExercises, true);
            workoutAdapter.setOnWorkoutChangedListener(() -> onWorkoutChanged());
            recyclerView.setAdapter(workoutAdapter);

            if (searchAdapter != null) {
                searchAdapter.setAlreadyAdded(alreadyAddedNames);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading workout exercises: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to load workouts: " + e.getMessage(), Toast.LENGTH_LONG).show();

            // Still initialize adapter with empty list so UI doesn't break
            workoutAdapter = new WorkoutExerciseAdapter(this, workoutExercises, true);
            workoutAdapter.setOnWorkoutChangedListener(() -> onWorkoutChanged());
            recyclerView.setAdapter(workoutAdapter);
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

        // ✅ Updated search functionality with new adapter methods (using simple get + filter)
        searchWorkouts.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s == null ? "" : s.toString().trim();

                if (query.isEmpty()) {
                    searchAdapter.clearResults();
                    return;
                }

                String qLower = query.toLowerCase();

                // Simpler and more robust: load all, then filter by name in-memory
                workoutsRef.get()
                        .addOnSuccessListener(snapshot -> {
                            List<Map<String, Object>> newResults = new ArrayList<>();

                            for (DataSnapshot child : snapshot.getChildren()) {
                                Object val = child.getValue();
                                if (!(val instanceof Map)) continue;
                                Map<String, Object> exercise = (Map<String, Object>) val;

                                String name = exercise.get("name") != null
                                        ? exercise.get("name").toString()
                                        : "";
                                if (!name.isEmpty() && name.toLowerCase().contains(qLower)) {
                                    newResults.add(exercise);
                                }
                            }

                            searchAdapter.updateResults(newResults);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "search error", e);
                            Toast.makeText(Client_workouts_details.this,
                                    "Search failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            searchAdapter.clearResults();
                        });
            }
        });

        // --- 4) Save changes button ---
        saveButton.setOnClickListener(v -> saveWorkoutToFirestore());

        // Handle back press to clear search if visible
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (searchOverlayContainer.getVisibility() == View.VISIBLE) {
                    clearSearch();
                } else {
                    finish();
                }
            }
        });
    }

    // Show search overlay at the top
    private void showSearchOverlay() {
        searchOverlayContainer.setVisibility(View.VISIBLE);
        searchWorkouts.requestFocus();

        // Show keyboard
        android.view.inputmethod.InputMethodManager imm =
            (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(searchWorkouts, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // Clear search and hide overlay
    private void clearSearch() {
        searchWorkouts.setText("");
        searchAdapter.clearResults();
        searchOverlayContainer.setVisibility(View.GONE);
        searchWorkouts.clearFocus();
        hideKeyboard();
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
        info.setTargetMuscles((List<String>) exercise.get("targetMuscles"));
        info.setEquipments((List<String>) exercise.get("equipments"));

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

        // ✅ Mark as changed so Save button shows up
        onWorkoutChanged();

        // ✅ Clear search after adding exercise
        clearSearch();

        Toast.makeText(this, "Exercise added! Tap Save to persist changes.", Toast.LENGTH_SHORT).show();
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
                info.put("targetMuscles", we.getExerciseInfo().getTargetMuscles());
                info.put("equipments", we.getExerciseInfo().getEquipments());
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

    // Load PT Sessions from membership
    private void loadPTSessions(FirebaseFirestore db) {
        db.collection("memberships")
                .document(clientUid)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading PT sessions", error);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Long sessions = documentSnapshot.getLong("sessions");
                        currentSessions = sessions != null ? sessions.intValue() : 0;
                        clientRemainingSessions.setText(String.valueOf(currentSessions));

                        // Disable button if no sessions remaining
                        if (currentSessions <= 0) {
                            markSessionCompleteButton.setEnabled(false);
                            markSessionCompleteButton.setAlpha(0.5f);
                        } else {
                            markSessionCompleteButton.setEnabled(true);
                            markSessionCompleteButton.setAlpha(1.0f);
                        }
                    } else {
                        currentSessions = 0;
                        clientRemainingSessions.setText("0");
                        markSessionCompleteButton.setEnabled(false);
                        markSessionCompleteButton.setAlpha(0.5f);
                    }
                });
    }

    // Mark session as complete and decrease count
    private void markSessionComplete(FirebaseFirestore db) {
        if (currentSessions <= 0) {
            Toast.makeText(this, "No sessions remaining!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Mark Session Complete")
                .setMessage("Confirm that you completed a session with this client? This will reduce their remaining sessions by 1.")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    // Decrease sessions in membership document
                    int newSessionCount = currentSessions - 1;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("sessions", newSessionCount);
                    updates.put("scheduleDate", null);  // Clear schedule
                    updates.put("scheduleTime", null);  // Clear schedule

                    db.collection("memberships")
                            .document(clientUid)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Session marked complete! Remaining: " + newSessionCount, Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Session completed. New count: " + newSessionCount + ". Schedule cleared.");

                                // Create notification for user to book next schedule
                                if (newSessionCount > 0) {
                                    createRescheduleNotification(clientUid, newSessionCount);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to update sessions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Error updating sessions", e);
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createRescheduleNotification(String userId, int remainingSessions) {
        String title = "Session Completed! 🎉";
        String message = "Your session is complete. You have " + remainingSessions +
                        " session(s) remaining. Tap the schedule icon on your membership card to book your next session!";

        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("type", "session_completed");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "✅ Reschedule notification created: " + docRef.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to create reschedule notification", e);
                });
    }

    private void onWorkoutChanged() {
        if (!hasChanges) {
            hasChanges = true;
            saveButton.setVisibility(View.VISIBLE); // show the save button
        }
    }

    // Hide soft keyboard
    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        android.view.inputmethod.InputMethodManager imm =
            (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // Helper method to safely convert Object to int, preventing crashes from type mismatches
    private int safeInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof Double) return ((Double) value).intValue();
        if (value instanceof Integer) return (Integer) value;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            Log.w(TAG, "Could not parse int from: " + value + ", using default: " + defaultValue);
            return defaultValue;
        }
    }

    // ✅ helper to format numeric fields that might be Long / Double / String / null
    private String formatNumberField(Object value, String unit) {
        if (value == null) return "";
        try {
            double num;
            if (value instanceof Long) num = ((Long) value).doubleValue();
            else if (value instanceof Double) num = (Double) value;
            else if (value instanceof Integer) num = ((Integer) value).doubleValue();
            else num = Double.parseDouble(value.toString());
            if (unit == null || unit.isEmpty()) {
                return String.valueOf((int) num);
            } else {
                return ((int) num) + " " + unit;
            }
        } catch (Exception e) {
            Log.w(TAG, "formatNumberField: could not parse " + value, e);
            return value.toString();
        }
    }

    // ✅ helper to safely handle String or List<String> Firestore fields
    private String formatFlexibleStringOrList(Object field, String defaultText) {
        if (field == null) return defaultText;
        if (field instanceof String) {
            String s = ((String) field).trim();
            if (s.isEmpty()) return defaultText;
            return s;
        }
        if (field instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) field;
            if (list.isEmpty()) return defaultText;
            List<String> parts = new ArrayList<>();
            for (Object o : list) {
                if (o != null) {
                    String s = o.toString().trim();
                    if (!s.isEmpty()) parts.add(s);
                }
            }
            if (parts.isEmpty()) return defaultText;
            return android.text.TextUtils.join(", ", parts);
        }
        return field.toString();
    }

    /**
     * Opens food management activity for this specific client
     */
    private void openFoodManagement() {
        Intent intent = new Intent(this, CoachFoodManagementActivity.class);
        intent.putExtra("clientId", clientUid);
        intent.putExtra("clientName", clientName.getText().toString());
        startActivity(intent);
    }
}