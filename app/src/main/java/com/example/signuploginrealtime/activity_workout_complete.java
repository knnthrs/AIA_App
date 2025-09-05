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

public class activity_workout_complete extends AppCompatActivity {

    private Button btnFinish, btnViewStreak;
    private SharedPreferences workoutPrefs;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_complete);

        btnFinish = findViewById(R.id.btn_end_session);
        btnViewStreak = findViewById(R.id.btn_view_streaks);

        workoutPrefs = getSharedPreferences("workout_prefs", MODE_PRIVATE);

        // Initialize TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                tts.speak("Well done! Congratulations!", TextToSpeech.QUEUE_FLUSH, null, "well_done");
            }
        });

        // End session → save workout → update streaks → go to MainActivity
        btnFinish.setOnClickListener(v -> {
            if (!isWorkoutRecordedToday()) {
                saveWorkoutForToday();
                updateStreakData();
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

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}