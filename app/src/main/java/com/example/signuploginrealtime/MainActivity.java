    package com.example.signuploginrealtime;
    
    import android.Manifest;
    import android.annotation.SuppressLint;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.content.pm.PackageManager;
    import android.os.Build;
    import android.os.Bundle;
    import android.widget.ImageView;
    import android.widget.TextView;
    import android.util.Log;
    import androidx.activity.OnBackPressedCallback;
    import androidx.annotation.Nullable;
    import androidx.appcompat.app.AlertDialog;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.cardview.widget.CardView;
    import androidx.core.app.ActivityCompat;
    import androidx.core.content.ContextCompat;
    import java.util.concurrent.TimeUnit;
    import com.bumptech.glide.Glide;
    import com.google.android.material.bottomnavigation.BottomNavigationView;
    import com.google.android.material.floatingactionbutton.FloatingActionButton;
    import com.google.firebase.Timestamp;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.auth.FirebaseUser;



    // Firestore imports (for user data and promotions)
    import com.google.firebase.firestore.DocumentReference;
    import com.google.firebase.firestore.DocumentSnapshot; // Explicitly keep for Firestore
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.google.firebase.firestore.ListenerRegistration;

    import java.text.SimpleDateFormat;
    import java.util.Calendar;
    import java.util.Date;
    import java.util.HashMap;
    import java.util.List;
    import java.util.ArrayList;
    import java.util.Locale;
    import java.util.Map;

    import android.widget.LinearLayout;
    import android.view.LayoutInflater;
    import android.view.View;
    
    
    import com.example.signuploginrealtime.UserInfo.AgeInput;
    
    public class MainActivity extends AppCompatActivity {
    
        private static final String TAG = "MainActivity";
        // Track daily workouts
        private static final String PREFS_DAILY = "daily_workout_prefs";
        private static final String KEY_DATE = "last_date";
        private static final String KEY_COUNT = "count";
    
    
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

        private boolean isMembershipLoaded = false;
    
    
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
    
            NotificationHelper.init(this);
    
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                }
            }
    
            // 🔹 Check role before continuing
            SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            String role = prefs.getString("role", "");
    
            if ("coach".equals(role)) {
                // Coaches should never enter MainActivity → redirect to coach dashboard
                Intent intent = new Intent(this, coach_clients.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return; // stop running MainActivity setup
            }
    
            // 🔹 If user → continue with MainActivity setup
            setContentView(R.layout.activity_main);
    
            mAuth = FirebaseAuth.getInstance();
            dbFirestore = FirebaseFirestore.getInstance();
    
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                goToLogin();
                return;
            }
    
            if (currentUser != null) {
                String userId = currentUser.getUid();
                workoutPrefs = getSharedPreferences("workout_prefs_" + userId, MODE_PRIVATE);
            } else {
                workoutPrefs = getSharedPreferences("workout_prefs_default", MODE_PRIVATE);
            }
    
    
            initializeViews();
            setupPromoListener();
            setupClickListeners();
            loadUserDataFromFirestore();
            updateStreakDisplay();

            new android.os.Handler().postDelayed(this::checkAndHandleMembershipExpiration, 800);

            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    showExitDialog();
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
    
        private void showExitDialog() {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Exit App?")
                    .setMessage("Do you want to exit?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        finishAffinity(); // closes all activities and exits app
                    })
                    .setNegativeButton("No", null)
                    .show();
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
    
    
        // Helper method to get current week's workout progress
    // Helper method to get current week's workout progress
        private void updateGoalsProgressDisplay(DocumentSnapshot firestoreSnapshot) {
            TextView goalsProgressText = findViewById(R.id.goals_progress_text);
    
            if (goalsProgressText != null && firestoreSnapshot != null) {
                Long workoutFrequency = firestoreSnapshot.getLong("workoutDaysPerWeek");
    
                if (workoutFrequency != null && workoutFrequency > 0) {
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser == null) return;
    
                    // 🔹 fetch the progress subcollection
                    dbFirestore.collection("users")
                            .document(currentUser.getUid())
                            .collection("progress")
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                int completedCount = 0;
                                for (DocumentSnapshot doc : querySnapshot) {
                                    String dateStr = doc.getString("date");
                                    if (dateStr != null && isDateInCurrentWeek(dateStr)) {
                                        completedCount++;
                                    }
                                }
    
                                goalsProgressText.setText(completedCount + "/" + workoutFrequency);
    
                                if (completedCount >= workoutFrequency) {
                                    goalsProgressText.setTextColor(getColor(R.color.green));
                                } else if (completedCount > 0) {
                                    goalsProgressText.setTextColor(getColor(R.color.orange));
                                } else {
                                    goalsProgressText.setTextColor(getColor(R.color.gray));
                                }
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Error fetching progress subcollection", e));
                } else {
                    goalsProgressText.setText("0/0");
                    goalsProgressText.setTextColor(getColor(R.color.gray));
                }
            }
        }
    
        // Load the next workout from Firestore
        // Updated loadNextWorkoutFromFirestore method in MainActivity.java
        private void loadNextWorkoutFromFirestore() {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "Current user is null");
                return;
            }
    
            Log.d(TAG, "=== LOADING WORKOUT DEBUG START ===");
            Log.d(TAG, "User ID: " + currentUser.getUid());
    
            dbFirestore.collection("users")
                    .document(currentUser.getUid())
                    .collection("currentWorkout")
                    .document("week_1")
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Log.d(TAG, "Firestore query successful");
                        Log.d(TAG, "Document exists: " + documentSnapshot.exists());
    
                        if (documentSnapshot.exists()) {
                            // Temporarily ignore completed status - show all workouts
                            Log.d(TAG, "Document fields: " + documentSnapshot.getData());
    
                            // Get the exercises array
                            List<Map<String, Object>> exercisesList =
                                    (List<Map<String, Object>>) documentSnapshot.get("exercises");
    
                            Log.d(TAG, "Exercises list is null: " + (exercisesList == null));
                            if (exercisesList != null) {
                                Log.d(TAG, "Found " + exercisesList.size() + " exercises");
    
                                if (!exercisesList.isEmpty()) {
                                    List<String> exerciseNames = new ArrayList<>();
                                    List<String> exerciseGifs = new ArrayList<>();
    
                                    for (int i = 0; i < exercisesList.size(); i++) {
                                        Map<String, Object> exerciseMap = exercisesList.get(i);
                                        Log.d(TAG, "Processing exercise " + i);
    
                                        // Get the exerciseInfo map
                                        Map<String, Object> exerciseInfo =
                                                (Map<String, Object>) exerciseMap.get("exerciseInfo");
    
                                        if (exerciseInfo != null) {
                                            String name = (String) exerciseInfo.get("name");
                                            String gifUrl = (String) exerciseInfo.get("gifUrl");
    
                                            Log.d(TAG, "Exercise " + i + " - Name: " + name + ", GIF: " + gifUrl);
    
                                            exerciseNames.add(name != null ? name : "Unknown Exercise");
                                            exerciseGifs.add(gifUrl != null ? gifUrl : "");
                                        } else {
                                            Log.e(TAG, "Exercise " + i + " - exerciseInfo is null!");
                                        }
                                    }
    
                                    Log.d(TAG, "Parsed " + exerciseNames.size() + " exercise names");
                                    Log.d(TAG, "Exercise names: " + exerciseNames);
    
                                    if (!exerciseNames.isEmpty()) {
                                        Log.d(TAG, "Calling displayYourWorkouts...");
                                        displayYourWorkouts(exerciseNames, exerciseGifs);
                                    } else {
                                        Log.d(TAG, "No exercise names found, showing no workouts");
                                        showNoWorkouts();
                                    }
                                } else {
                                    Log.d(TAG, "Exercises list is empty");
                                    showNoWorkouts();
                                }
                            } else {
                                Log.e(TAG, "Exercises field is null or not a list");
                                showNoWorkouts();
                            }
                        } else {
                            Log.d(TAG, "No workout document found");
                            showNoWorkouts();
                        }
                        Log.d(TAG, "=== LOADING WORKOUT DEBUG END ===");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading workouts", e);
                        showNoWorkouts();
                    });
        }    // Updated displayYourWorkouts to handle names and GIFs
        private void displayYourWorkouts(List<String> exercises, @Nullable List<String> gifs) {
            Log.d(TAG, "=== displayYourWorkouts DEBUG START ===");
            Log.d(TAG, "Method called with " + exercises.size() + " exercises");
    
            // Check if activitiesContainer exists
            if (activitiesContainer == null) {
                Log.e(TAG, "ERROR: activitiesContainer is NULL! Check R.id.activities_horizontal_container");
                return;
            }
    
            Log.d(TAG, "activitiesContainer found successfully");
            activitiesContainer.removeAllViews();
    
            if (exercises.isEmpty()) {
                Log.d(TAG, "No exercises to display, calling showNoWorkouts");
                showNoWorkouts();
                return;
            }
    
            LayoutInflater inflater = LayoutInflater.from(this);
            int max = Math.min(exercises.size(), 5);
            Log.d(TAG, "Will create " + max + " exercise cards");
    
            for (int i = 0; i < max; i++) {
                String name = exercises.get(i);
                String gifUrl = (gifs != null && i < gifs.size()) ? gifs.get(i) : null;
    
                Log.d(TAG, "Creating card " + i + ": name=" + name + ", gif=" + gifUrl);
    
                // Check if the layout file exists
                View exerciseCard;
                try {
                    exerciseCard = inflater.inflate(R.layout.item_activity_card, activitiesContainer, false);
                    Log.d(TAG, "Successfully inflated item_activity_card layout");
                } catch (Exception e) {
                    Log.e(TAG, "ERROR: Failed to inflate item_activity_card layout: " + e.getMessage());
                    continue;
                }
    
                // Find the views inside the card
                TextView exerciseNameView = exerciseCard.findViewById(R.id.tv_activity_name);
                ImageView exerciseGifView = exerciseCard.findViewById(R.id.iv_activity_gif);
    
                if (exerciseNameView == null) {
                    Log.e(TAG, "ERROR: tv_activity_name not found in item_activity_card layout");
                } else {
                    Log.d(TAG, "Found tv_activity_name successfully");
                }
    
                if (exerciseGifView == null) {
                    Log.e(TAG, "ERROR: iv_activity_gif not found in item_activity_card layout");
                } else {
                    Log.d(TAG, "Found iv_activity_gif successfully");
                }
    
                // Set the exercise name
                if (exerciseNameView != null) {
                    String displayName = name.length() > 15 ? name.substring(0, 12) + "..." : name;
                    exerciseNameView.setText(displayName);
                    Log.d(TAG, "Set exercise name to: " + displayName);
                }
    
                // Load the GIF
                if (exerciseGifView != null) {
                    if (gifUrl != null && !gifUrl.isEmpty()) {
                        Log.d(TAG, "Loading GIF: " + gifUrl);
                        Glide.with(this)
                                .asGif()
                                .load(gifUrl)
                                .placeholder(R.drawable.no_image_placeholder)
                                .error(R.drawable.no_image_placeholder)
                                .into(exerciseGifView);
                    } else {
                        Log.d(TAG, "No GIF URL, using placeholder");
                        exerciseGifView.setImageResource(R.drawable.no_image_placeholder);
                    }
                }
    
                // Add the card to the container
                try {
                    activitiesContainer.addView(exerciseCard);
                    Log.d(TAG, "Successfully added exercise card " + i + " to container");
                } catch (Exception e) {
                    Log.e(TAG, "ERROR: Failed to add card to container: " + e.getMessage());
                }
            }
    
            Log.d(TAG, "Final container child count: " + activitiesContainer.getChildCount());
            Log.d(TAG, "Container visibility: " + activitiesContainer.getVisibility());
            Log.d(TAG, "=== displayYourWorkouts DEBUG END ===");
        }
    
        // Fallback if no workouts
        private void showNoWorkouts() {
            activitiesContainer.removeAllViews();
            TextView noWorkouts = new TextView(this);
            noWorkouts.setText("No workouts assigned yet");
            noWorkouts.setTextColor(getResources().getColor(R.color.gray));
            noWorkouts.setTextSize(14);
            noWorkouts.setPadding(16, 16, 16, 16);
            activitiesContainer.addView(noWorkouts);
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
    
                // Remove previous listener if any
                if (userDataListenerRegistrationFS != null) {
                    userDataListenerRegistrationFS.remove();
                }
    
                // Add snapshot listener
                userDataListenerRegistrationFS = userDocRefFS.addSnapshotListener((firestoreSnapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Firestore listen failed for user data.", e);
                        setDefaultValues();
                        return;
                    }
    
                    if (firestoreSnapshot == null || !firestoreSnapshot.exists()) {
                        // User document deleted → account no longer available
                        showAccountDeletedDialog();
                    } else if (firestoreSnapshot.contains("fullname") && firestoreSnapshot.contains("age") &&
                            firestoreSnapshot.contains("gender") && firestoreSnapshot.contains("height") &&
                            firestoreSnapshot.contains("weight") && firestoreSnapshot.contains("fitnessLevel") &&
                            firestoreSnapshot.contains("fitnessGoal")) {
                        Log.d(TAG, "User data complete in Firestore. Updating UI.");
                        updateGreeting(firestoreSnapshot);
                        updateMembershipDisplay(firestoreSnapshot);
                        updateGoalsProgressDisplay(firestoreSnapshot); // Add this line
    
                        SharedPreferences.Editor editor = getSharedPreferences("user_profile_prefs", MODE_PRIVATE).edit();
                        editor.putBoolean("profile_complete_firebase", true);
                        editor.apply();
                    } else {
                        Log.d(TAG, "User data INCOMPLETE in Firestore. Redirecting to AgeInput.");
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
            String name = firestoreSnapshot.getString("fullname");
            greetingText.setText((name != null && !name.trim().isEmpty())
                    ? "Hi, " + name
                    : "Hi, User");
        }
    

        @SuppressLint("SetTextI18n")
        private void updateMembershipDisplay(DocumentSnapshot firestoreSnapshot) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            // Show loading state instead of default values
            if (!isMembershipLoaded) {
                membershipStatus.setText("Loading...");
                membershipStatus.setTextColor(getColor(R.color.gray));
                planType.setText("Checking membership...");
                expiryDate.setText("—");
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("memberships")
                    .whereEqualTo("userId", user.getUid())
                    .whereEqualTo("membershipStatus", "active")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        isMembershipLoaded = true;

                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot membership = queryDocumentSnapshots.getDocuments().get(0);

                            String plan = membership.getString("membershipPlanLabel");
                            Timestamp expirationTimestamp = membership.getTimestamp("membershipExpirationDate");

                            if (plan != null) planType.setText(plan);

                            if (expirationTimestamp != null) {
                                Date expDate = expirationTimestamp.toDate();
                                expiryDate.setText(new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(expDate));

                                long diffInMillis = expDate.getTime() - new Date().getTime();
                                long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);
                                long diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillis);

                                if (diffInMillis < 0) {
                                    membershipStatus.setText("EXPIRED");
                                    membershipStatus.setTextColor(getColor(R.color.red));
                                    planType.setText(plan + " (Expired)");

                                } else if (diffInDays <= 0 && diffInHours <= 6 && diffInHours > 0) {
                                    membershipStatus.setText("EXPIRING SOON");
                                    membershipStatus.setTextColor(getColor(R.color.orange));
                                    planType.setText(plan + " (Expires in " + diffInHours + "h)");

                                } else if (diffInDays >= 1 && diffInDays <= 3) {
                                    membershipStatus.setText("EXPIRING SOON");
                                    membershipStatus.setTextColor(getColor(R.color.orange));
                                    planType.setText(plan + " (" + diffInDays + " days left)");

                                } else {
                                    membershipStatus.setText("ACTIVE");
                                    membershipStatus.setTextColor(getColor(R.color.green));
                                    planType.setText(plan);
                                }
                            } else {
                                membershipStatus.setText("INACTIVE");
                                membershipStatus.setTextColor(getColor(R.color.red));
                                expiryDate.setText("—");
                                planType.setText("No plan");
                            }
                        } else {
                            Log.d(TAG, "No active membership found");
                            setDefaultMembershipValues();
                        }
                    })
                    .addOnFailureListener(e -> {
                        isMembershipLoaded = true;
                        Log.e(TAG, "Failed to load membership info", e);
                        setDefaultMembershipValues();
                    });
        }        private String extractPlanName(String planLabel) {
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
                isMembershipLoaded = false;
                updateStreakDisplay();
    
                // Refresh membership display when returning to MainActivity
                loadUserDataFromFirestore(); // This will trigger updateMembershipDisplay
    
                // Refresh workout display when returning from other activities
                loadNextWorkoutFromFirestore();
                checkAndHandleMembershipExpiration();
                
                // Check if a workout was just completed
                boolean workoutCompleted = workoutPrefs.getBoolean("workout_completed", false);
                if (workoutCompleted) {
                    // Reset the flag
                    workoutPrefs.edit().putBoolean("workout_completed", false).apply();
                }
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
            if (mAuth.getCurrentUser() != null) {
                String userId = mAuth.getCurrentUser().getUid();
                getSharedPreferences("workout_prefs_" + userId, MODE_PRIVATE).edit().clear().apply();
            }
    
            if (userDataListenerRegistrationFS != null) userDataListenerRegistrationFS.remove();
            mAuth.signOut();
            getSharedPreferences("user_profile_prefs", MODE_PRIVATE).edit().clear().apply();
            goToLogin();
        }
    
        @Override
        protected void onDestroy() {
            super.onDestroy();
            if (userDataListenerRegistrationFS != null) userDataListenerRegistrationFS.remove();
        }
    
        private void showAccountDeletedDialog() {
            new AlertDialog.Builder(this)
                    .setTitle("Account Unavailable")
                    .setMessage("Your account has been deleted by the admin. You will be logged out.")
                    .setCancelable(false)
                    .setPositiveButton("OK", (dialog, which) -> {
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            String userId = currentUser.getUid();
                            getSharedPreferences("workout_prefs_" + userId, MODE_PRIVATE).edit().clear().apply();
                        }
                        getSharedPreferences("user_profile_prefs", MODE_PRIVATE).edit().clear().apply();
    
                        FirebaseAuth.getInstance().signOut();
    
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .show();
        }
    
    
    
        private boolean isDateInCurrentWeek(String dateStr) {
            try {
                java.time.LocalDate workoutDate = java.time.LocalDate.parse(dateStr); // format yyyy-MM-dd
                java.time.LocalDate now = java.time.LocalDate.now();
    
                java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault());
                int workoutWeek = workoutDate.get(weekFields.weekOfWeekBasedYear());
                int currentWeek = now.get(weekFields.weekOfWeekBasedYear());
    
                return workoutWeek == currentWeek && workoutDate.getYear() == now.getYear();
            } catch (Exception e) {
                Log.e(TAG, "Date parsing failed: " + dateStr, e);
                return false;
            }
        }

        private void checkAndHandleMembershipExpiration() {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("memberships")
                    .whereEqualTo("userId", user.getUid())
                    .whereEqualTo("membershipStatus", "active")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);

                            com.google.firebase.Timestamp expirationTimestamp = doc.getTimestamp("membershipExpirationDate");
                            if (expirationTimestamp == null) return;

                            Date expirationDate = expirationTimestamp.toDate();
                            Date today = new Date();
                            long diffInMillis = expirationDate.getTime() - today.getTime();
                            long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);

                            Log.d(TAG, "Membership expires in " + diffInDays + " days");

                            if (diffInDays < 0) {
                                // 🔴 EXPIRED - Update status and notify
                                db.collection("memberships").document(doc.getId())
                                        .update("membershipStatus", "expired")
                                        .addOnSuccessListener(aVoid -> {
                                            showExpirationPopup("Your membership has expired.");
                                            saveNotificationToFirestore("expired", 0);
                                            loadUserDataFromFirestore(); // Refresh UI
                                        });

                            } else if (diffInDays <= 3 && diffInDays >= 0) {
                                // 🟠 EXPIRING SOON
                                saveNotificationToFirestore("expiring_soon", (int) diffInDays);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error checking expiration", e));
        }
        private void showExpirationPopup(String message) {
            new AlertDialog.Builder(this)
                    .setTitle("Membership Notice")
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        }

        private void saveNotificationToFirestore(String notificationType, int daysRemaining) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            long todayStart = getTodayStartAsLong();
            long todayEnd = todayStart + 86400000L; // 24h later

            db.collection("notifications")
                    .whereEqualTo("userId", user.getUid())
                    .whereEqualTo("type", notificationType)
                    .whereGreaterThanOrEqualTo("timestamp", todayStart)
                    .whereLessThan("timestamp", todayEnd)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot.isEmpty()) {
                            String title, message;

                            if ("expired".equals(notificationType)) {
                                title = "Membership Expired";
                                message = "Your membership has expired. Renew now to continue enjoying gym access.";
                            } else {
                                title = "Membership Expiring Soon";
                                message = "Your membership will expire in " + daysRemaining + " day(s). Renew soon!";
                            }

                            Map<String, Object> notification = new HashMap<>();
                            notification.put("userId", user.getUid());
                            notification.put("title", title);
                            notification.put("message", message);
                            notification.put("type", notificationType);
                            notification.put("timestamp", System.currentTimeMillis());
                            notification.put("read", false);

                            db.collection("notifications").add(notification);
                        } else {
                            Log.d(TAG, "⚠️ Skipping duplicate " + notificationType + " notification for today");
                        }
                    });
        }

        // Helper method to create notification without duplicate check
        private void createNotificationDirectly(String userId, String notificationType, int daysRemaining, FirebaseFirestore db) {
            String title, message;

            if ("expired".equals(notificationType)) {
                title = "Membership Expired";
                message = "Your membership has expired. Renew now to continue enjoying gym access.";
            } else {
                title = "Membership Expiring Soon";
                message = "Your membership will expire in " + daysRemaining + " day(s). Renew soon!";
            }

            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", userId);
            notification.put("title", title);
            notification.put("message", message);
            notification.put("type", notificationType);
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("read", false);

            db.collection("notifications")
                    .add(notification)
                    .addOnSuccessListener(docRef -> {
                        Log.d(TAG, "✅ Notification created directly! Doc ID: " + docRef.getId());
                    });
        }

        private long getTodayStartAsLong() {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        }


    }
