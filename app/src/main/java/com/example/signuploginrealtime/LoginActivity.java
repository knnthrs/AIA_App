package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    EditText loginEmail, loginPassword;
    Button loginButton;
    TextView signupRedirectText, aboutUsRedirectText;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginEmail = findViewById(R.id.login_username); // now used for email
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        signupRedirectText = findViewById(R.id.signupRedirectText);
        aboutUsRedirectText = findViewById(R.id.AboutusRedirectText);

        mAuth = FirebaseAuth.getInstance();

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

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            // Show login success dialog
                            new AlertDialog.Builder(LoginActivity.this)
                                    .setTitle("Success")
                                    .setMessage("Login Successful! Let's set up your profile.")
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        // Navigate to GenderSelection to start profile setup
                                        Intent intent = new Intent(LoginActivity.this, GenderSelection.class);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .show();

                        } else {
                            Toast.makeText(LoginActivity.this, "Invalid credentials. Try again.", Toast.LENGTH_SHORT).show();
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
}