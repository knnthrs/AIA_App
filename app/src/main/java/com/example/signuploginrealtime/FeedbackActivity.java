package com.example.signuploginrealtime;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {

    private EditText etFeedback;
    private CheckBox cbFacilities, cbStaff, cbWorkouts, cbPayments, cbGeneral;
    private Button btnSendFeedback;
    private ImageView btnBackFeedback;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        etFeedback = findViewById(R.id.et_feedback);
        cbFacilities = findViewById(R.id.cb_facilities);
        cbStaff = findViewById(R.id.cb_staff);
        cbWorkouts = findViewById(R.id.cb_workouts);
        cbPayments = findViewById(R.id.cb_payments);
        cbGeneral = findViewById(R.id.cb_general);
        btnSendFeedback = findViewById(R.id.btn_send_feedback);
        btnBackFeedback = findViewById(R.id.btn_back_feedback);

        // Set up back button
        btnBackFeedback.setOnClickListener(v -> finish());

        // Set up submit button
        btnSendFeedback.setOnClickListener(v -> submitFeedback());
    }

    private void submitFeedback() {
        String feedbackText = etFeedback.getText().toString().trim();

        // Validate input
        if (feedbackText.isEmpty()) {
            Toast.makeText(this, "Please enter your feedback", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current user
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to submit feedback", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected categories
        List<String> categories = new ArrayList<>();
        if (cbFacilities.isChecked()) categories.add("Facilities & Equipment");
        if (cbStaff.isChecked()) categories.add("Staff & Trainers");
        if (cbWorkouts.isChecked()) categories.add("Workouts");
        if (cbPayments.isChecked()) categories.add("Payments & Membership");
        if (cbGeneral.isChecked()) categories.add("General Suggestions / Others");

        // If no category is selected, default to General
        if (categories.isEmpty()) {
            categories.add("General Suggestions / Others");
            cbGeneral.setChecked(true);
        }

        // Show loading state
        btnSendFeedback.setEnabled(false);
        btnSendFeedback.setText("Sending...");

        // Get user's full name from Firestore
        String userId = currentUser.getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    String name = document.getString("fullname");
                    if (name == null || name.isEmpty()) {
                        name = "Unknown User";
                    }

                    // Create feedback data
                    Map<String, Object> feedbackData = new HashMap<>();
                    feedbackData.put("userId", userId);
                    feedbackData.put("fullname", name);
                    feedbackData.put("message", feedbackText);
                    feedbackData.put("categories", categories);
                    feedbackData.put("timestamp", new Date());

                    // Save to Firestore
                    db.collection("feedback").document() // Auto-generated unique ID
                            .set(feedbackData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(FeedbackActivity.this, "Feedback submitted successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnSendFeedback.setEnabled(true);
                                btnSendFeedback.setText("Send Feedback");
                                Toast.makeText(FeedbackActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    btnSendFeedback.setEnabled(true);
                    btnSendFeedback.setText("Send Feedback");
                    Toast.makeText(FeedbackActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}