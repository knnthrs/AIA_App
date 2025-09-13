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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// RTDB imports (for exercises)
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot; // Explicitly keep for RTDB
import com.google.firebase.database.DatabaseError;

// Firestore imports (for user data and promotions)
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot; // Explicitly keep for Firestore
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.view.View;

import com.example.signuploginrealtime.models.ExerciseInfo;
import com.example.signuploginrealtime.logic.AdvancedWorkoutDecisionMaker;
import com.example.signuploginrealtime.logic.WorkoutProgression;
import com.example.signuploginrealtime.models.UserProfile;
import com.example.signuploginrealtime.models.Workout;
import com.example.signuploginrealtime.models.WorkoutExercise;
import com.example.signuploginrealtime.UserInfo.AgeInput;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    TextView greetingText;
    TextView membershipStatus;
    TextView planType;
    TextView expiryDate;
    TextView streakDisplay;
    CardView streakCard;
    CardView activitiesCard;
    LinearLayout activitiesContainer;
    FloatingActionButton fab;
    FirebaseAuth mAuth;
    BottomNavigationView bottomNavigationView;
    SharedPreferences workoutPrefs;

    FirebaseFirestore dbFirestore;
    DocumentReference userDocRefFS;
    ListenerRegistration userDataListenerRegistrationFS;

    DatabaseReference exercisesRefRTDB;
    ValueEventListener exercisesRTDBListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        dbFirestore = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        workoutPrefs = getSharedPreferences("workout_prefs", MODE_PRIVATE);
        exercisesRefRTDB = FirebaseDatabase.getInstance().getReference("exercises");

        initializeViews();
        setupPromoListener();
        setupClickListeners();

        loadUserDataFromFirestore();
        loadTodaysActivitiesFromRTDB();
        updateStreakDisplay();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showLogoutDialog();
            }
        });
    }

    private void initializeViews() {
        fab = findViewById(R.id.fab);
        greetingText = findViewById(R.id.greeting_text);
        membershipStatus = findViewById(R.id.membershipStatus);
        planType = findViewById(R.id.planType);
        expiryDate = findViewById(R.id.expiryDate);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        streakDisplay = findViewById(R.id.streak_number);
        streakCard = findViewById(R.id.streak_counter_card);
        activitiesCard = findViewById(R.id.activities_card);
        activitiesContainer = findViewById(R.id.activities_horizontal_container);
    }

    private void setupPromoListener() {
        DocumentReference latestPromoRef = dbFirestore.collection("promotions").document("latest");
        latestPromoRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed for promotions.", e);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                String imageUrl = snapshot.getString("imageUrl");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    ImageView testImage = findViewById(R.id.testImage);
                    Glide.with(this).load(imageUrl)
                            .placeholder(R.drawable.no_image_placeholder)
                            .error(R.drawable.no_image_placeholder)
                            .into(testImage);
                    LinearLayout promoLayout = findViewById(R.id.promoLayout);
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
    }

    private void setupClickListeners() {
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
            if (itemId == R.id.item_1) return true;
            else if (itemId == R.id.item_2) {
                startActivity(new Intent(getApplicationContext(), Profile.class));
                overridePendingTransition(0, 0); finish(); return true;
            }
            else if (itemId == R.id.item_3) {
                startActivity(new Intent(getApplicationContext(), WorkoutList.class));
                overridePendingTransition(0, 0); return true;
            }
            else if (itemId == R.id.item_4) {
                startActivity(new Intent(getApplicationContext(), Achievement.class));
                overridePendingTransition(0, 0); return true;
            }
            return false;
        });
    }

    private void loadTodaysActivitiesFromRTDB() {
        if (exercisesRefRTDB != null && exercisesRTDBListener != null) {
            exercisesRefRTDB.removeEventListener(exercisesRTDBListener);
        }
        exercisesRTDBListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot rtdbSnapshot) {
                List<ExerciseInfo> exercises = new ArrayList<>();
                if (rtdbSnapshot.exists()) {
                    for (DataSnapshot exerciseSnap : rtdbSnapshot.getChildren()) {
                        ExerciseInfo exercise = exerciseSnap.getValue(ExerciseInfo.class);
                        if (exercise != null) {
                            exercises.add(exercise);
                        }
                    }
                }
                if (!exercises.isEmpty()) {
                    generateTodaysWorkout(exercises);
                } else {
                    Log.d(TAG, "No exercises found in RTDB, using dummy activities.");
                    useDummyActivities();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to fetch exercises from RTDB: " + databaseError.getMessage());
                useDummyActivities();
            }
        };
        exercisesRefRTDB.addValueEventListener(exercisesRTDBListener);
    }

    private void generateTodaysWorkout(List<ExerciseInfo> availableExercises) {
        UserProfile userProfile = createEmptyProfileForProgression();
        userProfile.setFitnessGoal("lose weight"); userProfile.setAge(25);
        Workout baseWorkout = AdvancedWorkoutDecisionMaker.generatePersonalizedWorkout(availableExercises, userProfile);
        Workout finalWorkout = WorkoutProgression.generateProgressiveWorkout(baseWorkout, 1, userProfile);
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
                exerciseNameView.setText(name.length() > 15 ? name.substring(0, 12) + "..." : name);
            } else {
                exerciseNameView.setText("Exercise " + (i + 1));
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
        UserProfile p = new UserProfile(); p.setFitnessGoal("general fitness"); p.setFitnessLevel("beginner");
        p.setAge(25); p.setGender("not specified"); p.setHealthIssues(new ArrayList<>());
        p.setAvailableEquipment(new ArrayList<>()); p.setDislikedExercises(new ArrayList<>());
        p.setHasGymAccess(false); return p;
    }

    private void updateStreakDisplay() {
        if (streakDisplay != null) {
            int currentStreak = workoutPrefs.getInt("current_streak", 0);
            streakDisplay.setText(String.valueOf(currentStreak));
        }
    }

    private void loadUserDataFromFirestore() {
        FirebaseUser currentUserAuth = mAuth.getCurrentUser();
        if (currentUserAuth != null) {
            String uid = currentUserAuth.getUid();
            userDocRefFS = dbFirestore.collection("users").document(uid);

            if (userDataListenerRegistrationFS != null) {
                userDataListenerRegistrationFS.remove();
            }
            userDataListenerRegistrationFS = userDocRefFS.addSnapshotListener((firestoreSnapshot, e) -> {
                if (e != null) {
                    Log.w(TAG, "Firestore listen failed for user data.", e);
                    setDefaultValues(); return;
                }
                if (firestoreSnapshot != null && firestoreSnapshot.exists()) {
                    if (firestoreSnapshot.contains("full name") && firestoreSnapshot.contains("age") &&
                        firestoreSnapshot.contains("gender") && firestoreSnapshot.contains("height") &&
                        firestoreSnapshot.contains("weight") && firestoreSnapshot.contains("fitnessLevel") &&
                        firestoreSnapshot.contains("fitnessGoal")) {
                        Log.d(TAG, "User data complete in Firestore. Updating UI.");
                        updateGreeting(firestoreSnapshot);
                        updateMembershipDisplay(firestoreSnapshot);
                        SharedPreferences.Editor editor = getSharedPreferences("user_profile_prefs", MODE_PRIVATE).edit();
                        editor.putBoolean("profile_complete_firebase", true); editor.apply();
                    } else {
                        Log.d(TAG, "User data INCOMPLETE in Firestore. Redirecting to AgeInput.");
                        redirectToProfileCompletion();
                    }
                } else {
                    Log.d(TAG, "User document does NOT exist in Firestore. Redirecting to AgeInput.");
                    redirectToProfileCompletion();
                }
            });
        } else {
            Log.d(TAG, "No authenticated user in loadUserDataFromFirestore. Should have been caught by onCreate.");
            goToLogin();
        }
    }

    private void redirectToProfileCompletion() {
        SharedPreferences.Editor editor = getSharedPreferences("user_profile_prefs", MODE_PRIVATE).edit();
        editor.putBoolean("profile_complete_firebase", false); editor.apply();
        Intent intent = new Intent(MainActivity.this, AgeInput.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent); finish();
    }

    @SuppressLint("SetTextI18n")
    private void updateGreeting(DocumentSnapshot firestoreSnapshot) {
        String name = firestoreSnapshot.getString("full name");
        greetingText.setText((name != null && !name.trim().isEmpty())
                ? "Hi, " + name
                : "Hi, User");
    }

    @SuppressLint("SetTextI18n")
    private void updateMembershipDisplay(DocumentSnapshot firestoreSnapshot) {
        String planCode = firestoreSnapshot.getString("membershipPlanCode");
        String planLabel = firestoreSnapshot.getString("membershipPlanLabel");

        Log.d(TAG, "updateMembershipDisplay: planCode from Firestore: '" + planCode + "'");
        Log.d(TAG, "updateMembershipDisplay: planLabel from Firestore: '" + planLabel + "'");

        boolean planCodeValid = (planCode != null && !planCode.trim().isEmpty() && !"null".equals(planCode));
        boolean planLabelValid = (planLabel != null && !planLabel.trim().isEmpty() && !"null".equals(planLabel));
        boolean hasValidPlan = planCodeValid || planLabelValid;

        Log.d(TAG, "updateMembershipDisplay: planCodeValid: " + planCodeValid);
        Log.d(TAG, "updateMembershipDisplay: planLabelValid: " + planLabelValid);
        Log.d(TAG, "updateMembershipDisplay: hasValidPlan: " + hasValidPlan);

        if (hasValidPlan) {
            Log.d(TAG, "Setting membership to ACTIVE");
            membershipStatus.setText("ACTIVE");
            try {
                membershipStatus.setTextColor(getColor(R.color.green));
            } catch (Exception colorEx) {
                Log.e(TAG, "Error setting green color: " + colorEx.getMessage());
                membershipStatus.setTextColor(android.graphics.Color.GREEN);
            }
            planType.setText(extractPlanName(planLabel != null ? planLabel : planCode));
            expiryDate.setText(calculateExpiryDate(planCode != null ? planCode : "STANDARD_1M"));
        } else {
            Log.d(TAG, "Setting membership to INACTIVE");
            setDefaultMembershipValues();
        }
    }

    private String extractPlanName(String planLabel) {
        if (planLabel != null) {
            if (planLabel.contains(" – ")) return planLabel.split(" – ")[0];
            if (planLabel.contains("\n")) return planLabel.split("\n")[0];
            return planLabel;
        }
        return "Unknown Plan";
    }

    private String calculateExpiryDate(String planCode) {
        Calendar calendar = Calendar.getInstance();
        if (planCode == null) planCode = "";
        if (planCode.contains("1M") && !planCode.contains("12M")) calendar.add(Calendar.MONTH, 1);
        else if (planCode.contains("3M")) calendar.add(Calendar.MONTH, 3);
        else if (planCode.contains("6M")) calendar.add(Calendar.MONTH, 6);
        else if (planCode.contains("12M")) calendar.add(Calendar.MONTH, 12);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    @SuppressLint("SetTextI18n")
    private void setDefaultMembershipValues() {
        membershipStatus.setText("INACTIVE");
        try {
            membershipStatus.setTextColor(getColor(R.color.red));
        } catch (Exception colorEx) {
            Log.e(TAG, "Error setting red color: " + colorEx.getMessage());
            membershipStatus.setTextColor(android.graphics.Color.RED);
        }
        planType.setText("No plan selected");
        expiryDate.setText("—");
    }

    @SuppressLint("SetTextI18n")
    private void setDefaultValues() {
        greetingText.setText("Hi, User");
        setDefaultMembershipValues();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            updateStreakDisplay();
        } else {
            goToLogin();
        }
    }

    private void goToLogin(){
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent); finish();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Log out?").setMessage("Do you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> logoutUser())
                .setNegativeButton("No", null).show();
    }

    private void logoutUser() {
        if (userDataListenerRegistrationFS != null) userDataListenerRegistrationFS.remove();
        mAuth.signOut();
        getSharedPreferences("user_profile_prefs", MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences("workout_prefs", MODE_PRIVATE).edit().clear().apply();
        goToLogin();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userDataListenerRegistrationFS != null) userDataListenerRegistrationFS.remove();
        if (exercisesRefRTDB != null && exercisesRTDBListener != null) {
            exercisesRefRTDB.removeEventListener(exercisesRTDBListener);
        }
    }
}
