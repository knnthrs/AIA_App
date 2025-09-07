package com.example.signuploginrealtime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

// ADD ONLY THESE FIREBASE IMPORTS
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class activity_workout_complete extends AppCompatActivity {

    private Button btnFinish, btnViewStreak;
    private SharedPreferences workoutPrefs;
    private TextToSpeech tts;

    // ADD ONLY THESE FIREBASE VARIABLES
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_complete);

        btnFinish = findViewById(R.id.btn_end_session);
        btnViewStreak = findViewById(R.id.btn_view_streaks);

        workoutPrefs = getSharedPreferences("workout_prefs", MODE_PRIVATE);

        // ADD ONLY THESE FIREBASE INITIALIZATIONS
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                tts.speak("Well done! Congratulations!", TextToSpeech.QUEUE_FLUSH, null, "well_done");
            }
        });


        // Save flag so MainActivity knows to show tomorrow's activities
        SharedPreferences prefs = getSharedPreferences("workout_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("showTomorrow", true).apply();


        // End session → save workout → update streaks → go to MainActivity
        btnFinish.setOnClickListener(v -> {
            if (!isWorkoutRecordedToday()) {
                saveWorkoutForToday();
                updateStreakData();
                // ADD ONLY THIS FIREBASE SAVE CALL
                saveWorkoutToFirebase();
                Toast.makeText(this, "Workout recorded! Keep up the streak!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Workout already recorded for today!", Toast.LENGTH_SHORT).show();
            }
            goToMain();
        });

        // View streak calendar
        btnViewStreak.setOnClickListener(v -> {
            if (!isWorkoutRecordedToday()) {
                saveWorkoutForToday();
                updateStreakData();
                // ADD ONLY THIS FIREBASE SAVE CALL
                saveWorkoutToFirebase();
            }

            Intent intent = new Intent(activity_workout_complete.this, StreakCalendar.class);
            startActivity(intent);
        });
    }

    private boolean isWorkoutRecordedToday() {
        Set<String> workoutDates = workoutPrefs.getStringSet("workout_dates", new HashSet<>());
        return workoutDates != null && workoutDates.contains(getCurrentDateString());
    }

    private void saveWorkoutForToday() {
        String today = getCurrentDateString();

        // Get workout details from intent if available
        String workoutName = getIntent().getStringExtra("workout_name");
        if (workoutName == null || workoutName.isEmpty()) {
            workoutName = "Workout session";
        }
        String workoutDetails = workoutName + " completed";

        // Get existing workout dates
        Set<String> workoutDates = workoutPrefs.getStringSet("workout_dates", new HashSet<>());
        if (workoutDates == null) {
            workoutDates = new HashSet<>();
        } else {
            workoutDates = new HashSet<>(workoutDates); // Create new set to avoid issues
        }

        // Add today's date
        workoutDates.add(today);

        // Save to preferences
        SharedPreferences.Editor editor = workoutPrefs.edit();
        editor.putStringSet("workout_dates", workoutDates);
        editor.putString("workout_details_" + today, workoutDetails);
        editor.commit(); // Use commit for immediate save
    }

    private void updateStreakData() {
        // Calculate current streak
        int currentStreak = calculateCurrentStreak();
        int longestStreak = workoutPrefs.getInt("longest_streak", 0);

        // Update longest streak if current streak is higher
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
        }

        // Save streaks
        SharedPreferences.Editor editor = workoutPrefs.edit();
        editor.putInt("current_streak", currentStreak);
        editor.putInt("longest_streak", longestStreak);
        editor.putString("last_workout_date", getCurrentDateString());
        editor.commit();
    }

    private int calculateCurrentStreak() {
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

            // Check each day backwards from today
            for (int i = 0; i < 365; i++) { // Prevent infinite loop
                String dateStr = sdf.format(cal.getTime());

                if (workoutDates.contains(dateStr)) {
                    streak++;
                    cal.add(Calendar.DAY_OF_MONTH, -1); // Go back one day
                } else {
                    break;
                }
            }
        } catch (Exception e) {
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

    // ADD ONLY THESE NEW FIREBASE METHODS
    private void saveWorkoutToFirebase() {
        // Check if user is authenticated
        if (mAuth.getCurrentUser() == null) {
            return; // User not logged in, can't save data
        }

        String userId = mAuth.getCurrentUser().getUid();
        String currentDate = getCurrentDateString();
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        // Get workout details from intent
        String workoutName = getIntent().getStringExtra("workout_name");
        int totalExercises = getIntent().getIntExtra("total_exercises", 0);
        String workoutDuration = getIntent().getStringExtra("workout_duration");

        // Create workout data map
        Map<String, Object> workoutData = new HashMap<>();
        workoutData.put("workoutName", workoutName != null ? workoutName : "Workout session");
        workoutData.put("totalExercises", totalExercises);
        workoutData.put("completedExercises", totalExercises); // Assuming all exercises were completed
        workoutData.put("duration", workoutDuration != null ? workoutDuration : "Unknown");
        workoutData.put("date", currentDate);
        workoutData.put("time", currentTime);
        workoutData.put("timestamp", System.currentTimeMillis());
        workoutData.put("status", "completed");

        // Save to Firestore: users/{userId}/progress/{date}
        db.collection("users")
                .document(userId)
                .collection("progress")
                .document(currentDate)
                .set(workoutData)
                .addOnSuccessListener(aVoid -> {
                    // Successfully saved
                    updateUserStats(userId);
                })
                .addOnFailureListener(e -> {
                    // Handle error
                    e.printStackTrace();
                });
    }

    private void updateUserStats(String userId) {
        // Update user's overall statistics
        Map<String, Object> statsUpdate = new HashMap<>();

        String currentDate = getCurrentDateString();

        statsUpdate.put("lastWorkoutDate", currentDate);
        statsUpdate.put("lastActive", System.currentTimeMillis());
        statsUpdate.put("totalWorkouts", com.google.firebase.firestore.FieldValue.increment(1));

        // Update or create user stats
        db.collection("users")
                .document(userId)
                .collection("stats")
                .document("overall")
                .set(statsUpdate, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Stats updated successfully
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
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