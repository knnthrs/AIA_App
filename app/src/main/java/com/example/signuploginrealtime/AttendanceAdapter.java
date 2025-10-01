package com.example.signuploginrealtime;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder> {

    private List<AttendanceRecord> attendanceRecords;

    public AttendanceAdapter(List<AttendanceRecord> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_record, parent, false);
        return new AttendanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        AttendanceRecord record = attendanceRecords.get(position);

        // Format date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        holder.recordDate.setText(dateFormat.format(new Date(record.getTimeIn())));

        // Format time in
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        holder.timeInValue.setText(timeFormat.format(new Date(record.getTimeIn())));

        // Format time out and duration
        if (record.getTimeOut() > 0) {
            holder.timeOutValue.setText(timeFormat.format(new Date(record.getTimeOut())));

            // Calculate duration
            long durationMillis = record.getTimeOut() - record.getTimeIn();
            long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60;
            holder.durationValue.setText(String.format(Locale.getDefault(), "%dh %dm", hours, minutes));

            // Status: Completed
            holder.statusText.setText("COMPLETED");
            holder.statusText.setTextColor(0xFF388E3C);
            holder.statusBadge.setCardBackgroundColor(0xFFE8F5E8);
        } else {
            holder.timeOutValue.setText("--");
            holder.durationValue.setText("--");

            // Status: Active
            holder.statusText.setText("ACTIVE");
            holder.statusText.setTextColor(0xFF2196F3);
            holder.statusBadge.setCardBackgroundColor(0xFFE3F2FD);
        }
    }

    @Override
    public int getItemCount() {
        return attendanceRecords.size();
    }

    static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        TextView recordDate, timeInValue, timeOutValue, durationValue, statusText;
        CardView statusBadge;

        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            recordDate = itemView.findViewById(R.id.record_date);
            timeInValue = itemView.findViewById(R.id.time_in_value);
            timeOutValue = itemView.findViewById(R.id.time_out_value);
            durationValue = itemView.findViewById(R.id.duration_value);
            statusText = itemView.findViewById(R.id.status_text);
            statusBadge = itemView.findViewById(R.id.status_badge);
        }
    }
}