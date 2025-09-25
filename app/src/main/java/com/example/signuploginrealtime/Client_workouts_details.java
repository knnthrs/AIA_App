package com.example.signuploginrealtime;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.adapters.WorkoutExerciseAdapter;
import com.example.signuploginrealtime.models.ExerciseInfo;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

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
    }
}
