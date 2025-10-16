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
                            NotificationItem notif = doc.toObject(NotificationItem.class);
                            if (notif != null) {
                                notif.setId(doc.getId());
                                notificationList.add(notif);
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
        if (!notification.isRead()) {
            db.collection("notifications")
                    .document(notification.getId())
                    .update("read", true);
            notification.setRead(true);

            // Only update this item in RecyclerView instead of all
            int index = notificationList.indexOf(notification);
            if (index != -1) {
                adapter.notifyItemChanged(index);
            }
        }
    }

    private void clearAllNotifications() {
        if (!notificationList.isEmpty()) {
            WriteBatch batch = db.batch();
            for (NotificationItem notif : notificationList) {
                batch.delete(db.collection("notifications").document(notif.getId()));
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