package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 seconds
    private ProgressBar progressBar;
    private TextView loadingText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize loading components
        progressBar = findViewById(R.id.progressBar);
        loadingText = findViewById(R.id.loadingText);

        // Show loading indicator
        showLoading();

        new Handler().postDelayed(() -> {
            // Hide loading indicator
            hideLoading();

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null) {
                // ✅ User already logged in → go to Home
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
            } else {
                // ❌ Not logged in → go to Signup/Login
                Intent intent = new Intent(SplashActivity.this, SignupActivity.class);
                startActivity(intent);
            }

            finish(); // close splash
        }, SPLASH_DELAY);
    }

    private void showLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }
        if (loadingText != null) {
            loadingText.setVisibility(TextView.VISIBLE);
            loadingText.setText("Loading...");
        }
    }

    private void hideLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(ProgressBar.GONE);
        }
        if (loadingText != null) {
            loadingText.setVisibility(TextView.GONE);
        }
    }
}