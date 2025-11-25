package com.example.signuploginrealtime;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CheckBox;
import android.widget.ScrollView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import android.app.ProgressDialog;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.cardview.widget.CardView;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import java.util.HashMap;
import java.util.Map;


public class Profile extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;
    private TextView profileName, profileEmail, tvPhone, tvDob, tvStatus;
    private LinearLayout layoutDob, layoutEmail, layoutPhone;

    // Fitness profile fields
    private TextView tvFitnessLevel, tvFitnessGoal, tvWorkoutFrequency, tvAge, tvWeight, tvHeight, tvHealthIssues, tvBodyFocus;
    private TextView tvPreferredDays; // NEW
    private LinearLayout layoutFitnessLevel, layoutFitnessGoal, layoutWorkoutFrequency, layoutAge, layoutWeight, layoutHeight, layoutHealthIssues, layoutBodyFocus;
    private LinearLayout layoutPreferredDays; // NEW

    // Firestore references
    private FirebaseFirestore firestore;
    private DocumentReference userDocRef;
    private ListenerRegistration userDataListener;

    // Date picker components
    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;
    private ImageView profilePicture;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ProgressDialog uploadProgressDialog;

    private ActivityResultLauncher<Intent> cropImageLauncher;
    private static final int REQUEST_CROP_IMAGE = 200;

    private static final int MIN_PASSWORD_LENGTH = 8;
    // Email and phone validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-ZaZ]{2,})$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^0\\d{10}$" // Philippine format: 0 followed by 10 digits (e.g., 09123456789)
    );
    private LinearLayout layoutPaymentHistory;
    private static String cachedMembershipStatus = null;
    private static Integer cachedStatusColor = null;
    private static String cachedUserName = null; // ✅ ADD THIS


    // Callback interface for phone check
    interface PhoneCheckCallback {
        void onResult(boolean exists);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // ===== Firebase Setup =====
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();

        // Initialize Firestore reference (but don't setup listener yet)
        if (currentUser != null) {
            userDocRef = firestore.collection("users").document(currentUser.getUid());
        }

        // ===== Initialize UI Components =====
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        tvPhone = findViewById(R.id.tv_phone);
        tvDob = findViewById(R.id.tv_dob);
        tvStatus = findViewById(R.id.tv_status);
        layoutDob = findViewById(R.id.layout_dob);
        layoutEmail = findViewById(R.id.layout_email);
        layoutPhone = findViewById(R.id.layout_phone);

        // ✅ Display cached membership status and name immediately (NO FLICKER)
        SharedPreferences cache = getSharedPreferences("Profile_cache", MODE_PRIVATE);
        String savedStatus = cache.getString("cached_status", null);
        int savedColor = cache.getInt("cached_color", -1);
        String savedName = cache.getString("cached_name", null);

        if (savedStatus != null && tvStatus != null) {
            cachedMembershipStatus = savedStatus;
            tvStatus.setText(savedStatus);

            if (savedColor != -1) {
                cachedStatusColor = savedColor;
                tvStatus.setTextColor(savedColor);
            }
        }

        // ✅ Display cached name
        if (savedName != null && profileName != null) {
            cachedUserName = savedName;
            profileName.setText(savedName);
        }

        // Fitness profile views
        tvFitnessLevel = findViewById(R.id.tv_fitness_level);
        tvFitnessGoal = findViewById(R.id.tv_fitness_goal);
        tvWorkoutFrequency = findViewById(R.id.tv_workout_frequency);
        layoutFitnessLevel = findViewById(R.id.layout_fitness_level);
        layoutFitnessGoal = findViewById(R.id.layout_fitness_goal);
        layoutWorkoutFrequency = findViewById(R.id.layout_workout_frequency);
        profilePicture = findViewById(R.id.iv_profile_picture);
        tvAge = findViewById(R.id.tv_age);
        tvWeight = findViewById(R.id.tv_weight);
        tvHeight = findViewById(R.id.tv_height);
        tvHealthIssues = findViewById(R.id.tv_health_issues);
        tvBodyFocus = findViewById(R.id.tv_body_focus);
        layoutAge = findViewById(R.id.layout_age);
        layoutWeight = findViewById(R.id.layout_weight);
        layoutHeight = findViewById(R.id.layout_height);
        layoutHealthIssues = findViewById(R.id.layout_health_issues);
        layoutBodyFocus = findViewById(R.id.layout_body_focus);
        tvPreferredDays = findViewById(R.id.tv_preferred_days);           // NEW
        layoutPreferredDays = findViewById(R.id.layout_preferred_days);   // NEW

        // ===== Progress Dialog =====
        uploadProgressDialog = new ProgressDialog(this);
        uploadProgressDialog.setTitle("Uploading Image");
        uploadProgressDialog.setMessage("Please wait...");
        uploadProgressDialog.setCancelable(false);

        // ===== Payment History  =====
        layoutPaymentHistory = findViewById(R.id.layout_payment_history);

        // ===== Image Picker =====
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        Intent cropIntent = new Intent(Profile.this, ImageCropActivity.class);
                        cropIntent.putExtra("imageUri", uri.toString());
                        startActivityForResult(cropIntent, REQUEST_CROP_IMAGE);
                    }
                }
        );

        profilePicture.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        findViewById(R.id.cv_edit_button).setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Payment History
        findViewById(R.id.layout_payment_history).setOnClickListener(v -> {
            Intent intent = new Intent(Profile.this, PaymentHistoryActivity.class);
            startActivity(intent);
        });


        // ===== Navigation: Feedback & Contact =====
        LinearLayout layoutFeedback = findViewById(R.id.layout_feedback);
        layoutFeedback.setOnClickListener(v -> {
            startActivity(new Intent(Profile.this, FeedbackActivity.class));
            overridePendingTransition(0, 0);
        });


        LinearLayout layoutAboutUs = findViewById(R.id.layout_contact);
        layoutAboutUs.setOnClickListener(v -> {
            startActivity(new Intent(Profile.this, AboutusActivity.class));
            overridePendingTransition(0, 0);
        });

        // ===== Date Setup =====
        selectedDate = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

        // ===== Setup Functionality =====
        setupDatePicker();
        setupEditableFields();
        setupFitnessProfileEditing();
        setupSecurityClickListeners();
        initCloudinary();

        // ===== Back Press Handler =====
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // ✅ Check if MainActivity is in the back stack
                if (isTaskRoot()) {
                    // No MainActivity in back stack, create new one
                    Intent intent = new Intent(Profile.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
                finish();  // Finish Profile activity
                overridePendingTransition(0, 0);
            }
        });

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.item_2);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.item_1) {
                // Going back to MainActivity
                if (isTaskRoot()) {
                    // No MainActivity in back stack, create new one
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.item_2) {
                // Already on Profile
                return true;
            } else if (itemId == R.id.item_3) {
                // Going to WorkoutList - finish Profile first
                startActivity(new Intent(getApplicationContext(), WorkoutList.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.item_4) {
                // Going to Achievement - finish Profile first
                startActivity(new Intent(getApplicationContext(), Achievement.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });

        // ===== Logout Button =====
        findViewById(R.id.btn_logout).setOnClickListener(v -> showLogoutDialog());

        // ===== TEMPORARY: Seed Foods Button =====
        findViewById(R.id.btn_seed_foods).setOnClickListener(v -> {
            Intent intent = new Intent(Profile.this, FoodSeederActivity.class);
            startActivity(intent);
        });

        // ===== Setup Firestore Listener (AFTER all views are initialized) =====
        if (currentUser != null && userDocRef != null) {
            setupUserDataListener();
        }
    }


    private void setupFitnessProfileEditing() {
        // Fitness Level click listener
        if (layoutFitnessLevel != null) {
            layoutFitnessLevel.setOnClickListener(v -> showFitnessLevelDialog());
        }

        // Fitness Goal click listener
        if (layoutFitnessGoal != null) {
            layoutFitnessGoal.setOnClickListener(v -> showFitnessGoalDialog());
        }

        // Workout Frequency click listener
        if (layoutWorkoutFrequency != null) {
            layoutWorkoutFrequency.setOnClickListener(v -> showWorkoutFrequencyDialog());
        }

        if (layoutAge != null) {
            layoutAge.setOnClickListener(v -> showAgeDialog());
        }
        if (layoutWeight != null) {
            layoutWeight.setOnClickListener(v -> showWeightDialog());
        }
        if (layoutHeight != null) {
            layoutHeight.setOnClickListener(v -> showHeightDialog());
        }
        if (layoutHealthIssues != null) {
            layoutHealthIssues.setOnClickListener(v -> showHealthIssuesDialog());
        }
        if (layoutBodyFocus != null) {
            layoutBodyFocus.setOnClickListener(v -> showBodyFocusDialog());
        }
        if (layoutPreferredDays != null) {
            layoutPreferredDays.setOnClickListener(v -> showPreferredDaysDialog()); // NEW
        }
    }

    private void showFitnessLevelDialog() {
        String[] levels = {"Sedentary", "Lightly Active", "Moderately Active", "Very Active"};
        String[] levelValues = {"sedentary", "lightly active", "moderately active", "very active"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle("Select Fitness Level");

        builder.setItems(levels, (dialog, which) -> {
            String selectedLevel = levels[which];
            String selectedValue = levelValues[which];

            // Show confirmation before updating
            showConfirmationDialog(
                    "Confirm Fitness Level",
                    "Set fitness level to:\n" + selectedLevel + "?",
                    () -> updateFitnessLevel(selectedValue, selectedLevel)
            );
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }

        dialog.show();

        // Style buttons AFTER showing dialog
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }


    private void showFitnessGoalDialog() {
        String[] goals = {"Lose Weight", "Gain Muscle", "Increase Endurance", "General Fitness"};
        String[] goalValues = {"lose weight", "gain muscle", "increase endurance", "general fitness"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle("Select Fitness Goal");

        builder.setItems(goals, (dialog, which) -> {
            String selectedGoal = goals[which];
            String selectedValue = goalValues[which];

            // Show confirmation before updating
            showConfirmationDialog(
                    "Confirm Fitness Goal",
                    "Set fitness goal to:\n" + selectedGoal + "?",
                    () -> updateFitnessGoal(selectedValue, selectedGoal)
            );
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }

        dialog.show();

        // Style buttons AFTER showing dialog
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }


    private void showWorkoutFrequencyDialog() {
        String[] frequencies = {"1 day per week", "2 days per week", "3 days per week",
                "4 days per week", "5 days per week", "6 days per week",
                "7 days per week"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle("Select Workout Frequency");

        builder.setItems(frequencies, (dialog, which) -> {
            int daysPerWeek = which + 1;
            String displayText = frequencies[which];

            // Show confirmation before updating
            showConfirmationDialog(
                    "Confirm Workout Frequency",
                    "Set workout frequency to:\n" + displayText + "?",
                    () -> updateWorkoutFrequency(daysPerWeek, displayText)
            );
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }

        dialog.show();

        // Style buttons AFTER showing dialog
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void updateFitnessLevel(String value, String displayText) {
        if (userDocRef != null) {
            userDocRef.update("fitnessLevel", value)
                    .addOnSuccessListener(aVoid -> {
                        tvFitnessLevel.setText(displayText);
                        markProfileAsChanged();
                        Toast.makeText(this, "Fitness level updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateFitnessGoal(String value, String displayText) {
        if (userDocRef != null) {
            userDocRef.update("fitnessGoal", value)
                    .addOnSuccessListener(aVoid -> {
                        tvFitnessGoal.setText(displayText);
                        markProfileAsChanged();
                        Toast.makeText(this, "Fitness goal updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateWorkoutFrequency(int days, String displayText) {
        if (userDocRef != null) {
            userDocRef.update("workoutDaysPerWeek", days)
                    .addOnSuccessListener(aVoid -> {
                        tvWorkoutFrequency.setText(displayText);
                        markProfileAsChanged();
                        Toast.makeText(this, "Workout frequency updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }
    private void loadFitnessProfileData() {
        if (userDocRef != null) {
            userDocRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot != null && snapshot.exists()) {
                    // Load Fitness Level
                    String fitnessLevel = snapshot.getString("fitnessLevel");
                    if (fitnessLevel != null && !fitnessLevel.isEmpty()) {
                        tvFitnessLevel.setText(formatFitnessLevel(fitnessLevel));
                    } else {
                        tvFitnessLevel.setText("Not set");
                    }

                    // Load Fitness Goal
                    String fitnessGoal = snapshot.getString("fitnessGoal");
                    if (fitnessGoal != null && !fitnessGoal.isEmpty()) {
                        tvFitnessGoal.setText(formatFitnessGoal(fitnessGoal));
                    } else {
                        tvFitnessGoal.setText("Not set");
                    }

                    // Load Workout Frequency
                    Long workoutDays = snapshot.getLong("workoutDaysPerWeek");
                    if (workoutDays != null) {
                        int days = workoutDays.intValue();
                        String frequencyText = days + (days == 1 ? " day per week" : " days per week");
                        tvWorkoutFrequency.setText(frequencyText);
                    } else {
                        tvWorkoutFrequency.setText("Not set");
                    }

                    Long age = snapshot.getLong("age");
                    if (age != null) {
                        tvAge.setText(age + " years old");
                    } else {
                        tvAge.setText("Not set");
                    }

                    Double weight = snapshot.getDouble("weight");
                    if (weight != null) {
                        tvWeight.setText(weight + " kg");
                    } else {
                        tvWeight.setText("Not set");
                    }

                    Double height = snapshot.getDouble("height");
                    if (height != null) {
                        tvHeight.setText(height + " cm");
                    } else {
                        tvHeight.setText("Not set");
                    }

                    // Load Health Issues - with safe type handling
                    if (tvHealthIssues != null) {
                        try {
                            Object healthIssuesObj = snapshot.get("healthIssues");
                            String healthIssues = null;

                            if (healthIssuesObj instanceof String) {
                                healthIssues = (String) healthIssuesObj;
                            } else if (healthIssuesObj != null) {
                                healthIssues = healthIssuesObj.toString();
                            }

                            if (healthIssues != null && !healthIssues.trim().isEmpty()) {
                                tvHealthIssues.setText(healthIssues);
                            } else {
                                tvHealthIssues.setText("None");
                            }
                        } catch (Exception e) {
                            Log.e("Profile", "Error loading health issues", e);
                            tvHealthIssues.setText("None");
                        }
                    }

                    // Load Body Focus
                    if (tvBodyFocus != null) {
                        try {
                            Object bodyFocusObj = snapshot.get("bodyFocus");
                            if (bodyFocusObj instanceof List) {
                                List<String> bodyFocusList = (List<String>) bodyFocusObj;
                                if (!bodyFocusList.isEmpty()) {
                                    tvBodyFocus.setText(String.join(", ", bodyFocusList));
                                } else {
                                    tvBodyFocus.setText("Not set");
                                }
                            } else {
                                tvBodyFocus.setText("Not set");
                            }
                        } catch (Exception e) {
                            Log.e("Profile", "Error loading body focus", e);
                            tvBodyFocus.setText("Not set");
                        }
                    }

                    // NEW: Load preferred workout days
                    if (tvPreferredDays != null) {
                        try {
                            Object field = snapshot.get("preferredWorkoutDays");
                            List<String> codes = new ArrayList<>();
                            if (field instanceof List) {
                                for (Object o : (List<?>) field) {
                                    if (o != null) codes.add(o.toString());
                                }
                            } else if (field instanceof String) {
                                String[] parts = ((String) field).split(",");
                                for (String p : parts) {
                                    String s = p.trim();
                                    if (!s.isEmpty()) codes.add(s);
                                }
                            }
                            tvPreferredDays.setText(formatPreferredDaysForDisplay(codes));
                        } catch (Exception e) {
                            Log.e("Profile", "Error loading preferredWorkoutDays", e);
                            tvPreferredDays.setText("Not set");
                        }
                    }
                }

            });
        }

    }

    private String formatFitnessLevel(String level) {
        // Convert database format to display format
        switch (level.toLowerCase()) {
            case "sedentary":
                return "Sedentary";
            case "lightly active":
                return "Lightly Active";
            case "moderately active":
                return "Moderately Active";
            case "very active":
                return "Very Active";
            default:
                return level;
        }
    }

    private String formatFitnessGoal(String goal) {
        // Convert database format to display format
        switch (goal.toLowerCase()) {
            case "lose weight":
            case "weight loss":
                return "Lose Weight";
            case "gain muscle":
            case "muscle gain":
                return "Gain Muscle";
            case "increase endurance":
            case "endurance":
                return "Increase Endurance";
            case "general fitness":
                return "General Fitness";
            default:
                return goal;
        }
    }

    // NEW: helper to format preferred days list into a nice string
    private String formatPreferredDaysForDisplay(List<String> codes) {
        if (codes == null || codes.isEmpty()) return "Not set";
        // order: Mon..Sun
        String[] order = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        Map<String, String> map = new HashMap<>();
        map.put("Mon", "Mon");
        map.put("Tue", "Tue");
        map.put("Wed", "Wed");
        map.put("Thu", "Thu");
        map.put("Fri", "Fri");
        map.put("Sat", "Sat");
        map.put("Sun", "Sun");
        List<String> ordered = new ArrayList<>();
        for (String o : order) {
            if (codes.contains(o)) ordered.add(map.get(o));
        }
        if (ordered.isEmpty()) return "Not set";
        return android.text.TextUtils.join(", ", ordered);
    }

    private void setupSecurityClickListeners() {
        // Change Password click listener
        LinearLayout layoutChangePassword = findViewById(R.id.layout_change_password);
        layoutChangePassword.setOnClickListener(v -> showChangePasswordDialog());
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle("Change Password");
        builder.setMessage("To change your password, you'll need to verify your current password first.");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        final EditText currentPasswordInput = new EditText(this);
        currentPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        currentPasswordInput.setHint("Current Password");
        layout.addView(currentPasswordInput);

        final EditText newPasswordInput = new EditText(this);
        newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPasswordInput.setHint("New Password (min 8 characters)");
        layout.addView(newPasswordInput);

        // Add password strength indicator
        final TextView strengthIndicator = new TextView(this);
        strengthIndicator.setPadding(0, 8, 0, 0);
        strengthIndicator.setTextSize(12);
        strengthIndicator.setVisibility(View.GONE);
        layout.addView(strengthIndicator);

        // Add password requirements
        final TextView requirementsText = new TextView(this);
        requirementsText.setPadding(0, 8, 0, 0);
        requirementsText.setTextSize(11);
        requirementsText.setVisibility(View.GONE);
        layout.addView(requirementsText);

        final EditText confirmPasswordInput = new EditText(this);
        confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmPasswordInput.setHint("Confirm New Password");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, (int) (8 * getResources().getDisplayMetrics().density), 0, 0);
        confirmPasswordInput.setLayoutParams(params);
        layout.addView(confirmPasswordInput);

        // Add TextWatcher for real-time validation
        newPasswordInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString();
                if (password.isEmpty()) {
                    strengthIndicator.setVisibility(View.GONE);
                    requirementsText.setVisibility(View.GONE);
                } else {
                    strengthIndicator.setVisibility(View.VISIBLE);
                    requirementsText.setVisibility(View.VISIBLE);

                    int strength = calculatePasswordStrength(password);
                    String strengthText = "Password Strength: " + getPasswordStrengthText(strength);
                    int color = getPasswordStrengthColor(strength);

                    strengthIndicator.setText(strengthText);
                    strengthIndicator.setTextColor(color);

                    requirementsText.setText(getPasswordRequirementsText(password));
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        builder.setView(layout);

        builder.setPositiveButton("Change Password", (dialog, which) -> {
            String currentPassword = currentPasswordInput.getText().toString().trim();
            String newPassword = newPasswordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            if (validatePasswordChange(currentPassword, newPassword, confirmPassword)) {
                showConfirmationDialog(
                        "Confirm Password Change",
                        "Are you sure you want to change your password?\n\nYou will need to use the new password for future logins.",
                        () -> changeUserPassword(currentPassword, newPassword)
                );
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }

        dialog.show();

        // Style buttons AFTER showing dialog
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }
    private boolean validatePasswordChange(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword.isEmpty()) {
            Toast.makeText(this, "Please enter your current password", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Please enter a new password", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            Toast.makeText(this, "New password must be at least " + MIN_PASSWORD_LENGTH + " characters", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check for uppercase letter
        if (!newPassword.matches(".*[A-Z].*")) {
            Toast.makeText(this, "Password must contain at least one uppercase letter", Toast.LENGTH_LONG).show();
            return false;
        }

        // Check for lowercase letter
        if (!newPassword.matches(".*[a-z].*")) {
            Toast.makeText(this, "Password must contain at least one lowercase letter", Toast.LENGTH_LONG).show();
            return false;
        }

        // Check for digit
        if (!newPassword.matches(".*\\d.*")) {
            Toast.makeText(this, "Password must contain at least one number", Toast.LENGTH_LONG).show();
            return false;
        }

        // Check for special character
        if (!newPassword.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            Toast.makeText(this, "Password must contain at least one special character (!@#$%^&*...)", Toast.LENGTH_LONG).show();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (currentPassword.equals(newPassword)) {
            Toast.makeText(this, "New password must be different from current password", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }


    private void changeUserPassword(String currentPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Changing password...", Toast.LENGTH_SHORT).show();

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    user.updatePassword(newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> passwordTask) {
                            if (passwordTask.isSuccessful()) {
                                Toast.makeText(Profile.this, "Password changed successfully!", Toast.LENGTH_LONG).show();

                                if (userDocRef != null) {
                                    userDocRef.update("lastPasswordChange", System.currentTimeMillis());
                                }
                            } else {
                                String errorMessage = passwordTask.getException() != null ?
                                        passwordTask.getException().getMessage() : "Failed to change password";
                                Toast.makeText(Profile.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(Profile.this, "Current password is incorrect", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setupEditableFields() {
        layoutEmail.setOnClickListener(v -> showEditEmailDialog());
        layoutPhone.setOnClickListener(v -> showEditPhoneDialog());
    }

    private void showEditEmailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle("Edit Email Address");
        builder.setMessage("To change your email, you'll need to verify your current password first.");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        final EditText newEmailInput = new EditText(this);
        newEmailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        newEmailInput.setText(profileEmail.getText().toString());
        newEmailInput.setSelection(newEmailInput.getText().length());
        newEmailInput.setHint("New Email Address");
        layout.addView(newEmailInput);

        final EditText passwordInput = new EditText(this);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint("Current Password");
        layout.addView(passwordInput);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newEmail = newEmailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (password.isEmpty()) {
                Toast.makeText(this, "Password is required to change email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (validateEmail(newEmail)) {
                showConfirmationDialog(
                        "Confirm Email Change",
                        "Change email to:\n" + newEmail + "?\n\nYou will need to use this email for future logins.",
                        () -> updateEmailWithAuth(newEmail, password)  // ✅ NOW CALLING THE RIGHT METHOD
                );
            } else {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }

        dialog.show();

        // Style buttons AFTER showing dialog
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        newEmailInput.requestFocus();
    }

    private void showEditPhoneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle("Edit Phone Number");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding / 2, padding, padding);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);

        String currentPhone = tvPhone.getText().toString();
        if (!currentPhone.equals("Phone not set")) {
            input.setText(currentPhone);
            input.setSelection(input.getText().length());
        }

        input.setHint("Enter your phone number");

        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newPhone = input.getText().toString().trim();
            if (newPhone.isEmpty()) {
                Toast.makeText(this, "Phone number cannot be empty", Toast.LENGTH_SHORT).show();
            } else if (validatePhone(newPhone)) {
                showConfirmationDialog(
                        "Confirm Phone Change",
                        "Change phone number to:\n" + newPhone + "?",
                        () -> updatePhone(newPhone)
                );
            } else {
                Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }

        dialog.show();

        // CRITICAL: Style buttons AFTER showing dialog
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        input.requestFocus();
    }


    private boolean validateEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        // Remove spaces, dashes, and parentheses
        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)]", "");

        // Check if it matches Philippine format using the constant
        return PHONE_PATTERN.matcher(cleanPhone).matches();  // ✅ Uses the constant
    }


    private void updateEmailWithAuth(String newEmail, String password) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Updating email...", Toast.LENGTH_SHORT).show();

        // Step 1: Re-authenticate the user
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Step 2: Update Firebase Authentication email
                    user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> emailTask) {
                            if (emailTask.isSuccessful()) {
                                // Step 3: Update Firestore email
                                if (userDocRef != null) {
                                    userDocRef.update("email", newEmail)
                                            .addOnSuccessListener(aVoid -> {
                                                profileEmail.setText(newEmail);
                                                markProfileAsChanged();

                                                // ✅ FIXED: Added proper styling to the verification dialog
                                                AlertDialog.Builder builder = new AlertDialog.Builder(Profile.this, R.style.RoundedDialogStyle);
                                                builder.setTitle("⚠️ Email Verification Required");
                                                builder.setMessage(
                                                        "📧 A verification email has been sent to:\n" +
                                                                newEmail +
                                                                "\n\n" +
                                                                "🔐 IMPORTANT STEPS:\n" +
                                                                "1. Check your email inbox/spam\n" +
                                                                "2. Click the verification link\n" +
                                                                "3. After verification, you can login with your new email\n\n" +
                                                                "⚠️ NOTE: You cannot login your old and new email until you verify it."
                                                );
                                                builder.setPositiveButton("I Understand", null);
                                                builder.setCancelable(false);

                                                AlertDialog dialog = builder.create();

                                                // ✅ Apply rounded background
                                                if (dialog.getWindow() != null) {
                                                    dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
                                                }

                                                dialog.show();

                                                // ✅ Style the button AFTER showing
                                                if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                                                            getResources().getColor(android.R.color.holo_green_dark)
                                                    );
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(Profile.this,
                                                        "Failed to update email in database: " + e.getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                            });
                                }
                            } else {
                                String errorMessage = emailTask.getException() != null ?
                                        emailTask.getException().getMessage() : "Failed to update email";

                                if (errorMessage.contains("already in use")) {
                                    Toast.makeText(Profile.this,
                                            "This email is already in use by another account",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(Profile.this, "Error: " + errorMessage,
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    });
                } else {
                    Toast.makeText(Profile.this, "Incorrect password. Please try again.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    private void updatePhone(String newPhone) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e("Profile", "❌ User not authenticated");
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Normalize phone number
        String normalizedPhone = newPhone.replaceAll("[\\s\\-\\(\\)]", "");

        // Debug logs
        Log.d("Profile", "Original phone input: " + newPhone);
        Log.d("Profile", "Normalized phone: " + normalizedPhone);
        Log.d("Profile", "Pattern matches: " + PHONE_PATTERN.matcher(normalizedPhone).matches());

        // Validate format
        if (!PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            Log.e("Profile", "❌ Invalid phone format");
            Toast.makeText(this, "Invalid phone format. Use: 09123456789", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d("Profile", "✅ Phone format valid");

        // Check if this is the same as current phone
        String currentPhone = tvPhone.getText().toString();
        Log.d("Profile", "Current phone display: " + currentPhone);

        if (!currentPhone.equals("Phone not set")) {
            String currentNormalized = currentPhone.replaceAll("[\\s\\-\\(\\)]", "");
            Log.d("Profile", "Current normalized: " + currentNormalized);

            if (normalizedPhone.equals(currentNormalized)) {
                Log.e("Profile", "❌ Same as current phone");
                Toast.makeText(this, "This is already your current phone number", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Log.d("Profile", "✅ Phone is different from current");

        // Show progress
        Log.d("Profile", "📞 Checking if phone exists...");
        Toast.makeText(this, "Updating phone number...", Toast.LENGTH_SHORT).show();

        // Check if phone number already exists (excluding current user)
        checkPhoneNumberExists(normalizedPhone, currentUser.getUid(), exists -> {
            Log.d("Profile", "Phone exists check result: " + exists);

            if (exists) {
                Log.e("Profile", "❌ Phone already registered");
                Toast.makeText(Profile.this,
                        "This phone number is already registered to another account.",
                        Toast.LENGTH_LONG).show();
            } else {
                Log.d("Profile", "✅ Phone is unique, proceeding with update");

                // Phone number is unique, proceed with update
                if (userDocRef != null) {
                    userDocRef.update("phone", normalizedPhone)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Profile", "✅ Phone updated successfully");
                                tvPhone.setText(normalizedPhone);
                                markProfileAsChanged();
                                Toast.makeText(Profile.this,
                                        "Phone number updated successfully",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Profile", "❌ Phone update error", e);
                                Toast.makeText(Profile.this,
                                        "Failed to update: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                } else {
                    Log.e("Profile", "❌ userDocRef is null");
                }
            }
        });
    }
    private void checkPhoneNumberExists(String phone, String currentUserId, PhoneCheckCallback callback) {
        Log.d("Profile", "════════════════════════════════════════");
        Log.d("Profile", "🔍 CHECKING PHONE EXISTENCE");
        Log.d("Profile", "Phone to check: [" + phone + "]");
        Log.d("Profile", "Phone length: " + phone.length());
        Log.d("Profile", "Current user ID: " + currentUserId);
        Log.d("Profile", "════════════════════════════════════════");

        firestore.collection("users")
                .whereEqualTo("phone", phone)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();

                        Log.d("Profile", "📊 Query successful!");
                        Log.d("Profile", "Query result is null? " + (querySnapshot == null));

                        if (querySnapshot != null) {
                            Log.d("Profile", "Is empty? " + querySnapshot.isEmpty());
                            Log.d("Profile", "Document count: " + querySnapshot.size());

                            if (!querySnapshot.isEmpty()) {
                                Log.d("Profile", "📋 Found documents with this phone:");

                                int index = 0;
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    index++;
                                    String docId = doc.getId();
                                    String docPhone = doc.getString("phone");
                                    String docEmail = doc.getString("email");
                                    String docName = doc.getString("fullname");

                                    Log.d("Profile", "--- Document #" + index + " ---");
                                    Log.d("Profile", "  User ID: " + docId);
                                    Log.d("Profile", "  Phone: [" + docPhone + "]");
                                    Log.d("Profile", "  Phone length: " + (docPhone != null ? docPhone.length() : "null"));
                                    Log.d("Profile", "  Email: " + docEmail);
                                    Log.d("Profile", "  Name: " + docName);
                                    Log.d("Profile", "  Is current user? " + docId.equals(currentUserId));
                                    Log.d("Profile", "  Phone matches exactly? " + phone.equals(docPhone));
                                }

                                // Check if phone belongs to another user
                                boolean belongsToOtherUser = false;
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    if (!doc.getId().equals(currentUserId)) {
                                        belongsToOtherUser = true;
                                        Log.e("Profile", "❌ PHONE BELONGS TO ANOTHER USER!");
                                        break;
                                    }
                                }

                                if (!belongsToOtherUser) {
                                    Log.d("Profile", "✅ Phone belongs to current user only");
                                }

                                Log.d("Profile", "Final result: belongsToOtherUser = " + belongsToOtherUser);
                                callback.onResult(belongsToOtherUser);
                            } else {
                                Log.d("Profile", "✅ NO DOCUMENTS FOUND - Phone is available!");
                                callback.onResult(false);
                            }
                        } else {
                            Log.e("Profile", "❌ QuerySnapshot is NULL!");
                            callback.onResult(false);
                        }
                    } else {
                        Exception e = task.getException();
                        Log.e("Profile", "❌ Query FAILED!");
                        Log.e("Profile", "Error: " + (e != null ? e.getMessage() : "Unknown error"));
                        if (e != null) {
                            e.printStackTrace();
                        }

                        Toast.makeText(Profile.this,
                                "Error checking phone number. Please try again.",
                                Toast.LENGTH_SHORT).show();
                        callback.onResult(true);
                    }
                    Log.d("Profile", "════════════════════════════════════════");
                });
    }



    private void setupDatePicker() {
        layoutDob.setOnClickListener(v -> showDatePickerDialog());
    }

    private void showDatePickerDialog() {
        int currentYear = selectedDate.get(Calendar.YEAR);
        int currentMonth = selectedDate.get(Calendar.MONTH);
        int currentDay = selectedDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar tempDate = Calendar.getInstance();
                        tempDate.set(Calendar.YEAR, year);
                        tempDate.set(Calendar.MONTH, month);
                        tempDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        String formattedDate = dateFormat.format(tempDate.getTime());

                        // Show confirmation dialog before updating
                        showConfirmationDialog(
                                "Confirm Date of Birth",
                                "Set date of birth to:\n" + formattedDate + "?",
                                () -> {
                                    selectedDate.set(Calendar.YEAR, year);
                                    selectedDate.set(Calendar.MONTH, month);
                                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                    tvDob.setText(formattedDate);
                                    saveDateOfBirth(formattedDate);
                                    Toast.makeText(Profile.this, "Date of birth updated", Toast.LENGTH_SHORT).show();
                                }
                        );
                    }
                },
                currentYear,
                currentMonth,
                currentDay
        );

        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.YEAR, -13);
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

        Calendar minDate = Calendar.getInstance();
        minDate.set(1900, 0, 1);
        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

        datePickerDialog.show();
    }


    private void saveDateOfBirth(String dateOfBirth) {
        SharedPreferences prefs = getSharedPreferences("user_profile", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("date_of_birth", dateOfBirth);
        editor.putLong("date_of_birth_timestamp", selectedDate.getTimeInMillis());
        editor.apply();

        if (userDocRef != null) {
            // Save both the formatted string and timestamp to Firestore
            // ✅ FIXED: Use "birthdate" instead of "dateOfBirth"
            Map<String, Object> updates = new HashMap<>();
            updates.put("birthdate", dateOfBirth);
            updates.put("birthdateTimestamp", selectedDate.getTimeInMillis());

            userDocRef.update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Profile", "Date of birth saved: " + dateOfBirth);
                        markProfileAsChanged();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Profile", "Failed to save date of birth", e);
                    });
        }
    }

    private void setupUserDataListener() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        userDataListener = userDocRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Toast.makeText(Profile.this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                String name = snapshot.getString("fullname");
                String email = snapshot.getString("email");
                String phone = snapshot.getString("phone");
                String dateOfBirth = snapshot.getString("birthdate");
                String profilePictureUrl = snapshot.getString("profilePictureUrl");
                String userType = snapshot.getString("userType");
                if (userType == null || userType.isEmpty()) {
                    userDocRef.update("userType", "user");
                }

                // ✅ Load membership status separately from memberships collection
                loadMembershipStatus(currentUser.getUid());

                updateProfileDisplay(name, email, phone, dateOfBirth, null, currentUser);
                loadProfilePicture(profilePictureUrl);
            } else {
                createDefaultUserProfile(currentUser);
            }
        });
    }

    private void loadMembershipStatus(String userId) {
        try {
            firestore.collection("memberships")
                    .document(userId)
                    .addSnapshotListener((snapshot, error) -> {
                        try {
                            if (error != null) {
                                Log.e("Profile", "Error loading membership", error);
                                if (tvStatus != null) {
                                    tvStatus.setText("INACTIVE");
                                    updateMembershipStatusColor("Inactive");
                                }
                                return;
                            }

                            if (snapshot != null && snapshot.exists()) {
                                String status = snapshot.getString("membershipStatus");

                                // Check expiration date
                                Timestamp expirationTimestamp = snapshot.getTimestamp("membershipExpirationDate");
                                boolean isExpired = false;

                                if (expirationTimestamp != null) {
                            Date expirationDate = expirationTimestamp.toDate();
                            Date now = new Date();
                            isExpired = now.after(expirationDate);
                        }

                        // Simple: Active or Inactive
                        if ("active".equalsIgnoreCase(status) && !isExpired) {
                            cachedMembershipStatus = "ACTIVE";
                            cachedStatusColor = getResources().getColor(android.R.color.holo_green_dark);

                            // ✅ SAVE TO SHARED PREFERENCES
                            getSharedPreferences("Profile_cache", MODE_PRIVATE)
                                    .edit()
                                    .putString("cached_status", "ACTIVE")
                                    .putInt("cached_color", cachedStatusColor)
                                    .apply();

                            tvStatus.setText("ACTIVE");
                            updateMembershipStatusColor("Active");
                        } else {
                            cachedMembershipStatus = "INACTIVE";
                            cachedStatusColor = getResources().getColor(android.R.color.holo_red_dark);

                            // ✅ SAVE TO SHARED PREFERENCES
                            getSharedPreferences("Profile_cache", MODE_PRIVATE)
                                    .edit()
                                    .putString("cached_status", "INACTIVE")
                                    .putInt("cached_color", cachedStatusColor)
                                    .apply();

                            if (tvStatus != null) {
                                tvStatus.setText("INACTIVE");
                                updateMembershipStatusColor("Inactive");
                            }

                            // Auto-update status in database if expired
                            if (isExpired && "active".equalsIgnoreCase(status)) {
                                firestore.collection("memberships")
                                        .document(userId)
                                        .update("membershipStatus", "inactive");
                            }
                        }
                    } else {
                        // No membership document = Inactive
                        if (tvStatus != null) {
                            tvStatus.setText("INACTIVE");
                            updateMembershipStatusColor("Inactive");
                        }
                    }
                } catch (Exception e) {
                    Log.e("Profile", "Exception in membership listener", e);
                    if (tvStatus != null) {
                        tvStatus.setText("INACTIVE");
                        updateMembershipStatusColor("Inactive");
                    }
                }
            });
        } catch (Exception e) {
            Log.e("Profile", "Exception setting up membership listener", e);
            if (tvStatus != null) {
                tvStatus.setText("INACTIVE");
                updateMembershipStatusColor("Inactive");
            }
        }
    }



    private void updateProfileDisplay(String name, String email, String phone, String dateOfBirth, String membershipStatus, FirebaseUser currentUser) {
        // Update Name
        String displayName;
        if (name != null && !name.isEmpty()) {
            displayName = name;
        } else if (currentUser != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
            displayName = currentUser.getDisplayName();
        } else {
            displayName = "Gym Member";
        }

        if (profileName != null) {
            profileName.setText(displayName);
        }

        // ✅ SAVE TO CACHE
        cachedUserName = displayName;
        getSharedPreferences("Profile_cache", MODE_PRIVATE)
                .edit()
                .putString("cached_name", displayName)
                .apply();

        // Update Email
        if (profileEmail != null) {
            if (email != null && !email.isEmpty()) {
                profileEmail.setText(email);
            } else {
                String currentEmail = currentUser != null ? currentUser.getEmail() : null;
                profileEmail.setText(currentEmail != null ? currentEmail : "No email");
                if (currentEmail != null && userDocRef != null) {
                    userDocRef.update("email", currentEmail);
                }
            }
        }

        // Update Phone
        if (tvPhone != null) {
            if (phone != null && !phone.isEmpty()) {
                tvPhone.setText(phone);
            } else {
                tvPhone.setText("Phone not set");
            }
        }

        // ✅ FIXED: Update Date of Birth - handle both formats with DEBUG LOGS
        Log.d("Profile", "Received dateOfBirth: " + dateOfBirth); // DEBUG

        if (tvDob != null && dateOfBirth != null && !dateOfBirth.isEmpty()) {
            try {
                Date parsedDate = null;

                // Try parsing with display format first (MMMM dd, yyyy)
                try {
                    parsedDate = dateFormat.parse(dateOfBirth);
                    Log.d("Profile", "Parsed with display format successfully"); // DEBUG
                } catch (Exception e) {
                    Log.d("Profile", "Display format failed, trying database format"); // DEBUG
                    // If that fails, try parsing with database format (yyyy-MM-dd)
                    SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    parsedDate = dbFormat.parse(dateOfBirth);

                    // Convert to display format
                    if (parsedDate != null) {
                        dateOfBirth = dateFormat.format(parsedDate);
                        Log.d("Profile", "Converted to display format: " + dateOfBirth); // DEBUG
                    }
                }

                if (parsedDate != null) {
                    selectedDate.setTime(parsedDate);
                    tvDob.setText(dateOfBirth);
                    Log.d("Profile", "Final display: " + dateOfBirth); // DEBUG
                } else {
                    tvDob.setText(dateOfBirth); // Fallback: just show the text
                    Log.d("Profile", "parsedDate is null, showing raw text"); // DEBUG
                }
            } catch (Exception e) {
                Log.e("Profile", "Failed to parse date: " + dateOfBirth, e);
                if (tvDob != null) {
                    tvDob.setText(dateOfBirth); // Fallback: just show the text
                }
            }
        } else {
            Log.d("Profile", "dateOfBirth is null or empty"); // DEBUG
            if (tvDob != null) {
                tvDob.setText("Select your date of birth");
            }
        }

        // Load fitness profile data
        loadFitnessProfileData();
    }


    // Add this method in Profile.java after your update methods
    private void markProfileAsChanged() {
        if (userDocRef != null) {
            userDocRef.update("profileLastModified", System.currentTimeMillis())
                    .addOnSuccessListener(aVoid ->
                            Log.d("Profile", "Profile modification timestamp updated"))
                    .addOnFailureListener(e ->
                            Log.e("Profile", "Failed to update profile timestamp", e));
        }
    }

    private void createDefaultUserProfile(FirebaseUser currentUser) {
        if (currentUser == null) return;

        String email = currentUser.getEmail();
        String fullname = currentUser.getDisplayName();
        if (fullname == null || fullname.isEmpty()) {
            fullname = "Gym Member";
        }

        if (userDocRef != null) {
            UserProfileFirestore profile = new UserProfileFirestore(fullname, email);
            firestore.runTransaction(transaction -> {
                transaction.set(userDocRef, profile);
                transaction.update(userDocRef, "userType", "user");
                return null;
            });
        }

        profileName.setText(fullname);
        profileEmail.setText(email != null ? email : "No email");
        tvPhone.setText("Phone not set");
        tvStatus.setText("INACTIVE");
        updateMembershipStatusColor("Inactive");
        tvDob.setText("Select your date of birth");
        tvFitnessLevel.setText("Not set");
        tvFitnessGoal.setText("Not set");
        tvWorkoutFrequency.setText("Not set");
    }

    private int calculateAge() {
        if (selectedDate == null) return 0;

        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - selectedDate.get(Calendar.YEAR);

        if (today.get(Calendar.DAY_OF_YEAR) < selectedDate.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }

        return age;
    }

    private String getDateOfBirthForAPI() {
        SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return apiFormat.format(selectedDate.getTime());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload fitness profile data when returning from other activities
        loadFitnessProfileData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userDataListener != null) {
            userDataListener.remove();
        }
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to log out?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            mAuth.signOut();
            startActivity(new Intent(Profile.this, LoginActivity.class));
            finish();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        // Apply rounded background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }

        dialog.show();

        // Style the buttons AFTER showing the dialog
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
    }

    private void initCloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dgxwz6qzg");

        try {
            MediaManager.init(this, config);
            Log.d("Cloudinary", "MediaManager initialized successfully");
        } catch (IllegalStateException e) {
            Log.d("Cloudinary", "MediaManager already initialized");
        }
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri == null) {
            Toast.makeText(this, "Invalid image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadProgressDialog.setMessage("Preparing upload...");
        uploadProgressDialog.show();


        MediaManager.get().upload(imageUri)
                .unsigned("profile_uploads")
                .option("folder", "profile_pictures")
                .option("resource_type", "image")
                .constrain(com.cloudinary.android.policy.TimeWindow.immediate())
                .maxFileSize(5 * 1024 * 1024)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d("Cloudinary", "Upload started: " + requestId);
                        runOnUiThread(() ->
                                uploadProgressDialog.setMessage("Uploading image...")
                        );
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        double progress = (bytes * 100.0) / totalBytes;
                        runOnUiThread(() ->
                                uploadProgressDialog.setMessage(
                                        String.format("Uploading: %d%%", (int) progress)
                                )
                        );
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");

                        runOnUiThread(() -> {
                            uploadProgressDialog.dismiss();

                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                saveProfilePictureUrl(imageUrl);
                                Toast.makeText(Profile.this,
                                        "Profile picture updated!",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(Profile.this,
                                        "Upload succeeded but no URL returned",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e("Cloudinary", "Upload error: " + error.getDescription());

                        runOnUiThread(() -> {
                            uploadProgressDialog.dismiss();

                            String errorMsg = "Upload failed";
                            if (error.getDescription() != null) {
                                errorMsg += ": " + error.getDescription();
                            }

                            Toast.makeText(Profile.this, errorMsg, Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.d("Cloudinary", "Upload rescheduled: " + error.getDescription());
                    }
                })
                .dispatch(Profile.this);  // FIXED: Added context parameter here
    }


    private static class UserProfileFirestore {
        public String fullname, email, phone, dateOfBirth, membershipStatus, userType;

        public UserProfileFirestore(String fullname, String email) {
            this.fullname = fullname;
            this.email = email;
            this.phone = "";
            this.dateOfBirth = "";
            this.membershipStatus = "Active Member";
            this.userType = "user";
        }
    }


    private void saveProfilePictureUrl(String imageUrl) {
        if (userDocRef != null) {
            userDocRef.update("profilePictureUrl", imageUrl)
                    .addOnSuccessListener(aVoid -> {
                        loadProfilePicture(imageUrl);
                    });
        }
    }

    private void loadProfilePicture(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(profilePicture);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CROP_IMAGE && resultCode == RESULT_OK && data != null) {
            String croppedUriString = data.getStringExtra("croppedImageUri");
            if (croppedUriString != null) {
                Uri croppedUri = Uri.parse(croppedUriString);
                uploadImageToCloudinary(croppedUri);
            }
        }
    }

    private void showConfirmationDialog(String title, String message, Runnable onConfirm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle(title);
        builder.setMessage(message);

        builder.setPositiveButton("Yes", (dialog, which) -> {
            if (onConfirm != null) {
                onConfirm.run();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        // Apply rounded background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }

        dialog.show();

        // CRITICAL: Style the buttons AFTER showing the dialog (same as logout dialog)
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }
    private int calculatePasswordStrength(String password) {
        int strength = 0;

        if (password.length() >= MIN_PASSWORD_LENGTH) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[a-z].*")) strength++;
        if (password.matches(".*\\d.*")) strength++;
        if (password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) strength++;

        return strength;
    }

    private String getPasswordStrengthText(int strength) {
        switch (strength) {
            case 0:
            case 1:
                return "Weak";
            case 2:
            case 3:
                return "Medium";
            case 4:
                return "Good";
            case 5:
                return "Strong";
            default:
                return "Weak";
        }
    }

    private int getPasswordStrengthColor(int strength) {
        switch (strength) {
            case 0:
            case 1:
                return getResources().getColor(android.R.color.holo_red_dark);
            case 2:
            case 3:
                return getResources().getColor(android.R.color.holo_orange_dark);
            case 4:
                return getResources().getColor(android.R.color.holo_blue_dark);
            case 5:
                return getResources().getColor(android.R.color.holo_green_dark);
            default:
                return getResources().getColor(android.R.color.holo_red_dark);
        }
    }


    private String getPasswordRequirementsText(String password) {
        StringBuilder requirements = new StringBuilder("\nPassword Requirements:\n");

        if (password.length() >= MIN_PASSWORD_LENGTH) {
            requirements.append("✓ At least 8 characters\n");
        } else {
            requirements.append("✗ At least 8 characters\n");
        }

        if (password.matches(".*[A-Z].*")) {
            requirements.append("✓ One uppercase letter\n");
        } else {
            requirements.append("✗ One uppercase letter\n");
        }

        if (password.matches(".*[a-z].*")) {
            requirements.append("✓ One lowercase letter\n");
        } else {
            requirements.append("✗ One lowercase letter\n");
        }

        if (password.matches(".*\\d.*")) {
            requirements.append("✓ One number\n");
        } else {
            requirements.append("✗ One number\n");
        }

        if (password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            requirements.append("✓ One special character");
        } else {
            requirements.append("✗ One special character");
        }

        return requirements.toString();
    }

    private void showAgeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle("Enter Your Age");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding / 2, padding, padding);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Age (13-100)");
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String ageStr = input.getText().toString().trim();
            if (!ageStr.isEmpty()) {
                int age = Integer.parseInt(ageStr);
                if (age >= 13 && age <= 100) {
                    showConfirmationDialog(
                            "Confirm Age",
                            "Set age to: " + age + " years old?",
                            () -> updateAge(age)
                    );
                } else {
                    Toast.makeText(this, "Age must be between 13-100", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }
        dialog.show();

        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void showWeightDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle("Enter Your Weight");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding / 2, padding, padding);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Weight in kg (30-300)");
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String weightStr = input.getText().toString().trim();
            if (!weightStr.isEmpty()) {
                double weight = Double.parseDouble(weightStr);
                if (weight >= 30 && weight <= 300) {
                    showConfirmationDialog(
                            "Confirm Weight",
                            "Set weight to: " + weight + " kg?",
                            () -> updateWeight(weight)
                    );
                } else {
                    Toast.makeText(this, "Weight must be between 30-300 kg", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }
        dialog.show();

        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void showHeightDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle("Enter Your Height");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding / 2, padding, padding);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Height in cm (100-250)");
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String heightStr = input.getText().toString().trim();
            if (!heightStr.isEmpty()) {
                double height = Double.parseDouble(heightStr);
                if (height >= 100 && height <= 250) {
                    showConfirmationDialog(
                            "Confirm Height",
                            "Set height to: " + height + " cm?",
                            () -> updateHeight(height)
                    );
                } else {
                    Toast.makeText(this, "Height must be between 100-250 cm", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }
        dialog.show();

        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void showHealthIssuesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle("Health Issues");

        // Create scrollable container
        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding / 2, padding, padding);

        // Add instruction text
        TextView instructions = new TextView(this);
        instructions.setText("Select common conditions or add custom ones:");
        instructions.setTextSize(14);
        instructions.setTextColor(getResources().getColor(android.R.color.darker_gray));
        instructions.setPadding(0, 0, 0, (int) (12 * getResources().getDisplayMetrics().density));
        container.addView(instructions);

        // Common health issues
        String[] commonIssues = {
            "Knee Injury", "Back Pain", "Shoulder Injury", "Ankle Injury",
            "High Blood Pressure", "Diabetes", "Asthma", "Heart Condition",
            "Arthritis", "Previous Surgery", "Joint Problems", "Neck Pain"
        };

        // Parse current health issues
        String currentHealthIssues = tvHealthIssues.getText().toString();
        List<String> selectedIssues = new ArrayList<>();
        if (!currentHealthIssues.equals("None") && !currentHealthIssues.equals("Not set")) {
            String[] current = currentHealthIssues.split(",");
            for (String issue : current) {
                selectedIssues.add(issue.trim());
            }
        }

        // Create checkboxes
        List<CheckBox> checkBoxes = new ArrayList<>();
        for (String issue : commonIssues) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(issue);
            checkBox.setTextSize(14);
            checkBox.setChecked(selectedIssues.contains(issue));
            checkBoxes.add(checkBox);
            container.addView(checkBox);
        }

        // Add divider
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (int) (1 * getResources().getDisplayMetrics().density)
        ));
        divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        LinearLayout.LayoutParams dividerParams = (LinearLayout.LayoutParams) divider.getLayoutParams();
        dividerParams.setMargins(0, (int) (16 * getResources().getDisplayMetrics().density),
                                  0, (int) (16 * getResources().getDisplayMetrics().density));
        divider.setLayoutParams(dividerParams);
        container.addView(divider);

        // Add "Other" label
        TextView otherLabel = new TextView(this);
        otherLabel.setText("Other (Custom):");
        otherLabel.setTextSize(14);
        otherLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        otherLabel.setPadding(0, 0, 0, (int) (8 * getResources().getDisplayMetrics().density));
        container.addView(otherLabel);

        // Custom input field
        final EditText customInput = new EditText(this);
        customInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        customInput.setHint("Add any other conditions not listed above");
        customInput.setMinLines(2);
        customInput.setMaxLines(3);
        customInput.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        customInput.setBackground(getResources().getDrawable(R.drawable.spinner_background));
        customInput.setPadding((int) (12 * getResources().getDisplayMetrics().density),
                              (int) (12 * getResources().getDisplayMetrics().density),
                              (int) (12 * getResources().getDisplayMetrics().density),
                              (int) (12 * getResources().getDisplayMetrics().density));

        // Check if there are custom issues (not in common list)
        List<String> customIssues = new ArrayList<>();
        for (String issue : selectedIssues) {
            boolean isCommon = false;
            for (String common : commonIssues) {
                if (common.equalsIgnoreCase(issue)) {
                    isCommon = true;
                    break;
                }
            }
            if (!isCommon) {
                customIssues.add(issue);
            }
        }
        if (!customIssues.isEmpty()) {
            customInput.setText(String.join(", ", customIssues));
        }

        container.addView(customInput);

        scrollView.addView(container);
        builder.setView(scrollView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            List<String> finalIssues = new ArrayList<>();

            // Add selected checkboxes
            for (CheckBox checkBox : checkBoxes) {
                if (checkBox.isChecked()) {
                    finalIssues.add(checkBox.getText().toString());
                }
            }

            // Add custom issues
            String customText = customInput.getText().toString().trim();
            if (!customText.isEmpty()) {
                String[] customs = customText.split(",");
                for (String custom : customs) {
                    String trimmed = custom.trim();
                    if (!trimmed.isEmpty()) {
                        finalIssues.add(trimmed);
                    }
                }
            }

            // Combine all issues
            String healthIssues = finalIssues.isEmpty() ? "" : String.join(", ", finalIssues);
            updateHealthIssues(healthIssues);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }
        dialog.show();

        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void showBodyFocusDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle("Body Focus");

        // Create scrollable container
        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding / 2, padding, padding);

        // Add instruction text
        TextView instructions = new TextView(this);
        instructions.setText("Select the body parts you want to focus on:");
        instructions.setTextSize(14);
        instructions.setTextColor(getResources().getColor(android.R.color.darker_gray));
        instructions.setPadding(0, 0, 0, (int) (12 * getResources().getDisplayMetrics().density));
        container.addView(instructions);

        // Body focus options
        String[] bodyParts = {"Chest", "Back", "Shoulders", "Arms", "Legs", "Abs"};

        // Parse current body focus
        String currentBodyFocus = tvBodyFocus.getText().toString();
        List<String> selectedFocus = new ArrayList<>();
        if (!currentBodyFocus.equals("Not set")) {
            String[] current = currentBodyFocus.split(",");
            for (String focus : current) {
                selectedFocus.add(focus.trim());
            }
        }

        // Create checkboxes
        List<CheckBox> checkBoxes = new ArrayList<>();
        for (String part : bodyParts) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(part);
            checkBox.setTextSize(16);
            checkBox.setPadding(0, (int) (8 * getResources().getDisplayMetrics().density),
                               0, (int) (8 * getResources().getDisplayMetrics().density));
            checkBox.setChecked(selectedFocus.contains(part));
            checkBoxes.add(checkBox);
            container.addView(checkBox);
        }

        scrollView.addView(container);
        builder.setView(scrollView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            List<String> finalFocus = new ArrayList<>();

            // Add selected checkboxes
            for (CheckBox checkBox : checkBoxes) {
                if (checkBox.isChecked()) {
                    finalFocus.add(checkBox.getText().toString());
                }
            }

            // Update body focus
            updateBodyFocus(finalFocus);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }
        dialog.show();

        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void updateBodyFocus(List<String> bodyFocus) {
        if (userDocRef != null) {
            userDocRef.update("bodyFocus", bodyFocus)
                    .addOnSuccessListener(aVoid -> {
                        String displayText = bodyFocus.isEmpty() ? "Not set" : String.join(", ", bodyFocus);
                        tvBodyFocus.setText(displayText);
                        markProfileAsChanged();

                        // ✅ DELETE CACHED WORKOUT TO FORCE REGENERATION
                        deleteCachedWorkout();

                        Toast.makeText(this, "Body focus updated. Your next workout will reflect these changes!", Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // ✅ NEW METHOD: Delete cached workout to force regeneration
    private void deleteCachedWorkout() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        // Get user's current week from preferences or default to 1
        SharedPreferences prefs = getSharedPreferences("workout_prefs_" + userId, MODE_PRIVATE);
        int currentWeek = prefs.getInt("current_week", 1);

        // Delete the cached workout document
        firestore.collection("users")
                .document(userId)
                .collection("currentWorkout")
                .document("week_" + currentWeek)
                .delete()
                .addOnSuccessListener(aVoid ->
                    Log.d("Profile", "✅ Cached workout deleted. Will regenerate on next visit."))
                .addOnFailureListener(e ->
                    Log.e("Profile", "Failed to delete cached workout", e));
    }

    // Show preferred workout days selection dialog
    private void showPreferredDaysDialog() {
        // Launch the dedicated activity instead of showing a dialog
        Intent intent = new Intent(this, PreferredWorkoutDaysActivity.class);
        startActivity(intent);
    }


    private void updateAge(int age) {
        if (userDocRef != null) {
            userDocRef.update("age", age)
                    .addOnSuccessListener(aVoid -> {
                        tvAge.setText(age + " years old");
                        markProfileAsChanged();
                        Toast.makeText(this, "Age updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateWeight(double weight) {
        if (userDocRef != null) {
            userDocRef.update("weight", weight)
                    .addOnSuccessListener(aVoid -> {
                        tvWeight.setText(weight + " kg");
                        markProfileAsChanged();
                        Toast.makeText(this, "Weight updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateHeight(double height) {
        if (userDocRef != null) {
            userDocRef.update("height", height)
                    .addOnSuccessListener(aVoid -> {
                        tvHeight.setText(height + " cm");
                        markProfileAsChanged();
                        Toast.makeText(this, "Height updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateHealthIssues(String healthIssues) {
        if (userDocRef != null) {
            // Ensure we're always saving as a String, even if empty
            String valueToSave = (healthIssues != null && !healthIssues.trim().isEmpty())
                ? healthIssues.trim()
                : "";

            userDocRef.update("healthIssues", valueToSave)
                    .addOnSuccessListener(aVoid -> {
                        if (tvHealthIssues != null) {
                            String displayText = valueToSave.isEmpty() ? "None" : valueToSave;
                            tvHealthIssues.setText(displayText);
                        }
                        markProfileAsChanged();
                        Toast.makeText(this, "Health issues updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateMembershipStatusColor(String status) {
        if (status != null) {
            if (status.equalsIgnoreCase("Active Member") || status.equalsIgnoreCase("Active")) {
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else if (status.equalsIgnoreCase("Inactive") || status.equalsIgnoreCase("Inactive Member")) {
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                // Default color for other statuses
                tvStatus.setTextColor(getResources().getColor(android.R.color.white));
            }
        }
    }

}
