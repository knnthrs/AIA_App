package com.example.signuploginrealtime;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.content.Intent;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SelectMembership extends AppCompatActivity {

    private View backButton;
    private CardView confirmButtonCard;
    private ProgressBar loadingProgress;

    private String selectedPackageId = null;
    private String selectedPlanLabel = null;
    private String selectedPlanType = null;
    private int selectedMonths = 0;
    private int selectedSessions = 0;
    private double selectedPrice = 0;

    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_membership);

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be logged in to select a plan.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        currentUserId = user.getUid();

        backButton = findViewById(R.id.back_button);
        confirmButtonCard = findViewById(R.id.confirm_membership_button);
        loadingProgress = findViewById(R.id.loading_progress);

        backButton.setOnClickListener(v -> finish());
        confirmButtonCard.setVisibility(View.GONE);

        // Check if user already has an active membership
        checkExistingMembership();

        // Load packages from Firestore
        loadPackagesFromFirestore();

        confirmButtonCard.setOnClickListener(v -> {
            if (selectedPackageId == null || selectedPlanLabel == null) {
                Toast.makeText(this, "Please select a plan first.", Toast.LENGTH_SHORT).show();
                return;
            }
            saveMembership();
        });
    }

    private void checkExistingMembership() {
        db.collection("memberships")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("membershipStatus", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // User already has an active membership
                        Toast.makeText(this, "You already have an active membership", Toast.LENGTH_LONG).show();
                        // Optionally, you can still allow them to upgrade/change
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle error silently or show message
                });
    }

    private void saveMembership() {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.VISIBLE);
        }
        confirmButtonCard.setEnabled(false);

        // First, check if user already has an active membership and deactivate it
        db.collection("memberships")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("membershipStatus", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Deactivate all existing active memberships
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().update("membershipStatus", "replaced");
                    }

                    // Now create the new membership
                    createNewMembership();
                })
                .addOnFailureListener(e -> {
                    // If check fails, still try to create membership
                    createNewMembership();
                });
    }

    private void createNewMembership() {
        // Calculate expiration date based on months
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, selectedMonths);
        Date expirationDate = calendar.getTime();
        Timestamp expirationTimestamp = new Timestamp(expirationDate);
        Timestamp startTimestamp = Timestamp.now();

        Map<String, Object> membershipData = new HashMap<>();
        membershipData.put("userId", currentUserId);
        membershipData.put("packageId", selectedPackageId);
        membershipData.put("membershipPlanLabel", selectedPlanLabel);
        membershipData.put("membershipPlanType", selectedPlanType);
        membershipData.put("membershipStatus", "active");
        membershipData.put("membershipStartDate", startTimestamp);
        membershipData.put("membershipExpirationDate", expirationTimestamp);
        membershipData.put("months", selectedMonths);
        membershipData.put("sessions", selectedSessions);
        membershipData.put("sessionsRemaining", selectedSessions); // Track remaining PT sessions
        membershipData.put("price", selectedPrice);
        membershipData.put("createdAt", startTimestamp);

        // Save to memberships collection
        db.collection("memberships")
                .add(membershipData)
                .addOnSuccessListener(documentReference -> {
                    if (loadingProgress != null) {
                        loadingProgress.setVisibility(View.GONE);
                    }

                    Toast.makeText(this, "Membership activated: " + selectedPlanLabel, Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("selectedPackageId", selectedPackageId);
                    resultIntent.putExtra("selectedPlanLabel", selectedPlanLabel);
                    resultIntent.putExtra("expirationDate", expirationTimestamp.toDate().getTime());
                    resultIntent.putExtra("membershipId", documentReference.getId());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (loadingProgress != null) {
                        loadingProgress.setVisibility(View.GONE);
                    }
                    confirmButtonCard.setEnabled(true);
                    Toast.makeText(this, "Failed to save membership: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadPackagesFromFirestore() {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.VISIBLE);
        }

        db.collection("packages")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (loadingProgress != null) {
                        loadingProgress.setVisibility(View.GONE);
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String packageId = document.getId();
                        String type = document.getString("type");
                        Long months = document.getLong("months");
                        Long sessions = document.getLong("sessions");
                        Double price = document.getDouble("price");

                        if (type == null || months == null || price == null) continue;

                        CardView card = getCardViewForPackage(packageId);
                        if (card != null) {
                            setPlanClick(card, packageId, type,
                                    months.intValue(),
                                    sessions != null ? sessions.intValue() : 0,
                                    price);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (loadingProgress != null) {
                        loadingProgress.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, "Failed to load packages: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private CardView getCardViewForPackage(String packageId) {
        int cardId = 0;

        switch (packageId) {
            case "5YjEV258bysDMUxZuTUC":
                cardId = R.id.daily_card;
                break;
            case "GDKVR24VY7DNFmdKJroC":
                cardId = R.id.one_month_card;
                break;
            case "QbTXP0cY0M7Wxyrcgt5O":
                cardId = R.id.three_month_card;
                break;
            case "ZyvkZ8WNJb6ZPzGkG74w":
                cardId = R.id.six_month_card;
                break;
            case "fix4Hyr5nVCaC1FpcuFk":
                cardId = R.id.one_year_card;
                break;
            case "kZX2cCdxYAahOfwDrzaK":
                cardId = R.id.one_month_10pt_card;
                break;
            case "q5DtmQjdP0kWoljfe2yf":
                cardId = R.id.three_month_10pt_card;
                break;
            case "rctrNNKdSWLGHe1dmGcy":
                cardId = R.id.three_month_15pt_card;
                break;
            case "w6KSFtEnx3CIk66xkGEW":
                cardId = R.id.three_month_24pt_card;
                break;
            default:
                return null;
        }

        return findViewById(cardId);
    }

    private void setPlanClick(CardView card, String packageId, String type,
                              int months, int sessions, double price) {
        if (card == null) return;

        String planLabel = generatePlanLabel(type, months, sessions, price);

        card.setOnClickListener(v -> {
            selectedPackageId = packageId;
            selectedPlanLabel = planLabel;
            selectedPlanType = type;
            selectedMonths = months;
            selectedSessions = sessions;
            selectedPrice = price;

            resetAllCards();
            enlargeCard(card);

            if (confirmButtonCard.getVisibility() != View.VISIBLE) {
                confirmButtonCard.setVisibility(View.VISIBLE);
            }
        });
    }

    private String generatePlanLabel(String type, int months, int sessions, double price) {
        StringBuilder label = new StringBuilder();

        if (months == 0 || months < 1) {
            label.append("Daily Pass");
        } else if (months == 1) {
            label.append("1 Month");
        } else if (months == 12) {
            label.append("12 Months / 1 Year");
        } else {
            label.append(months).append(" Months");
        }

        if (sessions > 0) {
            label.append(" + ").append(sessions).append(" PT Sessions");
        }

        label.append(" — ₱").append(String.format("%.0f", price));

        return label.toString();
    }

    private void resetAllCards() {
        int[] cardIds = {
                R.id.daily_card, R.id.one_month_card, R.id.three_month_card,
                R.id.six_month_card, R.id.one_year_card, R.id.one_month_10pt_card,
                R.id.three_month_10pt_card, R.id.three_month_15pt_card, R.id.three_month_24pt_card
        };

        for (int cardId : cardIds) {
            CardView card = findViewById(cardId);
            if (card != null) {
                resetCardSize(card);
            }
        }
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