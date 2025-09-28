package com.example.signuploginrealtime;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";

    private static final FirebaseFirestore mDatabase = FirebaseFirestore.getInstance();

    // 🔔 needed for showing local notifications
    private static final String CHANNEL_ID = "general_channel";
    private static Context appContext;

    // initialize once in MainActivity or Application
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static void createWorkoutReminder(String userId, String workoutType) {
        String title = "Workout Reminder";
        String message = "Time for your " + workoutType + " workout! Let's get moving.";
        createNotification(userId, title, message, "workout");
    }

    public static void createAchievementNotification(String userId, String achievementTitle, int count, String period) {
        String title = "New Achievement!";
        String message = "Congratulations! You've " + achievementTitle + " " + count + " times " + period + ".";
        createNotification(userId, title, message, "achievement");
    }

    public static void createPromoNotification(String userId, String promoTitle, String discount) {
        String title = promoTitle;
        String message = "Get " + discount + " - Limited time only!";
        createNotification(userId, title, message, "promo");
    }

    public static void createGeneralNotification(String userId, String title, String message) {
        createNotification(userId, title, message, "general");
    }

    private static void createNotification(String userId, String title, String message, String type) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot create notification, userId is null/empty");
            return;
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("type", type);
        notification.put("timestamp", Timestamp.now());
        notification.put("read", false);

        mDatabase.collection(COLLECTION_NOTIFICATIONS)
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Notification created successfully: " + documentReference.getId());

                    // 🔔 show local notification too
                    if (appContext != null) {
                        showNotification(appContext, title, message);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error creating notification", e));
    }

    public static void createSampleNotifications(String userId) {
        createWorkoutReminder(userId, "Upper Body");
        createAchievementNotification(userId, "completed workouts", 15, "this month");
        createPromoNotification(userId, "Special Offer!", "50% OFF on Personal Training");
        createGeneralNotification(userId, "Welcome!", "Welcome to FitnessPro! Start your journey today.");
    }

    // ✅ this actually shows notification on device
    static void showNotification(Context context, String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // ⚠️ add a proper icon in res/drawable
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);

        // ✅ check permission safely
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping notification");
            return;
        }

        managerCompat.notify((int) System.currentTimeMillis(), builder.build());
    }
}
