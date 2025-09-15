package com.example.signuploginrealtime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.signuploginrealtime.ExercisePerformanceData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

        performanceDataList = (ArrayList<ExercisePerformanceData>) getIntent()
                .getSerializableExtra("performanceData");
        if (performanceDataList == null) performanceDataList = new ArrayList<>();

        // TTS: speak automatically when activity starts
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());
                tts.setSpeechRate(1.0f);
                tts.speak("Well done, congratulations!", TextToSpeech.QUEUE_FLUSH, null, "WORKOUT_COMPLETE");
            } else {
                Log.e(TAG, "TTS initialization failed");
            }
        });

        // --- Button Clicks ---
        btnFinish.setOnClickListener(v -> finishWorkout());
        btnViewStreak.setOnClickListener(v -> viewStreaks());
    }

    private void finishWorkout() {
        String workoutName = saveWorkoutForToday(); // save workout
        updateStreakData();
        saveWorkoutToFirebase(performanceDataList, workoutName);

        Toast.makeText(this, "Workout recorded! Keep up the streak!", Toast.LENGTH_SHORT).show();
        goToMain();
    }

    private void viewStreaks() {
        Intent intent = new Intent(activity_workout_complete.this, StreakCalendar.class);
        startActivity(intent);
    }

    private String saveWorkoutForToday() {
        String today = getCurrentDateString();
        String workoutName = generateWorkoutName(); // generate unique name
        String workoutDetails = workoutName + " completed";

        Set<String> workoutNamesToday = workoutPrefs.getStringSet("workout_names_" + today, new HashSet<>());
        workoutNamesToday = new HashSet<>(workoutNamesToday);
        workoutNamesToday.add(workoutName);

        SharedPreferences.Editor editor = workoutPrefs.edit();
        editor.putStringSet("workout_names_" + today, workoutNamesToday);
        editor.putString("last_workout_name", workoutName); // store for reference
        editor.putString("workout_details_" + today + "_" + workoutName, workoutDetails);
        editor.apply();

        return workoutName;
    }

    private void updateStreakData() {
        int currentStreak = calculateCurrentStreak();
        int longestStreak = workoutPrefs.getInt("longest_streak", 0);
        if (currentStreak > longestStreak) longestStreak = currentStreak;

        int totalWorkouts = workoutPrefs.getInt("total_workouts", 0) + 1;

        SharedPreferences.Editor editor = workoutPrefs.edit();
        editor.putInt("current_streak", currentStreak);
        editor.putInt("longest_streak", longestStreak);
        editor.putString("last_workout_date", getCurrentDateString());
        editor.putInt("total_workouts", totalWorkouts);
        editor.apply();
    }

    private int calculateCurrentStreak() {
        Set<String> workoutDates = workoutPrefs.getStringSet("workout_names_" + getCurrentDateString(), new HashSet<>());
        if (workoutDates == null || workoutDates.isEmpty()) return 0;

        String today = getCurrentDateString();
        int streak = 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(today));
            for (int i = 0; i < 365; i++) {
                String dateStr = sdf.format(cal.getTime());
                Set<String> workouts = workoutPrefs.getStringSet("workout_names_" + dateStr, new HashSet<>());
                if (workouts != null && !workouts.isEmpty()) {
                    streak++;
                    cal.add(Calendar.DAY_OF_MONTH, -1);
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating streak", e);
        }
        return streak;
    }

    private String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String generateWorkoutName() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        String period;
        if (hour < 12) period = "Morning";
        else if (hour < 18) period = "Afternoon";
        else period = "Evening";

        String today = getCurrentDateString();
        Set<String> workoutNamesToday = workoutPrefs.getStringSet("workout_names_" + today, new HashSet<>());

        int count = 1;
        if (workoutNamesToday != null) {
            for (String name : workoutNamesToday) {
                if (name.startsWith(period)) count++;
            }
        }

        return period + " Workout " + count;
    }

    private void goToMain() {
        Intent intent = new Intent(activity_workout_complete.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void saveWorkoutToFirebase(ArrayList<ExercisePerformanceData> performances, String workoutName) {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        String currentDate = getCurrentDateString();
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        int totalExercises = getIntent().getIntExtra("total_exercises", 0);
        String workoutDuration = getIntent().getStringExtra("workout_duration");

        Map<String, Object> workoutData = new HashMap<>();
        workoutData.put("workoutName", workoutName);
        workoutData.put("totalExercises", totalExercises);
        workoutData.put("completedExercises", totalExercises);
        workoutData.put("duration", workoutDuration != null ? workoutDuration : "Unknown");
        workoutData.put("date", currentDate);
        workoutData.put("time", currentTime);
        workoutData.put("timestamp", System.currentTimeMillis());
        workoutData.put("status", "completed");

        if (performances != null && !performances.isEmpty()) {
            workoutData.put("exercisePerformances", performances);
        }

        db.collection("users")
                .document(userId)
                .collection("progress")
                .document(currentDate + "_" + workoutName)
                .set(workoutData)
                .addOnSuccessListener(aVoid -> updateUserStats(userId))
                .addOnFailureListener(e -> Log.e(TAG, "Error writing workout data", e));
    }

    private void updateUserStats(String userId) {
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
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User stats updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating stats", e));
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
