package com.example.signuploginrealtime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

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
    private static final String PREFS_NAME = "LoginPrefs";
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

            if (email.isEmpty()) {
                loginEmail.setError("Email is required");
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
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            String errorMessage = "Login failed. Please try again.";

                            // Get specific error message if available
                            if (task.getException() != null) {
                                String error = task.getException().getMessage();
                                if (error != null) {
                                    if (error.contains("user not found") || error.contains("invalid-user-token") || error.contains("INVALID_LOGIN_CREDENTIALS") || error.contains("ERROR_USER_NOT_FOUND")) {
                                        errorMessage = "No account found with this email.";
                                    } else if (error.contains("wrong-password") || error.contains("invalid-credential") || error.contains("INVALID_LOGIN_CREDENTIALS") || error.contains("ERROR_WRONG_PASSWORD")) {
                                        errorMessage = "Invalid email or password.";
                                    } else if (error.contains("network") || error.contains("NETWORK_ERROR")) {
                                        errorMessage = "Network error. Please check your connection.";
                                    }
                                }
                            }

                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
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
            // Check coaches collection
            mDatabase.collection("coaches").document(userId).get()
                    .addOnCompleteListener(task -> {
                        showLoading(false, true);
                        if (task.isSuccessful()) {
                            DocumentSnapshot coachDoc = task.getResult();
                            if (coachDoc != null && coachDoc.exists()) {
                                String userType = coachDoc.getString("userType");
                                if ("coach".equals(userType)) {
                                    // ✅ Save coach login state
                                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                                    prefs.edit()
                                            .putBoolean(KEY_COACH_LOGGED_IN, true)
                                            .putString(KEY_UID, userId)
                                            .putString(KEY_ROLE, "coach")
                                            .apply();

                                    Toast.makeText(LoginActivity.this, "Welcome Coach! Redirecting to dashboard.", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(LoginActivity.this, coach_clients.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(LoginActivity.this, "This account is not registered as a coach.", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, "No coach account found with this email. Please select 'Login as User' if you have a user account.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e(TAG, "Error checking coach status: ", task.getException());
                            Toast.makeText(LoginActivity.this, "Error verifying account type. Please try again.", Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            // Check users collection
            mDatabase.collection("users").document(userId).get()
                    .addOnCompleteListener(task -> {
                        showLoading(false, false);
                        if (task.isSuccessful()) {
                            DocumentSnapshot userDoc = task.getResult();
                            if (userDoc != null && userDoc.exists()) {
                                String userType = userDoc.getString("userType");
                                if ("user".equals(userType)) {
                                    // ✅ Save user login state
                                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                                    prefs.edit()
                                            .putBoolean(KEY_COACH_LOGGED_IN, false)
                                            .putString(KEY_UID, userId)
                                            .putString(KEY_ROLE, "user")
                                            .apply();

                                    Toast.makeText(LoginActivity.this, "Login Successful! Welcome back!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(LoginActivity.this, "This account is not registered as a user.", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, "No user account found with this email. Please select 'Login as Coach' if you have a coach account.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e(TAG, "Error checking user status: ", task.getException());
                            Toast.makeText(LoginActivity.this, "Error verifying account type. Please try again.", Toast.LENGTH_LONG).show();
                        }
                    });
        }
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
