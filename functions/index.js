const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// This scheduled function runs once per day (Philippine time)
exports.expireMemberships = functions.pubsub
  .schedule("every 24 hours")
  .timeZone("Asia/Manila")
  .onRun(async (context) => {
    const now = admin.firestore.Timestamp.now();
    const membershipsRef = admin.firestore().collection("memberships");

    const snapshot = await membershipsRef
      .where("membershipStatus", "==", "active")
      .where("membershipExpirationDate", "<", now)
      .get();

    if (snapshot.empty) {
      console.log("✅ No memberships to expire today.");
      return null;
    }

    const batch = admin.firestore().batch();
    snapshot.forEach((doc) => {
      batch.update(doc.ref, { membershipStatus: "expired" });
    });

    await batch.commit();
    console.log(`✅ Expired ${snapshot.size} memberships.`);
    return null;
  });
