package com.example.signuploginrealtime;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
import java.util.Locale;
import java.util.regex.Pattern;


public class Profile extends AppCompatActivity {

    private ImageView btnBack;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;
    private TextView profileName, profileEmail, tvPhone, tvDob, tvStatus;
    private LinearLayout layoutDob, layoutEmail, layoutPhone;

    // Fitness profile fields
    private TextView tvFitnessLevel, tvFitnessGoal, tvWorkoutFrequency;
    private LinearLayout layoutFitnessLevel, layoutFitnessGoal, layoutWorkoutFrequency;

    // Firestore references
    private FirebaseFirestore firestore;
    private DocumentReference userDocRef;
    private ListenerRegistration userDataListener;

    // Date picker components
    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;

    // Email and phone validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[1-9]\\d{1,14}$" // International phone format
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Initialize TextViews and LinearLayouts based on your existing XML layout
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        tvPhone = findViewById(R.id.tv_phone);
        tvDob = findViewById(R.id.tv_dob);
        tvStatus = findViewById(R.id.tv_status);
        layoutDob = findViewById(R.id.layout_dob);
        layoutEmail = findViewById(R.id.layout_email);
        layoutPhone = findViewById(R.id.layout_phone);

        // Initialize fitness profile views
        tvFitnessLevel = findViewById(R.id.tv_fitness_level);
        tvFitnessGoal = findViewById(R.id.tv_fitness_goal);
        tvWorkoutFrequency = findViewById(R.id.tv_workout_frequency);
        layoutFitnessLevel = findViewById(R.id.layout_fitness_level);
        layoutFitnessGoal = findViewById(R.id.layout_fitness_goal);
        layoutWorkoutFrequency = findViewById(R.id.layout_workout_frequency);

        LinearLayout layoutFeedback = findViewById(R.id.layout_feedback);
        layoutFeedback.setOnClickListener(v -> {
            startActivity(new Intent(Profile.this, FeedbackActivity.class));
        });

        // Initialize date components
        selectedDate = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

        // Set up functionality
        setupDatePicker();
        setupEditableFields();
        setupFitnessProfileEditing();
        setupSecurityClickListeners();

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        if (currentUser != null) {
            // Set up Firestore document reference
            userDocRef = firestore.collection("users").document(currentUser.getUid());

            // Set up real-time listener for user data
            setupUserDataListener();
        }

        // Handle back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(Profile.this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
        });

        btnBack = findViewById(R.id.btn_back);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(Profile.this, MainActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        bottomNavigationView.setSelectedItemId(R.id.item_2);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.item_1) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
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
            } else if (itemId == R.id.item_2) {
                return true;
            }
            return false;
        });

        // Logout button
        findViewById(R.id.btn_logout).setOnClickListener(v -> showLogoutDialog());
    }

    private void setupFitnessProfileEditing() {
        // Fitness Level click listener
        layoutFitnessLevel.setOnClickListener(v -> showFitnessLevelDialog());

        // Fitness Goal click listener
        layoutFitnessGoal.setOnClickListener(v -> showFitnessGoalDialog());

        // Workout Frequency click listener
        layoutWorkoutFrequency.setOnClickListener(v -> showWorkoutFrequencyDialog());
    }

    private void showFitnessLevelDialog() {
        // Match exactly with AdvancedWorkoutDecisionMaker cases
        String[] levels = {"Sedentary", "Lightly Active", "Moderately Active", "Very Active"};
        String[] levelValues = {"sedentary", "lightly active", "moderately active", "very active"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Fitness Level");

        builder.setItems(levels, (dialog, which) -> {
            String selectedLevel = levels[which];
            String selectedValue = levelValues[which];
            updateFitnessLevel(selectedValue, selectedLevel);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showFitnessGoalDialog() {
        // Match exactly with AdvancedWorkoutDecisionMaker cases
        String[] goals = {"Lose Weight", "Gain Muscle", "Increase Endurance", "General Fitness"};
        String[] goalValues = {"lose weight", "gain muscle", "increase endurance", "general fitness"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Fitness Goal");

        builder.setItems(goals, (dialog, which) -> {
            String selectedGoal = goals[which];
            String selectedValue = goalValues[which];
            updateFitnessGoal(selectedValue, selectedGoal);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showWorkoutFrequencyDialog() {
        String[] frequencies = {"1 day per week", "2 days per week", "3 days per week",
                "4 days per week", "5 days per week", "6 days per week",
                "7 days per week"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Workout Frequency");

        builder.setItems(frequencies, (dialog, which) -> {
            int daysPerWeek = which + 1;
            String displayText = frequencies[which];
            updateWorkoutFrequency(daysPerWeek, displayText);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void updateFitnessLevel(String value, String displayText) {
        if (userDocRef != null) {
            userDocRef.update("fitnessLevel", value)
                    .addOnSuccessListener(aVoid -> {
                        tvFitnessLevel.setText(displayText);
                        markProfileAsChanged(); // ✅ ADD THIS LINE
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
                        markProfileAsChanged(); // ✅ ADD THIS LINE
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
                        markProfileAsChanged(); // ✅ ADD THIS LINE
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

    private void setupSecurityClickListeners() {
        // Change Password click listener
        LinearLayout layoutChangePassword = findViewById(R.id.layout_change_password);
        layoutChangePassword.setOnClickListener(v -> showChangePasswordDialog());
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");
        builder.setMessage("To change your password, you\'ll need to verify your current password first.");

        // Create a LinearLayout to hold multiple EditText views
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        // Current password field
        final EditText currentPasswordInput = new EditText(this);
        currentPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        currentPasswordInput.setHint("Current Password");
        layout.addView(currentPasswordInput);

        // New password field
        final EditText newPasswordInput = new EditText(this);
        newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPasswordInput.setHint("New Password (min 6 characters)");
        layout.addView(newPasswordInput);

        // Confirm new password field
        final EditText confirmPasswordInput = new EditText(this);
        confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmPasswordInput.setHint("Confirm New Password");
        layout.addView(confirmPasswordInput);

        builder.setView(layout);

        builder.setPositiveButton("Change Password", (dialog, which) -> {
            String currentPassword = currentPasswordInput.getText().toString().trim();
            String newPassword = newPasswordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            if (validatePasswordChange(currentPassword, newPassword, confirmPassword)) {
                changeUserPassword(currentPassword, newPassword);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
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

        if (newPassword.length() < 6) {
            Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords don\'t match", Toast.LENGTH_SHORT).show();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Email Address");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setText(profileEmail.getText().toString());
        input.setSelection(input.getText().length());

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newEmail = input.getText().toString().trim();
            if (validateEmail(newEmail)) {
                updateEmail(newEmail);
            } else {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
        input.requestFocus();
    }

    private void showEditPhoneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Phone Number");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);

        String currentPhone = tvPhone.getText().toString();
        if (!currentPhone.equals("Phone not set")) {
            input.setText(currentPhone);
            input.setSelection(input.getText().length());
        }

        input.setHint("Enter your phone number");

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newPhone = input.getText().toString().trim();
            if (newPhone.isEmpty()) {
                Toast.makeText(this, "Phone number cannot be empty", Toast.LENGTH_SHORT).show();
            } else if (validatePhone(newPhone)) {
                updatePhone(newPhone);
            } else {
                Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
        input.requestFocus();
    }

    private boolean validateEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)]", "");

        return cleanPhone.length() >= 10 &&
                (cleanPhone.matches("^\\+?[1-9]\\d{9,14}$") ||
                        cleanPhone.matches("^[0-9]{10,15}$"));
    }

    private void updateEmail(String newEmail) {
        if (userDocRef != null) {
            userDocRef.update("email", newEmail)
                    .addOnSuccessListener(aVoid -> {
                        profileEmail.setText(newEmail);
                        Toast.makeText(this, "Email updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update email: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updatePhone(String newPhone) {
        if (userDocRef != null) {
            userDocRef.update("phone", newPhone)
                    .addOnSuccessListener(aVoid -> {
                        tvPhone.setText(newPhone);
                        Toast.makeText(this, "Phone number updated successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update phone: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
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
                        selectedDate.set(Calendar.YEAR, year);
                        selectedDate.set(Calendar.MONTH, month);
                        selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        String formattedDate = dateFormat.format(selectedDate.getTime());
                        tvDob.setText(formattedDate);

                        saveDateOfBirth(formattedDate);

                        Toast.makeText(Profile.this, "Date of birth updated", Toast.LENGTH_SHORT).show();
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
            userDocRef.update("dateOfBirth", dateOfBirth);
        }
    }

    private void loadDateOfBirthFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("user_profile", MODE_PRIVATE);
        String savedDate = prefs.getString("date_of_birth", "");
        long savedTimestamp = prefs.getLong("date_of_birth_timestamp", -1);

        if (!savedDate.isEmpty()) {
            tvDob.setText(savedDate);
            if (savedTimestamp != -1) {
                selectedDate.setTimeInMillis(savedTimestamp);
            }
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
                String dateOfBirth = snapshot.getString("dateOfBirth");
                String membershipStatus = snapshot.getString("membershipStatus");

                String userType = snapshot.getString("userType");
                if (userType == null || userType.isEmpty()) {
                    userDocRef.update("userType", "user");
                }

                updateProfileDisplay(name, email, phone, dateOfBirth, membershipStatus, currentUser);
            } else {
                createDefaultUserProfile(currentUser);
            }
        });
    }

    private void updateProfileDisplay(String name, String email, String phone, String dateOfBirth, String membershipStatus, FirebaseUser currentUser) {
        // Update Name
        if (name != null && !name.isEmpty()) {
            profileName.setText(name);
        } else if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
            profileName.setText(currentUser.getDisplayName());
        } else {
            profileName.setText("Gym Member");
        }

        // Update Email
        if (email != null && !email.isEmpty()) {
            profileEmail.setText(email);
        } else {
            String currentEmail = currentUser.getEmail();
            profileEmail.setText(currentEmail != null ? currentEmail : "No email");
            if (currentEmail != null && userDocRef != null) userDocRef.update("email", currentEmail);
        }

        // Update Phone
        if (phone != null && !phone.isEmpty()) {
            tvPhone.setText(phone);
        } else {
            tvPhone.setText("Phone not set");
        }

        // Update Date of Birth
        if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
            tvDob.setText(dateOfBirth);
            try {
                selectedDate.setTime(dateFormat.parse(dateOfBirth));
            } catch (Exception e) {}
        } else {
            loadDateOfBirthFromPrefs();
            if (tvDob.getText().toString().equals("Not set")) {
                tvDob.setText("Select your date of birth");
            }
        }

        // Update Membership Status
        if (membershipStatus != null && !membershipStatus.isEmpty()) {
            tvStatus.setText(membershipStatus.toUpperCase());
        } else {
            tvStatus.setText("ACTIVE MEMBER");
            if (userDocRef != null) userDocRef.update("membershipStatus", "Active Member");
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
        tvStatus.setText("ACTIVE MEMBER");
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
    protected void onDestroy() {
        super.onDestroy();
        if (userDataListener != null) {
            userDataListener.remove();
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    startActivity(new Intent(Profile.this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
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
}