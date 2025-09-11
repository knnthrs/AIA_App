package com.example.signuploginrealtime;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore; // Changed import

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    EditText signupFullname, signupEmail, signupPassword, signupConfirmPassword, signupPhone;
    Button signupButton;
    TextView loginRedirectText;
    ProgressBar loadingProgressBar;
    TextView loadingText;
    FirebaseAuth mAuth;
    FirebaseFirestore db; // Changed from FirebaseDatabase and DatabaseReference

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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Changed to Firestore instance

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

            if (!password.equals(confirmPassword)) {
                Toast.makeText(SignupActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading indicator
            showLoading();

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        // Hide loading indicator
                        hideLoading();

                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                // Show loading for database operation
                                showLoading("Saving user data...");

                                String userId = firebaseUser.getUid();
                                HelperClass helperClass = new HelperClass(fullname, email, phone);

                                // Add userType field to Firestore document
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("name", fullname);
                                userData.put("email", email);
                                userData.put("phone", phone);
                                userData.put("userType", "user");

                                db.collection("users").document(userId).set(userData)
                                        .addOnCompleteListener(dbTask -> {
                                            hideLoading();
                                            if (dbTask.isSuccessful()) {
                                                showSuccessDialog();
                                            } else {
                                                Toast.makeText(SignupActivity.this, "Failed to save user data to Firestore", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(SignupActivity.this, "Sign up failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        loginRedirectText.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });
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

        // Disable the signup button to prevent multiple submissions
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

        // Re-enable the signup button
        signupButton.setEnabled(true);
        signupButton.setAlpha(1.0f);
    }

    private void showSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SignupActivity.this);
        builder.setTitle("Registration Successful");
        builder.setMessage("You have successfully registered! You may now log in.");
        builder.setCancelable(false);
        builder.setPositiveButton("OK", (dialog, which) -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
        builder.show();
    }
}

// Make sure LoginActivity.java exists in the same package and is declared as:
// package com.example.signuploginrealtime;
// public class LoginActivity extends AppCompatActivity { ... }
