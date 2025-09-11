package com.example.signuploginrealtime;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
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
    private TextView profileName, profileEmail, tvMemberId, tvPhone, tvDob, tvStatus;
    private LinearLayout layoutDob, layoutEmail, layoutPhone;

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
        tvMemberId = findViewById(R.id.tv_member_id);
        tvPhone = findViewById(R.id.tv_phone);
        tvDob = findViewById(R.id.tv_dob);
        tvStatus = findViewById(R.id.tv_status);
        layoutDob = findViewById(R.id.layout_dob);
        layoutEmail = findViewById(R.id.layout_email);
        layoutPhone = findViewById(R.id.layout_phone);

        // Initialize date components
        selectedDate = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

        // Set up functionality
        setupDatePicker();
        setupEditableFields();
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
                // Navigate to Achievements activity
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

        // Show loading toast
        Toast.makeText(this, "Changing password...", Toast.LENGTH_SHORT).show();

        // Re-authenticate user with current password
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Re-authentication successful, now change password
                    user.updatePassword(newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> passwordTask) {
                            if (passwordTask.isSuccessful()) {
                                Toast.makeText(Profile.this, "Password changed successfully!", Toast.LENGTH_LONG).show();

                                // Save password change timestamp to Firestore
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
                    // Re-authentication failed
                    Toast.makeText(Profile.this, "Current password is incorrect", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setupEditableFields() {
        // Set up email editing
        layoutEmail.setOnClickListener(v -> showEditEmailDialog());

        // Set up phone editing
        layoutPhone.setOnClickListener(v -> showEditPhoneDialog());
    }

    private void showEditEmailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Email Address");

        // Create EditText
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setText(profileEmail.getText().toString());
        input.setSelection(input.getText().length()); // Place cursor at end

        // Set padding
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

        // Focus on input and show keyboard
        input.requestFocus();
    }

    private void showEditPhoneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Phone Number");

        // Create EditText
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);

        String currentPhone = tvPhone.getText().toString();
        if (!currentPhone.equals("Phone not set")) {
            input.setText(currentPhone);
            input.setSelection(input.getText().length()); // Place cursor at end
        }

        input.setHint("Enter your phone number");

        // Set padding
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

        // Focus on input and show keyboard
        input.requestFocus();
    }

    private boolean validateEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        // Remove spaces, dashes, and parentheses for validation
        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)]", "");

        // Check if it\'s a valid format (at least 10 digits, can start with +)
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
                        // Update the selected date
                        selectedDate.set(Calendar.YEAR, year);
                        selectedDate.set(Calendar.MONTH, month);
                        selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        // Format and display the selected date
                        String formattedDate = dateFormat.format(selectedDate.getTime());
                        tvDob.setText(formattedDate);

                        // Save the date to Firebase and SharedPreferences
                        saveDateOfBirth(formattedDate);

                        // Show confirmation toast
                        Toast.makeText(Profile.this, "Date of birth updated", Toast.LENGTH_SHORT).show();
                    }
                },
                currentYear,
                currentMonth,
                currentDay
        );

        // Set date constraints
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.YEAR, -13); // Minimum age of 13
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

        Calendar minDate = Calendar.getInstance();
        minDate.set(1900, 0, 1); // Minimum year 1900
        datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

        datePickerDialog.show();
    }

    private void saveDateOfBirth(String dateOfBirth) {
        // Save to SharedPreferences
        SharedPreferences prefs = getSharedPreferences("user_profile", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("date_of_birth", dateOfBirth);
        editor.putLong("date_of_birth_timestamp", selectedDate.getTimeInMillis());
        editor.apply();

        // Save to Firestore
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
                // Get user data from Firestore
                String name = snapshot.getString("name");
                String email = snapshot.getString("email");
                String phone = snapshot.getString("phone");
                String memberId = snapshot.getString("memberId");
                String dateOfBirth = snapshot.getString("dateOfBirth");
                String membershipStatus = snapshot.getString("membershipStatus");

                // --- Add this block to upgrade old users ---
                String userType = snapshot.getString("userType");
                if (userType == null || userType.isEmpty()) {
                    // Automatically add userType: "user" for old users
                    userDocRef.update("userType", "user");
                }
                // --- end upgrade block ---

                updateProfileDisplay(name, email, phone, memberId, dateOfBirth, membershipStatus, currentUser);
            } else {
                // If no data exists, create default profile
                createDefaultUserProfile(currentUser);
            }
        });
    }

    private void updateProfileDisplay(String name, String email, String phone, String memberId, String dateOfBirth, String membershipStatus, FirebaseUser currentUser) {
        // Update Name
        if (name != null && !name.isEmpty()) {
            profileName.setText(name);
        } else {
            String defaultName = getDefaultName(currentUser.getEmail());
            profileName.setText(defaultName);
            if (userDocRef != null) userDocRef.update("name", defaultName);
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

        // Update Member ID
        if (memberId != null && !memberId.isEmpty()) {
            tvMemberId.setText("Member ID: #" + memberId);
        } else {
            String generatedMemberId = generateMemberId();
            tvMemberId.setText("Member ID: #" + generatedMemberId);
            if (userDocRef != null) userDocRef.update("memberId", generatedMemberId);
        }

        // Update Date of Birth - prioritize Firebase data
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
    }

    private void createDefaultUserProfile(FirebaseUser currentUser) {
        if (currentUser == null) return;

        String email = currentUser.getEmail();
        String defaultName = getDefaultName(email);
        String memberId = generateMemberId();

        // Create user profile in Firestore with userType field
        if (userDocRef != null) {
            // Add userType: "user" to the Firestore document
            UserProfileFirestore profile = new UserProfileFirestore(defaultName, email, memberId);
            firestore.runTransaction(transaction -> {
                transaction.set(userDocRef, profile);
                transaction.update(userDocRef, "userType", "user");
                return null;
            });
        }

        profileName.setText(defaultName);
        profileEmail.setText(email != null ? email : "No email");
        tvPhone.setText("Phone not set");
        tvMemberId.setText("Member ID: #" + memberId);
        tvStatus.setText("ACTIVE MEMBER");
        tvDob.setText("Select your date of birth");
    }

    private String getDefaultName(String email) {
        if (email != null && email.contains("@")) {
            return email.split("@")[0];
        }
        return "Gym Member";
    }

    private String generateMemberId() {
        // Generate unique member ID - you can customize this format
        long timestamp = System.currentTimeMillis();
        return "GYM" + String.valueOf(timestamp).substring(7); // GYM + last 6 digits
    }

    // Helper method to calculate age from date of birth
    private int calculateAge() {
        if (selectedDate == null) return 0;

        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - selectedDate.get(Calendar.YEAR);

        // Check if birthday has occurred this year
        if (today.get(Calendar.DAY_OF_YEAR) < selectedDate.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }

        return age;
    }

    // Method to get formatted date for API calls or database storage
    private String getDateOfBirthForAPI() {
        SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return apiFormat.format(selectedDate.getTime());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firestore listener
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

    // Helper Firestore user profile class
    private static class UserProfileFirestore {
        public String name, email, memberId, phone, dateOfBirth, membershipStatus, userType;
        public UserProfileFirestore(String name, String email, String memberId) {
            this.name = name;
            this.email = email;
            this.memberId = memberId;
            this.phone = "";
            this.dateOfBirth = "";
            this.membershipStatus = "Active Member";
            this.userType = "user"; // Always set userType for new users
        }
    }
}
