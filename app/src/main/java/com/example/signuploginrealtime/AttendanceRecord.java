package com.example.signuploginrealtime;

public class AttendanceRecord {
    private long timeIn;
    private long timeOut;
    private String status; // "active" or "completed"

    // Required empty constructor for Firestore
    public AttendanceRecord() {
    }

    public AttendanceRecord(long timeIn, long timeOut, String status) {
        this.timeIn = timeIn;
        this.timeOut = timeOut;
        this.status = status;
    }

    public long getTimeIn() {
        return timeIn;
    }

    public void setTimeIn(long timeIn) {
        this.timeIn = timeIn;
    }

    public long getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(long timeOut) {
        this.timeOut = timeOut;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}