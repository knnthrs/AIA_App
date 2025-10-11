package com.example.signuploginrealtime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.google.firebase.firestore.QuerySnapshot;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Paint;
import com.example.signuploginrealtime.UserInfo.GenderSelection;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
// Firestore imports
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    EditText loginEmail, loginPassword;
    Button loginButton;
    TextView signupRedirectText, aboutUsRedirectText;
    ProgressBar progressBar;
    RadioGroup userTypeGroup;
    RadioButton radioUser, radioCoach;
    FirebaseAuth mAuth;
    FirebaseFirestore mDatabase;

    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_COACH_LOGGED_IN = "isCoachLoggedIn";
    private static final String KEY_UID = "LOGGED_IN_UID";
    private static final String KEY_ROLE = "LOGGED_IN_ROLE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginEmail = findViewById(R.id.login_username); // now used for email
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        signupRedirectText = findViewById(R.id.signupRedirectText);
        aboutUsRedirectText = findViewById(R.id.AboutusRedirectText);
        progressBar = findViewById(R.id.progressBar);

        TextView forgotPasswordText = findViewById(R.id.forgotPasswordText);

        forgotPasswordText.setPaintFlags(
                forgotPasswordText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        // Initialize new radio button components
        userTypeGroup = findViewById(R.id.userTypeGroup);
        radioUser = findViewById(R.id.radioUser);
        radioCoach = findViewById(R.id.radioCoach);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseFirestore.getInstance();

        forgotPasswordText.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();

            if (email.isEmpty()) {
                loginEmail.setError("Please enter your email first");
                loginEmail.requestFocus();
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Reset link sent to your email.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String role = prefs.getString(KEY_ROLE, "");
        String uid = prefs.getString(KEY_UID, null);

// If Firebase still has a logged-in user AND SharedPreferences has role/uid → skip login screen
        if (FirebaseAuth.getInstance().getCurrentUser() != null && uid != null && !role.isEmpty()) {
            if ("coach".equals(role)) {
                Intent intent = new Intent(this, coach_clients.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return; // stop running the rest of onCreate
            } else if ("user".equals(role)) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }
        }



        // Optional: Update button text based on selection
        userTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioCoach) {
                loginButton.setText("Login as Coach");
                signupRedirectText.setVisibility(View.GONE); // hide sign up text for coach
            } else {
                loginButton.setText("Login as User");
                signupRedirectText.setVisibility(View.VISIBLE); // show sign up text for user
            }
        });

        loginButton.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();

            // Clear previous errors
            loginEmail.setError(null);
            loginPassword.setError(null);

            if (email.isEmpty()) {
                loginEmail.setError("Email is required");
                loginEmail.requestFocus();
                return;
            }

            // Validate email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                loginEmail.setError("Please enter a valid email address");
                loginEmail.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                loginPassword.setError("Password is required");
                loginPassword.requestFocus();
                return;
            }

            // Check if coach or user is selected
            boolean isCoach = userTypeGroup.getCheckedRadioButtonId() == R.id.radioCoach;

            // Show loading state
            showLoading(true, isCoach);

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Always validate user type after successful login
                                validateUserTypeAndNavigate(user.getUid(), isCoach);
                            }
                        } else {
                            // Hide loading state
                            showLoading(false, isCoach);

                            // Get the exception to determine what went wrong
                            Exception exception = task.getException();
                            if (exception != null) {
                                String errorMessage = exception.getMessage();
                                Log.w(TAG, "signInWithEmail:failure", exception);

                                // Parse specific error codes
                                if (errorMessage != null) {
                                    if (errorMessage.contains("There is no user record") ||
                                            errorMessage.contains("user-not-found") ||
                                            errorMessage.contains("ERROR_USER_NOT_FOUND")) {
                                        // Email doesn't exist in the system
                                        loginEmail.setError("No account found with this email");
                                        loginEmail.requestFocus();
                                        Toast.makeText(LoginActivity.this,
                                                "Email not registered. Please sign up first.",
                                                Toast.LENGTH_LONG).show();

                                    } else if (errorMessage.contains("password is invalid") ||
                                            errorMessage.contains("wrong-password") ||
                                            errorMessage.contains("INVALID_LOGIN_CREDENTIALS") ||
                                            errorMessage.contains("invalid-credential") ||
                                            errorMessage.contains("ERROR_WRONG_PASSWORD")) {
                                        // Password is incorrect
                                        loginPassword.setError("Incorrect password");
                                        loginPassword.requestFocus();
                                        Toast.makeText(LoginActivity.this,
                                                "Wrong password. Please try again.",
                                                Toast.LENGTH_LONG).show();

                                    } else if (errorMessage.contains("network") ||
                                            errorMessage.contains("NETWORK_ERROR")) {
                                        // Network error
                                        Toast.makeText(LoginActivity.this,
                                                "Network error. Please check your internet connection.",
                                                Toast.LENGTH_LONG).show();

                                    } else if (errorMessage.contains("too-many-requests")) {
                                        // Too many failed attempts
                                        Toast.makeText(LoginActivity.this,
                                                "Too many failed attempts. Please try again later.",
                                                Toast.LENGTH_LONG).show();

                                    } else if (errorMessage.contains("user-disabled")) {
                                        // Account disabled
                                        Toast.makeText(LoginActivity.this,
                                                "This account has been disabled. Please contact support.",
                                                Toast.LENGTH_LONG).show();

                                    } else {
                                        // Generic INVALID_LOGIN_CREDENTIALS error
                                        // This is tricky - Firebase doesn't tell us which is wrong for security
                                        // But we can check if the email exists first
                                        checkEmailAndShowError(email, loginEmail, loginPassword);
                                    }
                                } else {
                                    // Unknown error
                                    Toast.makeText(LoginActivity.this,
                                            "Login failed. Please try again.",
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    });
        });


        signupRedirectText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        aboutUsRedirectText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, AboutusActivity.class);
            startActivity(intent);
        });
    }

    private void validateUserTypeAndNavigate(String userId, boolean isCoach) {
        if (isCoach) {
            mDatabase.collection("coaches").document(userId).get()
                    .addOnCompleteListener(task -> {
                        showLoading(false, true);
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            String userType = task.getResult().getString("userType");
                            if ("coach".equals(userType)) {
                                saveRoleAndProceed("coach", coach_clients.class);
                            } else {
                                // Show specific error with AlertDialog
                                new AlertDialog.Builder(LoginActivity.this)
                                        .setTitle("Account Type Mismatch")
                                        .setMessage("This email is registered as a regular user, not a coach. Please select 'User' and try again.")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        } else {
                            // No coach record found
                            new AlertDialog.Builder(LoginActivity.this)
                                    .setTitle("Coach Account Not Found")
                                    .setMessage("This email is not registered as a coach. Please check your account type or contact support.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    });
        } else {
            mDatabase.collection("users").document(userId).get()
                    .addOnCompleteListener(task -> {
                        showLoading(false, false);
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            String userType = task.getResult().getString("userType");
                            if ("user".equals(userType)) {
                                saveRoleAndProceed("user", MainActivity.class);
                            } else {
                                // Show specific error with AlertDialog
                                new AlertDialog.Builder(LoginActivity.this)
                                        .setTitle("Account Type Mismatch")
                                        .setMessage("This email is registered as a coach, not a regular user. Please select 'Coach' and try again.")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        } else {
                            // No user record found
                            new AlertDialog.Builder(LoginActivity.this)
                                    .setTitle("User Account Not Found")
                                    .setMessage("This email is not registered as a user. Please check your account type or sign up first.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    });
        }
    }

    // Check if email exists in Firestore (both users and coaches collections)
// Check if email exists in Firestore (both users and coaches collections)
    private void checkEmailAndShowError(String email, EditText emailField, EditText passwordField) {
        Log.d(TAG, "=== STARTING EMAIL CHECK ===");
        Log.d(TAG, "Searching for email: " + email);

        // First check in users collection
        mDatabase.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(userTask -> {
                    Log.d(TAG, "Users query completed");
                    Log.d(TAG, "Query successful: " + userTask.isSuccessful());

                    if (userTask.isSuccessful()) {
                        Log.d(TAG, "Number of documents found in users: " + userTask.getResult().size());

                        if (userTask.getResult() != null && !userTask.getResult().isEmpty()) {
                            // Email exists in users collection, so password must be wrong
                            Log.d(TAG, "✓ Email FOUND in users collection - showing wrong password");
                            passwordField.setError("Incorrect password");
                            passwordField.requestFocus();
                            Toast.makeText(LoginActivity.this,
                                    "Wrong password. Please try again.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            // Not in users, check coaches collection
                            Log.d(TAG, "Email NOT in users, checking coaches collection...");
                            mDatabase.collection("coaches")
                                    .whereEqualTo("email", email)
                                    .get()
                                    .addOnCompleteListener(coachTask -> {
                                        Log.d(TAG, "Coaches query completed");
                                        Log.d(TAG, "Number of documents found in coaches: " + coachTask.getResult().size());

                                        if (coachTask.isSuccessful() && coachTask.getResult() != null && !coachTask.getResult().isEmpty()) {
                                            // Email exists in coaches collection, so password must be wrong
                                            Log.d(TAG, "✓ Email FOUND in coaches collection - showing wrong password");
                                            passwordField.setError("Incorrect password");
                                            passwordField.requestFocus();
                                            Toast.makeText(LoginActivity.this,
                                                    "Wrong password. Please try again.",
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            // Email doesn't exist in either collection
                                            Log.d(TAG, "✗ Email NOT FOUND in either collection - showing email not found");
                                            emailField.setError("No account found with this email");
                                            emailField.requestFocus();
                                            Toast.makeText(LoginActivity.this,
                                                    "Email not registered. Please sign up first.",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    } else {
                        Log.e(TAG, "Query FAILED: " + userTask.getException().getMessage());
                    }
                });
    }
    private void showSuccessDialogAndNavigate(String message, Class<?> targetActivity) {
        new AlertDialog.Builder(LoginActivity.this)
                .setTitle("Success")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(LoginActivity.this, targetActivity);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void saveRoleAndProceed(String role, Class<?> targetActivity) {
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        prefs.edit()
                .putString("role", role)
                .apply();

        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(LoginActivity.this, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    private void showLoading(boolean isLoading, boolean isCoach) {
        if (isLoading) {
            // Show loading spinner, hide button text, disable button
            progressBar.setVisibility(View.VISIBLE);
            loginButton.setText("");
            loginButton.setEnabled(false);
        } else {
            // Hide loading spinner, show button text, enable button
            progressBar.setVisibility(View.GONE);
            String buttonText = isCoach ? "Login as Coach" : "Login as User";
            loginButton.setText(buttonText);
            loginButton.setEnabled(true);
        }
    }
}
