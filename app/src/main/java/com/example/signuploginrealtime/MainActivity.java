package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class MainActivity extends AppCompatActivity {

    TextView greetingText;
    FloatingActionButton fab;
    FirebaseAuth mAuth;
    DatabaseReference userRef;
    BottomNavigationView bottomNavigationView;
    ValueEventListener userDataListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button payNowButton = findViewById(R.id.pay_now_button);
        payNowButton.setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Payment feature coming soon!", android.widget.Toast.LENGTH_SHORT).show();
        });

        fab = findViewById(R.id.fab);
        greetingText = findViewById(R.id.greeting_text);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        mAuth = FirebaseAuth.getInstance();

        // Notification Bell icon click listener
        ImageView bellIcon = findViewById(R.id.bell_icon);
        if (bellIcon != null) {
            bellIcon.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, Notification.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        // FAB click listener
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, QR.class);
            startActivity(intent);
        });

        // Setup bottom navigation
        bottomNavigationView.setSelectedItemId(R.id.item_1);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.item_1) {
                return true;
            } else if (itemId == R.id.item_2) {
                startActivity(new Intent(getApplicationContext(), Profile.class));
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
            }
            return false;
        });

        // Badge for Profile
        BadgeDrawable badgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.item_2);

        // Load user data with real-time updates
        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

            // Set up real-time listener to get user name
            userDataListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Try to get name from different possible fields
                        String name = null;

                        // Try "name" field first
                        if (snapshot.child("name").exists()) {
                            name = snapshot.child("name").getValue(String.class);
                        }
                        // Try "fullname" field
                        else if (snapshot.child("fullname").exists()) {
                            name = snapshot.child("fullname").getValue(String.class);
                        }

                        // Display the name or fallback to User
                        if (name != null && !name.trim().isEmpty()) {
                            greetingText.setText("Hi, " + name);
                        } else {
                            greetingText.setText("Hi, User");
                        }
                    } else {
                        greetingText.setText("Hi, User");
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    greetingText.setText("Hi, User");
                }
            };

            // Attach listener for real-time updates
            userRef.addValueEventListener(userDataListener);
        } else {
            greetingText.setText("Hi, User");
        }
    }

    @Override
    public void onBackPressed() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Log out?")
                .setMessage("Do you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    logoutUser();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void logoutUser() {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();

        // Go back to LoginActivity and clear the back stack
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firebase listener to prevent memory leaks
        if (userRef != null && userDataListener != null) {
            userRef.removeEventListener(userDataListener);
        }
    }
}