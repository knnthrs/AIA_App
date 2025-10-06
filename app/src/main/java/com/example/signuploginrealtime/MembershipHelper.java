package com.example.signuploginrealtime;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;

public class MembershipHelper {

    public interface MembershipStatusCallback {
        void onResult(boolean isActive, DocumentSnapshot membership);
        void onError(Exception e);
    }

    public interface MembershipDetailsCallback {
        void onResult(MembershipDetails details);
        void onError(Exception e);
    }

    public static class MembershipDetails {
        public boolean isActive;
        public String membershipId;
        public String planLabel;
        public String planType;
        public Date startDate;
        public Date expirationDate;
        public int sessionsRemaining;
        public long daysRemaining;

        public MembershipDetails() {
            this.isActive = false;
        }
    }

    /**
     * Check if a user has an active membership
     * @param userId The user's Firebase UID
     * @param callback Callback with result
     */
    public static void checkMembershipStatus(String userId, MembershipStatusCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("memberships")
                .whereEqualTo("userId", userId)
                .whereEqualTo("membershipStatus", "active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onResult(false, null);
                        return;
                    }

                    // Get the first active membership
                    DocumentSnapshot membership = queryDocumentSnapshots.getDocuments().get(0);

                    // Check if membership has expired
                    Timestamp expirationTimestamp = membership.getTimestamp("membershipExpirationDate");
                    if (expirationTimestamp != null) {
                        Date expirationDate = expirationTimestamp.toDate();
                        Date currentDate = new Date();

                        if (currentDate.after(expirationDate)) {
                            // Membership has expired - update status
                            membership.getReference().update("membershipStatus", "expired");
                            callback.onResult(false, null);
                        } else {
                            // Membership is still active
                            callback.onResult(true, membership);
                        }
                    } else {
                        callback.onResult(true, membership);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Get detailed membership information
     * @param userId The user's Firebase UID
     * @param callback Callback with membership details
     */
    public static void getMembershipDetails(String userId, MembershipDetailsCallback callback) {
        checkMembershipStatus(userId, new MembershipStatusCallback() {
            @Override
            public void onResult(boolean isActive, DocumentSnapshot membership) {
                MembershipDetails details = new MembershipDetails();
                details.isActive = isActive;

                if (isActive && membership != null) {
                    details.membershipId = membership.getId();
                    details.planLabel = membership.getString("membershipPlanLabel");
                    details.planType = membership.getString("membershipPlanType");

                    Timestamp startTimestamp = membership.getTimestamp("membershipStartDate");
                    if (startTimestamp != null) {
                        details.startDate = startTimestamp.toDate();
                    }

                    Timestamp expirationTimestamp = membership.getTimestamp("membershipExpirationDate");
                    if (expirationTimestamp != null) {
                        details.expirationDate = expirationTimestamp.toDate();

                        // Calculate days remaining
                        long diff = details.expirationDate.getTime() - new Date().getTime();
                        details.daysRemaining = diff / (1000 * 60 * 60 * 24);
                    }

                    Long sessionsRemaining = membership.getLong("sessionsRemaining");
                    if (sessionsRemaining != null) {
                        details.sessionsRemaining = sessionsRemaining.intValue();
                    }
                }

                callback.onResult(details);
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Update remaining PT sessions
     * @param membershipId The membership document ID
     * @param sessionsUsed Number of sessions to deduct
     */
    public static void usePTSession(String membershipId, int sessionsUsed) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("memberships")
                .document(membershipId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long currentSessions = documentSnapshot.getLong("sessionsRemaining");
                        if (currentSessions != null && currentSessions > 0) {
                            int newSessions = Math.max(0, currentSessions.intValue() - sessionsUsed);
                            documentSnapshot.getReference().update("sessionsRemaining", newSessions);
                        }
                    }
                });
    }

    /**
     * Automatically expire memberships (call this periodically or on app start)
     */
    public static void expireOldMemberships() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Date currentDate = new Date();
        Timestamp currentTimestamp = new Timestamp(currentDate);

        db.collection("memberships")
                .whereEqualTo("membershipStatus", "active")
                .whereLessThan("membershipExpirationDate", currentTimestamp)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        doc.getReference().update("membershipStatus", "expired");
                    }
                });
    }
}