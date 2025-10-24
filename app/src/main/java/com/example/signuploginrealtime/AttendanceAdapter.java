package com.example.signuploginrealtime;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder> {

    private List<AttendanceRecord> attendanceRecords;
    private boolean isDeleteMode = false;
    private Set<Integer> selectedPositions = new HashSet<>();
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(int position);
    }

    public AttendanceAdapter(List<AttendanceRecord> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }

    public void setOnClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setDeleteMode(boolean deleteMode) {
        this.isDeleteMode = deleteMode;
        notifyDataSetChanged();
    }

    public void selectItem(int position) {
        selectedPositions.add(position);
        notifyItemChanged(position);
    }

    public void deselectItem(int position) {
        selectedPositions.remove(position);
        notifyItemChanged(position);
    }

    public void clearSelections() {
        selectedPositions.clear();
        notifyDataSetChanged();
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

        // Show/hide checkbox based on delete mode
        if (isDeleteMode) {
            holder.checkbox.setVisibility(View.VISIBLE);
            holder.checkbox.setChecked(selectedPositions.contains(position));
        } else {
            holder.checkbox.setVisibility(View.GONE);
            holder.checkbox.setChecked(false);
        }

        // Highlight selected items - apply to the CardView parent
        View parentCard = (View) holder.itemView;
        if (selectedPositions.contains(position)) {
            parentCard.setBackgroundColor(0xFFE3F2FD); // Light blue
            holder.itemView.setAlpha(0.7f); // Add slight transparency
        } else {
            parentCard.setBackgroundColor(0xFFF8F8F8); // Match XML default
            holder.itemView.setAlpha(1.0f); // Full opacity
        }

        // Format date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        holder.recordDate.setText(dateFormat.format(new Date(record.getTimeIn())));

        // Format time in
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        holder.timeInValue.setText(timeFormat.format(new Date(record.getTimeIn())));

        // Format time out and duration
        if (record.getTimeOut() > 0) {
            holder.timeOutValue.setText(timeFormat.format(new Date(record.getTimeOut())));

            long durationMillis = record.getTimeOut() - record.getTimeIn();
            long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60;
            holder.durationValue.setText(String.format(Locale.getDefault(), "%dh %dm", hours, minutes));

            holder.statusText.setText("COMPLETED");
            holder.statusText.setTextColor(0xFF388E3C);
            holder.statusBadge.setCardBackgroundColor(0xFFE8F5E8);
        } else {
            holder.timeOutValue.setText("--");
            holder.durationValue.setText("--");

            holder.statusText.setText("ACTIVE");
            holder.statusText.setTextColor(0xFF2196F3);
            holder.statusBadge.setCardBackgroundColor(0xFFE3F2FD);
        }

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                return longClickListener.onItemLongClick(position);
            }
            return false;
        });

        holder.checkbox.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return attendanceRecords.size();
    }

    static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        TextView recordDate, timeInValue, timeOutValue, durationValue, statusText;
        CardView statusBadge;
        CheckBox checkbox;

        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            recordDate = itemView.findViewById(R.id.record_date);
            timeInValue = itemView.findViewById(R.id.time_in_value);
            timeOutValue = itemView.findViewById(R.id.time_out_value);
            durationValue = itemView.findViewById(R.id.duration_value);
            statusText = itemView.findViewById(R.id.status_text);
            statusBadge = itemView.findViewById(R.id.status_badge);
            checkbox = itemView.findViewById(R.id.checkbox_select);
        }
    }
}