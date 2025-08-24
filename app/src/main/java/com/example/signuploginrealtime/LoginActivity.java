package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    EditText loginEmail, loginPassword;
    Button loginButton;
    TextView signupRedirectText, aboutUsRedirectText;
    ProgressBar progressBar;
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;

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

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

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

            // Show loading state
            showLoading(true);

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Check profile completion status
                                checkProfileCompletionAndNavigate(user.getUid());
                            }
                        } else {
                            // Hide loading state
                            showLoading(false);

                            String errorMessage = "Login failed. Please try again.";

                            // Get specific error message if available
                            if (task.getException() != null) {
                                String error = task.getException().getMessage();
                                if (error != null) {
                                    if (error.contains("user not found") || error.contains("invalid-user-token")) {
                                        errorMessage = "No account found with this email.";
                                    } else if (error.contains("wrong-password") || error.contains("invalid-credential")) {
                                        errorMessage = "Invalid email or password.";
                                    } else if (error.contains("network")) {
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

    private void checkProfileCompletionAndNavigate(String userId) {
        mDatabase.child("users").child(userId).child("profile")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Hide loading state
                        showLoading(false);

                        boolean profileCompleted = false;
                        if (dataSnapshot.exists()) {
                            Boolean completed = dataSnapshot.child("profileCompleted").getValue(Boolean.class);
                            profileCompleted = (completed != null && completed);
                        }

                        if (profileCompleted) {
                            // Profile is complete, go to MainActivity (dashboard)
                            showSuccessDialogAndNavigate("Login Successful! Welcome back!", MainActivity.class);
                        } else {
                            // Profile is incomplete, start from GenderSelection
                            showSuccessDialogAndNavigate("Login Successful! Let's set up your profile.", GenderSelection.class);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Hide loading state
                        showLoading(false);

                        Toast.makeText(LoginActivity.this,
                                "Error checking profile: " + databaseError.getMessage(),
                                Toast.LENGTH_LONG).show();

                        // Default to profile setup if there's an error
                        showSuccessDialogAndNavigate("Login Successful! Let's set up your profile.", GenderSelection.class);
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

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            // Show loading spinner, hide button text, disable button
            progressBar.setVisibility(View.VISIBLE);
            loginButton.setText("");
            loginButton.setEnabled(false);
        } else {
            // Hide loading spinner, show button text, enable button
            progressBar.setVisibility(View.GONE);
            loginButton.setText("Login");
            loginButton.setEnabled(true);
        }
    }
}