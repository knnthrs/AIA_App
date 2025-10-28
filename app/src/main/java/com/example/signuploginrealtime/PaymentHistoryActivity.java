package com.example.signuploginrealtime;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class PaymentHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvNoPayments;
    private Button btnDeleteSelected;
    private ImageView btnSelectMode;
    private PaymentHistoryAdapter adapter;
    private List<PaymentHistoryItem> paymentList;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    private boolean isSelectionMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);
        overridePendingTransition(0, 0);

        recyclerView = findViewById(R.id.recyclerViewPaymentHistory);
        tvNoPayments = findViewById(R.id.tv_no_payments);
        btnDeleteSelected = findViewById(R.id.btn_delete_selected);
        btnSelectMode = findViewById(R.id.btn_select_mode);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        paymentList = new ArrayList<>();
        adapter = new PaymentHistoryAdapter(paymentList, mAuth, firestore, () -> {
            updateDeleteButton();
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadPaymentHistory();

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (isSelectionMode) {
                exitSelectionMode();
            } else {
                finish();
                overridePendingTransition(0, 0);
            }
        });

        btnSelectMode.setOnClickListener(v -> {
            if (isSelectionMode) {
                exitSelectionMode();
            } else {
                enterSelectionMode();
            }
        });

        btnDeleteSelected.setOnClickListener(v -> {
            List<PaymentHistoryItem> selectedItems = adapter.getSelectedItems();
            if (selectedItems.isEmpty()) return;

            AlertDialog dialog = new AlertDialog.Builder(this, R.style.RoundedAlertDialog)
                    .setTitle("Delete Payments")
                    .setMessage("Delete " + selectedItems.size() + " payment record(s)?")
                    .setPositiveButton("Delete", (d, which) -> {
                        deleteMultiplePayments(selectedItems);
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            dialog.show();
        });
    }

    private void enterSelectionMode() {
        isSelectionMode = true;
        adapter.setSelectionMode(true);
        btnDeleteSelected.setVisibility(View.VISIBLE);
        // Optional: change icon to indicate cancel/exit mode
        // btnSelectMode.setImageResource(R.drawable.ic_close);
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        adapter.setSelectionMode(false);
        adapter.clearSelection();
        btnDeleteSelected.setVisibility(View.GONE);
        // Optional: restore original icon
        // btnSelectMode.setImageResource(R.drawable.ic_check_circle);
    }

    private void updateDeleteButton() {
        int selectedCount = adapter.getSelectedItems().size();
        if (selectedCount > 0) {
            btnDeleteSelected.setText("Delete (" + selectedCount + ")");
            btnDeleteSelected.setEnabled(true);
        } else {
            btnDeleteSelected.setText("Delete");
            btnDeleteSelected.setEnabled(false);
        }
    }

    private void deleteMultiplePayments(List<PaymentHistoryItem> items) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        for (PaymentHistoryItem item : items) {
            if (item.documentId != null) {
                firestore.collection("users")
                        .document(currentUser.getUid())
                        .collection("paymentHistory")
                        .document(item.documentId)
                        .delete();
            }
        }

        exitSelectionMode();
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
                            String membershipPlanType = doc.getString("membershipPlanType");
                            Long months = doc.getLong("months");
                            Long sessions = doc.getLong("sessions");
                            Double amount = doc.getDouble("price");
                            String paymentMethod = doc.getString("paymentMethod");
                            String paymentStatus = doc.getString("paymentStatus");

                            String displayName = generateFormattedPlanName(
                                    membershipPlanType,
                                    months,
                                    sessions
                            );

                            if (displayName != null && amount != null) {
                                paymentList.add(new PaymentHistoryItem(
                                        displayName,
                                        amount,
                                        paymentMethod != null ? paymentMethod : "Unknown",
                                        paymentStatus != null ? paymentStatus : "paid",
                                        doc.getTimestamp("timestamp"),
                                        doc.getTimestamp("startDate"),
                                        doc.getTimestamp("expirationDate"),
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
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private String generateFormattedPlanName(String type, Long months, Long sessions) {
        if (type == null) return "Unknown Plan";

        int monthsVal = (months != null) ? months.intValue() : 0;
        int sessionsVal = (sessions != null) ? sessions.intValue() : 0;

        if ("Daily".equals(type) || monthsVal == 0) {
            return "Daily";
        }

        if (sessionsVal == 0) {
            if (monthsVal == 1) return "Standard Monthly";
            else if (monthsVal == 3) return "Standard 3 Months";
            else if (monthsVal == 6) return "Standard 6 Months";
            else if (monthsVal == 12) return "Standard Annual";
        }

        if (sessionsVal > 0) {
            if (monthsVal == 1) return "Monthly with " + sessionsVal + " PT";
            else if (monthsVal == 3) return "3 Months with " + sessionsVal + " PT";
            else if (monthsVal == 6) return "6 Months with " + sessionsVal + " PT";
            else if (monthsVal == 12) return "Annual with " + sessionsVal + " PT";
        }

        return type;
    }

    @Override
    public void onBackPressed() {
        if (isSelectionMode) {
            exitSelectionMode();
        } else {
            super.onBackPressed();
            overridePendingTransition(0, 0);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}