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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectMembership extends AppCompatActivity {

    private View backButton;
    private List<CardView> membershipCards;
    private CardView confirmButtonCard;

    private String selectedPlanCode = null;
    private String selectedPlanLabel = null;
    private int selectedDurationDays = 0;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_membership);

        db = FirebaseFirestore.getInstance();

        backButton = findViewById(R.id.back_button);

        CardView dailyCard = findViewById(R.id.daily_card);
        CardView oneMonthCard = findViewById(R.id.one_month_card);
        CardView threeMonthCard = findViewById(R.id.three_month_card);
        CardView sixMonthCard = findViewById(R.id.six_month_card);
        CardView oneYearCard = findViewById(R.id.one_year_card);
        CardView oneMonth10PtCard = findViewById(R.id.one_month_10pt_card);
        CardView threeMonth10PtCard = findViewById(R.id.three_month_10pt_card);
        CardView threeMonth15PtCard = findViewById(R.id.three_month_15pt_card);
        CardView threeMonth24PtCard = findViewById(R.id.three_month_24pt_card);

        confirmButtonCard = findViewById(R.id.confirm_membership_button);

        membershipCards = Arrays.asList(
                dailyCard, oneMonthCard, threeMonthCard, sixMonthCard, oneYearCard,
                oneMonth10PtCard, threeMonth10PtCard, threeMonth15PtCard, threeMonth24PtCard
        );

        backButton.setOnClickListener(v -> finish());

        // Set plan clicks with duration in days
        setPlanClick(dailyCard, "DAILY",
                "Daily Pass — ₱150\nFull gym access • All equipment • Locker room", 1);

        setPlanClick(oneMonthCard, "MONTHLY",
                "1 Month — ₱1,500\nFull gym access • All equipment • Locker room", 30);
        setPlanClick(threeMonthCard, "MONTHLY",
                "3 Months — ₱3,600 (₱1,200/month)", 90);
        setPlanClick(sixMonthCard, "MONTHLY",
                "6 Months — ₱6,000 (₱1,000/month)", 180);
        setPlanClick(oneYearCard, "YEARLY",
                "12 Months / 1 Year — ₱9,000 (₱750/month)", 365);

        setPlanClick(oneMonth10PtCard, "MONTHLY",
                "1 Month + 10 PT Sessions — ₱4,500", 30);
        setPlanClick(threeMonth10PtCard, "MONTHLY",
                "3 Months + 10 PT Sessions — ₱6,000", 90);
        setPlanClick(threeMonth15PtCard, "MONTHLY",
                "3 Months + 15 PT Sessions — ₱7,500", 90);
        setPlanClick(threeMonth24PtCard, "MONTHLY",
                "3 Months + 24 PT Sessions — ₱9,000", 90);

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

            // Calculate expiration date
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, selectedDurationDays);
            Date expirationDate = calendar.getTime();
            Timestamp expirationTimestamp = new Timestamp(expirationDate);

            Map<String, Object> membershipData = new HashMap<>();
            membershipData.put("userId", uid);
            membershipData.put("membershipPlanCode", selectedPlanCode);
            membershipData.put("membershipPlanLabel", selectedPlanLabel);
            membershipData.put("membershipStatus", "active");
            membershipData.put("membershipStartDate", Timestamp.now());
            membershipData.put("membershipExpirationDate", expirationTimestamp);

            //Save to memberships collection
            db.collection("memberships")
                    .add(membershipData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Plan saved: " + selectedPlanLabel, Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("selectedPlanCode", selectedPlanCode);
                        resultIntent.putExtra("selectedPlanLabel", selectedPlanLabel);
                        resultIntent.putExtra("expirationDate", expirationTimestamp.toDate().getTime());
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to save membership: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        confirmButtonCard.setVisibility(View.GONE);
    }

    private void setPlanClick(CardView card, String planCode, String planLabel, int durationDays) {
        if (card == null) return;
        card.setOnClickListener(v -> {
            selectedPlanCode = planCode;
            selectedPlanLabel = planLabel;
            selectedDurationDays = durationDays;

            for (CardView c : membershipCards) {
                if (c != null) {
                    resetCardSize(c);
                }
            }

            enlargeCard(card);

            if (confirmButtonCard.getVisibility() != View.VISIBLE) {
                confirmButtonCard.setVisibility(View.VISIBLE);
            }
        });
    }

    private void enlargeCard(CardView card) {
        card.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(200)
                .start();
    }

    private void resetCardSize(CardView card) {
        card.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .start();
    }
}