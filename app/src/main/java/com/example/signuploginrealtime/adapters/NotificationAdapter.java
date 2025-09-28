package com.example.signuploginrealtime.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.NotificationItem;
import com.example.signuploginrealtime.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<NotificationItem> notifications = new ArrayList<>();
    private final OnNotificationClickListener clickListener;
    private Context context;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem notification);
    }

    public NotificationAdapter(List<NotificationItem> notificationsList, OnNotificationClickListener clickListener) {
        this.clickListener = clickListener;
        // ✅ ADD THIS LINE:
        if (notificationsList != null) {
            this.notifications.addAll(notificationsList);
        }
    }
    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem notification = notifications.get(position);

        // Set text content
        holder.titleTextView.setText(notification.getTitle());
        holder.messageTextView.setText(notification.getMessage());
        holder.timeTextView.setText(getRelativeTime(notification.getTimestamp()));

        // Set icon + colors
        holder.iconImageView.setImageResource(notification.getIconResource());
        holder.iconCardView.setCardBackgroundColor(notification.getBackgroundColorInt());
        holder.iconImageView.setColorFilter(notification.getIconColorInt());

        // Read/unread state
        if (notification.isRead()) {
            holder.unreadIndicator.setVisibility(View.GONE);
            holder.cardView.setAlpha(0.7f);
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.gray));
        } else {
            holder.unreadIndicator.setVisibility(View.VISIBLE);
            holder.unreadIndicator.setBackgroundColor(notification.getIconColorInt());
            holder.cardView.setAlpha(1.0f);
            holder.titleTextView.setTextColor(ContextCompat.getColor(context, R.color.black));
        }

        // Click listener (mark read + notify outside)
        holder.cardView.setOnClickListener(v -> {
            if (!notification.isRead()) {
                notification.setRead(true);
                notifyItemChanged(holder.getAdapterPosition());
            }
            if (clickListener != null) {
                clickListener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    // --- Helpers ---
    private String getRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Just now";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (diff < TimeUnit.DAYS.toMillis(7)) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days + (days == 1 ? " day ago" : " days ago");
        } else {
            Date date = new Date(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat(
                    (new Date().getYear() == date.getYear()) ? "MMM dd" : "MMM dd, yyyy",
                    Locale.getDefault()
            );
            return sdf.format(date);
        }
    }

    // --- Efficient list updates using DiffUtil ---
    public void updateNotifications(List<NotificationItem> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return notifications.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return notifications.get(oldItemPosition).getId()
                        .equals(newList.get(newItemPosition).getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                NotificationItem oldItem = notifications.get(oldItemPosition);
                NotificationItem newItem = newList.get(newItemPosition);
                return oldItem.isRead() == newItem.isRead()
                        && oldItem.getTitle().equals(newItem.getTitle())
                        && oldItem.getMessage().equals(newItem.getMessage())
                        && oldItem.getTimestamp() == newItem.getTimestamp();
            }
        });

        notifications.clear();
        notifications.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        CardView iconCardView;
        ImageView iconImageView;
        TextView titleTextView;
        TextView messageTextView;
        TextView timeTextView;
        View unreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.notification_card);
            iconCardView = itemView.findViewById(R.id.icon_card);
            iconImageView = itemView.findViewById(R.id.notification_icon);
            titleTextView = itemView.findViewById(R.id.notification_title);
            messageTextView = itemView.findViewById(R.id.notification_message);
            timeTextView = itemView.findViewById(R.id.notification_time);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
        }
    }
}
