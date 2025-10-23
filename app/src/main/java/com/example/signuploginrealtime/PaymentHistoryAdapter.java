package com.example.signuploginrealtime;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PaymentHistoryAdapter extends RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder> {

    private final List<PaymentHistoryItem> paymentList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());

    public PaymentHistoryAdapter(List<PaymentHistoryItem> paymentList) {
        this.paymentList = paymentList;
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

        // Date
        if (item.timestamp != null) {
            Date d = item.timestamp.toDate();
            holder.tvDate.setText(dateFormat.format(d));
        } else {
            holder.tvDate.setText("");
        }

        // Membership period
        if (item.membershipStartDate != null && item.membershipExpirationDate != null) {
            String start = dateFormat.format(item.membershipStartDate.toDate());
            String end = dateFormat.format(item.membershipExpirationDate.toDate());
            holder.tvMembershipPeriod.setText(String.format("Membership: %s - %s", start, end));
            holder.tvMembershipPeriod.setVisibility(View.VISIBLE);
        } else {
            holder.tvMembershipPeriod.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return paymentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlanName, tvAmount, tvDate, tvPaymentMethod, tvStatus, tvMembershipPeriod;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlanName = itemView.findViewById(R.id.tv_plan_name);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvPaymentMethod = itemView.findViewById(R.id.tv_payment_method);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvMembershipPeriod = itemView.findViewById(R.id.tv_membership_period);
        }
    }
}
