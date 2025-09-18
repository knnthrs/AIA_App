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
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String role = prefs.getString(KEY_ROLE, "");

        if (currentUser != null) {
            // User is logged in, check if Firestore document exists
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Account exists → route based on role
                            if ("coach".equals(role)) {
                                startActivity(new Intent(SplashActivity.this, coach_clients.class));
                            } else {
                                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                            }
                            finish();
                        } else {
                            // Account deleted → show dialog then go to login
                            showAccountDeletedDialog();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Firestore read failed → fallback to login
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                        finish();
                    });
        } else {
            // Not logged in → go to signup/login
            startActivity(new Intent(SplashActivity.this, SignupActivity.class));
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
}
