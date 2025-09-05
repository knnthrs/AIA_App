package com.example.signuploginrealtime;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class StreakCalendar extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView selectedDateInfo;
    private TextView streakStats;
    private SharedPreferences workoutPrefs;
    private Set<String> workoutDates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streak_calendar);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Workout History");
        }

        // Initialize views
        calendarView = findViewById(R.id.calendarView);
        selectedDateInfo = findViewById(R.id.selectedDateInfo);
        streakStats = findViewById(R.id.streakStats);

        workoutPrefs = getSharedPreferences("workout_prefs", MODE_PRIVATE);

        // Load workout dates from preferences
        loadWorkoutDates();

        // Update streak statistics
        updateStreakStatistics();

        // Set calendar listener
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            showDateInfo(selectedDate);
        });

        // Show today's info initially
        showDateInfo(Calendar.getInstance());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        loadWorkoutDates();
        updateStreakStatistics();
        showDateInfo(Calendar.getInstance());
    }

    private void loadWorkoutDates() {
        workoutDates = workoutPrefs.getStringSet("workout_dates", new HashSet<>());
        if (workoutDates == null) {
            workoutDates = new HashSet<>();
        }
    }

    private void updateStreakStatistics() {
        int currentStreak = calculateCurrentStreak();
        int totalWorkouts = workoutDates.size();
        int longestStreak = workoutPrefs.getInt("longest_streak", calculateLongestStreak());

        // Update current streak in preferences
        workoutPrefs.edit().putInt("current_streak", currentStreak).apply();

        // Update longest streak if current is higher
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
            workoutPrefs.edit().putInt("longest_streak", longestStreak).apply();
        }

        String statsText = "Current Streak: " + currentStreak + " days\n" +
                "Total Workouts: " + totalWorkouts + "\n" +
                "Longest Streak: " + longestStreak + " days";

        streakStats.setText(statsText);
    }

    private int calculateCurrentStreak() {
        if (workoutDates.isEmpty()) return 0;

        String today = formatDateToString(Calendar.getInstance());
        int streak = 0;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(today));

            // Check each day backwards from today
            while (true) {
                String dateStr = sdf.format(cal.getTime());
                if (workoutDates.contains(dateStr)) {
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

    private int calculateLongestStreak() {
        if (workoutDates.isEmpty()) return 0;

        // Convert dates to sorted array
        String[] sortedDates = workoutDates.toArray(new String[0]);
        java.util.Arrays.sort(sortedDates);

        int maxStreak = 1;
        int currentStreakCount = 1;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            for (int i = 1; i < sortedDates.length; i++) {
                Date prevDate = sdf.parse(sortedDates[i - 1]);
                Date currDate = sdf.parse(sortedDates[i]);

                if (prevDate == null || currDate == null) continue;

                // Check if dates are consecutive
                long diffInMillies = currDate.getTime() - prevDate.getTime();
                long diffInDays = diffInMillies / (24 * 60 * 60 * 1000);

                if (diffInDays == 1) {
                    currentStreakCount++;
                    maxStreak = Math.max(maxStreak, currentStreakCount);
                } else {
                    currentStreakCount = 1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return maxStreak;
    }

    private void showDateInfo(Calendar selectedDate) {
        String dateString = formatDateToString(selectedDate);
        String displayDate = formatDateForDisplay(selectedDate);

        if (workoutDates.contains(dateString)) {
            String workoutDetails = getWorkoutDetailsForDate(dateString);
            selectedDateInfo.setText("✅ " + displayDate + "\n" + workoutDetails);
            selectedDateInfo.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            selectedDateInfo.setText("📅 " + displayDate + "\nNo workout recorded");
            selectedDateInfo.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    private String getWorkoutDetailsForDate(String dateString) {
        String workoutKey = "workout_details_" + dateString;
        String details = workoutPrefs.getString(workoutKey, "");

        if (details != null && !details.isEmpty()) {
            return "✨ " + details;
        } else {
            return "✨ Workout completed";
        }
    }

    private String formatDateToString(Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    private String formatDateForDisplay(Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    // Static method to save workout for a specific date
    public static void saveWorkoutForDate(SharedPreferences prefs, String date, String workoutDetails) {
        Set<String> workoutDates = prefs.getStringSet("workout_dates", new HashSet<>());
        if (workoutDates == null) {
            workoutDates = new HashSet<>();
        } else {
            workoutDates = new HashSet<>(workoutDates); // Create new set to avoid modifying original
        }

        workoutDates.add(date);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("workout_dates", workoutDates);
        editor.putString("workout_details_" + date, workoutDetails);
        editor.apply();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}