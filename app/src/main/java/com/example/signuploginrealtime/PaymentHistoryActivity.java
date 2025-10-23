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

        recyclerView = findViewById(R.id.rv_payment_history);
        tvNoPayments = findViewById(R.id.tv_no_payments);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        paymentList = new ArrayList<>();
        adapter = new PaymentHistoryAdapter(paymentList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadPaymentHistory();
    }

    private void loadPaymentHistory() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        firestore.collection("users")
                .document(currentUser.getUid())
                .collection("paymentHistory")
                .get()
                .addOnSuccessListener(querySnapshot -> {
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
                                    paymentStatus != null ? paymentStatus : "Paid"
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
                })
                .addOnFailureListener(e -> {
                    recyclerView.setVisibility(View.GONE);
                    tvNoPayments.setVisibility(View.VISIBLE);
                    tvNoPayments.setText("Error loading payments.");
                });
    }
}
