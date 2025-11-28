package com.example.signuploginrealtime;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signuploginrealtime.adapters.NotificationAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class Notification extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private TextView btnClearAll;
    private NotificationAdapter adapter;
    private List<NotificationItem> notificationList = new ArrayList<>();
    private ListenerRegistration notificationListener;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        recyclerView = findViewById(R.id.notifications_recycler_view);
        emptyState = findViewById(R.id.empty_state);
        btnClearAll = findViewById(R.id.btn_clear_all);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new NotificationAdapter(notificationList, notification -> {
            markNotificationAsRead(notification);
            routeToActivity(notification);
        });
        recyclerView.setAdapter(adapter);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadNotificationsRealtime();
        } else {
            Log.e("NotificationActivity", "User not authenticated!");
            finish();
            return;
        }

        // Clear All button logic
        btnClearAll.setOnClickListener(v -> clearAllNotifications());

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void loadNotificationsRealtime() {
        CollectionReference notifRef = db.collection("notifications");

        notificationListener = notifRef.whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("NotificationActivity", "Listen failed.", e);
                        return;
                    }

                    notificationList.clear();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots) {
                            try {
                                NotificationItem notif = doc.toObject(NotificationItem.class);
                                if (notif != null) {
                                    notif.setId(doc.getId());
                                    notificationList.add(notif);
                                } else {
                                    Log.w("NotificationActivity", "Null notification from doc: " + doc.getId());
                                }
                            } catch (Exception ex) {
                                Log.e("NotificationActivity", "Failed to parse notification doc: " + doc.getId(), ex);
                                // Continue to next document instead of crashing
                            }
                        }
                    }
                    adapter.updateNotifications(new ArrayList<>(notificationList));
                    toggleEmptyState();
                });
    }

    private void toggleEmptyState() {
        if (notificationList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void markNotificationAsRead(NotificationItem notification) {
        if (notification == null || notification.getId() == null) {
            Log.e("NotificationActivity", "Cannot mark as read: notification or ID is null");
            return;
        }

        if (!notification.isRead()) {
            db.collection("notifications")
                    .document(notification.getId())
                    .update("read", true)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("NotificationActivity", "Marked notification as read: " + notification.getId());
                    })
                    .addOnFailureListener(e -> {
                        Log.e("NotificationActivity", "Failed to mark notification as read", e);
                    });
            notification.setRead(true);

            // Only update this item in RecyclerView instead of all
            int index = notificationList.indexOf(notification);
            if (index != -1) {
                adapter.notifyItemChanged(index);
            }
        }
    }

    private void routeToActivity(NotificationItem notification) {
        if (notification == null) {
            Log.e("NotificationActivity", "Cannot route: notification is null");
            return;
        }

        String type = notification.getType();
        if (type == null) type = "";

        android.content.Intent intent = null;

        switch (type) {
            case "promo":
                intent = new android.content.Intent(this, Promo.class);
                // Pass the promo image URL so Promo activity can load it
                String promoUrl = notification.getPromoImageUrl();
                if (promoUrl != null && !promoUrl.isEmpty()) {
                    intent.putExtra("promoUrl", promoUrl);
                    Log.d("NotificationActivity", "Opening promo with URL: " + promoUrl);
                } else {
                    Log.w("NotificationActivity", "Promo notification missing promoImageUrl");
                }
                break;

            case "achievement":
                intent = new android.content.Intent(this, Achievement.class);
                break;

            case "workout_reminder":
            case "workout":
                intent = new android.content.Intent(this, WorkoutList.class);
                break;

            case "membership_expired":
            case "expiring_soon":
            case "membership":
                intent = new android.content.Intent(this, SelectMembership.class);
                break;

            default:
                // Unknown type, don't route
                Log.d("NotificationActivity", "Unknown notification type: " + type);
                return;
        }

        if (intent != null) {
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }

    private void clearAllNotifications() {
        if (!notificationList.isEmpty()) {
            WriteBatch batch = db.batch();
            for (NotificationItem notif : notificationList) {
                if (notif != null && notif.getId() != null) {
                    batch.delete(db.collection("notifications").document(notif.getId()));
                }
            }

            batch.commit().addOnSuccessListener(aVoid -> {
                notificationList.clear();
                adapter.updateNotifications(new ArrayList<>(notificationList));
                toggleEmptyState();
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}