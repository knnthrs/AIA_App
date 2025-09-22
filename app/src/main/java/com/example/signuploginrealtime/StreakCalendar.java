package com.example.signuploginrealtime;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class StreakCalendar extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView selectedDateInfo;
    private TextView streakStats;
    private SharedPreferences workoutPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streak_calendar);

        ImageButton backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(v -> finish());

        calendarView = findViewById(R.id.calendarView);
        selectedDateInfo = findViewById(R.id.selectedDateInfo);
        streakStats = findViewById(R.id.streakStats);

        // ✅ Per-user SharedPreferences
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        workoutPrefs = getSharedPreferences("workout_prefs_" + userId, MODE_PRIVATE);

        updateStreakStatistics();
        showDateInfo(Calendar.getInstance());

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            showDateInfo(selectedDate);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStreakStatistics();
        showDateInfo(Calendar.getInstance());
    }

    private void updateStreakStatistics() {
        int currentStreak = calculateCurrentStreak();
        int totalWorkouts = workoutPrefs.getInt("total_workouts", 0);
        int longestStreak = workoutPrefs.getInt("longest_streak", 0);

        String statsText = "Current Streak: " + currentStreak + " days\n" +
                "Total Workouts: " + totalWorkouts + "\n" +
                "Longest Streak: " + longestStreak + " days";

        streakStats.setText(statsText);
    }

    private int calculateCurrentStreak() {
        Set<String> allDates = workoutPrefs.getStringSet("workout_names_" + getCurrentDateString(), new HashSet<>());
        if (allDates == null || allDates.isEmpty()) return 0;

        int streak = 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();

            while (true) {
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
            e.printStackTrace();
        }
        return streak;
    }

    private void showDateInfo(Calendar selectedDate) {
        String dateString = formatDateToString(selectedDate);
        String displayDate = formatDateForDisplay(selectedDate);

        Set<String> workouts = workoutPrefs.getStringSet("workout_names_" + dateString, new HashSet<>());
        if (workouts != null && !workouts.isEmpty()) {
            StringBuilder details = new StringBuilder();
            for (String workout : workouts) {
                String detail = workoutPrefs.getString("workout_details_" + dateString + "_" + workout, workout);
                details.append("✨ ").append(detail).append("\n");
            }
            selectedDateInfo.setText("✅ " + displayDate + "\n" + details.toString().trim());
            selectedDateInfo.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            selectedDateInfo.setText("📅 " + displayDate + "\nNo workout recorded");
            selectedDateInfo.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    private String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(Calendar.getInstance().getTime());

    }

    private String formatDateToString(Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    private String formatDateForDisplay(Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }
}
