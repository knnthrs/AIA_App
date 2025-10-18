package com.example.signuploginrealtime;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.SharedPreferences;
import com.example.signuploginrealtime.UserInfo.GenderSelection;
import com.example.signuploginrealtime.models.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;



public class SignupActivity extends AppCompatActivity {

    EditText signupFullname, signupEmail, signupPassword, signupConfirmPassword, signupPhone;
    Button signupButton;
    TextView loginRedirectText;
    ProgressBar loadingProgressBar;
    TextView loadingText;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    // Password strength indicator views
    LinearLayout passwordStrengthContainer;
    TextView passwordStrengthText;
    View passwordStrengthBar;
    TextView requirementLength, requirementUppercase, requirementLowercase, requirementDigit, requirementSpecial;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final int MIN_PASSWORD_LENGTH = 8;

    private static final Pattern PH_MOBILE_PATTERN = Pattern.compile(
            "^0\\d{10}$" // Philippine format: 0 followed by 10 digits (e.g., 09123456789)
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        signupFullname = findViewById(R.id.signup_fullname);
        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        signupConfirmPassword = findViewById(R.id.signup_confirm_password);
        signupPhone = findViewById(R.id.signup_phone);
        signupButton = findViewById(R.id.signup_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        loadingText = findViewById(R.id.loadingText);
        // Initialize password strength views
        passwordStrengthContainer = findViewById(R.id.password_strength_container);
        passwordStrengthText = findViewById(R.id.password_strength_text);
        passwordStrengthBar = findViewById(R.id.password_strength_bar);
        requirementLength = findViewById(R.id.requirement_length);
        requirementUppercase = findViewById(R.id.requirement_uppercase);
        requirementLowercase = findViewById(R.id.requirement_lowercase);
        requirementDigit = findViewById(R.id.requirement_digit);
        requirementSpecial = findViewById(R.id.requirement_special);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Add TextWatcher for real-time password validation
        signupPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString();
                if (password.isEmpty()) {
                    passwordStrengthContainer.setVisibility(View.GONE);
                } else {
                    passwordStrengthContainer.setVisibility(View.VISIBLE);
                    updatePasswordStrength(password);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        signupButton.setOnClickListener(v -> {
            String fullname = signupFullname.getText().toString().trim();
            String email = signupEmail.getText().toString().trim();
            String password = signupPassword.getText().toString().trim();
            String confirmPassword = signupConfirmPassword.getText().toString().trim();
            String phone = signupPhone.getText().toString().trim();

            if (fullname.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phone.isEmpty()) {
                Toast.makeText(SignupActivity.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidFullName(fullname)) {
                Toast.makeText(SignupActivity.this, "Please enter your full name (first and last name)", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidEmail(email)) {
                Toast.makeText(SignupActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidPhilippineNumber(phone)) {
                Toast.makeText(SignupActivity.this, "Please enter a valid Philippine mobile number (e.g., 09123456789)", Toast.LENGTH_LONG).show();
                return;
            }

            if (!isValidPassword(password)) {
                Toast.makeText(SignupActivity.this, getPasswordErrorMessage(password), Toast.LENGTH_LONG).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(SignupActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if phone number already exists
            showLoading("Checking phone number...");
            String normalizedPhone = normalizePhoneNumber(phone);

            checkPhoneNumberExists(normalizedPhone, exists -> {
                hideLoading();

                if (exists) {
                    Toast.makeText(SignupActivity.this,
                            "This phone number is already registered. Please use a different number or login.",
                            Toast.LENGTH_LONG).show();
                } else {
                    // Phone number is unique, proceed with registration
                    proceedWithSignup(fullname, email, password, normalizedPhone);
                }
            });
        });

        loginRedirectText.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });

    }

    private void checkPhoneNumberExists(String phone, PhoneCheckCallback callback) {
        db.collection("users")
                .whereEqualTo("phone", phone)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        callback.onResult(querySnapshot != null && !querySnapshot.isEmpty());
                    } else {
                        // If check fails, show error and don't proceed
                        Toast.makeText(SignupActivity.this,
                                "Error checking phone number. Please try again.",
                                Toast.LENGTH_SHORT).show();
                        callback.onResult(true); // Treat as exists to prevent signup on error
                    }
                });
    }

    private void proceedWithSignup(String fullname, String email, String password, String normalizedPhone) {
        showLoading("Creating account...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    hideLoading();

                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            showLoading("Saving user data...");

                            String userId = firebaseUser.getUid();

                            Map<String, Object> userData = new HashMap<>();
                            userData.put("fullname", fullname);
                            userData.put("email", email);
                            userData.put("phone", normalizedPhone);
                            userData.put("userType", "user");
                            userData.put("emailVerified", false); // Track verification status

                            db.collection("users").document(userId).set(userData)
                                    .addOnCompleteListener(dbTask -> {
                                        hideLoading();
                                        if (dbTask.isSuccessful()) {
                                            Map<String, Object> initialStats = new HashMap<>();
                                            initialStats.put("totalWorkouts", 0);
                                            initialStats.put("totalMinutes", 0);
                                            initialStats.put("totalCalories", 0);

                                            db.collection("users")
                                                    .document(userId)
                                                    .collection("stats")
                                                    .document("overall")
                                                    .set(initialStats);

                                            SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                                            prefs.edit().putString("role", "user").apply();

                                            // Send email verification
                                            sendEmailVerification(firebaseUser);
                                        } else {
                                            firebaseUser.delete().addOnCompleteListener(deleteTask -> {
                                                Toast.makeText(SignupActivity.this,
                                                        "Failed to save user data to Firestore. Please try again.",
                                                        Toast.LENGTH_LONG).show();
                                            });
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(SignupActivity.this, "Sign up failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Callback interface for phone check
    interface PhoneCheckCallback {
        void onResult(boolean exists);
    }

    private void sendEmailVerification(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showEmailVerificationDialog();
                    } else {
                        Toast.makeText(SignupActivity.this,
                                "Failed to send verification email. Please try again later.",
                                Toast.LENGTH_LONG).show();
                        // Still allow them to proceed, but they'll need to verify later
                        showSuccessDialog();
                    }
                });
    }

    private void showEmailVerificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SignupActivity.this);
        builder.setTitle("Verify Your Email");
        builder.setMessage("A verification email has been sent to your email address. Please check your inbox (and spam/junk folder) and click the verification link.\n\nAfter verifying, come back to login to complete your profile.");
        builder.setCancelable(false);
        builder.setPositiveButton("Go to Login", (dialog, which) -> {
            // Sign out the user so they must login after verification
            mAuth.signOut();
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        builder.setNegativeButton("Resend Email", (dialog, which) -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                sendEmailVerification(user);
            }
        });
        builder.show();
    }

    private boolean isValidFullName(String fullname) {
        if (fullname == null || fullname.trim().isEmpty()) {
            return false;
        }

        String[] nameParts = fullname.trim().split("\\s+");

        if (nameParts.length < 2) {
            return false;
        }

        for (String part : nameParts) {
            if (part.length() < 2 || !part.matches("[a-zA-Z]+")) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isValidPhilippineNumber(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }

        // Remove spaces and dashes
        String cleanedPhone = phone.replaceAll("[\\s-]", "");

        // Check if it matches Philippine format: starts with 0 and has exactly 11 digits
        return cleanedPhone.matches("^0\\d{10}$");
    }

    private String normalizePhoneNumber(String phone) {
        // Just remove spaces and dashes, keep the 0 prefix
        return phone.replaceAll("[\\s-]", "");
    }

    private void showLoading() {
        showLoading("Creating account...");
    }

    private void showLoading(String message) {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.VISIBLE);
        }
        if (loadingText != null) {
            loadingText.setVisibility(View.VISIBLE);
            loadingText.setText(message);
        }

        signupButton.setEnabled(false);
        signupButton.setAlpha(0.5f);
    }

    private void hideLoading() {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.GONE);
        }
        if (loadingText != null) {
            loadingText.setVisibility(View.GONE);
        }

        signupButton.setEnabled(true);
        signupButton.setAlpha(1.0f);
    }

    private void showSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SignupActivity.this);
        builder.setTitle("Registration Successful");
        builder.setMessage("You have successfully registered! Let's complete your profile.");
        builder.setCancelable(false);
        builder.setPositiveButton("OK", (dialog, which) -> {
            UserProfile userProfile = new UserProfile();
            userProfile.setFitnessGoal("general fitness");
            userProfile.setFitnessLevel("beginner");
            userProfile.setGender(null);
            userProfile.setAge(0);
            userProfile.setWeight(0);
            userProfile.setHeight(0);

            Intent intent = new Intent(SignupActivity.this, GenderSelection.class);
            intent.putExtra("userProfile", userProfile);
            startActivity(intent);
            finish();
        });
        builder.show();
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }

        // Check for at least one uppercase letter
        boolean hasUpperCase = password.matches(".*[A-Z].*");

        // Check for at least one lowercase letter
        boolean hasLowerCase = password.matches(".*[a-z].*");

        // Check for at least one digit
        boolean hasDigit = password.matches(".*\\d.*");

        // Check for at least one special character
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");

        return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar;
    }

    private String getPasswordErrorMessage(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one number";
        }
        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            return "Password must contain at least one special character (!@#$%^&*...)";
        }
        return "";
    }

    private void updatePasswordStrength(String password) {
        int strength = calculatePasswordStrength(password);

        // Update requirements checklist
        updateRequirement(requirementLength, password.length() >= MIN_PASSWORD_LENGTH);
        updateRequirement(requirementUppercase, password.matches(".*[A-Z].*"));
        updateRequirement(requirementLowercase, password.matches(".*[a-z].*"));
        updateRequirement(requirementDigit, password.matches(".*\\d.*"));
        updateRequirement(requirementSpecial, password.matches(".*[!@#$%^&*(),.?\":{}|<>].*"));

        // Update strength indicator
        switch (strength) {
            case 0:
            case 1:
                passwordStrengthText.setText("Weak");
                passwordStrengthText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                passwordStrengthBar.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                break;
            case 2:
            case 3:
                passwordStrengthText.setText("Medium");
                passwordStrengthText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
                passwordStrengthBar.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
                break;
            case 4:
                passwordStrengthText.setText("Good");
                passwordStrengthText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
                passwordStrengthBar.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
                break;
            case 5:
                passwordStrengthText.setText("Strong");
                passwordStrengthText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                passwordStrengthBar.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                break;
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

    private void updateRequirement(TextView textView, boolean met) {
        if (met) {
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            String text = textView.getText().toString().replaceFirst("✗", "✓");
            textView.setText(text);
        } else {
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            String text = textView.getText().toString().replaceFirst("✓", "✗");
            textView.setText(text);
        }
    }
}