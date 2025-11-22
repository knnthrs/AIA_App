// TESTING SCRIPT: Set Up Membership Expiration Test Data
// Run this in Firebase Console or your testing environment

// Test Case 1: 3 Days Before Expiration
function setupTest3DaysExpiration(userId) {
    const db = firebase.firestore();

    // Calculate 3 days from today (November 25, 2025)
    const expirationDate = new Date(2025, 10, 25, 23, 59, 59); // Month is 0-indexed, so 10 = November

    return db.collection('memberships').doc(userId).update({
        membershipExpirationDate: firebase.firestore.Timestamp.fromDate(expirationDate),
        membershipStatus: 'active'
    });
}

// Test Case 2: 1 Day Before Expiration
function setupTest1DayExpiration(userId) {
    const db = firebase.firestore();

    // Calculate 1 day from today (November 23, 2025)
    const expirationDate = new Date(2025, 10, 23, 23, 59, 59);

    return db.collection('memberships').doc(userId).update({
        membershipExpirationDate: firebase.firestore.Timestamp.fromDate(expirationDate),
        membershipStatus: 'active'
    });
}

// Test Case 3: Expired Membership
function setupTestExpired(userId) {
    const db = firebase.firestore();

    // Set to yesterday (November 21, 2025)
    const expirationDate = new Date(2025, 10, 21, 23, 59, 59);

    return db.collection('memberships').doc(userId).update({
        membershipExpirationDate: firebase.firestore.Timestamp.fromDate(expirationDate),
        membershipStatus: 'active' // Will change to 'expired' when Cloud Function runs
    });
}

// Usage Examples:
// setupTest3DaysExpiration('your-user-id');
// setupTest1DayExpiration('your-user-id');
// setupTestExpired('your-user-id');

console.log('Test setup functions ready!');
console.log('Current date for reference:', new Date().toDateString());
console.log('3 days test date:', new Date(2025, 10, 25).toDateString());
console.log('1 day test date:', new Date(2025, 10, 23).toDateString());
console.log('Expired test date:', new Date(2025, 10, 21).toDateString());
