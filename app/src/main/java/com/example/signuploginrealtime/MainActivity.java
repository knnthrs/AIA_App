package com.example.signuploginrealtime;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

// Import the necessary classes for workout generation
import com.example.signuploginrealtime.models.ExerciseInfo;
import com.example.signuploginrealtime.logic.AdvancedWorkoutDecisionMaker;
import com.example.signuploginrealtime.logic.WorkoutProgression;
import com.example.signuploginrealtime.models.UserProfile;
import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

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
    DatabaseReference exercisesRef; // Firebase reference for exercises
    BottomNavigationView bottomNavigationView;
    ValueEventListener userDataListener;
    SharedPreferences workoutPrefs; // For streak data

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

        // Initialize Firebase Database reference for exercises
        exercisesRef = FirebaseDatabase.getInstance().getReference("exercises"); // Assuming "exercises" is your node

        // Initialize views
        fab = findViewById(R.id.fab);
        greetingText = findViewById(R.id.greeting_text);
        membershipStatus = findViewById(R.id.membershipStatus);
        planType = findViewById(R.id.planType);
        expiryDate = findViewById(R.id.expiryDate);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        // Initialize new streak views
        streakDisplay = findViewById(R.id.streak_number);
        streakCard = findViewById(R.id.streak_counter_card);

        // Initialize activities views
        activitiesCard = findViewById(R.id.activities_card);
        activitiesContainer = findViewById(R.id.activities_horizontal_container);

        ImageView testImage = findViewById(R.id.testImage);
        LinearLayout promoLayout = findViewById(R.id.promoLayout);

        // Firestore instance for promotions
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference latestPromoRef = db.collection("promotions").document("latest");

        latestPromoRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed for promotions.", e);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                String imageUrl = snapshot.getString("imageUrl");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(this)
                            .load(imageUrl)
                            .placeholder(android.R.drawable.ic_menu_report_image)
                            .error(android.R.drawable.ic_delete)
                            .into(testImage);
                    promoLayout.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, Promo.class);
                        intent.putExtra("promoUrl", imageUrl);
                        startActivity(intent);
                    });
                }
            } else {
                Log.d(TAG, "No data found in latest promotion document");
            }
        });

        findViewById(R.id.membershipCard).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SelectMembership.class);
            startActivity(intent);
        });

        if (streakCard != null) {
            streakCard.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, StreakCalendar.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        if (activitiesCard != null) {
            activitiesCard.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, WorkoutList.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        ImageView bellIcon = findViewById(R.id.bell_icon);
        if (bellIcon != null) {
            bellIcon.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, Notification.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, QR.class);
            startActivity(intent);
        });

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

        // Badge for Profile (variable 'badgeDrawable' was unused, you might want to customize or use it)
        BadgeDrawable badgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.item_2);
        // badgeDrawable.setVisible(true); // Example: make it visible
        // badgeDrawable.setNumber(5); // Example: set a number

        loadUserData();
        updateStreakDisplay();
        loadTodaysActivities();

        // Handle back press for logout dialog
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Log out?")
                        .setMessage("Do you want to log out?")
                        .setPositiveButton("Yes", (dialog, which) -> logoutUser())
                        .setNegativeButton("No", null)
                        .show();
            }
        });
    }

    private void loadTodaysActivities() {
        exercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ExerciseInfo> exercises = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot exerciseSnap : snapshot.getChildren()) {
                        ExerciseInfo exercise = exerciseSnap.getValue(ExerciseInfo.class);
                        if (exercise != null) {
                            // Ensure ExerciseInfo has a name field populated from Firebase
                            // If your ExerciseDB data in Firebase doesn't directly map to ExerciseInfo.name,
                            // you might need to adjust ExerciseInfo.class or mapping here.
                            // For example, if name is under a different field:
                            // String nameFromDb = exerciseSnap.child("exerciseName").getValue(String.class);
                            // exercise.setName(nameFromDb);
                            exercises.add(exercise);
                        }
                    }
                }

                if (!exercises.isEmpty()) {
                    generateTodaysWorkout(exercises);
                } else {
                    Log.d(TAG, "No exercises found in Firebase or list is empty, using dummy activities.");
                    useDummyActivities();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch exercises from Firebase: " + error.getMessage());
                useDummyActivities();
            }
        });
    }

    private void generateTodaysWorkout(List<ExerciseInfo> availableExercises) {
        UserProfile userProfile = createEmptyProfileForProgression();
        // TODO: Ideally, fetch or use actual user profile data here
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

        if (finalWorkout != null && finalWorkout.getExercises() != null && !finalWorkout.getExercises().isEmpty()) {
            displayTodaysActivities(finalWorkout.getExercises());
        } else {
            Log.d(TAG, "Generated workout has no exercises, using dummy activities.");
            useDummyActivities();
        }
    }

    private void displayTodaysActivities(List<WorkoutExercise> workoutExercises) {
        activitiesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int maxExercises = Math.min(workoutExercises.size(), 5);

        for (int i = 0; i < maxExercises; i++) {
            WorkoutExercise exercise = workoutExercises.get(i);
            View exerciseCard = inflater.inflate(R.layout.item_activity_card, activitiesContainer, false);
            TextView exerciseNameView = exerciseCard.findViewById(R.id.tv_activity_name);

            if (exercise.getExerciseInfo() != null && exercise.getExerciseInfo().getName() != null) {
                String name = exercise.getExerciseInfo().getName();
                if (name.length() > 15) {
                    name = name.substring(0, 12) + "...";
                }
                exerciseNameView.setText(name);
            } else {
                exerciseNameView.setText("Exercise " + (i + 1)); // Consider using string resources
            }
            activitiesContainer.addView(exerciseCard);
        }
    }

    private void useDummyActivities() {
        activitiesContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        String[] dummyExercises = {"Push-ups", "Squats", "Plank", "Lunges", "Burpees"}; // Consider using string resources

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

    private void saveWorkoutForToday() { // This method is unused according to analysis
        String today = getCurrentDateString();
        String workoutDetails = "Gym session completed"; // Consider using string resources
        StreakCalendar.saveWorkoutForDate(workoutPrefs, today, workoutDetails);
        updateCurrentStreak();
        updateStreakDisplay();
        Toast.makeText(this, "Workout recorded for today!", Toast.LENGTH_SHORT).show(); // Consider string resources
    }

    private void updateCurrentStreak() {
        int newStreak = calculateCurrentStreak();
        SharedPreferences.Editor editor = workoutPrefs.edit();
        editor.putInt("current_streak", newStreak);
        editor.apply();
    }

    private int calculateCurrentStreak() {
        // String today = getCurrentDateString(); // Variable 'today' is never used
        int streak = 0;
        try {
            // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // 'sdf' is never used
            Calendar cal = Calendar.getInstance();
            while (true) {
                String dateStr = formatCalendarToString(cal);
                // Potential NPE warning: workoutPrefs.getStringSet("workout_dates", null).contains(dateStr)
                // Add a null check for the Set
                java.util.Set<String> workoutDates = workoutPrefs.getStringSet("workout_dates", null);
                if (workoutDates != null && workoutDates.contains(dateStr)) {
                    streak++;
                    cal.add(Calendar.DAY_OF_MONTH, -1);
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating streak", e); // Replaced e.printStackTrace()
        }
        return streak;
    }

    private void updateStreakDisplay() {
        if (streakDisplay != null) {
            int currentStreak = workoutPrefs.getInt("current_streak", 0);
            streakDisplay.setText(String.valueOf(currentStreak));
        }
    }

    private String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String formatCalendarToString(Calendar cal) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(cal.getTime());
    }

    private void loadUserData() {
        FirebaseUser currentUserAuth = mAuth.getCurrentUser();
        if (currentUserAuth != null) {
            String uid = currentUserAuth.getUid();
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

            if (userDataListener != null) { // Remove existing listener before adding a new one
                userRef.removeEventListener(userDataListener);
            }
            userDataListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        updateGreeting(snapshot);
                        updateMembershipDisplay(snapshot);
                    } else {
                        setDefaultValues();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "User data fetch cancelled: " + error.getMessage());
                    setDefaultValues();
                }
            };
            userRef.addValueEventListener(userDataListener);
        } else {
            setDefaultValues();
        }
    }

    @SuppressLint("SetTextI18n") // Acknowledge for "Hi, " + name
    private void updateGreeting(DataSnapshot snapshot) {
        String name = null;
        if (snapshot.child("name").exists()) {
            name = snapshot.child("name").getValue(String.class);
        } else if (snapshot.child("fullname").exists()) {
            name = snapshot.child("fullname").getValue(String.class);
        }

        if (name != null && !name.trim().isEmpty()) {
            greetingText.setText("Hi, " + name); // Consider string resources for "Hi, %s"
        } else {
            greetingText.setText("Hi, User"); // Consider string resources
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateMembershipDisplay(DataSnapshot snapshot) {
        String planCode = snapshot.child("membershipPlanCode").getValue(String.class);
        String planLabel = snapshot.child("membershipPlanLabel").getValue(String.class);

        boolean hasValidPlan = (planCode != null && !planCode.trim().isEmpty() && !planCode.equals("null")) ||
                (planLabel != null && !planLabel.trim().isEmpty() && !planLabel.equals("null"));

        if (hasValidPlan) {
            membershipStatus.setText("ACTIVE"); // Consider string resources
            membershipStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark)); // getColor is deprecated, consider ContextCompat.getColor
            String planDisplay = extractPlanName(planLabel != null ? planLabel : planCode);
            planType.setText(planDisplay);
            String expiryDateStr = calculateExpiryDate(planCode != null ? planCode : "STANDARD_1M"); // Default to prevent NPE if both null
            expiryDate.setText(expiryDateStr);
        } else {
            setDefaultMembershipValues();
        }
    }

    private String extractPlanName(String planLabel) {
        if (planLabel != null) {
            if (planLabel.contains(" – ")) {
                return planLabel.split(" – ")[0];
            } else if (planLabel.contains("\n")) {
                return planLabel.split("\n")[0];
            }
            return planLabel;
        }
        return "Unknown Plan"; // Consider string resources
    }

    private String calculateExpiryDate(String planCode) {
        Calendar calendar = Calendar.getInstance();
        if (planCode == null) planCode = ""; // Avoid NPE

        if (planCode.contains("1M") && !planCode.contains("12M")) {
            calendar.add(Calendar.MONTH, 1);
        } else if (planCode.contains("3M")) {
            calendar.add(Calendar.MONTH, 3);
        } else if (planCode.contains("6M")) {
            calendar.add(Calendar.MONTH, 6);
        } else if (planCode.contains("12M")) {
            calendar.add(Calendar.MONTH, 12);
        }
        // else: no change to calendar if planCode is unknown, effectively "expires now" or "no valid duration"

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    @SuppressLint("SetTextI18n")
    private void setDefaultMembershipValues() {
        membershipStatus.setText("INACTIVE"); // Consider string resources
        membershipStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark)); // getColor is deprecated
        planType.setText("No plan selected"); // Consider string resources
        expiryDate.setText("—");
    }

    @SuppressLint("SetTextI18n")
    private void setDefaultValues() {
        greetingText.setText("Hi, User"); // Consider string resources
        setDefaultMembershipValues();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            loadUserData();
            updateStreakDisplay();
            loadTodaysActivities();
        }
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userRef != null && userDataListener != null) {
            userRef.removeEventListener(userDataListener);
        }
    }
}
