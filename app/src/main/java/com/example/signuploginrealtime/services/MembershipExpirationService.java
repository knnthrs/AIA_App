package com.example.signuploginrealtime.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.signuploginrealtime.NotificationHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Service to check user's membership expiration status and show local notifications
 * This runs when the app starts to notify users of upcoming expirations
 */
public class MembershipExpirationService extends Service {

    private static final String TAG = "MembershipExpiration";
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    @Override
    public void onCreate() {
        super.onCreate();
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkUserMembershipExpiration();
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void checkUserMembershipExpiration() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.d(TAG, "No authenticated user, skipping membership check");
            stopSelf();
            return;
        }

        String userId = user.getUid();

        // Check user's current membership
        firestore.collection("memberships")
                .whereEqualTo("userId", userId)
                .whereEqualTo("membershipStatus", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No active membership found for user");
                        stopSelf();
                        return;
                    }

                    // Get the most recent active membership
                    DocumentSnapshot membership = queryDocumentSnapshots.getDocuments().get(0);
                    Timestamp expirationDate = membership.getTimestamp("membershipExpirationDate");

                    if (expirationDate != null) {
                        checkAndNotifyExpiration(userId, expirationDate);
                    }

                    stopSelf();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking membership expiration", e);
                    stopSelf();
                });
    }

    private void checkAndNotifyExpiration(String userId, Timestamp expirationDate) {
        Date now = new Date();
        Date expDate = expirationDate.toDate();

        // Calculate days until expiration
        long diffInMillis = expDate.getTime() - now.getTime();
        long daysUntilExpiration = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);

        Log.d(TAG, "Membership expires in " + daysUntilExpiration + " days");

        // Show notification if expiring within 7 days
        if (daysUntilExpiration <= 7 && daysUntilExpiration > 0) {
            if (daysUntilExpiration == 7 || daysUntilExpiration == 3 || daysUntilExpiration == 1) {
                // Only show notifications on key days (7, 3, 1) to avoid spam
                NotificationHelper.createMembershipExpirationNotification(userId, (int) daysUntilExpiration);
                Log.d(TAG, "Created expiration notification for " + daysUntilExpiration + " days");
            }
        } else if (daysUntilExpiration <= 0) {
            // Membership has already expired
            String title = "Membership Expired";
            String message = "Your membership has expired. Please renew to continue accessing all features.";
            NotificationHelper.createGeneralNotification(userId, title, message);
            Log.d(TAG, "Created expired membership notification");
        }
    }
}
