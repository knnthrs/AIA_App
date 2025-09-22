package com.example.signuploginrealtime;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

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
            // First check in "users"
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            saveRoleAndProceed("user", MainActivity.class);
                        } else {
                            // Not in users → check in "coaches"
                            db.collection("coaches").document(currentUser.getUid()).get()
                                    .addOnSuccessListener(coachDoc -> {
                                        if (coachDoc.exists()) {
                                            saveRoleAndProceed("coach", coach_clients.class);
                                        } else {
                                            showAccountDeletedDialog();
                                        }
                                    })
                                    .addOnFailureListener(e -> goToLogin());
                        }
                    })
                    .addOnFailureListener(e -> goToLogin());
        } else {
            // Not logged in
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void showAccountDeletedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Account Unavailable")
                .setMessage("Your account is no longer available. Please sign up or log in with another account.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    // Sign out from Firebase just in case
                    mAuth.signOut();

                    // Clear stored role
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    prefs.edit().remove(KEY_ROLE).apply();

                    // Go to login
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();
                })
                .show();
    }

    private void saveRoleAndProceed(String role, Class<?> activity) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_ROLE, role).apply();

        startActivity(new Intent(this, activity));
        finish();
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

}
