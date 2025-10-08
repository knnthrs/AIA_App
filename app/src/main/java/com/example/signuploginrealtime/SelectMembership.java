package com.example.signuploginrealtime;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.content.Intent;
import android.widget.ProgressBar;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.Timestamp;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SelectMembership extends AppCompatActivity {

    private static final String TAG = "SelectMembership";
    private static final String PAYMONGO_SECRET_KEY = "sk_test_7AjfDjSecFKtHZX6ee8Sa95B"; // Replace with your key

    private View backButton;
    private CardView confirmButtonCard;
    private ProgressBar loadingProgress;

    private String selectedPackageId = null;
    private String selectedPlanLabel = null;
    private String selectedPlanType = null;
    private int selectedMonths = 0;
    private int selectedDurationDays = 0;
    private int selectedSessions = 0;
    private double selectedPrice = 0;

    private FirebaseFirestore db;
    private String currentUserId;
    private Executor executor = Executors.newSingleThreadExecutor();

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

        checkExistingMembership();
        loadPackagesFromFirestore();

        confirmButtonCard.setOnClickListener(v -> {
            if (selectedPackageId == null || selectedPlanLabel == null) {
                Toast.makeText(this, "Please select a plan first.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Start payment process instead of directly saving
            initiatePayMongoPayment();
        });
    }

    private void checkExistingMembership() {
        db.collection("memberships")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("membershipStatus", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "You already have an active membership", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking membership", e);
                });
    }

    private void initiatePayMongoPayment() {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.VISIBLE);
        }
        confirmButtonCard.setEnabled(false);

        // Convert price to cents (PayMongo uses centavos)
        int amountInCents = (int) (selectedPrice * 100);

        executor.execute(() -> {
            try {
                // Create PayMongo Payment Link
                String paymentLinkUrl = createPayMongoPaymentLink(amountInCents);

                runOnUiThread(() -> {
                    if (loadingProgress != null) {
                        loadingProgress.setVisibility(View.GONE);
                    }
                    confirmButtonCard.setEnabled(true);

                    if (paymentLinkUrl != null) {
                        // Open payment page
                        Intent intent = new Intent(SelectMembership.this, PayMongoPaymentActivity.class);
                        intent.putExtra("paymentUrl", paymentLinkUrl);
                        intent.putExtra("packageId", selectedPackageId);
                        intent.putExtra("planLabel", selectedPlanLabel);
                        intent.putExtra("planType", selectedPlanType);
                        intent.putExtra("months", selectedMonths);
                        intent.putExtra("durationDays", selectedDurationDays);
                        intent.putExtra("sessions", selectedSessions);
                        intent.putExtra("price", selectedPrice);
                        startActivityForResult(intent, 100);
                    } else {
                        Toast.makeText(SelectMembership.this,
                                "Failed to create payment link. Please try again.",
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error creating payment", e);
                runOnUiThread(() -> {
                    if (loadingProgress != null) {
                        loadingProgress.setVisibility(View.GONE);
                    }
                    confirmButtonCard.setEnabled(true);
                    Toast.makeText(SelectMembership.this,
                            "Payment error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String createPayMongoPaymentLink(int amountInCents) {
        try {
            URL url = new URL("https://api.paymongo.com/v1/links");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Basic " +
                    android.util.Base64.encodeToString(
                            (PAYMONGO_SECRET_KEY + ":").getBytes(),
                            android.util.Base64.NO_WRAP
                    ));
            conn.setDoOutput(true);

            // Create payment link data
            JSONObject data = new JSONObject();
            JSONObject attributes = new JSONObject();

            attributes.put("amount", amountInCents);
            attributes.put("description", selectedPlanLabel);
            attributes.put("remarks", "Membership: " + selectedPlanLabel);

            data.put("data", new JSONObject().put("attributes", attributes));

            // Send request
            OutputStream os = conn.getOutputStream();
            os.write(data.toString().getBytes());
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "PayMongo Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                String checkoutUrl = jsonResponse.getJSONObject("data")
                        .getJSONObject("attributes")
                        .getString("checkout_url");

                Log.d(TAG, "Payment URL created: " + checkoutUrl);
                return checkoutUrl;
            } else {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                Log.e(TAG, "PayMongo Error: " + response.toString());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error creating PayMongo link", e);
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100) {
            if (resultCode == RESULT_OK && data != null) {
                boolean paymentSuccess = data.getBooleanExtra("paymentSuccess", false);

                if (paymentSuccess) {
                    // Payment successful, save membership
                    saveMembership();
                } else {
                    Toast.makeText(this, "Payment was not completed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void saveMembership() {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.VISIBLE);
        }
        confirmButtonCard.setEnabled(false);

        db.collection("memberships")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("membershipStatus", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    createNewMembership();
                })
                .addOnFailureListener(e -> createNewMembership());
    }

    private void createNewMembership() {
        // First, fetch the user's full name from users collection
        db.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String fullName = "Unknown User"; // Default value

                    if (userDoc.exists()) {
                        fullName = userDoc.getString("fullname"); // or "fullName" depending on your field name
                        if (fullName == null || fullName.isEmpty()) {
                            fullName = "Unknown User";
                        }
                    }

                    // Now create the membership with the user's name
                    saveMembershipWithUserName(fullName);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user name", e);
                    // Continue with default name if fetch fails
                    saveMembershipWithUserName("Unknown User");
                });
    }

    private void saveMembershipWithUserName(String fullName) {
        Calendar calendar = Calendar.getInstance();

        if (selectedMonths > 0) {
            calendar.add(Calendar.MONTH, selectedMonths);
        } else {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Date expirationDate = calendar.getTime();
        Timestamp expirationTimestamp = new Timestamp(expirationDate);
        Timestamp startTimestamp = Timestamp.now();

        Map<String, Object> membershipData = new HashMap<>();
        membershipData.put("userId", currentUserId);
        membershipData.put("fullName", fullName);  // Add full name instead of userId
        membershipData.put("packageId", selectedPackageId);
        membershipData.put("membershipPlanLabel", selectedPlanLabel);
        membershipData.put("membershipPlanType", selectedPlanType);
        membershipData.put("membershipStatus", "active");
        membershipData.put("membershipStartDate", startTimestamp);
        membershipData.put("membershipExpirationDate", expirationTimestamp);
        membershipData.put("months", selectedMonths);
        membershipData.put("durationDays", selectedDurationDays);
        membershipData.put("sessions", selectedSessions);
        membershipData.put("sessionsRemaining", selectedSessions);
        membershipData.put("price", selectedPrice);
        membershipData.put("paymentStatus", "paid");
        membershipData.put("createdAt", startTimestamp);

        Log.d(TAG, "Saving membership with userId: " + currentUserId);
        Log.d(TAG, "Membership data: " + membershipData.toString());

        // Use userId as document ID
        db.collection("memberships")
                .document(currentUserId)
                .set(membershipData)
                .addOnSuccessListener(aVoid -> {
                    if (loadingProgress != null) {
                        loadingProgress.setVisibility(View.GONE);
                    }

                    Toast.makeText(this, "Membership activated: " + selectedPlanLabel, Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("selectedPackageId", selectedPackageId);
                    resultIntent.putExtra("selectedPlanLabel", selectedPlanLabel);
                    resultIntent.putExtra("expirationDate", expirationTimestamp.toDate().getTime());
                    resultIntent.putExtra("membershipId", currentUserId);
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
                        Long durationDays = document.getLong("durationDays");
                        Long sessions = document.getLong("sessions");
                        Double price = document.getDouble("price");

                        if (type == null || price == null) continue;
                        if (months == null) months = 0L;
                        if (durationDays == null) durationDays = 0L;

                        CardView card = getCardViewForPackage(packageId);
                        if (card != null) {
                            setPlanClick(card, packageId, type,
                                    months.intValue(),
                                    durationDays.intValue(),
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
            case "5YjEV258bysDMUxZuTUC": cardId = R.id.daily_card; break;
            case "GDKVR24VY7DNFmdKJroC": cardId = R.id.one_month_card; break;
            case "QbTXP0cY0M7Wxyrcgt5O": cardId = R.id.three_month_card; break;
            case "ZyvkZ8WNJb6ZPzGkG74w": cardId = R.id.six_month_card; break;
            case "fix4Hyr5nVCaC1FpcuFk": cardId = R.id.one_year_card; break;
            case "kZX2cCdxYAahOfwDrzaK": cardId = R.id.one_month_10pt_card; break;
            case "q5DtmQjdP0kWoljfe2yf": cardId = R.id.three_month_10pt_card; break;
            case "rctrNNKdSWLGHe1dmGcy": cardId = R.id.three_month_15pt_card; break;
            case "w6KSFtEnx3CIk66xkGEW": cardId = R.id.three_month_24pt_card; break;
            default: return null;
        }

        return findViewById(cardId);
    }

    private void setPlanClick(CardView card, String packageId, String type,
                              int months, int durationDays, int sessions, double price) {
        if (card == null) return;

        String planLabel = generatePlanLabel(type, months, durationDays, sessions, price);

        card.setOnClickListener(v -> {
            selectedPackageId = packageId;
            selectedPlanLabel = planLabel;
            selectedPlanType = type;
            selectedMonths = months;
            selectedDurationDays = durationDays;
            selectedSessions = sessions;
            selectedPrice = price;

            resetAllCards();
            enlargeCard(card);

            if (confirmButtonCard.getVisibility() != View.VISIBLE) {
                confirmButtonCard.setVisibility(View.VISIBLE);
            }
        });
    }

    private String generatePlanLabel(String type, int months, int durationDays, int sessions, double price) {
        StringBuilder label = new StringBuilder();

        if (durationDays > 0) {
            if (durationDays == 1) {
                label.append("Daily Pass");
            } else {
                label.append(durationDays).append(" Days");
            }
        } else if (months == 0 || months < 1) {
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
        card.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
    }

    private void resetCardSize(CardView card) {
        card.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
    }
}