package com.example.signuploginrealtime;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScheduleSelectionActivity extends AppCompatActivity {

    private static final String TAG = "ScheduleSelection";

    private TextView coachNameText;
    private TextView sessionsInfoText;
    private LinearLayout scheduleContainer;
    private CardView confirmScheduleButton;
    private ProgressBar loadingProgress;
    private View backButton;
    private LinearLayout headerLayout;

    private FirebaseFirestore db;
    private String coachId;
    private String coachName;
    private int sessions;
    private String selectedDate;
    private String selectedTime;
    private CardView currentlySelectedCard;

    // Data to pass back
    private String packageId;
    private String planType;
    private int months;
    private int durationDays;
    private double price;

    // Flag to determine if rescheduling
    private boolean isRescheduling;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_selection);

        Log.d(TAG, "ScheduleSelectionActivity onCreate called");

        // Enable edge-to-edge display
        getWindow().setDecorFitsSystemWindows(false);

        db = FirebaseFirestore.getInstance();

        // Get data from intent
        Intent intent = getIntent();
        coachId = intent.getStringExtra("coachId");
        coachName = intent.getStringExtra("coachName");
        sessions = intent.getIntExtra("sessions", 0);
        packageId = intent.getStringExtra("packageId");
        planType = intent.getStringExtra("planType");
        months = intent.getIntExtra("months", 0);
        durationDays = intent.getIntExtra("durationDays", 0);
        price = intent.getDoubleExtra("price", 0);

        // Determine if this is rescheduling (called from MainActivity) or new booking
        isRescheduling = (packageId == null || packageId.isEmpty());

        Log.d(TAG, "Intent extras - coachId: " + coachId + ", coachName: " + coachName + ", sessions: " + sessions + ", isRescheduling: " + isRescheduling);

        initializeViews();
        setupWindowInsets();
        setupListeners();

        // Check if we have the required data
        if (coachId == null || coachId.isEmpty()) {
            Log.e(TAG, "ERROR: coachId is null or empty!");
            Toast.makeText(this, "Error: Coach ID not provided", Toast.LENGTH_LONG).show();
            // Generate UI anyway with no booked slots
            generateScheduleUI(new ArrayList<>());
        } else {
            loadCoachSchedule();
        }
    }

    private void initializeViews() {
        coachNameText = findViewById(R.id.coach_name_text);
        sessionsInfoText = findViewById(R.id.sessions_info_text);
        scheduleContainer = findViewById(R.id.schedule_container);
        confirmScheduleButton = findViewById(R.id.confirm_schedule_button);
        loadingProgress = findViewById(R.id.loading_progress);
        backButton = findViewById(R.id.back_button);
        headerLayout = findViewById(R.id.header_layout);

        coachNameText.setText(coachName);
        sessionsInfoText.setText(sessions + " PT Sessions");
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;

            // Apply top padding to header to push it below the status bar/notch
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) headerLayout.getLayoutParams();
            params.topMargin = topInset;
            headerLayout.setLayoutParams(params);

            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        confirmScheduleButton.setOnClickListener(v -> {
            if (selectedDate == null || selectedTime == null) {
                Toast.makeText(this, "Please select a schedule", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isRescheduling) {
                // For rescheduling, save directly without payment
                saveRescheduledBooking();
            } else {
                // For new booking, proceed to payment
                proceedToPayment();
            }
        });
    }

    private void loadCoachSchedule() {
        Log.d(TAG, "Loading schedule for coach: " + coachId);
        loadingProgress.setVisibility(View.VISIBLE);

        // Get coach's existing bookings
        db.collection("schedules")
                .whereEqualTo("coachId", coachId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Successfully loaded schedules. Count: " + querySnapshot.size());
                    List<String> bookedSlots = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String date = doc.getString("date");
                        String time = doc.getString("time");
                        if (date != null && time != null) {
                            bookedSlots.add(date + "_" + time);
                            Log.d(TAG, "Booked slot: " + date + " at " + time);
                        }
                    }

                    Log.d(TAG, "Total booked slots: " + bookedSlots.size());
                    generateScheduleUI(bookedSlots);
                    loadingProgress.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading schedule", e);
                    Toast.makeText(this, "Error loading schedule. Showing all slots as available.", Toast.LENGTH_SHORT).show();
                    // Generate UI with no booked slots (all available)
                    generateScheduleUI(new ArrayList<>());
                    loadingProgress.setVisibility(View.GONE);
                });
    }

    private void generateScheduleUI(List<String> bookedSlots) {
        Log.d(TAG, "generateScheduleUI called with " + bookedSlots.size() + " booked slots");
        scheduleContainer.removeAllViews();

        // Generate next 14 days
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());

        for (int day = 0; day < 14; day++) {
            Date date = calendar.getTime();
            String dateStr = dateFormat.format(date);
            String dayStr = dayFormat.format(date);

            Log.d(TAG, "Creating schedule for day " + day + ": " + dayStr + " - " + dateStr);

            // Create day header
            TextView dayHeader = new TextView(this);
            dayHeader.setText(dayStr + " - " + dateStr);
            dayHeader.setTextColor(Color.parseColor("#333333"));
            dayHeader.setTextSize(16);
            dayHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            headerParams.setMargins(0, (int) (16 * getResources().getDisplayMetrics().density), 0,
                    (int) (8 * getResources().getDisplayMetrics().density));
            dayHeader.setLayoutParams(headerParams);
            scheduleContainer.addView(dayHeader);

            // Create time slots
            LinearLayout timeSlotRow = new LinearLayout(this);
            timeSlotRow.setOrientation(LinearLayout.HORIZONTAL);
            timeSlotRow.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            // Time slots: 6AM, 9AM, 12PM, 3PM, 6PM, 9PM
            String[] times = {"6:00 AM", "9:00 AM", "12:00 PM", "3:00 PM", "6:00 PM", "9:00 PM"};

            for (String time : times) {
                boolean isBooked = bookedSlots.contains(dateStr + "_" + time);
                CardView timeCard = createTimeSlotCard(dateStr, time, isBooked);

                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        0,
                        (int) (50 * getResources().getDisplayMetrics().density),
                        1f
                );
                cardParams.setMargins(
                        (int) (4 * getResources().getDisplayMetrics().density),
                        0,
                        (int) (4 * getResources().getDisplayMetrics().density),
                        (int) (8 * getResources().getDisplayMetrics().density)
                );
                timeCard.setLayoutParams(cardParams);

                timeSlotRow.addView(timeCard);

                // Break into 2 rows of 3
                if (timeSlotRow.getChildCount() == 3) {
                    scheduleContainer.addView(timeSlotRow);
                    timeSlotRow = new LinearLayout(this);
                    timeSlotRow.setOrientation(LinearLayout.HORIZONTAL);
                    timeSlotRow.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    ));
                }
            }

            if (timeSlotRow.getChildCount() > 0) {
                scheduleContainer.addView(timeSlotRow);
            }

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Log.d(TAG, "Schedule UI generation complete. Container child count: " + scheduleContainer.getChildCount());
    }

    private CardView createTimeSlotCard(String date, String time, boolean isBooked) {
        CardView card = new CardView(this);
        card.setRadius(12 * getResources().getDisplayMetrics().density);
        card.setCardElevation(2 * getResources().getDisplayMetrics().density);

        if (isBooked) {
            card.setCardBackgroundColor(Color.parseColor("#CCCCCC"));
            card.setEnabled(false);
        } else {
            card.setCardBackgroundColor(Color.parseColor("#4CAF50"));
            card.setClickable(true);
            card.setFocusable(true);
            card.setForeground(getDrawable(android.R.drawable.list_selector_background));
        }

        TextView timeText = new TextView(this);
        timeText.setText(time.replace(":00 ", "\n"));
        timeText.setTextColor(Color.WHITE);
        timeText.setTextSize(10);
        timeText.setGravity(Gravity.CENTER);
        timeText.setTypeface(null, android.graphics.Typeface.BOLD);

        card.addView(timeText);

        if (!isBooked) {
            card.setOnClickListener(v -> selectTimeSlot(card, date, time));
        }

        return card;
    }

    private void selectTimeSlot(CardView card, String date, String time) {
        // Reset previous selection
        if (currentlySelectedCard != null) {
            currentlySelectedCard.setCardBackgroundColor(Color.parseColor("#4CAF50"));
            currentlySelectedCard.setCardElevation(2 * getResources().getDisplayMetrics().density);
        }

        // Highlight selected
        card.setCardBackgroundColor(Color.parseColor("#FFC107"));
        card.setCardElevation(8 * getResources().getDisplayMetrics().density);
        currentlySelectedCard = card;

        selectedDate = date;
        selectedTime = time;

        confirmScheduleButton.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Selected: " + date + " at " + time, Toast.LENGTH_SHORT).show();
    }

    private void proceedToPayment() {
        // Save selected schedule temporarily
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selectedDate", selectedDate);
        resultIntent.putExtra("selectedTime", selectedTime);
        resultIntent.putExtra("coachId", coachId);
        resultIntent.putExtra("coachName", coachName);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void saveRescheduledBooking() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingProgress.setVisibility(View.VISIBLE);
        confirmScheduleButton.setEnabled(false);

        // Update membership document with new schedule
        Map<String, Object> membershipUpdates = new HashMap<>();
        membershipUpdates.put("scheduleDate", selectedDate);
        membershipUpdates.put("scheduleTime", selectedTime);

        db.collection("memberships")
                .document(userId)
                .update(membershipUpdates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Membership updated with new schedule");

                    // Save to schedules collection (create or update)
                    Map<String, Object> scheduleData = new HashMap<>();
                    scheduleData.put("userId", userId);
                    scheduleData.put("userName", FirebaseAuth.getInstance().getCurrentUser().getDisplayName() != null ?
                            FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : "User");
                    scheduleData.put("coachId", coachId);
                    scheduleData.put("coachName", coachName);
                    scheduleData.put("date", selectedDate);
                    scheduleData.put("time", selectedTime);
                    scheduleData.put("status", "scheduled");
                    scheduleData.put("createdAt", Timestamp.now());

                    // Check if schedule already exists for this user
                    db.collection("schedules")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    // Update existing schedule
                                    String scheduleId = querySnapshot.getDocuments().get(0).getId();
                                    db.collection("schedules").document(scheduleId)
                                            .update(scheduleData)
                                            .addOnSuccessListener(aVoid2 -> {
                                                Log.d(TAG, "📅 Schedule booking updated: " + scheduleId);
                                                Toast.makeText(this, "Schedule updated successfully!", Toast.LENGTH_SHORT).show();
                                                loadingProgress.setVisibility(View.GONE);
                                                confirmScheduleButton.setEnabled(true);

                                                // Return result and finish
                                                Intent resultIntent = new Intent();
                                                resultIntent.putExtra("selectedDate", selectedDate);
                                                resultIntent.putExtra("selectedTime", selectedTime);
                                                setResult(RESULT_OK, resultIntent);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "❌ Failed to update schedule booking", e);
                                                Toast.makeText(this, "Failed to update schedule", Toast.LENGTH_SHORT).show();
                                                loadingProgress.setVisibility(View.GONE);
                                                confirmScheduleButton.setEnabled(true);
                                            });
                                } else {
                                    // Create new schedule
                                    db.collection("schedules")
                                            .add(scheduleData)
                                            .addOnSuccessListener(docRef -> {
                                                Log.d(TAG, "📅 Schedule booking created: " + docRef.getId());
                                                Toast.makeText(this, "Schedule booked successfully!", Toast.LENGTH_SHORT).show();
                                                loadingProgress.setVisibility(View.GONE);
                                                confirmScheduleButton.setEnabled(true);

                                                // Return result and finish
                                                Intent resultIntent = new Intent();
                                                resultIntent.putExtra("selectedDate", selectedDate);
                                                resultIntent.putExtra("selectedTime", selectedTime);
                                                setResult(RESULT_OK, resultIntent);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "❌ Failed to create schedule booking", e);
                                                Toast.makeText(this, "Failed to book schedule", Toast.LENGTH_SHORT).show();
                                                loadingProgress.setVisibility(View.GONE);
                                                confirmScheduleButton.setEnabled(true);
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Failed to check existing schedules", e);
                                Toast.makeText(this, "Failed to check existing schedules", Toast.LENGTH_SHORT).show();
                                loadingProgress.setVisibility(View.GONE);
                                confirmScheduleButton.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update membership", e);
                    Toast.makeText(this, "Failed to update membership", Toast.LENGTH_SHORT).show();
                    loadingProgress.setVisibility(View.GONE);
                    confirmScheduleButton.setEnabled(true);
                });
    }
}
