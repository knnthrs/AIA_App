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
    } else {
      const batch = admin.firestore().batch();
      snapshot.forEach((doc) => {
        batch.update(doc.ref, { membershipStatus: "expired" });
      });

      await batch.commit();
      console.log(`✅ Expired ${snapshot.size} memberships.`);
    }

    // Check for upcoming expirations and send notifications
    await checkExpirationNotifications();

    return null;
  });

// New function to check for upcoming membership expirations and send notifications
async function checkExpirationNotifications() {
  const now = admin.firestore.Timestamp.now();
  const nowDate = now.toDate();

  // Calculate dates for 7, 3, and 1 day warnings
  const in7Days = new Date(nowDate.getTime() + (7 * 24 * 60 * 60 * 1000));
  const in3Days = new Date(nowDate.getTime() + (3 * 24 * 60 * 60 * 1000));
  const in1Day = new Date(nowDate.getTime() + (1 * 24 * 60 * 60 * 1000));

  const membershipsRef = admin.firestore().collection("memberships");

  // Check for memberships expiring in 7 days
  await sendExpirationNotifications(membershipsRef, in7Days, 7);

  // Check for memberships expiring in 3 days
  await sendExpirationNotifications(membershipsRef, in3Days, 3);

  // Check for memberships expiring in 1 day
  await sendExpirationNotifications(membershipsRef, in1Day, 1);
}

async function sendExpirationNotifications(membershipsRef, targetDate, daysUntilExpiration) {
  // Create date range for the target day (start and end of day)
  const startOfDay = new Date(targetDate);
  startOfDay.setHours(0, 0, 0, 0);

  const endOfDay = new Date(targetDate);
  endOfDay.setHours(23, 59, 59, 999);

  const startTimestamp = admin.firestore.Timestamp.fromDate(startOfDay);
  const endTimestamp = admin.firestore.Timestamp.fromDate(endOfDay);

  try {
    const snapshot = await membershipsRef
      .where("membershipStatus", "==", "active")
      .where("membershipExpirationDate", ">=", startTimestamp)
      .where("membershipExpirationDate", "<=", endTimestamp)
      .get();

    if (snapshot.empty) {
      console.log(`✅ No memberships expiring in ${daysUntilExpiration} days.`);
      return;
    }

    const notifications = [];

    snapshot.forEach((doc) => {
      const membership = doc.data();
      const userId = membership.userId;

      if (!userId) {
        console.log(`⚠️ Membership ${doc.id} has no userId, skipping notification`);
        return;
      }

      // Create notification message based on days until expiration
      let title, message;
      if (daysUntilExpiration === 7) {
        title = "Membership Expiring Soon";
        message = "Your membership expires in 7 days. Renew now to continue enjoying our services!";
      } else if (daysUntilExpiration === 3) {
        title = "Membership Expiring Soon";
        message = "Your membership expires in 3 days. Don't miss out - renew today!";
      } else if (daysUntilExpiration === 1) {
        title = "Membership Expires Tomorrow";
        message = "Your membership expires tomorrow! Renew now to avoid service interruption.";
      }

      const notificationData = {
        userId: userId,
        title: title,
        message: message,
        type: "membership_expiration",
        timestamp: admin.firestore.Timestamp.now(),
        read: false,
        daysUntilExpiration: daysUntilExpiration,
        membershipId: doc.id
      };

      notifications.push(notificationData);
    });

    // Batch create all notifications
    if (notifications.length > 0) {
      const notificationsRef = admin.firestore().collection("notifications");
      const batch = admin.firestore().batch();

      notifications.forEach((notificationData) => {
        const newNotificationRef = notificationsRef.doc();
        batch.set(newNotificationRef, notificationData);
      });

      await batch.commit();
      console.log(`✅ Created ${notifications.length} expiration notifications for ${daysUntilExpiration} days warning.`);
    }

  } catch (error) {
    console.error(`❌ Error checking memberships expiring in ${daysUntilExpiration} days:`, error);
  }
}

