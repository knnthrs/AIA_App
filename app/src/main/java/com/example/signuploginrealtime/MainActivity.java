package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    TextView greetingText;
    TextView membershipStatus;
    TextView planType;
    TextView expiryDate;
    FloatingActionButton fab;
    FirebaseAuth mAuth;
    DatabaseReference userRef;
    BottomNavigationView bottomNavigationView;
    ValueEventListener userDataListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        fab = findViewById(R.id.fab);
        greetingText = findViewById(R.id.greeting_text);
        membershipStatus = findViewById(R.id.membershipStatus);
        planType = findViewById(R.id.planType);
        expiryDate = findViewById(R.id.expiryDate);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        mAuth = FirebaseAuth.getInstance();

        // Membership card click → go to selection screen
        findViewById(R.id.membershipCard).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SelectMembership.class);
            startActivity(intent);
        });

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

            // Set up real-time listener to get user data
            userDataListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Update greeting
                        updateGreeting(snapshot);

                        // Update membership display
                        updateMembershipDisplay(snapshot);
                    } else {
                        setDefaultValues();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    setDefaultValues();
                }
            };

            // Attach listener for real-time updates
            userRef.addValueEventListener(userDataListener);
        } else {
            setDefaultValues();
        }
    }

    private void updateGreeting(DataSnapshot snapshot) {
        String name = null;

        // Try to get name from different possible fields
        if (snapshot.child("name").exists()) {
            name = snapshot.child("name").getValue(String.class);
        } else if (snapshot.child("fullname").exists()) {
            name = snapshot.child("fullname").getValue(String.class);
        }

        // Display the name or fallback to User
        if (name != null && !name.trim().isEmpty()) {
            greetingText.setText("Hi, " + name);
        } else {
            greetingText.setText("Hi, User");
        }
    }

    private void updateMembershipDisplay(DataSnapshot snapshot) {
        // Check for membership data
        String planCode = null;
        String planLabel = null;

        if (snapshot.child("membershipPlanCode").exists()) {
            planCode = snapshot.child("membershipPlanCode").getValue(String.class);
        }

        if (snapshot.child("membershipPlanLabel").exists()) {
            planLabel = snapshot.child("membershipPlanLabel").getValue(String.class);
        }

        if (planCode != null && planLabel != null) {
            // User has selected a membership
            membershipStatus.setText("ACTIVE");
            membershipStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

            // Set plan type - extract just the duration part
            String planDisplay = extractPlanName(planLabel);
            planType.setText(planDisplay);

            // Calculate and set expiry date based on plan
            String expiryDateStr = calculateExpiryDate(planCode);
            expiryDate.setText(expiryDateStr);

        } else {
            // No membership selected
            setDefaultMembershipValues();
        }
    }

    private String extractPlanName(String planLabel) {
        // Extract plan name from full label
        // Example: "1 Month — ₱1,500\nFull gym access • All equipment • Locker room"
        // We want: "1 Month"
        if (planLabel != null) {
            if (planLabel.contains(" — ")) {
                return planLabel.split(" — ")[0];
            } else if (planLabel.contains("\n")) {
                return planLabel.split("\n")[0];
            }
            return planLabel;
        }
        return "Unknown Plan";
    }

    private String calculateExpiryDate(String planCode) {
        Calendar calendar = Calendar.getInstance();

        // Add months based on plan code
        if (planCode.contains("1M") && !planCode.contains("12M")) {
            calendar.add(Calendar.MONTH, 1);
        } else if (planCode.contains("3M")) {
            calendar.add(Calendar.MONTH, 3);
        } else if (planCode.contains("6M")) {
            calendar.add(Calendar.MONTH, 6);
        } else if (planCode.contains("12M")) {
            calendar.add(Calendar.MONTH, 12);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    private void setDefaultMembershipValues() {
        membershipStatus.setText("INACTIVE");
        membershipStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        planType.setText("No plan selected");
        expiryDate.setText("—");
    }

    private void setDefaultValues() {
        greetingText.setText("Hi, User");
        setDefaultMembershipValues();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from SelectMembership activity
        if (mAuth.getCurrentUser() != null) {
            loadUserData();
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