package com.example.signuploginrealtime;

import android.view.LayoutInflater;
import android.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PaymentHistoryAdapter extends RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder> {

    private final List<PaymentHistoryItem> paymentList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private SelectionCallback callback;

    private boolean selectionMode = false;
    private Set<String> selectedIds = new HashSet<>();

    public interface SelectionCallback {
        void onSelectionChanged();
    }

    public PaymentHistoryAdapter(List<PaymentHistoryItem> paymentList, FirebaseAuth auth,
                                 FirebaseFirestore firestore, SelectionCallback callback) {
        this.paymentList = paymentList;
        this.mAuth = auth;
        this.firestore = firestore;
        this.callback = callback;
    }

    @NonNull
    @Override
    public PaymentHistoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentHistoryAdapter.ViewHolder holder, int position) {
        PaymentHistoryItem item = paymentList.get(position);

        holder.tvPlanName.setText(item.planLabel != null ? item.planLabel : "Plan");
        holder.tvAmount.setText("₱" + String.format(Locale.getDefault(), "%.2f", item.amount));
        holder.tvPaymentMethod.setText(item.paymentMethod != null ? item.paymentMethod : "Unknown");
        holder.tvStatus.setText(item.paymentStatus != null ? item.paymentStatus : "Paid");

        if (item.timestamp != null) {
            Date d = item.timestamp.toDate();
            holder.tvDate.setText(dateFormat.format(d));
        } else {
            holder.tvDate.setText("");
        }

        if (item.membershipStartDate != null && item.membershipExpirationDate != null) {
            String start = dateFormat.format(item.membershipStartDate.toDate());
            String end = dateFormat.format(item.membershipExpirationDate.toDate());
            holder.tvMembershipPeriod.setText(String.format("Membership: %s - %s", start, end));
            holder.tvMembershipPeriod.setVisibility(View.VISIBLE);
        } else {
            holder.tvMembershipPeriod.setVisibility(View.GONE);
        }

        // Checkbox logic
        if (selectionMode) {
            holder.checkbox.setVisibility(View.VISIBLE);
            holder.checkbox.setChecked(selectedIds.contains(item.documentId));
        } else {
            holder.checkbox.setVisibility(View.GONE);
        }

        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedIds.add(item.documentId);
            } else {
                selectedIds.remove(item.documentId);
            }
            if (callback != null) {
                callback.onSelectionChanged();
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                holder.checkbox.setChecked(!holder.checkbox.isChecked());
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                AlertDialog dialog = new AlertDialog.Builder(v.getContext(), R.style.RoundedAlertDialog)
                        .setTitle("Delete Payment")
                        .setMessage("Are you sure you want to delete this payment record?")
                        .setPositiveButton("Delete", (d, which) -> {
                            deletePaymentRecord(item.documentId);
                        })
                        .setNegativeButton("Cancel", null)
                        .create();
                dialog.show();
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return paymentList.size();
    }

    public void setSelectionMode(boolean enabled) {
        this.selectionMode = enabled;
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
    }

    public List<PaymentHistoryItem> getSelectedItems() {
        List<PaymentHistoryItem> selected = new ArrayList<>();
        for (PaymentHistoryItem item : paymentList) {
            if (selectedIds.contains(item.documentId)) {
                selected.add(item);
            }
        }
        return selected;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlanName, tvAmount, tvDate, tvPaymentMethod, tvStatus, tvMembershipPeriod;
        CheckBox checkbox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlanName = itemView.findViewById(R.id.tv_plan_name);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvPaymentMethod = itemView.findViewById(R.id.tv_payment_method);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvMembershipPeriod = itemView.findViewById(R.id.tv_membership_period);
            checkbox = itemView.findViewById(R.id.checkbox_select);
        }
    }

    private void deletePaymentRecord(String documentId) {
        if (mAuth.getCurrentUser() == null || documentId == null) return;

        firestore.collection("users")
                .document(mAuth.getCurrentUser().getUid())
                .collection("paymentHistory")
                .document(documentId)
                .delete();
    }
}