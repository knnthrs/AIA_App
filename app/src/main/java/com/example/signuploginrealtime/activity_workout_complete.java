package com.example.signuploginrealtime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

import com.example.signuploginrealtime.ExercisePerformanceData;

public class activity_workout_complete extends AppCompatActivity {

    private Button btnFinish, btnViewStreak;
    private SharedPreferences workoutPrefs;
    private TextToSpeech tts;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ArrayList<ExercisePerformanceData> performanceDataList;
    private static final String TAG = "WorkoutComplete";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_complete);

        btnFinish = findViewById(R.id.btn_end_session);
        btnViewStreak = findViewById(R.id.btn_view_streaks);

        workoutPrefs = getSharedPreferences("workout_prefs", MODE_PRIVATE);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        performanceDataList = (ArrayList<ExercisePerformanceData>) getIntent().getSerializableExtra("performanceData");
        if (performanceDataList == null) {
            performanceDataList = new ArrayList<>();
            Log.d(TAG, "onCreate: performanceDataList was null, initialized to empty list.");
        } else {
            Log.d(TAG, "onCreate: Retrieved performanceDataList. Size: " + performanceDataList.size());
            if (!performanceDataList.isEmpty()) {
                for (ExercisePerformanceData data : performanceDataList) {
                    Log.d(TAG, "onCreate: Performance Data: " + data.toString());
                }
            } else {
                Log.d(TAG, "onCreate: performanceDataList is empty.");
            }
        }

        SharedPreferences prefs = getSharedPreferences("workout_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("showTomorrow", true).apply();

        btnFinish.setOnClickListener(v -> {
            Log.d(TAG, "btnFinish clicked."); // <<< LOG BUTTON CLICK
            if (!isWorkoutRecordedToday()) {
                Log.d(TAG, "btnFinish: Workout NOT recorded today. Calling saveWorkoutForToday, updateStreakData, and saveWorkoutToFirebase."); // <<< LOG BEFORE CALLING SAVE
                saveWorkoutForToday();
                updateStreakData();
                saveWorkoutToFirebase(performanceDataList);
                Toast.makeText(this, "Workout recorded! Keep up the streak!", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "btnFinish: Workout ALREADY recorded today."); // <<< LOG IF ALREADY RECORDED
                Toast.makeText(this, "Workout already recorded for today!", Toast.LENGTH_SHORT).show();
            }
            goToMain();
        });

        btnViewStreak.setOnClickListener(v -> {
            Log.d(TAG, "btnViewStreak clicked."); // <<< LOG BUTTON CLICK
            if (!isWorkoutRecordedToday()) {
                Log.d(TAG, "btnViewStreak: Workout NOT recorded today. Calling saveWorkoutForToday, updateStreakData, and saveWorkoutToFirebase."); // <<< LOG BEFORE CALLING SAVE
                saveWorkoutForToday();
                updateStreakData();
                saveWorkoutToFirebase(performanceDataList);
            } else {
                Log.d(TAG, "btnViewStreak: Workout ALREADY recorded today."); // <<< LOG IF ALREADY RECORDED
            }
            Intent intent = new Intent(activity_workout_complete.this, StreakCalendar.class);
            startActivity(intent);
        });
    }

    private boolean isWorkoutRecordedToday() {
        Set<String> workoutDates = workoutPrefs.getStringSet("workout_dates", new HashSet<>());
        String currentDate = getCurrentDateString();
        boolean isRecorded = workoutDates != null && workoutDates.contains(currentDate);
        // Log.d(TAG, "isWorkoutRecordedToday: Date - " + currentDate + ", Recorded - " + isRecorded); // Optional: more verbose logging
        return isRecorded;
    }

    private void saveWorkoutForToday() {
        // ... (rest of the method remains the same)
        String today = getCurrentDateString();
        String workoutName = getIntent().getStringExtra("workout_name");
        if (workoutName == null || workoutName.isEmpty()) {
            workoutName = "Workout session";
        }
        String workoutDetails = workoutName + " completed";

        Set<String> workoutDates = workoutPrefs.getStringSet("workout_dates", new HashSet<>());
        if (workoutDates == null) {
            workoutDates = new HashSet<>();
        } else {
            workoutDates = new HashSet<>(workoutDates);
        }
        workoutDates.add(today);

        SharedPreferences.Editor editor = workoutPrefs.edit();
        editor.putStringSet("workout_dates", workoutDates);
        editor.putString("workout_details_" + today, workoutDetails);
        editor.commit();
    }

    private void updateStreakData() {
        // ... (rest of the method remains the same)
        int currentStreak = calculateCurrentStreak();
        int longestStreak = workoutPrefs.getInt("longest_streak", 0);
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
        }
        SharedPreferences.Editor editor = workoutPrefs.edit();
        editor.putInt("current_streak", currentStreak);
        editor.putInt("longest_streak", longestStreak);
        editor.putString("last_workout_date", getCurrentDateString());
        editor.commit();
    }

    private int calculateCurrentStreak() {
        // ... (rest of the method remains the same)
        Set<String> workoutDates = workoutPrefs.getStringSet("workout_dates", new HashSet<>());
        if (workoutDates == null || workoutDates.isEmpty()) {
            return 0;
        }
        String today = getCurrentDateString();
        int streak = 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(today));
            for (int i = 0; i < 365; i++) {
                String dateStr = sdf.format(cal.getTime());
                if (workoutDates.contains(dateStr)) {
                    streak++;
                    cal.add(Calendar.DAY_OF_MONTH, -1);
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating streak", e);
            return 0;
        }
        return streak;
    }

    private String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void goToMain() {
        Intent intent = new Intent(activity_workout_complete.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void saveWorkoutToFirebase(ArrayList<ExercisePerformanceData> performances) {
        Log.d(TAG, "saveWorkoutToFirebase method entered."); // <<< LOG AT THE START OF THE METHOD
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "User not logged in, cannot save workout to Firebase.");
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        String currentDate = getCurrentDateString();
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        String workoutName = getIntent().getStringExtra("workout_name");
        int totalExercises = getIntent().getIntExtra("total_exercises", 0);
        String workoutDuration = getIntent().getStringExtra("workout_duration");

        Map<String, Object> workoutData = new HashMap<>();
        workoutData.put("workoutName", workoutName != null ? workoutName : "Workout session");
        workoutData.put("totalExercises", totalExercises);
        workoutData.put("completedExercises", totalExercises);
        workoutData.put("duration", workoutDuration != null ? workoutDuration : "Unknown");
        workoutData.put("date", currentDate);
        workoutData.put("time", currentTime);
        workoutData.put("timestamp", System.currentTimeMillis());
        workoutData.put("status", "completed");

        if (performances != null && !performances.isEmpty()) {
            Log.d(TAG, "Adding " + performances.size() + " exercise performances to Firebase map.");
            workoutData.put("exercisePerformances", performances);
        } else {
            Log.d(TAG, "No exercise performances to add to Firebase map.");
        }

        db.collection("users")
                .document(userId)
                .collection("progress")
                .document(currentDate)
                .set(workoutData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Workout data successfully written to Firestore for date: " + currentDate);
                    updateUserStats(userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error writing workout data to Firestore", e);
                });
    }

    private void updateUserStats(String userId) {
        // ... (rest of the method remains the same)
        Map<String, Object> statsUpdate = new HashMap<>();
        String currentDate = getCurrentDateString();
        statsUpdate.put("lastWorkoutDate", currentDate);
        statsUpdate.put("lastActive", System.currentTimeMillis());
        statsUpdate.put("totalWorkouts", com.google.firebase.firestore.FieldValue.increment(1));

        db.collection("users")
                .document(userId)
                .collection("stats")
                .document("overall")
                .set(statsUpdate, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User stats successfully updated in Firestore.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user stats in Firestore", e);
                });
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
