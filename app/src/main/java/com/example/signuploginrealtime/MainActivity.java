    package com.example.signuploginrealtime;
    
    import android.Manifest;
    import android.annotation.SuppressLint;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.content.pm.PackageManager;
    import android.os.Build;
    import android.os.Bundle;
    import android.widget.FrameLayout;
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

        private View notificationBadge;
        private ListenerRegistration unreadNotifListener;
        private ListenerRegistration workoutListener;
        private ListenerRegistration membershipListener;
        private ListenerRegistration coachNameListener;
        private static String cachedCoachName = null;
        private SharedPreferences membershipCache;
        private static String lastCoachId = null;
        private SharedPreferences coachCache;
        private static String cachedMembershipStatus = null;
        private static String cachedPlanType = null;
        private static String cachedExpiryDate = null;
        private static Integer cachedStatusColor = null;
        private static String cachedUserName = null;
        private static List<String> cachedExerciseNames = null;
        private static List<String> cachedExerciseGifs = null;
        private static String cachedPromoImageUrl = null;
        private ListenerRegistration expirationListener;


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

            coachCache = getSharedPreferences("MainActivity_cache", MODE_PRIVATE);

            displayCachedMembershipData();

            // ✅ Pre-fetch coach name immediately (don't wait for listener)
            if (currentUser != null) {
                dbFirestore.collection("users").document(currentUser.getUid())
                        .get()
                        .addOnSuccessListener(userDoc -> {
                            if (userDoc.exists()) {
                                String coachId = userDoc.getString("coachId");
                                if (coachId != null && !coachId.isEmpty()) {
                                    dbFirestore.collection("coaches").document(coachId)
                                            .get()
                                            .addOnSuccessListener(coachDoc -> {
                                                if (coachDoc.exists()) {
                                                    String coachName = coachDoc.getString("fullname");
                                                    if (coachName != null) {
                                                        cachedCoachName = coachName;
                                                        displayCoachName(coachName);
                                                    }
                                                }
                                            });
                                }
                            }
                        });
            }

            // ✅ Setup coach listener early (before waiting for user data)
            if (currentUser != null && coachNameListener == null) {
                setupCoachNameListener(currentUser.getUid());
            }

            setupPromoListener();
            setupClickListeners();
            loadUserDataFromFirestore();
            updateStreakDisplay();
            setupWorkoutListener();
            setupExpirationListener();


            new android.os.Handler().postDelayed(() -> {
                //checkAndHandleMembershipExpiration();
                checkAndSendWorkoutReminder();
            }, 800);


            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    showExitDialog();
                }
            });
    
    
        }
    
        private CardView membershipCardView;
        private LinearLayout membershipCtaContainer;

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
            notificationBadge = findViewById(R.id.notification_badge);

            // Initialize membership CTA views
            membershipCardView = findViewById(R.id.membershipCard);
            membershipCtaContainer = findViewById(R.id.membership_cta_container);
        }

        private void showExitDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.RoundedAlertDialog);
            builder.setTitle("Exit App?");
            builder.setMessage("Do you want to exit?");
            builder.setPositiveButton("Yes", (d, which) -> {
                finishAffinity(); // closes all activities and exits app
            });
            builder.setNegativeButton("No", null);

            AlertDialog dialog = builder.create();
            dialog.show();

            // ✅ Force button colors to be visible
            if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFF44336); // Red
            }
            if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF333333); // Dark gray
            }
        }

        /**
         * Show warning dialog when user with active membership tries to change plan
         * Uses the EXACT same warning message from SelectMembership
         */
        private void showChangeMembershipWarningDialog() {
            // Get current membership plan name and expiry
            String currentPlan = cachedPlanType != null ? cachedPlanType : "Unknown Plan";
            String expirationInfo = "";
            if (cachedExpiryDate != null && !cachedExpiryDate.isEmpty() && !cachedExpiryDate.equals("—")) {
                expirationInfo = "\n\nYour current plan expires on: " + cachedExpiryDate;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.RoundedAlertDialog);
            builder.setTitle("⚠️ Change Membership?");
            builder.setMessage("You currently have an active membership:\n\n" +
                    "Current Plan: " + currentPlan + expirationInfo +
                    "\n\n⚠️ WARNING: If you proceed to change your membership, you will:\n" +
                    "• Lose access to your current membership\n" +
                    "• Forfeit any remaining time on your current plan\n" +
                    "• Not receive a refund for the previous payment\n\n" +
                    "Do you want to continue to select a new plan?");

            builder.setPositiveButton("Yes, Continue", (d, which) -> {
                Intent intent = new Intent(MainActivity.this, SelectMembership.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
            builder.setNegativeButton("Cancel", null);

            AlertDialog dialog = builder.create();
            dialog.show();

            // Force button colors to be visible - use RED for warning (same as SelectMembership)
            if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFD32F2F); // Red (same as SelectMembership)
            }
            if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF666666); // Gray
            }
        }

        private void setupPromoListener() {
            CardView promoCard = findViewById(R.id.promo_card);
            ImageView testImage = findViewById(R.id.testImage);
            LinearLayout promoLayout = findViewById(R.id.promoLayout);

            // ✅ Add null checks before proceeding
            if (promoCard == null || promoLayout == null) {
                Log.e(TAG, "❌ Promo views not found in layout!");
                return;
            }

            // ✅ Display cached promo immediately if available
            if (cachedPromoImageUrl != null && !cachedPromoImageUrl.isEmpty()) {
                if (testImage != null) {
                    testImage.setVisibility(View.VISIBLE);
                    Glide.with(this).load(cachedPromoImageUrl)
                            .placeholder(R.drawable.no_image_placeholder)
                            .error(R.drawable.no_image_placeholder)
                            .into(testImage);
                }

                promoLayout.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, Promo.class);
                    intent.putExtra("promoUrl", cachedPromoImageUrl);
                    startActivity(intent);
                });
                promoCard.setCardBackgroundColor(getColor(android.R.color.black));
            } else {
                // Show "No Promo" state
                showNoPromoState(promoCard, testImage, promoLayout);
            }

            // Real-time listener
            DocumentReference latestPromoRef = dbFirestore.collection("promotions").document("latest");
            latestPromoRef.addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.w(TAG, "Listen failed for promotions.", e);
                    showNoPromoState(promoCard, testImage, promoLayout);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    String imageUrl = snapshot.getString("imageUrl");

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        cachedPromoImageUrl = imageUrl;

                        // ✅ CLEAR the "No Promo" layout first
                        promoLayout.removeAllViews();
                        promoLayout.setVisibility(View.GONE); // ✅ Hide the layout completely

                        // ✅ Make sure testImage is visible and loaded
                        if (testImage != null) {
                            testImage.setVisibility(View.VISIBLE);

                            // ✅ Force Glide to reload (clear cache for this URL)
                            Glide.with(this)
                                    .load(imageUrl)
                                    .skipMemoryCache(true)
                                    .placeholder(R.drawable.no_image_placeholder)
                                    .error(R.drawable.no_image_placeholder)
                                    .into(testImage);
                        }

                        // ✅ Set click listener on the CARD, not just the layout
                        promoCard.setOnClickListener(v -> {
                            Intent intent = new Intent(MainActivity.this, Promo.class);
                            intent.putExtra("promoUrl", imageUrl);
                            startActivity(intent);
                        });

                        // ✅ Change background back to black
                        promoCard.setCardBackgroundColor(getColor(android.R.color.black));

                        Log.d(TAG, "✅ Promo image updated: " + imageUrl);

                        // Create notification for new promo
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser != null) {
                            checkAndCreatePromoNotification(currentUser.getUid(), imageUrl);
                        }
                    }

                    else {
                        cachedPromoImageUrl = null;
                        showNoPromoState(promoCard, testImage, promoLayout);
                    }
                } else {
                    Log.d(TAG, "No data found in latest promotion document");
                    cachedPromoImageUrl = null;
                    showNoPromoState(promoCard, testImage, promoLayout);
                }
            });
        }



        private void showNoPromoState(CardView promoCard, ImageView testImage, LinearLayout promoLayout) {
            if (promoCard == null || promoLayout == null) {
                Log.e(TAG, "promoCard or promoLayout is null, cannot show no promo state");
                return;
            }

            if (testImage != null) {
                testImage.setVisibility(View.GONE);
            }

            promoLayout.removeAllViews();
            promoLayout.setVisibility(View.VISIBLE);

            // ❌ REMOVE THIS - DON'T SET LAYOUT PARAMS!
            // promoLayout.setLayoutParams(new FrameLayout.LayoutParams(...));

            // Create "No Promo" layout
            LinearLayout noPromoLayout = new LinearLayout(this);
            noPromoLayout.setOrientation(LinearLayout.VERTICAL);
            noPromoLayout.setGravity(android.view.Gravity.CENTER);
            noPromoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            ));

            // Lock icon
            TextView lockIcon = new TextView(this);
            lockIcon.setText("🔒");
            lockIcon.setTextSize(48);
            lockIcon.setGravity(android.view.Gravity.CENTER);
            noPromoLayout.addView(lockIcon);

            // "No Promo" text
            TextView noPromoText = new TextView(this);
            noPromoText.setText("No Promotions Available");
            noPromoText.setTextColor(getColor(android.R.color.white));
            noPromoText.setTextSize(16);
            noPromoText.setTypeface(null, android.graphics.Typeface.BOLD);
            noPromoText.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            textParams.setMargins(0, (int) (12 * getResources().getDisplayMetrics().density), 0, 0);
            noPromoText.setLayoutParams(textParams);
            noPromoLayout.addView(noPromoText);

            // Subtitle
            TextView subtitle = new TextView(this);
            subtitle.setText("Check back later for exciting offers!");
            subtitle.setTextColor(getColor(android.R.color.darker_gray));
            subtitle.setTextSize(12);
            subtitle.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            subtitleParams.setMargins(0, (int) (4 * getResources().getDisplayMetrics().density), 0, 0);
            subtitle.setLayoutParams(subtitleParams);
            noPromoLayout.addView(subtitle);

            promoLayout.addView(noPromoLayout);

            promoCard.setCardBackgroundColor(getColor(android.R.color.darker_gray));
            promoCard.setOnClickListener(null);
            promoCard.setClickable(false);
        }


        private void setupClickListeners() {
            findViewById(R.id.membershipCard).setOnClickListener(v -> {
                // Show warning if user already has ACTIVE or EXPIRING membership
                if ("ACTIVE".equals(cachedMembershipStatus) || "EXPIRING SOON".equals(cachedMembershipStatus)) {
                    showChangeMembershipWarningDialog();
                } else {
                    // For INACTIVE/EXPIRED, directly open SelectMembership
                    Intent intent = new Intent(MainActivity.this, SelectMembership.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                }
            });

            // CTA container click listener (for INACTIVE users)
            if (membershipCtaContainer != null) {
                membershipCtaContainer.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, SelectMembership.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                });
            }

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

            FrameLayout bellIconContainer = findViewById(R.id.bell_icon_container);
            if (bellIconContainer != null) {
                bellIconContainer.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, Notification.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                });
            }

            // ✅ KEEP THIS - with overridePendingTransition
            fab.setOnClickListener(v -> {
                Intent intent = new Intent(this, QR.class);
                startActivity(intent);
                overridePendingTransition(0, 0); // ✅ No animation
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

            // Start listening for unread notifications
            setupUnreadNotificationListener();
        }

        /**
         * Show membership CTA with pulse animation when membership is INACTIVE
         */
        private void showMembershipCta() {
            if (membershipCtaContainer != null) {
                membershipCtaContainer.setVisibility(View.VISIBLE);
                startPulseAnimation(membershipCtaContainer);
                Log.d(TAG, "✅ Membership CTA shown with pulse animation");
            }
        }

        /**
         * Hide membership CTA and stop animation when membership becomes ACTIVE
         */
        private void hideMembershipCta() {
            if (membershipCtaContainer != null) {
                membershipCtaContainer.setVisibility(View.GONE);
                stopPulseAnimation(membershipCtaContainer);
                Log.d(TAG, "✅ Membership CTA hidden");
            }
        }

        /**
         * Start pulse animation on a view
         */
        private void startPulseAnimation(View view) {
            if (view == null) return;

            android.view.animation.ScaleAnimation pulse = new android.view.animation.ScaleAnimation(
                    1f, 1.05f, 1f, 1.05f,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);

            pulse.setDuration(1000);
            pulse.setRepeatMode(android.view.animation.Animation.REVERSE);
            pulse.setRepeatCount(android.view.animation.Animation.INFINITE);

            view.startAnimation(pulse);
        }

        /**
         * Stop pulse animation on a view
         */
        private void stopPulseAnimation(View view) {
            if (view == null) return;
            view.clearAnimation();
        }

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

        private void setupWorkoutListener() {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "Current user is null");
                return;
            }

            // ✅ Prevent duplicate listeners
            if (workoutListener != null) {
                Log.d(TAG, "Workout listener already active");
                return;
            }

            // ✅ Display cached workouts immediately (NO FLICKER!)
            if (cachedExerciseNames != null && !cachedExerciseNames.isEmpty()) {
                displayYourWorkouts(cachedExerciseNames, cachedExerciseGifs);
            }

            Log.d(TAG, "🔄 Attaching workout listener (one-time setup)");

            // Set up real-time listener
            workoutListener = dbFirestore.collection("users")
                    .document(currentUser.getUid())
                    .collection("currentWorkout")
                    .document("week_1")
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            Log.e(TAG, "Error loading workouts", e);
                            cachedExerciseNames = null;
                            cachedExerciseGifs = null;
                            showNoWorkouts();
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            List<Map<String, Object>> exercisesList =
                                    (List<Map<String, Object>>) documentSnapshot.get("exercises");

                            if (exercisesList != null && !exercisesList.isEmpty()) {
                                List<String> exerciseNames = new ArrayList<>();
                                List<String> exerciseGifs = new ArrayList<>();

                                for (Map<String, Object> exerciseMap : exercisesList) {
                                    Map<String, Object> exerciseInfo =
                                            (Map<String, Object>) exerciseMap.get("exerciseInfo");

                                    if (exerciseInfo != null) {
                                        String name = (String) exerciseInfo.get("name");
                                        String gifUrl = (String) exerciseInfo.get("gifUrl");
                                        exerciseNames.add(name != null ? name : "Unknown Exercise");
                                        exerciseGifs.add(gifUrl != null ? gifUrl : "");
                                    }
                                }

                                if (!exerciseNames.isEmpty()) {
                                    cachedExerciseNames = new ArrayList<>(exerciseNames); // ✅ CACHE IT
                                    cachedExerciseGifs = new ArrayList<>(exerciseGifs);   // ✅ CACHE IT
                                    displayYourWorkouts(exerciseNames, exerciseGifs);
                                } else {
                                    cachedExerciseNames = null; // ✅ CLEAR CACHE
                                    cachedExerciseGifs = null;
                                    showNoWorkouts();
                                }
                            } else {
                                cachedExerciseNames = null;
                                cachedExerciseGifs = null;
                                showNoWorkouts();
                            }
                        } else {
                            cachedExerciseNames = null;
                            cachedExerciseGifs = null;
                            showNoWorkouts();
                        }
                    });
        }


        // Updated displayYourWorkouts to handle names and GIFs
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

                // ✅ ADD CLICK LISTENER TO EACH CARD
                exerciseCard.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, WorkoutList.class);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                });

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

                // ✅ Only attach listener if not already attached
                if (userDataListenerRegistrationFS != null) {
                    Log.d(TAG, "User data listener already active, skipping re-attach");
                    return;
                }

                Log.d(TAG, "🔄 Attaching user data listener (one-time setup)");

                // Add snapshot listener - will stay active until onDestroy
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

                        loadAndDisplayCache();
                        // ✅ Only setup membership listener once
                        if (membershipListener == null) {
                            setupMembershipListener();
                        }

                        updateGoalsProgressDisplay(firestoreSnapshot);

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

        private void loadAndDisplayCache() {
            if (membershipCache == null) {
                membershipCache = getSharedPreferences("MainActivity_cache", MODE_PRIVATE);
            }

            // Load from cache
            cachedMembershipStatus = membershipCache.getString("cached_status", null);
            cachedPlanType = membershipCache.getString("cached_plan", null);
            cachedExpiryDate = membershipCache.getString("cached_expiry", null);
            int colorValue = membershipCache.getInt("cached_color", 0);
            if (colorValue != 0) {
                cachedStatusColor = colorValue;
            }

            // Display cached data IMMEDIATELY
            if (cachedMembershipStatus != null && !cachedMembershipStatus.isEmpty()) {
                membershipStatus.setText(cachedMembershipStatus);
                if (cachedStatusColor != null) {
                    membershipStatus.setTextColor(cachedStatusColor);
                }
                Log.d(TAG, "✅ Displayed cached status: " + cachedMembershipStatus);
            } else {
                // No cache yet - show default without flicker
                membershipStatus.setText("LOADING...");
                membershipStatus.setTextColor(getColor(R.color.gray));
            }

            if (cachedPlanType != null && !cachedPlanType.isEmpty()) {
                planType.setText(cachedPlanType);
                Log.d(TAG, "✅ Displayed cached plan: " + cachedPlanType);
            } else {
                planType.setText("Loading...");
            }

            if (cachedExpiryDate != null && !cachedExpiryDate.isEmpty()) {
                expiryDate.setText(cachedExpiryDate);
                Log.d(TAG, "✅ Displayed cached expiry: " + cachedExpiryDate);
            } else {
                expiryDate.setText("—");
            }
        }


        @SuppressLint("SetTextI18n")
        private void updateGreeting(DocumentSnapshot firestoreSnapshot) {
            String name = firestoreSnapshot.getString("fullname");
            String displayName = (name != null && !name.trim().isEmpty()) ? name : "User";

            cachedUserName = displayName; // ✅ CACHE IT
            greetingText.setText("Hi, " + displayName);
        }


        private void setupMembershipListener() {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            // ✅ Prevent duplicate listeners
            if (membershipListener != null) {
                Log.d(TAG, "Membership listener already active");
                return;
            }

            Log.d(TAG, "🔄 Attaching membership listener (one-time setup)");

            // Set up real-time membership listener
            membershipListener = dbFirestore.collection("memberships")
                    .document(user.getUid())
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            Log.e(TAG, "Failed to listen to membership", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists() &&
                                "active".equals(documentSnapshot.getString("membershipStatus"))) {

                            String planTypeValue = documentSnapshot.getString("membershipPlanType");
                            Long months = documentSnapshot.getLong("months");
                            Long sessions = documentSnapshot.getLong("sessions");
                            Timestamp expirationTimestamp = documentSnapshot.getTimestamp("membershipExpirationDate");

                            // Generate formatted display name
                            String displayName = generateFormattedPlanName(planTypeValue, months, sessions);

                            // Check if plan is valid (not "None")
                            if (planTypeValue != null && !planTypeValue.isEmpty() && !planTypeValue.equals("None")) {

                                if (expirationTimestamp != null) {
                                    Date expDate = expirationTimestamp.toDate();
                                    String newExpiryDate = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(expDate);

                                    // ✅ Use server time for accurate comparison
                                    Map<String, Object> serverTimeData = new HashMap<>();
                                    serverTimeData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

                                    dbFirestore.collection("server_time").document("current")
                                            .set(serverTimeData)
                                            .addOnSuccessListener(aVoid -> {
                                                dbFirestore.collection("server_time").document("current")
                                                        .get()
                                                        .addOnSuccessListener(serverDoc -> {
                                                            Timestamp serverTimestamp = serverDoc.getTimestamp("timestamp");
                                                            Date currentDate = (serverTimestamp != null) ? serverTimestamp.toDate() : new Date();

                                                            long diffInMillis = expDate.getTime() - currentDate.getTime();
                                                            long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);
                                                            long diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillis);

                                                            // ✅ Calculate new status
                                                            String newStatus;
                                                            int newColor;
                                                            String newPlanText;

                                                            if (diffInMillis < 0) {
                                                                newStatus = "EXPIRED";
                                                                newColor = getColor(R.color.red);
                                                                newPlanText = displayName + " (Expired)";
                                                            } else if (diffInHours <= 6) {
                                                                newStatus = "EXPIRING SOON";
                                                                newColor = getColor(R.color.orange);
                                                                if (diffInHours > 0) {
                                                                    newPlanText = displayName + " (Expires in " + diffInHours + "h)";
                                                                } else {
                                                                    long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
                                                                    newPlanText = displayName + " (Expires in " + diffInMinutes + "m)";
                                                                }
                                                            } else if (diffInDays >= 1 && diffInDays <= 3) {
                                                                newStatus = "EXPIRING SOON";
                                                                newColor = getColor(R.color.orange);
                                                                newPlanText = displayName + " (" + diffInDays + " day(s) left)";
                                                            } else {
                                                                newStatus = "ACTIVE";
                                                                newColor = getColor(R.color.green);
                                                                newPlanText = displayName;
                                                            }

                                                            // ✅ ONLY UPDATE IF DATA CHANGED
                                                            boolean statusChanged = !newStatus.equals(cachedMembershipStatus);
                                                            boolean planChanged = !newPlanText.equals(cachedPlanType);
                                                            boolean expiryChanged = !newExpiryDate.equals(cachedExpiryDate);

                                                            if (statusChanged || planChanged || expiryChanged) {
                                                                Log.d(TAG, "📊 Membership data changed, updating UI");

                                                                // Update cache
                                                                cachedMembershipStatus = newStatus;
                                                                cachedStatusColor = newColor;
                                                                cachedPlanType = newPlanText;
                                                                cachedExpiryDate = newExpiryDate;

                                                                // Save to SharedPreferences
                                                                if (membershipCache == null) {
                                                                    membershipCache = getSharedPreferences("MainActivity_cache", MODE_PRIVATE);
                                                                }
                                                                membershipCache.edit()
                                                                        .putString("cached_status", newStatus)
                                                                        .putString("cached_plan", newPlanText)
                                                                        .putString("cached_expiry", newExpiryDate)
                                                                        .putInt("cached_color", newColor)
                                                                        .apply();

                                                                // Update UI
                                                                membershipStatus.setText(newStatus);
                                                                membershipStatus.setTextColor(newColor);
                                                                planType.setText(newPlanText);
                                                                expiryDate.setText(newExpiryDate);

                                                                // Hide CTA when membership is not inactive
                                                                if (!"INACTIVE".equals(newStatus)) {
                                                                    hideMembershipCta();
                                                                }
                                                            } else {
                                                                Log.d(TAG, "📊 Membership data unchanged, skipping UI update");
                                                            }
                                                        })
                                                        .addOnFailureListener(serverError -> {
                                                            // Fallback to system time
                                                            updateMembershipUI(displayName, newExpiryDate, expDate, new Date());
                                                        });
                                            })
                                            .addOnFailureListener(setError -> {
                                                // Fallback to system time
                                                updateMembershipUI(displayName, newExpiryDate, expDate, new Date());
                                            });
                                } else {
                                    updateMembershipToInactive();
                                }
                            } else {
                                // Plan is "None" or invalid
                                Log.d(TAG, "Plan is 'None' or invalid - showing inactive");
                                updateMembershipToInactive();
                                hideCoachName();
                            }
                        } else {
                            Log.d(TAG, "No active membership found");
                            updateMembershipToInactive();
                            hideCoachName();
                        }
                    });
        }
        private void updateMembershipUI(String displayName, String newExpiryDate, Date expDate, Date currentDate) {
            long diffInMillis = expDate.getTime() - currentDate.getTime();
            long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);
            long diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillis);

            String newStatus;
            int newColor;
            String newPlanText;

            if (diffInMillis < 0) {
                newStatus = "EXPIRED";
                newColor = getColor(R.color.red);
                newPlanText = displayName + " (Expired)";
            } else if (diffInHours <= 6) {
                newStatus = "EXPIRING SOON";
                newColor = getColor(R.color.orange);
                if (diffInHours > 0) {
                    newPlanText = displayName + " (Expires in " + diffInHours + "h)";
                } else {
                    long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
                    newPlanText = displayName + " (Expires in " + diffInMinutes + "m)";
                }
            } else if (diffInDays >= 1 && diffInDays <= 3) {
                newStatus = "EXPIRING SOON";
                newColor = getColor(R.color.orange);
                newPlanText = displayName + " (" + diffInDays + " day(s) left)";
            } else {
                newStatus = "ACTIVE";
                newColor = getColor(R.color.green);
                newPlanText = displayName;
            }

            // Only update if changed
            if (!newStatus.equals(cachedMembershipStatus) ||
                    !newPlanText.equals(cachedPlanType) ||
                    !newExpiryDate.equals(cachedExpiryDate)) {

                cachedMembershipStatus = newStatus;
                cachedStatusColor = newColor;
                cachedPlanType = newPlanText;
                cachedExpiryDate = newExpiryDate;

                if (membershipCache == null) {
                    membershipCache = getSharedPreferences("MainActivity_cache", MODE_PRIVATE);
                }
                membershipCache.edit()
                        .putString("cached_status", newStatus)
                        .putString("cached_plan", newPlanText)
                        .putString("cached_expiry", newExpiryDate)
                        .putInt("cached_color", newColor)
                        .apply();

                membershipStatus.setText(newStatus);
                membershipStatus.setTextColor(newColor);
                planType.setText(newPlanText);
                expiryDate.setText(newExpiryDate);

                // Hide CTA when membership is not inactive
                if (!"INACTIVE".equals(newStatus)) {
                    hideMembershipCta();
                }
            }
        }

        private void updateMembershipToInactive() {
            String newStatus = "INACTIVE";
            int newColor = getColor(R.color.red);
            String newPlanText = "No plan selected";
            String newExpiryDate = "—";

            // Only update if changed
            if (!newStatus.equals(cachedMembershipStatus)) {
                cachedMembershipStatus = newStatus;
                cachedStatusColor = newColor;
                cachedPlanType = newPlanText;
                cachedExpiryDate = newExpiryDate;

                if (membershipCache == null) {
                    membershipCache = getSharedPreferences("MainActivity_cache", MODE_PRIVATE);
                }
                membershipCache.edit()
                        .putString("cached_status", newStatus)
                        .putString("cached_plan", newPlanText)
                        .putString("cached_expiry", newExpiryDate)
                        .putInt("cached_color", newColor)
                        .apply();

                membershipStatus.setText(newStatus);
                membershipStatus.setTextColor(newColor);
                planType.setText(newPlanText);
                expiryDate.setText(newExpiryDate);

                // Show CTA when membership is inactive
                showMembershipCta();
            }
        }

        private void setupCoachNameListener(String userId) {
            if (coachNameListener != null) {
                return;
            }

            coachNameListener = dbFirestore.collection("users")
                    .document(userId)
                    .addSnapshotListener((userDoc, e) -> {
                        if (e != null) {
                            return;
                        }

                        if (userDoc != null && userDoc.exists()) {
                            String coachId = userDoc.getString("coachId");

                            if (coachId != null && !coachId.isEmpty()) {
                                if (!coachId.equals(lastCoachId)) {
                                    lastCoachId = coachId;

                                    dbFirestore.collection("coaches")
                                            .document(coachId)
                                            .get()
                                            .addOnSuccessListener(coachDoc -> {
                                                if (coachDoc.exists()) {
                                                    String coachFullName = coachDoc.getString("fullname");
                                                    if (coachFullName != null && !coachFullName.equals(cachedCoachName)) {
                                                        cachedCoachName = coachFullName;

                                                        if (coachCache == null) {
                                                            coachCache = getSharedPreferences("MainActivity_cache", MODE_PRIVATE);
                                                        }
                                                        coachCache.edit().putString("cached_coach_name", coachFullName).apply();

                                                        displayCoachName(coachFullName);
                                                    }
                                                }
                                            });
                                }
                            } else {
                                if (lastCoachId != null) {
                                    lastCoachId = null;
                                    cachedCoachName = null;

                                    if (coachCache == null) {
                                        coachCache = getSharedPreferences("MainActivity_cache", MODE_PRIVATE);
                                    }
                                    coachCache.edit().remove("cached_coach_name").apply();

                                    hideCoachName();
                                }
                            }
                        }
                    });
        }

        private String generateFormattedPlanName(String type, Long months, Long sessions) {
            if (type == null) return "Unknown Plan";

            int monthsVal = (months != null) ? months.intValue() : 0;
            int sessionsVal = (sessions != null) ? sessions.intValue() : 0;

            // For Daily Pass
            if ("Daily".equals(type) || monthsVal == 0) {
                return "Daily";
            }

            // For Standard (no PT sessions)
            if (sessionsVal == 0) {
                if (monthsVal == 1) return "Standard Monthly";
                else if (monthsVal == 3) return "Standard 3 Months";
                else if (monthsVal == 6) return "Standard 6 Months";
                else if (monthsVal == 12) return "Standard Annual";
            }

            // For Monthly with PT
            if (sessionsVal > 0) {
                if (monthsVal == 1) return "Monthly with " + sessionsVal + " PT";
                else if (monthsVal == 3) return "3 Months with " + sessionsVal + " PT";
                else if (monthsVal == 6) return "6 Months with " + sessionsVal + " PT";
                else if (monthsVal == 12) return "Annual with " + sessionsVal + " PT";
            }

            // Fallback
            return type;
        }

        // ✅ Helper for updating membership status in UI (called after time fetched)
        private void updateMembershipStatusBasedOnTime(long diffInMillis, long diffInDays, long diffInHours, String displayName) {
            if (diffInMillis < 0) {
                // EXPIRED
                cachedMembershipStatus = "EXPIRED";
                cachedStatusColor = getColor(R.color.red);
                membershipStatus.setText("EXPIRED");
                membershipStatus.setTextColor(cachedStatusColor);
                cachedPlanType = displayName + " (Expired)";
                planType.setText(displayName + " (Expired)");
            } else if (diffInHours <= 6) {
                // EXPIRING SOON - only in last 6 hours
                cachedMembershipStatus = "EXPIRING SOON";
                cachedStatusColor = getColor(R.color.orange);
                membershipStatus.setText("EXPIRING SOON");
                membershipStatus.setTextColor(cachedStatusColor);
                if (diffInHours > 0) {
                    cachedPlanType = displayName + " (Expires in " + diffInHours + "h)";
                    planType.setText(displayName + " (Expires in " + diffInHours + "h)");
                } else {
                    long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
                    cachedPlanType = displayName + " (Expires in " + diffInMinutes + "m)";
                    planType.setText(displayName + " (Expires in " + diffInMinutes + "m)");
                }
            } else if (diffInDays >= 1 && diffInDays <= 3) {
                // EXPIRING SOON - 1 to 3 days
                cachedMembershipStatus = "EXPIRING SOON";
                cachedStatusColor = getColor(R.color.orange);
                membershipStatus.setText("EXPIRING SOON");
                membershipStatus.setTextColor(cachedStatusColor);
                cachedPlanType = displayName + " (" + diffInDays + " day(s) left)";
                planType.setText(displayName + " (" + diffInDays + " day(s) left)");
            } else {
                // ACTIVE - more than 6 hours or more than 3 days
                cachedMembershipStatus = "ACTIVE";
                cachedStatusColor = getColor(R.color.green);
                membershipStatus.setText("ACTIVE");
                membershipStatus.setTextColor(cachedStatusColor);
                cachedPlanType = displayName;
                planType.setText(displayName);
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

            // ✅ ADD THIS - Force update bottom nav selection
            if (bottomNavigationView != null) {
                bottomNavigationView.setSelectedItemId(R.id.item_1);
            }

            if (mAuth.getCurrentUser() != null) {
                updateStreakDisplay();

                // Check if a workout was just completed
                boolean workoutCompleted = workoutPrefs.getBoolean("workout_completed", false);
                if (workoutCompleted) {
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
            if (userDataListenerRegistrationFS != null) {
                userDataListenerRegistrationFS.remove();
            }
            if (unreadNotifListener != null) {
                unreadNotifListener.remove();
            }
            if (workoutListener != null) {
                workoutListener.remove();
            }
            if (membershipListener != null) {
                membershipListener.remove();
            }
            if (coachNameListener != null) {
                coachNameListener.remove();
            }
            if (expirationListener != null) {  // ✅ ADD THESE 3 LINES
                expirationListener.remove();
                Log.d(TAG, "🧹 Expiration listener removed");
            }
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
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && "active".equals(doc.getString("membershipStatus"))) {
                            com.google.firebase.Timestamp expirationTimestamp = doc.getTimestamp("membershipExpirationDate");
                            if (expirationTimestamp == null) return;

                            Date expirationDate = expirationTimestamp.toDate();
                            Date today = new Date();
                            long diffInMillis = expirationDate.getTime() - today.getTime();
                            long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);

                            Log.d(TAG, "Membership expires in " + diffInDays + " days");

                            if (diffInMillis < 0) {
                                // 🔴 EXPIRED - Update status and set plan to "None"
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("membershipStatus", "expired");
                                updates.put("membershipPlanLabel", "None");
                                updates.put("membershipPlanType", "None");  // ✅ ADD THIS
                                updates.put("membershipPlanCode", null);

                                db.collection("memberships").document(user.getUid())
                                        .update(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            // Also update users collection
                                            Map<String, Object> userUpdates = new HashMap<>();
                                            userUpdates.put("membershipStatus", "expired");
                                            userUpdates.put("membershipActive", false);
                                            userUpdates.put("membershipPlanLabel", "None");
                                            userUpdates.put("membershipPlanType", "None");  // ✅ ADD THIS
                                            userUpdates.put("membershipPlanCode", null);

                                            db.collection("users").document(user.getUid())
                                                    .update(userUpdates)
                                                    .addOnSuccessListener(v -> Log.d(TAG, "User membership status and plan updated to None"))
                                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update user status", e));

                                            showExpirationPopup("Your membership has expired.");
                                            saveNotificationToFirestore("expired", 0);
                                            loadUserDataFromFirestore();
                                        })
                                        .addOnFailureListener(e -> Log.e(TAG, "Failed to update membership to expired", e));

                            } else if (diffInDays <= 3 && diffInDays >= 0) {
                                // 🟠 EXPIRING SOON - Notify ONCE per day
                                saveNotificationToFirestore("expiring_soon", (int) diffInDays);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error checking expiration", e));
        }

        private void setupExpirationListener() {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            // ✅ Prevent duplicate listeners
            if (expirationListener != null) {
                Log.d(TAG, "Expiration listener already active");
                return;
            }

            Log.d(TAG, "🔄 Setting up real-time expiration check");

            // ✅ Use addSnapshotListener for REAL-TIME monitoring
            expirationListener = dbFirestore.collection("memberships")
                    .document(user.getUid())
                    .addSnapshotListener((doc, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Error listening to expiration", error);
                            return;
                        }

                        if (doc == null || !doc.exists()) {
                            Log.d(TAG, "No membership document found");
                            return;
                        }

                        String status = doc.getString("membershipStatus");
                        String planType = doc.getString("membershipPlanType");
                        Timestamp expirationTimestamp = doc.getTimestamp("membershipExpirationDate");

                        // ✅ Check if plan is actually active (not "None")
                        if ("active".equals(status) && expirationTimestamp != null &&
                                planType != null && !planType.isEmpty() && !planType.equals("None")) {

                            Date expirationDate = expirationTimestamp.toDate();

                            // ✅ FIX: Use server timestamp instead of system date to prevent manual date changes
                            // Get server timestamp from Firestore to compare
                            Map<String, Object> serverTimeData = new HashMap<>();
                            serverTimeData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

                            dbFirestore.collection("server_time").document("current")
                                    .set(serverTimeData)
                                    .addOnSuccessListener(aVoid -> {
                                        // Now get the server timestamp we just set
                                        dbFirestore.collection("server_time").document("current")
                                                .get()
                                                .addOnSuccessListener(serverDoc -> {
                                                    Timestamp serverTimestamp = serverDoc.getTimestamp("timestamp");
                                                    Date currentDate = (serverTimestamp != null) ? serverTimestamp.toDate() : new Date();

                                                    Log.d(TAG, "📅 Checking expiration (using server time):");
                                                    Log.d(TAG, "   Server Date: " + currentDate);
                                                    Log.d(TAG, "   Expiration Date: " + expirationDate);
                                                    Log.d(TAG, "   Is Expired? " + currentDate.after(expirationDate));

                                                    // Check if expired using server time
                                                    if (currentDate.after(expirationDate)) {
                                                        Log.d(TAG, "⏰ Membership has EXPIRED (confirmed with server time)! Resetting...");
                                                        handleExpiredMembership(doc);
                                                    } else {
                                                        Log.d(TAG, "✅ Membership is still active (confirmed with server time)");
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "❌ Failed to get server time, using system time as fallback", e);
                                                    // Fallback to system time if server time fails
                                                    Date currentDate = new Date();
                                                    if (currentDate.after(expirationDate)) {
                                                        Log.d(TAG, "⏰ Membership has EXPIRED (fallback to system time)! Resetting...");
                                                        handleExpiredMembership(doc);
                                                    }
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "❌ Failed to set server timestamp, using system time as fallback", e);
                                        // Fallback to system time if server timestamp fails
                                        Date currentDate = new Date();
                                        if (currentDate.after(expirationDate)) {
                                            Log.d(TAG, "⏰ Membership has EXPIRED (fallback to system time)! Resetting...");
                                            handleExpiredMembership(doc);
                                        }
                                    });

                        } else if ("inactive".equals(status) || "None".equals(planType)) {
                            Log.d(TAG, "Membership already inactive or is 'None'");
                        }
                    });
        }

        // ✅ ADD THIS METHOD TOO ⬇️
        private void handleExpiredMembership(DocumentSnapshot doc) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            String userId = user.getUid();

            // ✅ Get current membership data before resetting
            String expiredPlanType = doc.getString("membershipPlanType");
            Long expiredSessions = doc.getLong("sessions");
            String expiredCoachId = doc.getString("coachId");

            Log.d(TAG, "⏰ Handling expired membership:");
            Log.d(TAG, "   Plan Type: " + expiredPlanType);
            Log.d(TAG, "   Sessions: " + expiredSessions);
            Log.d(TAG, "   Coach ID: " + expiredCoachId);

            // Reset to inactive
            Map<String, Object> membershipUpdates = new HashMap<>();
            membershipUpdates.put("membershipStatus", "inactive");
            membershipUpdates.put("membershipPlanType", "None");
            membershipUpdates.put("months", 0);
            membershipUpdates.put("sessions", 0);
            membershipUpdates.put("price", 0);
            membershipUpdates.put("membershipStartDate", null);
            membershipUpdates.put("membershipExpirationDate", null);
            membershipUpdates.put("lastUpdated", Timestamp.now());
            membershipUpdates.put("coachId", null);  // ✅ RESET COACH ID
            membershipUpdates.put("coachName", "No coach assigned");
            membershipUpdates.put("email", null);

            dbFirestore.collection("memberships").document(userId)
                    .update(membershipUpdates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Membership reset to inactive");

                        // ✅ CRITICAL: Also update users collection
                        Map<String, Object> userUpdates = new HashMap<>();
                        userUpdates.put("membershipStatus", "inactive");
                        userUpdates.put("membershipActive", false);
                        userUpdates.put("membershipPlanType", "None");
                        userUpdates.put("membershipExpirationDate", null);
                        userUpdates.put("months", 0);
                        userUpdates.put("sessions", 0);
                        userUpdates.put("coachId", null);  // ✅ THIS IS THE KEY FIX - RESET COACH ID IN USERS TOO

                        dbFirestore.collection("users").document(userId)
                                .update(userUpdates)
                                .addOnSuccessListener(v -> {
                                    Log.d(TAG, "✅ User document updated - coachId reset to null");

                                    // ✅ Archive the expired membership to history
                                    if (expiredPlanType != null && !expiredPlanType.equals("None")) {
                                        archiveExpiredMembershipToHistory(userId, doc);
                                    }

                                    showExpirationPopup("Your membership has expired.");
                                    saveNotificationToFirestore("membership_expired", 0);

                                    // ✅ Update UI immediately
                                    runOnUiThread(() -> {
                                        // Clear cached values
                                        cachedMembershipStatus = null;
                                        cachedPlanType = null;
                                        cachedExpiryDate = null;
                                        cachedStatusColor = null;
                                        cachedCoachName = null;

                                        // Clear coach cache
                                        if (coachCache != null) {
                                            coachCache.edit().remove("cached_coach_name").apply();
                                        }

                                        setDefaultMembershipValues();
                                        hideCoachName();

                                        Log.d(TAG, "🎯 UI updated - Coach name hidden, membership reset");
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "❌ Failed to update user document", e);
                                    e.printStackTrace();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Failed to update membership", e);
                        e.printStackTrace();
                    });
        }

        // ✅ ADD THIS NEW METHOD TO ARCHIVE EXPIRED MEMBERSHIPS
        private void archiveExpiredMembershipToHistory(String userId, DocumentSnapshot membershipDoc) {
            try {
                Map<String, Object> historyData = new HashMap<>();
                historyData.put("userId", userId);
                historyData.put("fullname", membershipDoc.getString("fullname"));
                historyData.put("email", membershipDoc.getString("email"));
                historyData.put("membershipPlanType", membershipDoc.getString("membershipPlanType"));
                historyData.put("months", membershipDoc.getLong("months"));
                historyData.put("sessions", membershipDoc.getLong("sessions"));
                historyData.put("price", membershipDoc.getDouble("price"));
                historyData.put("coachId", membershipDoc.getString("coachId"));
                historyData.put("coachName", membershipDoc.getString("coachName"));
                historyData.put("startDate", membershipDoc.getTimestamp("membershipStartDate"));
                historyData.put("expirationDate", membershipDoc.getTimestamp("membershipExpirationDate"));
                historyData.put("status", "expired");
                historyData.put("timestamp", Timestamp.now());
                historyData.put("paymentMethod", "auto-expired");

                dbFirestore.collection("history")
                        .add(historyData)
                        .addOnSuccessListener(docRef -> {
                            Log.d(TAG, "📜 Expired membership archived to history: " + docRef.getId());
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "❌ Failed to archive expired membership", e);
                        });
            } catch (Exception e) {
                Log.e(TAG, "❌ Error archiving expired membership", e);
            }
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
            String todayDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            // ✅ Check if notification already exists for TODAY
            db.collection("notifications")
                    .whereEqualTo("userId", user.getUid())
                    .whereEqualTo("type", notificationType)
                    .whereEqualTo("notificationDate", todayDateStr) // Use date string instead of timestamp range
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot.isEmpty()) {
                            String title, message;

                            if ("expired".equals(notificationType)) {
                                title = "Membership Expired";
                                message = "Your membership has expired. Renew now to continue enjoying gym access.";

                                // Create notification
                                createNotificationWithDate(user.getUid(), title, message, notificationType, todayDateStr);

                            } else {
                                title = "Membership Expiring Soon";

                                // Get the actual expiration date from Firestore
                                db.collection("memberships")
                                        .document(user.getUid())
                                        .get()
                                        .addOnSuccessListener(doc -> {
                                            if (doc.exists()) {
                                                Timestamp expirationTimestamp = doc.getTimestamp("membershipExpirationDate");
                                                if (expirationTimestamp != null) {
                                                    Date expDate = expirationTimestamp.toDate();
                                                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
                                                    String formattedDate = sdf.format(expDate);

                                                    String msg = "Your membership will expire on " + formattedDate + ". Renew soon!";

                                                    // Create notification with date tracking
                                                    createNotificationWithDate(user.getUid(), title, msg, notificationType, todayDateStr);
                                                }
                                            }
                                        });
                            }
                        } else {
                            Log.d(TAG, "⚠️ Skipping duplicate " + notificationType + " notification for today (" + todayDateStr + ")");
                        }
                    });
        }



        private void checkAndSendWorkoutReminder() {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) return;

            String userId = user.getUid();
            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            // ✅ First check if reminder already sent TODAY
            dbFirestore.collection("notifications")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("type", "workout_reminder")
                    .whereEqualTo("notificationDate", todayDate) // Check by date string
                    .get()
                    .addOnSuccessListener(existingNotifs -> {
                        if (!existingNotifs.isEmpty()) {
                            Log.d(TAG, "⚠️ Workout reminder already sent today, skipping");
                            return;
                        }

                        // Check if user already worked out today
                        dbFirestore.collection("users")
                                .document(userId)
                                .collection("progress")
                                .whereEqualTo("date", todayDate)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    if (querySnapshot.isEmpty()) {
                                        // No workout today, check weekly goal
                                        dbFirestore.collection("users")
                                                .document(userId)
                                                .get()
                                                .addOnSuccessListener(userDoc -> {
                                                    Long workoutGoal = userDoc.getLong("workoutDaysPerWeek");

                                                    if (workoutGoal != null && workoutGoal > 0) {
                                                        // Count this week's completed workouts
                                                        dbFirestore.collection("users")
                                                                .document(userId)
                                                                .collection("progress")
                                                                .get()
                                                                .addOnSuccessListener(progressSnapshot -> {
                                                                    int completedThisWeek = 0;
                                                                    for (DocumentSnapshot doc : progressSnapshot) {
                                                                        String dateStr = doc.getString("date");
                                                                        if (dateStr != null && isDateInCurrentWeek(dateStr)) {
                                                                            completedThisWeek++;
                                                                        }
                                                                    }

                                                                    // If not yet reached weekly goal, send reminder
                                                                    if (completedThisWeek < workoutGoal) {
                                                                        sendDailyWorkoutReminder(userId, workoutGoal.intValue(), completedThisWeek, todayDate);
                                                                    }
                                                                });
                                                    }
                                                });
                                    }
                                });
                    });
        }
        private void sendDailyWorkoutReminder(String userId, int weeklyGoal, int completed, String todayDate) {
            int remaining = weeklyGoal - completed;

            String title = "Daily Workout Reminder";
            String message = "You haven't worked out today! " + remaining + " workout(s) remaining this week to reach your goal.";

            Log.d(TAG, "🔔 Creating workout reminder for " + todayDate);

            // Create notification with date tracking to prevent duplicates
            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", userId);
            notification.put("title", title);
            notification.put("message", message);
            notification.put("type", "workout_reminder");
            notification.put("notificationDate", todayDate); // Track date
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("read", false);

            dbFirestore.collection("notifications")
                    .add(notification)
                    .addOnSuccessListener(docRef -> {
                        Log.d(TAG, "✅ Workout reminder created for " + todayDate + ": " + docRef.getId());
                        NotificationHelper.showNotification(MainActivity.this, title, message);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ FAILED to create workout reminder: " + e.getMessage(), e);
                    });
        }
        private void checkAndCreatePromoNotification(String userId, String imageUrl) {
            // Use imageUrl as unique identifier to prevent duplicate notifications
            dbFirestore.collection("notifications")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("type", "promo")
                    .whereEqualTo("promoImageUrl", imageUrl) // Check if we already notified for this image
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot.isEmpty()) {
                            // No notification exists for this promo yet, create one
                            String title = "New Promotion Available!";
                            String message = "Check out our latest promotion. Tap to view details!";

                            Map<String, Object> notification = new HashMap<>();
                            notification.put("userId", userId);
                            notification.put("title", title);
                            notification.put("message", message);
                            notification.put("type", "promo");
                            notification.put("promoImageUrl", imageUrl); // Store imageUrl to prevent duplicates
                            notification.put("timestamp", System.currentTimeMillis());
                            notification.put("read", false);

                            dbFirestore.collection("notifications")
                                    .add(notification)
                                    .addOnSuccessListener(docRef -> {
                                        Log.d(TAG, "✅ Promo notification created: " + docRef.getId());
                                        // Show local notification
                                        NotificationHelper.showNotification(MainActivity.this, title, message);
                                    })
                                    .addOnFailureListener(ex ->
                                            Log.e(TAG, "❌ Failed to create promo notification", ex));
                        } else {
                            Log.d(TAG, "⚠️ Promo notification already exists for this image, skipping");
                        }
                    })
                    .addOnFailureListener(ex ->
                            Log.e(TAG, "Error checking existing promo notifications", ex));
        }

        private void setupUnreadNotificationListener() {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) return;

            // ✅ Prevent duplicate listeners
            if (unreadNotifListener != null) {
                Log.d(TAG, "Unread notification listener already active");
                return;
            }

            String userId = currentUser.getUid();

            Log.d(TAG, "🔄 Attaching unread notification listener (one-time setup)");

            // Listen for unread notifications in real-time
            unreadNotifListener = dbFirestore.collection("notifications")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("read", false)
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) {
                            Log.w(TAG, "Listen failed for unread notifications.", e);
                            return;
                        }

                        if (snapshots != null && !snapshots.isEmpty()) {
                            // Has unread notifications - show badge
                            if (notificationBadge != null) {
                                notificationBadge.setVisibility(View.VISIBLE);
                            }
                            Log.d(TAG, "Unread notifications: " + snapshots.size());
                        } else {
                            // No unread notifications - hide badge
                            if (notificationBadge != null) {
                                notificationBadge.setVisibility(View.GONE);
                            }
                            Log.d(TAG, "No unread notifications");
                        }
                    });
        }


        private void createNotificationWithDate(String userId, String title, String message, String type, String dateStr) {
            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", userId);
            notification.put("title", title);
            notification.put("message", message);
            notification.put("type", type);
            notification.put("notificationDate", dateStr); // Track which date this notification was created
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("read", false);

            dbFirestore.collection("notifications")
                    .add(notification)
                    .addOnSuccessListener(docRef -> {
                        Log.d(TAG, "✅ Notification created for " + dateStr + ": " + docRef.getId());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Failed to create notification", e);
                    });
        }

        // ✅ Helper method to display coach name
        private void displayCoachName(String coachName) {
            LinearLayout coachNameContainer = findViewById(R.id.coachNameContainer);
            TextView coachNameView = findViewById(R.id.coach_name);

            if (coachName != null && !coachName.isEmpty()) {
                coachNameView.setText(coachName);
                coachNameContainer.setVisibility(View.VISIBLE);
                Log.d(TAG, "✅ Displaying coach: " + coachName);
            } else {
                hideCoachName();
            }
        }

        // ✅ Helper method to hide coach name
        private void hideCoachName() {
            LinearLayout coachNameContainer = findViewById(R.id.coachNameContainer);
            if (coachNameContainer != null) {
                coachNameContainer.setVisibility(View.GONE);
                Log.d(TAG, "❌ Hiding coach name container");
            }
        }

        private void displayCachedMembershipData() {
            // ✅ Display cached name first
            if (cachedUserName != null) {
                greetingText.setText("Hi, " + cachedUserName);
            }

            // ✅ Display cached MEMBERSHIP DATA (STATUS, PLAN, EXPIRY)
            if (membershipCache == null) {
                membershipCache = getSharedPreferences("MainActivity_cache", MODE_PRIVATE);
            }

            cachedMembershipStatus = membershipCache.getString("cached_status", null);
            cachedPlanType = membershipCache.getString("cached_plan", null);
            cachedExpiryDate = membershipCache.getString("cached_expiry", null);
            int colorValue = membershipCache.getInt("cached_color", 0);
            if (colorValue != 0) {
                cachedStatusColor = colorValue;
            }

            // Display membership status
            if (cachedMembershipStatus != null && !cachedMembershipStatus.isEmpty()) {
                membershipStatus.setText(cachedMembershipStatus);
                if (cachedStatusColor != null) {
                    membershipStatus.setTextColor(cachedStatusColor);
                }

                // Show CTA if membership is INACTIVE
                if ("INACTIVE".equals(cachedMembershipStatus)) {
                    showMembershipCta();
                }
            }

            // Display plan type
            if (cachedPlanType != null && !cachedPlanType.isEmpty()) {
                planType.setText(cachedPlanType);
            }

            // Display expiry date
            if (cachedExpiryDate != null && !cachedExpiryDate.isEmpty()) {
                expiryDate.setText(cachedExpiryDate);
            }

            // ✅ Load coach name from prefs
            if (coachCache == null) {
                coachCache = getSharedPreferences("MainActivity_cache", MODE_PRIVATE);
            }
            String savedCoachName = coachCache.getString("cached_coach_name", null);
            if (savedCoachName != null && !savedCoachName.isEmpty()) {
                cachedCoachName = savedCoachName;
                displayCoachName(savedCoachName);
            } else if (cachedCoachName != null) {
                displayCoachName(cachedCoachName);
            }

            // ✅ Display cached workouts
            if (cachedExerciseNames != null && !cachedExerciseNames.isEmpty()) {
                displayYourWorkouts(cachedExerciseNames, cachedExerciseGifs);
            }

            // ✅ Display cached promo or show "No Promo" state
            CardView promoCard = findViewById(R.id.promo_card);
            ImageView testImage = findViewById(R.id.testImage);
            LinearLayout promoLayout = findViewById(R.id.promoLayout);

            if (promoCard != null && promoLayout != null) {
                if (cachedPromoImageUrl != null && !cachedPromoImageUrl.isEmpty()) {
                    promoLayout.removeAllViews();
                    promoLayout.setVisibility(View.GONE);

                    if (testImage != null) {
                        testImage.setVisibility(View.VISIBLE);
                        Glide.with(this).load(cachedPromoImageUrl)
                                .skipMemoryCache(true)
                                .placeholder(R.drawable.no_image_placeholder)
                                .error(R.drawable.no_image_placeholder)
                                .into(testImage);
                    }

                    promoCard.setCardBackgroundColor(getColor(android.R.color.black));

                    promoCard.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, Promo.class);
                        intent.putExtra("promoUrl", cachedPromoImageUrl);
                        startActivity(intent);
                    });
                } else {
                    showNoPromoState(promoCard, testImage, promoLayout);
                }
            }
        }


    }// ← Closing brace ng MainActivity class
