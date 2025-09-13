package com.example.signuploginrealtime;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore; // Changed import

import java.util.Arrays;
import java.util.HashMap; // Added import
import java.util.List;
import java.util.Map; // Added import

public class SelectMembership extends AppCompatActivity {

    // Header
    private View backButton;

    // All packages (daily + standard + PT)
    private List<CardView> membershipCards;

    // Confirm button
    private CardView confirmButtonCard;

    // Keep the currently selected plan
    private String selectedPlanCode = null;
    private String selectedPlanLabel = null;

    private FirebaseFirestore db; // Added Firestore instance

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_membership);

        db = FirebaseFirestore.getInstance(); // Initialize Firestore

        // ---- find views (match your XML ids) ----
        backButton = findViewById(R.id.back_button);

        // Daily package
        CardView dailyCard = findViewById(R.id.daily_card);

        // Standard packages
        CardView oneMonthCard = findViewById(R.id.one_month_card);
        CardView threeMonthCard = findViewById(R.id.three_month_card);
        CardView sixMonthCard = findViewById(R.id.six_month_card);
        CardView oneYearCard = findViewById(R.id.one_year_card);

        // PT packages
        CardView oneMonth10PtCard = findViewById(R.id.one_month_10pt_card);
        CardView threeMonth10PtCard = findViewById(R.id.three_month_10pt_card);
        CardView threeMonth15PtCard = findViewById(R.id.three_month_15pt_card);
        CardView threeMonth24PtCard = findViewById(R.id.three_month_24pt_card);

        confirmButtonCard = findViewById(R.id.confirm_membership_button);

        // Put all cards in a list for easy reset
        membershipCards = Arrays.asList(
                dailyCard, oneMonthCard, threeMonthCard, sixMonthCard, oneYearCard,
                oneMonth10PtCard, threeMonth10PtCard, threeMonth15PtCard, threeMonth24PtCard
        );

        // ---- interactions ----
        backButton.setOnClickListener(v -> finish());

        // Daily package click
        setPlanClick(dailyCard,
                "DAILY",
                "Daily Pass — ₱150\nFull gym access • All equipment • Locker room");

        // Standard package clicks
        setPlanClick(oneMonthCard,
                "1 MONTH STANDARD",
                "1 Month — ₱1,500\nFull gym access • All equipment • Locker room");
        setPlanClick(threeMonthCard,
                "3 MONTHS STANDARD",
                "3 Months — ₱3,600 (₱1,200/month)");
        setPlanClick(sixMonthCard,
                "6 MONTHS STANDARD",
                "6 Months — ₱6,000 (₱1,000/month)");
        setPlanClick(oneYearCard,
                "1 YEAR STANDARD",
                "12 Months / 1 Year — ₱9,000 (₱750/month)");

        // PT package clicks
        setPlanClick(oneMonth10PtCard,
                "1 MONTH WITH 10PT",
                "1 Month + 10 PT Sessions — ₱4,500");
        setPlanClick(threeMonth10PtCard,
                "3 MONTHS WITH 10PT",
                "MONTHLY_3 Months + 10 PT Sessions — ₱6,000");
        setPlanClick(threeMonth15PtCard,
                "3 MONTHS WITH 15PT",
                "MONTHLY_3 Months + 15 PT Sessions — ₱7,500");
        setPlanClick(threeMonth24PtCard,
                "3 MONTHS WITH 24PT",
                "3 Months + 24 PT Sessions — ₱9,000");

        // Confirm: save to Firebase and return result
        confirmButtonCard.setOnClickListener(v -> {
            if (selectedPlanCode == null || selectedPlanLabel == null) {
                Toast.makeText(this, "Please select a plan first.", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "You must be logged in to select a plan.", Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED);
                finish();
                return;
            }

            String uid = user.getUid();

            Map<String, Object> membershipData = new HashMap<>();
            membershipData.put("membershipPlanCode", selectedPlanCode);
            membershipData.put("membershipPlanLabel", selectedPlanLabel);
            membershipData.put("membershipStatus", "active"); // Added membership status

            db.collection("users").document(uid)
                    .update(membershipData) // Using update to only change these fields
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Plan saved: " + selectedPlanLabel, Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("selectedPlanCode", selectedPlanCode);
                        resultIntent.putExtra("selectedPlanLabel", selectedPlanLabel);
                        // You might want to pass the status back too if needed by the calling activity
                        // resultIntent.putExtra("membershipStatus", "active");
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to save plan to Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        // Confirm hidden until a selection is made
        confirmButtonCard.setVisibility(View.GONE);
    }

    private void setPlanClick(CardView card, String planCode, String planLabel) {
        if (card == null) return;
        card.setOnClickListener(v -> {
            selectedPlanCode = planCode;
            selectedPlanLabel = planLabel;

            // Reset all cards to normal size
            for (CardView c : membershipCards) {
                if (c != null) {
                    resetCardSize(c);
                }
            }

            // Enlarge the selected card
            enlargeCard(card);

            // Show confirm button
            if (confirmButtonCard.getVisibility() != View.VISIBLE) {
                confirmButtonCard.setVisibility(View.VISIBLE);
            }
        });
    }

    // 🔹 helper methods for animations
    private void enlargeCard(CardView card) {
        card.animate()
                .scaleX(1.1f) // grow 10%
                .scaleY(1.1f)
                .setDuration(200)
                .start();
    }

    private void resetCardSize(CardView card) {
        card.animate()
                .scaleX(1f) // back to normal
                .scaleY(1f)
                .setDuration(200)
                .start();
    }
}