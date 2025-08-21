package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Profile extends AppCompatActivity {

    private ImageView btnBack;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;
    private TextView profileName, profileEmail, tvMemberId, tvPhone, tvDob, tvStatus;
    private DatabaseReference userRef;
    private ValueEventListener userDataListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Initialize TextViews based on your existing XML layout
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        tvMemberId = findViewById(R.id.tv_member_id);
        tvPhone = findViewById(R.id.tv_phone);
        tvDob = findViewById(R.id.tv_dob);
        tvStatus = findViewById(R.id.tv_status);

        if (currentUser != null) {
            // Set up database reference
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

            // Set up real-time listener for user data
            setupUserDataListener();
        }

        // Handle back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(Profile.this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
        });

        btnBack = findViewById(R.id.btn_back);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(Profile.this, MainActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        bottomNavigationView.setSelectedItemId(R.id.item_2);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.item_1) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.item_3) {
                startActivity(new Intent(getApplicationContext(), WorkoutList.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.item_4) {
                // Navigate to Achievements activity
                startActivity(new Intent(getApplicationContext(), Achievement.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.item_2) {
                return true;
            }
            return false;
        });

        // Logout button
        findViewById(R.id.btn_logout).setOnClickListener(v -> showLogoutDialog());
    }

    private void setupUserDataListener() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        userDataListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get user data from Firebase
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String memberId = snapshot.child("memberId").getValue(String.class);
                    String dateOfBirth = snapshot.child("dateOfBirth").getValue(String.class);
                    String membershipStatus = snapshot.child("membershipStatus").getValue(String.class);

                    // Update UI with real-time data
                    updateProfileDisplay(name, email, phone, memberId, dateOfBirth, membershipStatus, currentUser);
                } else {
                    // If no data exists, create default profile
                    createDefaultUserProfile(currentUser);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error - maybe show toast or log error
            }
        };

        // Attach the listener for real-time updates
        userRef.addValueEventListener(userDataListener);
    }

    private void updateProfileDisplay(String name, String email, String phone, String memberId, String dateOfBirth, String membershipStatus, FirebaseUser currentUser) {
        // Update Name
        if (name != null && !name.isEmpty()) {
            profileName.setText(name);
        } else {
            String defaultName = getDefaultName(currentUser.getEmail());
            profileName.setText(defaultName);
            // Save default name to Firebase
            userRef.child("name").setValue(defaultName);
        }

        // Update Email
        if (email != null && !email.isEmpty()) {
            profileEmail.setText(email);
        } else {
            String currentEmail = currentUser.getEmail();
            profileEmail.setText(currentEmail != null ? currentEmail : "No email");
            // Save email to Firebase
            if (currentEmail != null) {
                userRef.child("email").setValue(currentEmail);
            }
        }

        // Update Phone
        if (phone != null && !phone.isEmpty()) {
            tvPhone.setText(phone);
        } else {
            tvPhone.setText("Phone not set");
        }

        // Update Member ID
        if (memberId != null && !memberId.isEmpty()) {
            tvMemberId.setText("Member ID: #" + memberId);
        } else {
            // Generate member ID if not exists
            String generatedMemberId = generateMemberId();
            tvMemberId.setText("Member ID: #" + generatedMemberId);
            // Save generated member ID to Firebase
            userRef.child("memberId").setValue(generatedMemberId);
        }

        // Update Date of Birth
        if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
            tvDob.setText(dateOfBirth);
        } else {
            tvDob.setText("Not set");
        }

        // Update Membership Status
        if (membershipStatus != null && !membershipStatus.isEmpty()) {
            tvStatus.setText(membershipStatus.toUpperCase());
        } else {
            tvStatus.setText("ACTIVE MEMBER");
            // Save default status
            userRef.child("membershipStatus").setValue("Active Member");
        }
    }

    private void createDefaultUserProfile(FirebaseUser currentUser) {
        if (currentUser == null) return;

        String email = currentUser.getEmail();
        String defaultName = getDefaultName(email);
        String memberId = generateMemberId();

        // Create user profile in Firebase
        userRef.child("name").setValue(defaultName);
        userRef.child("email").setValue(email);
        userRef.child("memberId").setValue(memberId);
        userRef.child("phone").setValue(""); // Empty phone initially
        userRef.child("dateOfBirth").setValue(""); // Empty DOB initially
        userRef.child("membershipStatus").setValue("Active Member");

        // Update UI
        profileName.setText(defaultName);
        profileEmail.setText(email != null ? email : "No email");
        tvPhone.setText("Phone not set");
        tvMemberId.setText("Member ID: #" + memberId);
        tvStatus.setText("ACTIVE MEMBER");
        tvDob.setText("Not set");
    }

    private String getDefaultName(String email) {
        if (email != null && email.contains("@")) {
            return email.split("@")[0];
        }
        return "Gym Member";
    }

    private String generateMemberId() {
        // Generate unique member ID - you can customize this format
        long timestamp = System.currentTimeMillis();
        return "GYM" + String.valueOf(timestamp).substring(7); // GYM + last 6 digits
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the listener to prevent memory leaks
        if (userRef != null && userDataListener != null) {
            userRef.removeEventListener(userDataListener);
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    startActivity(new Intent(Profile.this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}