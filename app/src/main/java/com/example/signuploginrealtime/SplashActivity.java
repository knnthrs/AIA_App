package com.example.signuploginrealtime;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 800; // 0.8 seconds
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_ROLE = "role";
    private static final String KEY_UID = "uid";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        new Handler().postDelayed(this::checkUser, SPLASH_DELAY);
    }

    private void checkUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            Log.d("SplashCheck", "Firebase user found: " + currentUser.getEmail());

            // Check SharedPreferences first for saved role
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String savedRole = prefs.getString(KEY_ROLE, "");
            String savedUID = prefs.getString(KEY_UID, "");

            if (!savedRole.isEmpty() && !savedUID.isEmpty()) {
                Log.d("SplashCheck", "Found saved role: " + savedRole);
                // Use saved role to navigate directly
                if ("coach".equals(savedRole)) {
                    startActivity(new Intent(this, coach_clients.class));
                    finish();
                    return;
                } else if ("user".equals(savedRole)) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                    return;
                }
            }

            // If no saved role, validate from Firestore
            validateUserFromFirestore(currentUser);
        } else {
            Log.d("SplashCheck", "No Firebase user, going to login");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void validateUserFromFirestore(FirebaseUser currentUser) {
        // First check in "users" collection using UID
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        Log.d("SplashCheck", "User document found");
                        saveRoleAndProceed("user", MainActivity.class);
                    } else {
                        Log.d("SplashCheck", "User document not found, checking coaches by email");
                        // Not in users → check in "coaches" by EMAIL (not UID)
                        db.collection("coaches")
                                .whereEqualTo("email", currentUser.getEmail())
                                .get()
                                .addOnSuccessListener(coachQuery -> {
                                    if (!coachQuery.isEmpty()) {
                                        Log.d("SplashCheck", "Coach document found by email");
                                        // Check if it's actually a coach
                                        String userType = coachQuery.getDocuments().get(0).getString("userType");
                                        if ("coach".equals(userType)) {
                                            saveRoleAndProceed("coach", coach_clients.class);
                                        } else {
                                            Log.e("SplashCheck", "Document found but userType is not coach");
                                            showAccountDeletedDialog();
                                        }
                                    } else {
                                        Log.e("SplashCheck", "No coach document found by email");
                                        showAccountDeletedDialog();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("SplashCheck", "Coach query failed: " + e.getMessage());
                                    goToLogin();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SplashCheck", "User query failed: " + e.getMessage());
                    goToLogin();
                });
    }

    private void showAccountDeletedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Account Unavailable")
                .setMessage("Your account is no longer available. Please sign up or log in with another account.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    // Sign out from Firebase just in case
                    mAuth.signOut();

                    // Clear stored role and uid
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    prefs.edit()
                            .remove(KEY_ROLE)
                            .remove(KEY_UID)
                            .apply();

                    // Go to login
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();
                })
                .show();
    }

    private void saveRoleAndProceed(String role, Class<?> activity) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_ROLE, role)
                .putString(KEY_UID, mAuth.getCurrentUser().getUid())
                .apply();

        Log.d("SplashCheck", "Saved role: " + role + ", proceeding to: " + activity.getSimpleName());
        startActivity(new Intent(this, activity));
        finish();
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}