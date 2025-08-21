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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;

public class SelectMembership extends AppCompatActivity {

    // Header
    private View backButton;

    // All packages (standard + PT)
    private List<CardView> membershipCards;

    // Confirm button
    private CardView confirmButtonCard;

    // Keep the currently selected plan
    private String selectedPlanCode = null;
    private String selectedPlanLabel = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_membership);

        // ---- find views (match your XML ids) ----
        backButton = findViewById(R.id.back_button);

        CardView oneMonthCard = findViewById(R.id.one_month_card);
        CardView threeMonthCard = findViewById(R.id.three_month_card);
        CardView sixMonthCard = findViewById(R.id.six_month_card);
        CardView oneYearCard = findViewById(R.id.one_year_card);

        CardView oneMonth10PtCard = findViewById(R.id.one_month_10pt_card);
        CardView threeMonth10PtCard = findViewById(R.id.three_month_10pt_card);
        CardView threeMonth15PtCard = findViewById(R.id.three_month_15pt_card);
        CardView threeMonth24PtCard = findViewById(R.id.three_month_24pt_card);

        confirmButtonCard = findViewById(R.id.confirm_membership_button);

        // Put all cards in a list for easy reset
        membershipCards = Arrays.asList(
                oneMonthCard, threeMonthCard, sixMonthCard, oneYearCard,
                oneMonth10PtCard, threeMonth10PtCard, threeMonth15PtCard, threeMonth24PtCard
        );

        // ---- interactions ----
        backButton.setOnClickListener(v -> finish());

        // Standard package clicks
        setPlanClick(oneMonthCard,
                "STANDARD_1M",
                "1 Month — ₱1,500\nFull gym access • All equipment • Locker room");
        setPlanClick(threeMonthCard,
                "STANDARD_3M",
                "3 Months — ₱3,600 (₱1,200/month)");
        setPlanClick(sixMonthCard,
                "STANDARD_6M",
                "6 Months — ₱6,000 (₱1,000/month)");
        setPlanClick(oneYearCard,
                "STANDARD_12M",
                "12 Months / 1 Year — ₱9,000 (₱750/month)");

        // PT package clicks
        setPlanClick(oneMonth10PtCard,
                "PT_1M_10PT",
                "1 Month + 10 PT Sessions — ₱4,500");
        setPlanClick(threeMonth10PtCard,
                "PT_3M_10PT",
                "3 Months + 10 PT Sessions — ₱6,000");
        setPlanClick(threeMonth15PtCard,
                "PT_3M_15PT",
                "3 Months + 15 PT Sessions — ₱7,500");
        setPlanClick(threeMonth24PtCard,
                "PT_3M_24PT",
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
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid);

            // Save both a stable code and a human-readable label
            userRef.child("membershipPlanCode").setValue(selectedPlanCode);
            userRef.child("membershipPlanLabel").setValue(selectedPlanLabel)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Plan saved: " + selectedPlanLabel, Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("selectedPlanCode", selectedPlanCode);
                        resultIntent.putExtra("selectedPlanLabel", selectedPlanLabel);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to save plan: " + e.getMessage(), Toast.LENGTH_LONG).show());
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
