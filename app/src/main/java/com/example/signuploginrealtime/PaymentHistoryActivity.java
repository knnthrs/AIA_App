package com.example.signuploginrealtime;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaymentHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvNoPayments;
    private PaymentHistoryAdapter adapter;
    private List<PaymentHistoryItem> paymentList;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);
        overridePendingTransition(0, 0);

        recyclerView = findViewById(R.id.recyclerViewPaymentHistory);
        tvNoPayments = findViewById(R.id.tv_no_payments);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        paymentList = new ArrayList<>();
        adapter = new PaymentHistoryAdapter(paymentList, mAuth, firestore);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadPaymentHistory();

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });
    }

    private void loadPaymentHistory() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("paymentHistory")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        recyclerView.setVisibility(View.GONE);
                        tvNoPayments.setVisibility(View.VISIBLE);
                        tvNoPayments.setText("Error loading payments.");
                        return;
                    }

                    if (querySnapshot != null) {
                        paymentList.clear();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            // ✅ Get new fields
                            String membershipPlanType = doc.getString("membershipPlanType");
                            Long months = doc.getLong("months");
                            Long sessions = doc.getLong("sessions");
                            Double amount = doc.getDouble("price"); // Changed from "amount" to "price"
                            String paymentMethod = doc.getString("paymentMethod");
                            String paymentStatus = doc.getString("paymentStatus");

                            // ✅ Generate formatted display name
                            String displayName = generateFormattedPlanName(
                                    membershipPlanType,
                                    months,
                                    sessions
                            );

                            if (displayName != null && amount != null) {
                                paymentList.add(new PaymentHistoryItem(
                                        displayName, // Use formatted name
                                        amount,
                                        paymentMethod != null ? paymentMethod : "Unknown",
                                        paymentStatus != null ? paymentStatus : "paid",
                                        doc.getTimestamp("timestamp"),
                                        doc.getTimestamp("startDate"), // Changed from membershipStartDate
                                        doc.getTimestamp("expirationDate"), // Changed from membershipExpirationDate
                                        doc.getId()
                                ));
                            }
                        }

                        if (paymentList.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            tvNoPayments.setVisibility(View.VISIBLE);
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            tvNoPayments.setVisibility(View.GONE);
                            adapter.notifyDataSetChanged(); // No need to reverse, already ordered DESC
                        }
                    }
                });
    }


    private String generateFormattedPlanName(String type, Long months, Long sessions) {
        if (type == null) return "Unknown Plan";

        int monthsVal = (months != null) ? months.intValue() : 0;
        int sessionsVal = (sessions != null) ? sessions.intValue() : 0;

        // For Daily Pass
        if ("Daily".equals(type) || monthsVal == 0) {
            return "Daily";
        }

        // For Standard (no PT sessions)
        if (sessionsVal == 0) {
            if (monthsVal == 1) return "Standard Monthly";
            else if (monthsVal == 3) return "Standard 3 Months";
            else if (monthsVal == 6) return "Standard 6 Months";
            else if (monthsVal == 12) return "Standard Annual";
        }

        // For Monthly with PT
        if (sessionsVal > 0) {
            if (monthsVal == 1) return "Monthly with " + sessionsVal + " PT";
            else if (monthsVal == 3) return "3 Months with " + sessionsVal + " PT";
            else if (monthsVal == 6) return "6 Months with " + sessionsVal + " PT";
            else if (monthsVal == 12) return "Annual with " + sessionsVal + " PT";
        }

        // Fallback
        return type;
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}
