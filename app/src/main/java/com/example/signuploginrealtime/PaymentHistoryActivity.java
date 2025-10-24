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
                            String planLabel = doc.getString("planLabel");
                            Double amount = doc.getDouble("amount");
                            String paymentMethod = doc.getString("paymentMethod");
                            String paymentStatus = doc.getString("paymentStatus");
                            if (planLabel != null && amount != null) {
                                paymentList.add(new PaymentHistoryItem(
                                        planLabel,
                                        amount,
                                        paymentMethod != null ? paymentMethod : "Unknown",
                                        paymentStatus != null ? paymentStatus : "paid",
                                        doc.getTimestamp("timestamp"),
                                        doc.getTimestamp("membershipStartDate"),
                                        doc.getTimestamp("membershipExpirationDate"),
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
                            Collections.reverse(paymentList);
                            adapter.notifyDataSetChanged();
                        }
                    }
                });

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
