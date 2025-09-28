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
import com.google.firebase.firestore.FieldValue;
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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            workoutPrefs = getSharedPreferences("workout_prefs_" + userId, MODE_PRIVATE);
        } else {
            workoutPrefs = getSharedPreferences("workout_prefs_default", MODE_PRIVATE);
        }

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
        String workoutName = saveWorkoutForToday(); // save workout locally
        updateStreakData(); // update SharedPreferences streak
        saveWorkoutToFirebase(performanceDataList, workoutName); // save to Firestore

        // Update weekly goal in Firestore
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            updateWeeklyGoal(userId);
        }

        // Mark workout completed for MainActivity refresh
        SharedPreferences.Editor editor = workoutPrefs.edit();
        editor.putBoolean("workout_completed", true);
        editor.apply();

        Toast.makeText(this, "Workout recorded! Keep up the streak!", Toast.LENGTH_SHORT).show();
        goToMain();
    }

    private void viewStreaks() {
        Intent intent = new Intent(activity_workout_complete.this, StreakCalendar.class);
        startActivity(intent);
    }

    // --- Original SharedPreferences functions ---
    private String saveWorkoutForToday() {
        String today = getCurrentDateString();
        String workoutName = generateWorkoutName(); // generate unique name
        String workoutDetails = workoutName + " completed";

        Set<String> workoutNamesToday = workoutPrefs.getStringSet("workout_names_" + today, new HashSet<>());
        workoutNamesToday = new HashSet<>(workoutNamesToday);
        workoutNamesToday.add(workoutName);

        SharedPreferences.Editor editor = workoutPrefs.edit();
        editor.putStringSet("workout_names_" + today, workoutNamesToday);
        editor.putString("last_workout_name", workoutName);
        editor.putString("workout_details_" + today + "_" + workoutName, workoutDetails);
        editor.apply();

        return workoutName;
    }

    private void updateStreakData() {
        int currentStreak = calculateCurrentStreak();
        int longestStreak = workoutPrefs.getInt("longest_streak", 0);
        if (currentStreak > longestStreak) longestStreak = currentStreak;

        int previousWorkouts = workoutPrefs.getInt("total_workouts", 0); // Get previous count
        int totalWorkouts = previousWorkouts + 1; // New total

        SharedPreferences.Editor editor = workoutPrefs.edit();
        editor.putInt("current_streak", currentStreak);
        editor.putInt("longest_streak", longestStreak);
        editor.putString("last_workout_date", getCurrentDateString());
        editor.putInt("total_workouts", totalWorkouts);
        editor.apply();

        // Update Firestore achievements
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            Map<String, Object> updates = new HashMap<>();
            updates.put("workoutsCompleted", totalWorkouts);
            updates.put("currentStreak", currentStreak);

            db.collection("users")
                    .document(userId)
                    .set(updates, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        // ✅ CHECK ACHIEVEMENTS IMMEDIATELY AFTER FIRESTORE UPDATE
                        checkWorkoutAchievements(userId, previousWorkouts, totalWorkouts);
                        checkStreakAchievements(userId, currentStreak);
                    });
        }
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

    // --- Save detailed performance to Firestore ---
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
        statsUpdate.put("totalWorkouts", FieldValue.increment(1));

        db.collection("users")
                .document(userId)
                .collection("stats")
                .document("overall")
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        statsUpdate.put("totalWorkouts", 1);
                        db.collection("users")
                                .document(userId)
                                .collection("stats")
                                .document("overall")
                                .set(statsUpdate);
                    } else {
                        db.collection("users")
                                .document(userId)
                                .collection("stats")
                                .document("overall")
                                .update(statsUpdate);
                    }
                });
    }

    private void updateWeeklyGoal(String userId) {
        Calendar cal = Calendar.getInstance();
        int weekOfYear = cal.get(Calendar.WEEK_OF_YEAR);
        String weekDoc = "week_" + weekOfYear;

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long freqValue = documentSnapshot.getLong("workoutDaysPerWeek");
                        final long freq = (freqValue != null) ? freqValue : 3L;

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("target", freq);
                        updates.put("completed", FieldValue.increment(1));

                        db.collection("users")
                                .document(userId)
                                .collection("currentWorkout")
                                .document(weekDoc)
                                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Weekly goal updated");

                                    // ✅ Now check if goal is met
                                    db.collection("users")
                                            .document(userId)
                                            .collection("currentWorkout")
                                            .document(weekDoc)
                                            .get()
                                            .addOnSuccessListener(weekDocSnap -> {
                                                if (weekDocSnap.exists()) {
                                                    Long completed = weekDocSnap.getLong("completed");
                                                    Long target = weekDocSnap.getLong("target");

                                                    if (completed != null && target != null && completed >= target) {
                                                        sendWeeklyGoalNotification(weekOfYear);
                                                    }
                                                }
                                            });
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error updating weekly goal", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch frequency", e));
    }

    private void sendWeeklyGoalNotification(int weekOfYear) {
        String weekKey = "weekly_goal_notified_" + weekOfYear;

        if (!workoutPrefs.getBoolean(weekKey, false)) {
            NotificationHelper.showNotification(
                    this,
                    "Weekly Goal Achieved 🎉",
                    "Awesome job! You’ve completed your weekly workout goal!"
            );

            workoutPrefs.edit().putBoolean(weekKey, true).apply();
        }
    }

    private void checkWorkoutAchievements(String userId, int previousWorkouts, int newWorkouts) {
        int[] milestones = {1, 10, 25, 50, 100};
        String[] titles = {
                "First Steps! 👟",
                "Getting Strong! 💪",
                "Fitness Pro! 🔥",
                "Warrior! ⚡",
                "Legend! 👑"
        };
        String[] messages = {
                "Congratulations on completing your first workout!",
                "You've completed 10 workouts. Keep up the great work!",
                "25 workouts completed! You're becoming a fitness pro!",
                "50 workouts! You're a true warrior!",
                "100 workouts completed! You're a fitness legend!"
        };

        for (int i = 0; i < milestones.length; i++) {
            int milestone = milestones[i];
            if (previousWorkouts < milestone && newWorkouts >= milestone) {
                createAchievementNotification(userId, titles[i], messages[i]);
                Log.d(TAG, "Achievement unlocked: " + titles[i]);
            }
        }
    }

    private void checkStreakAchievements(String userId, int currentStreak) {
        // Check if we just reached these streak milestones
        String lastStreakKey = "last_streak_notified";
        int lastNotifiedStreak = workoutPrefs.getInt(lastStreakKey, 0);

        if (lastNotifiedStreak < 3 && currentStreak >= 3) {
            createAchievementNotification(userId, "On Fire! 🔥", "3 day workout streak! You're on fire!");
            workoutPrefs.edit().putInt(lastStreakKey, 3).apply();
        }
        if (lastNotifiedStreak < 7 && currentStreak >= 7) {
            createAchievementNotification(userId, "Lightning! ⚡", "7 day streak! You're unstoppable!");
            workoutPrefs.edit().putInt(lastStreakKey, 7).apply();
        }
        if (lastNotifiedStreak < 30 && currentStreak >= 30) {
            createAchievementNotification(userId, "Champion! 🏆", "30 day streak! You're a true champion!");
            workoutPrefs.edit().putInt(lastStreakKey, 30).apply();
        }
    }

    private void createAchievementNotification(String userId, String title, String message) {
        NotificationItem notification = new NotificationItem();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType("achievement");
        notification.setTimestamp(System.currentTimeMillis());
        notification.setRead(false);

        db.collection("notifications")
                .add(notification.toMap())
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Achievement notification created: " + title);

                    // Show local toast as well
                    runOnUiThread(() -> {
                        Toast.makeText(this, title, Toast.LENGTH_LONG).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create achievement notification", e);
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
