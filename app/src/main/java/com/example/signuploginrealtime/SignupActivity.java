package com.example.signuploginrealtime;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class SignupActivity extends AppCompatActivity {

    EditText signupFullname, signupEmail, signupPassword, signupConfirmPassword, signupPhone;
    Button signupButton;
    TextView loginRedirectText;
    FirebaseAuth mAuth;
    FirebaseDatabase database;
    DatabaseReference reference;

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

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        reference = database.getReference("users");

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

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                String userId = firebaseUser.getUid();
                                HelperClass helperClass = new HelperClass(fullname, email, phone);
                                reference.child(userId).setValue(helperClass);

                                showSuccessDialog();
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
