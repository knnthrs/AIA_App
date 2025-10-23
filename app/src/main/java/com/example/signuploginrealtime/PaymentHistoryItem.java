package com.example.signuploginrealtime;

import com.google.firebase.Timestamp;


public class PaymentHistoryItem {
    public String planLabel;
    public double amount;
    public String paymentMethod;
    public String paymentStatus;
    public Timestamp timestamp;
    public Timestamp membershipStartDate;
    public Timestamp membershipExpirationDate;

    // No-arg constructor required by Firestore deserialization
    public PaymentHistoryItem() { }

    // 4-arg constructor (matches your current usage)
    public PaymentHistoryItem(String planLabel, double amount, String paymentMethod, String paymentStatus) {
        this.planLabel = planLabel;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
    }

    // Full constructor (optional — useful later)
    public PaymentHistoryItem(String planLabel, double amount, String paymentMethod, String paymentStatus,
                              Timestamp timestamp, Timestamp membershipStartDate, Timestamp membershipExpirationDate) {
        this.planLabel = planLabel;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.timestamp = timestamp;
        this.membershipStartDate = membershipStartDate;
        this.membershipExpirationDate = membershipExpirationDate;
    }
}
