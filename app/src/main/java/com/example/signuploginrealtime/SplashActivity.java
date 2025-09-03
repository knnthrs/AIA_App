package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 800; // 0.8 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null) {
                // ✅ User already logged in → go to Home
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                // ❌ Not logged in → go to Signup/Login
                startActivity(new Intent(SplashActivity.this, SignupActivity.class));
            }

            finish(); // close splash
        }, SPLASH_DELAY);
    }
}
