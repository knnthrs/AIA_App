package com.example.signuploginrealtime;

import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;

public class NotificationItem {
    private String id;
    private String title;
    private String message;
    private String type; // "workout", "achievement", "promo", "general"
    private long timestamp;
    private boolean isRead;
    private String userId;

    // Default constructor for Firestore
    public NotificationItem() {}

    public NotificationItem(String id, String title, String message, String type, long timestamp, boolean isRead) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    public NotificationItem(String userId, String title, String message, String type) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }
    public String getUserId() { return userId; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setType(String type) { this.type = type; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setRead(boolean read) { isRead = read; }
    public void setUserId(String userId) { this.userId = userId; }

    // Convert to Map for Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("title", title);
        map.put("message", message);
        map.put("type", type);
        map.put("timestamp", timestamp);
        map.put("read", isRead);
        return map;
    }

    // Get icon resource based on type
    public int getIconResource() {
        switch (type) {
            case "workout":
                return R.drawable.ic_dumbell;
            case "achievement":
                return R.drawable.ic_achievement;
            case "promo":
                return R.drawable.ic_bell; // can change to promo icon later
            default:
                return R.drawable.ic_bell;
        }
    }

    // ✅ Return actual int colors instead of strings
    public int getBackgroundColorInt() {
        switch (type) {
            case "workout":
                return Color.parseColor("#FFF0F5"); // Light pink
            case "achievement":
                return Color.parseColor("#FFF4E6"); // Light orange
            case "promo":
                return Color.parseColor("#FFE8F0"); // Light pink
            default:
                return Color.parseColor("#F0F0FF"); // Light blue
        }
    }

    public int getIconColorInt() {
        switch (type) {
            case "workout":
                return Color.parseColor("#FF6B9D"); // Pink
            case "achievement":
                return Color.parseColor("#FF9F43"); // Orange
            case "promo":
                return Color.parseColor("#E84393"); // Deep pink
            default:
                return Color.parseColor("#6C5CE7"); // Purple
        }
    }
}
