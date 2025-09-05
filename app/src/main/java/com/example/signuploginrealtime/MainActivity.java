package com.example.signuploginrealtime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.text.Html;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

// Import the necessary classes for workout generation
import com.example.signuploginrealtime.models.WgerExerciseResponse;
import com.example.signuploginrealtime.models.WgerExercise;
import com.example.signuploginrealtime.models.ExerciseInfo;
import com.example.signuploginrealtime.logic.AdvancedWorkoutDecisionMaker;
import com.example.signuploginrealtime.api.ApiClient;
import com.example.signuploginrealtime.api.WgerApiService;
import com.example.signuploginrealtime.logic.WorkoutProgression;
import com.example.signuploginrealtime.models.UserProfile;
import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    TextView greetingText;
    TextView membershipStatus;
    TextView planType;
    TextView expiryDate;
    TextView streakDisplay; // New streak display
    CardView streakCard; // New streak card
    CardView activitiesCard; // Today's activities card
    LinearLayout activitiesContainer; // Container for exercise cards
    FloatingActionButton fab;
    FirebaseAuth mAuth;
    DatabaseReference userRef;
    BottomNavigationView bottomNavigationView;
    ValueEventListener userDataListener;
    SharedPreferences workoutPrefs; // For streak data
    private WgerApiService apiService; // For fetching exercises

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is authenticated first
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User not logged in, redirect to login
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Initialize SharedPreferences for workout data
        workoutPrefs = getSharedPreferences("workout_prefs", MODE_PRIVATE);

        // Initialize API service
        apiService = ApiClient.getClient().create(WgerApiService.class);

        // Initialize views
        fab = findViewById(R.id.fab);
        greetingText = findViewById(R.id.greeting_text);
        membershipStatus = findViewById(R.id.membershipStatus);
        planType = findViewById(R.id.planType);
        expiryDate = findViewById(R.id.expiryDate);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Initialize new streak views (you'll need to add these to your layout)
        streakDisplay = findViewById(R.id.streak_number);  // This will show just the number
        streakCard = findViewById(R.id.streak_counter_card);  // This is the clickable card

        // Initialize activities views
        activitiesCard = findViewById(R.id.activities_card);
        activitiesContainer = findViewById(R.id.activities_horizontal_container);

        ImageView testImage = findViewById(R.id.testImage);
        LinearLayout promoLayout = findViewById(R.id.promoLayout);

        // Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Reference to promotions/latest
        DocumentReference latestPromoRef = db.collection("promotions").document("latest");

        // Listen for changes in Firestore
        latestPromoRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w("Firestore", "Listen failed.", e);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                // Fetch the "imageUrl" field
                String imageUrl = snapshot.getString("imageUrl");

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    // Load the image into your preview ImageView
                    Glide.with(this)
                            .load(imageUrl)
                            .placeholder(android.R.drawable.ic_menu_report_image)
                            .error(android.R.drawable.ic_delete)
                            .into(testImage);

                    // ✅ Pass the URL to Promo Activity when clicked
                    promoLayout.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, Promo.class);
                        intent.putExtra("promoUrl", imageUrl);
                        startActivity(intent);
                    });
                }
            } else {
                Log.d("Firestore", "No data found in latest document");
            }
        });

        // Membership card click → go to selection screen
        findViewById(R.id.membershipCard).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SelectMembership.class);
            startActivity(intent);
        });

        // Streak card click → go to StreakCalendar
        if (streakCard != null) {
            streakCard.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, StreakCalendar.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        // Activities card click → go to WorkoutList
        if (activitiesCard != null) {
            activitiesCard.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, WorkoutList.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        // Notification Bell icon click listener
        ImageView bellIcon = findViewById(R.id.bell_icon);
        if (bellIcon != null) {
            bellIcon.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, Notification.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        // FAB click listener - Open QR scanner only (NO workout saved here)
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, QR.class);
            startActivity(intent);
        });

        // Setup bottom navigation
        bottomNavigationView.setSelectedItemId(R.id.item_1);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.item_1) {
                return true;
            } else if (itemId == R.id.item_2) {
                startActivity(new Intent(getApplicationContext(), Profile.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.item_3) {
                startActivity(new Intent(getApplicationContext(), WorkoutList.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.item_4) {
                startActivity(new Intent(getApplicationContext(), Achievement.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        // Badge for Profile
        BadgeDrawable badgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.item_2);

        // Load user data with real-time updates
        loadUserData();

        // Update streak display
        updateStreakDisplay();

        // Load today's activities
        loadTodaysActivities();
    }

    // New method to load today's activities
    private void loadTodaysActivities() {
        // Example exercise IDs (same as WorkoutList)
        String exerciseIds = "9,12,20,31,41,43,46,48,50,51,56,57";

        apiService.getExercisesInfo(exerciseIds, 2).enqueue(new Callback<WgerExerciseResponse>() {
            @Override
            public void onResponse(Call<WgerExerciseResponse> call, Response<WgerExerciseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ExerciseInfo> exercises = new ArrayList<>();

                    for (WgerExercise w : response.body().getResults()) {
                        ExerciseInfo e = new ExerciseInfo();
                        String name = "Unnamed Exercise";

                        if (w.getTranslations() != null) {
                            for (WgerExercise.Translation t : w.getTranslations()) {
                                if (t.getLanguage() == 2) { // English
                                    name = t.getName() != null ? t.getName() : name;
                                    break;
                                }
                            }
                        }

                        e.setName(name);
                        exercises.add(e);
                    }

                    // Generate workout to get exercise names
                    generateTodaysWorkout(exercises);
                } else {
                    // Use dummy data if API fails
                    useDummyActivities();
                }
            }

            @Override
            public void onFailure(Call<WgerExerciseResponse> call, Throwable t) {
                Log.e("MainActivity", "Failed to fetch exercises: " + t.getMessage());
                useDummyActivities();
            }
        });
    }

    private void generateTodaysWorkout(List<ExerciseInfo> availableExercises) {
        UserProfile userProfile = createEmptyProfileForProgression();
        userProfile.setFitnessGoal("lose weight");
        userProfile.setAge(25);

        Workout baseWorkout = AdvancedWorkoutDecisionMaker.generatePersonalizedWorkout(
                availableExercises,
                userProfile
        );

        Workout finalWorkout = WorkoutProgression.generateProgressiveWorkout(
                baseWorkout,
                1, // Current day
                userProfile
        );

        if (finalWorkout != null && finalWorkout.getExercises() != null) {
            displayTodaysActivities(finalWorkout.getExercises());
        } else {
            useDummyActivities();
        }
    }

    private void displayTodaysActivities(List<WorkoutExercise> workoutExercises) {
        activitiesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        // Limit to first 4-5 exercises to keep the UI clean
        int maxExercises = Math.min(workoutExercises.size(), 5);

        for (int i = 0; i < maxExercises; i++) {
            WorkoutExercise exercise = workoutExercises.get(i);
            View exerciseCard = inflater.inflate(R.layout.item_activity_card, activitiesContainer, false);

            TextView exerciseName = exerciseCard.findViewById(R.id.tv_activity_name);
            if (exercise.getExerciseInfo() != null) {
                String name = exercise.getExerciseInfo().getName();
                // Truncate long names
                if (name.length() > 15) {
                    name = name.substring(0, 12) + "...";
                }
                exerciseName.setText(name);
            } else {
                exerciseName.setText("Exercise " + (i + 1));
            }

            activitiesContainer.addView(exerciseCard);
        }
    }

    private void useDummyActivities() {
        activitiesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        String[] dummyExercises = {"Push-ups", "Squats", "Plank", "Lunges", "Burpees"};

        for (String exerciseName : dummyExercises) {
            View exerciseCard = inflater.inflate(R.layout.item_activity_card, activitiesContainer, false);
            TextView exerciseNameView = exerciseCard.findViewById(R.id.tv_activity_name);
            exerciseNameView.setText(exerciseName);
            activitiesContainer.addView(exerciseCard);
        }
    }

    private UserProfile createEmptyProfileForProgression() {
        UserProfile p = new UserProfile();
        p.setFitnessGoal("general fitness");
        p.setFitnessLevel("beginner");
        p.setAge(25);
        p.setGender("not specified");
        p.setHealthIssues(new ArrayList<>());
        p.setAvailableEquipment(new ArrayList<>());
        p.setDislikedExercises(new ArrayList<>());
        p.setHasGymAccess(false);
        return p;
    }

    // New method to save workout for today
    private void saveWorkoutForToday() {
        String today = getCurrentDateString();
        String workoutDetails = "Gym session completed";

        // Save workout using the StreakCalendar method
        StreakCalendar.saveWorkoutForDate(workoutPrefs, today, workoutDetails);

        // Update current streak
        updateCurrentStreak();

        // Refresh streak display
        updateStreakDisplay();

        Toast.makeText(this, "Workout recorded for today!", Toast.LENGTH_SHORT).show();
    }

    // Method to update current streak
    private void updateCurrentStreak() {
        int newStreak = calculateCurrentStreak();
        SharedPreferences.Editor editor = workoutPrefs.edit();
        editor.putInt("current_streak", newStreak);
        editor.apply();
    }

    // Method to calculate current streak
    private int calculateCurrentStreak() {
        String today = getCurrentDateString();
        int streak = 0;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();

            // Check consecutive days backwards from today
            while (true) {
                String dateStr = formatCalendarToString(cal);
                if (workoutPrefs.getStringSet("workout_dates", null) != null &&
                        workoutPrefs.getStringSet("workout_dates", null).contains(dateStr)) {
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

    // Method to update streak display on dashboard
    private void updateStreakDisplay() {
        if (streakDisplay != null) {
            int currentStreak = workoutPrefs.getInt("current_streak", 0);
            streakDisplay.setText(String.valueOf(currentStreak));
        }
    }

    // Helper method to get current date as string
    private String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Helper method to format calendar to string
    private String formatCalendarToString(Calendar cal) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(cal.getTime());
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

            // Set up real-time listener to get user data
            userDataListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Update greeting
                        updateGreeting(snapshot);

                        // Update membership display
                        updateMembershipDisplay(snapshot);
                    } else {
                        setDefaultValues();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    setDefaultValues();
                }
            };

            // Attach listener for real-time updates
            userRef.addValueEventListener(userDataListener);
        } else {
            setDefaultValues();
        }
    }

    private void updateGreeting(DataSnapshot snapshot) {
        String name = null;

        // Try to get name from different possible fields
        if (snapshot.child("name").exists()) {
            name = snapshot.child("name").getValue(String.class);
        } else if (snapshot.child("fullname").exists()) {
            name = snapshot.child("fullname").getValue(String.class);
        }

        // Display the name or fallback to User
        if (name != null && !name.trim().isEmpty()) {
            greetingText.setText("Hi, " + name);
        } else {
            greetingText.setText("Hi, User");
        }
    }

    private void updateMembershipDisplay(DataSnapshot snapshot) {
        // Check for actual membership plan selection - IGNORE membershipStatus field
        String planCode = null;
        String planLabel = null;

        if (snapshot.child("membershipPlanCode").exists()) {
            planCode = snapshot.child("membershipPlanCode").getValue(String.class);
        }

        if (snapshot.child("membershipPlanLabel").exists()) {
            planLabel = snapshot.child("membershipPlanLabel").getValue(String.class);
        }

        // Only show ACTIVE if user has actually selected a membership plan
        boolean hasValidPlan = (planCode != null && !planCode.trim().isEmpty() && !planCode.equals("null")) ||
                (planLabel != null && !planLabel.trim().isEmpty() && !planLabel.equals("null"));

        if (hasValidPlan) {
            // User has actually selected a membership plan
            membershipStatus.setText("ACTIVE");
            membershipStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

            // Set plan type - extract just the duration part
            String planDisplay = extractPlanName(planLabel != null ? planLabel : planCode);
            planType.setText(planDisplay);

            // Calculate and set expiry date based on plan
            String expiryDateStr = calculateExpiryDate(planCode != null ? planCode : "STANDARD_1M");
            expiryDate.setText(expiryDateStr);

        } else {
            // No membership plan selected - show inactive regardless of membershipStatus field
            setDefaultMembershipValues();
        }
    }

    private String extractPlanName(String planLabel) {
        // Extract plan name from full label
        if (planLabel != null) {
            if (planLabel.contains(" – ")) {
                return planLabel.split(" – ")[0];
            } else if (planLabel.contains("\n")) {
                return planLabel.split("\n")[0];
            }
            return planLabel;
        }
        return "Unknown Plan";
    }

    private String calculateExpiryDate(String planCode) {
        Calendar calendar = Calendar.getInstance();

        // Add months based on plan code
        if (planCode.contains("1M") && !planCode.contains("12M")) {
            calendar.add(Calendar.MONTH, 1);
        } else if (planCode.contains("3M")) {
            calendar.add(Calendar.MONTH, 3);
        } else if (planCode.contains("6M")) {
            calendar.add(Calendar.MONTH, 6);
        } else if (planCode.contains("12M")) {
            calendar.add(Calendar.MONTH, 12);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    private void setDefaultMembershipValues() {
        membershipStatus.setText("INACTIVE");
        membershipStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        planType.setText("No plan selected");
        expiryDate.setText("—");
    }

    private void setDefaultValues() {
        greetingText.setText("Hi, User");
        setDefaultMembershipValues();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from activities
        if (mAuth.getCurrentUser() != null) {
            loadUserData();
            updateStreakDisplay(); // Refresh streak display
            loadTodaysActivities(); // Refresh today's activities
        }
    }

    @Override
    public void onBackPressed() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Log out?")
                .setMessage("Do you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    logoutUser();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void logoutUser() {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();

        // Go back to LoginActivity and clear the back stack
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firebase listener to prevent memory leaks
        if (userRef != null && userDataListener != null) {
            userRef.removeEventListener(userDataListener);
        }
    }
}