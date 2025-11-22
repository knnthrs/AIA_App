# 📸 TESTING GUIDE: Membership Expiration Notifications

## 🎯 GOAL: Get Screenshots of Expiration Notifications

This guide will help you trigger and screenshot the membership expiration notifications.

---

## 🧪 TEST SETUP METHODS

### Method 1: Manual Firestore Database Modification (FASTEST)

#### **Step 1: Set Up Test User**
1. **Open Firebase Console** → Go to your project
2. **Navigate to Firestore Database** 
3. **Find your user's membership document** in the `memberships` collection
4. **Note down the document ID** (usually the user's UID)

#### **Step 2: Trigger "3 Days Before Expiration" Notification**
1. **Calculate target date**: 3 days from today = **November 25, 2025**
2. **In Firestore Console**:
   - Go to `memberships` collection
   - Find your user's membership document
   - Edit the `membershipExpirationDate` field
   - Set it to: **November 25, 2025 at 11:59 PM**

```javascript
// In Firestore, set membershipExpirationDate to:
Timestamp: November 25, 2025 at 11:59:00 PM UTC+8
```

#### **Step 3: Trigger Cloud Function Manually**
```bash
# Run this in your terminal to trigger the Cloud Function
firebase functions:shell
> expireMemberships()
```

OR wait for the next midnight (it runs automatically)

#### **Step 4: Test the App**
1. **Open your app**
2. **Should see**: Local notification "Membership Expiring Soon - Your membership expires in 3 days..."
3. **Screenshot this notification** 📸

---

### Method 2: Set Up "1 Day Before" Test

#### **Repeat above but set expiration to November 23, 2025**:
```javascript
// In Firestore, set membershipExpirationDate to:
Timestamp: November 23, 2025 at 11:59:00 PM UTC+8
```

**Expected Result**: "Membership Expires Tomorrow" notification

---

### Method 3: Set Up "Expired" Test

#### **Set expiration to past date**:
```javascript
// In Firestore, set membershipExpirationDate to:
Timestamp: November 21, 2025 at 11:59:00 PM UTC+8 (yesterday)
```

**Expected Result**: "Membership Expired" notification

---

## 🔧 DETAILED TESTING STEPS

### Test Scenario A: "3 Days Before Expiration"

#### **Setup**:
1. **Open Firebase Console** → Firestore Database
2. **Navigate to**: `memberships` collection → Your user's document
3. **Edit field**: `membershipExpirationDate`
4. **Set to**: `November 25, 2025 11:59:00 PM GMT+8`
5. **Ensure**: `membershipStatus = "active"`

#### **Trigger Notification**:
```bash
# Option 1: Manual trigger (immediate)
firebase functions:shell
> expireMemberships()

# Option 2: Wait for automatic trigger (next midnight)
# Cloud Function runs automatically at midnight Philippine time
```

#### **Test the App**:
1. **Force close** your app completely
2. **Reopen** the app
3. **Expected result**:
   - Android notification appears in notification tray
   - Title: "Membership Expiring Soon"
   - Message: "Your membership expires in 3 days. Don't miss out - renew today!"

#### **Screenshot Checklist**:
- 📸 Notification in Android notification tray
- 📸 In-app notification (if you have notification center)
- 📸 Android Logcat showing: `"Created expiration notification for 3 days"`

---

### Test Scenario B: "1 Day Before Expiration"

#### **Setup**:
1. **Change expiration date** to: `November 23, 2025 11:59:00 PM GMT+8`
2. **Trigger Cloud Function** (same as above)
3. **Reopen app**

#### **Expected Notification**:
- Title: "Membership Expires Tomorrow"
- Message: "Your membership expires tomorrow! Renew now to avoid service interruption."

---

### Test Scenario C: "Expired Membership"

#### **Setup**:
1. **Change expiration date** to: `November 21, 2025 11:59:00 PM GMT+8` (past date)
2. **Trigger Cloud Function**
3. **Reopen app**

#### **Expected Results**:
- **Membership status** changes to "expired" in Firestore
- **Android notification**: 
  - Title: "Membership Expired"
  - Message: "Your membership has expired. Please renew to continue accessing all features."

---

## 📱 HOW TO CAPTURE SCREENSHOTS

### Android Notification Screenshots:
1. **When notification appears**:
   - Pull down notification panel
   - Screenshot the notification
2. **Alternative**: Use `adb` to capture:
```bash
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png
```

### Logcat Evidence:
```bash
# Capture logs showing notification creation
adb logcat | findstr "MembershipExpiration" > logs.txt
```

---

## ⚡ QUICK TEST SETUP (5-MINUTE METHOD)

### **Fastest Way to See All Notifications**:

#### **Step 1**: Set expiration to 3 days from now
```javascript
// Firestore: membershipExpirationDate = November 25, 2025 11:59 PM
```

#### **Step 2**: Trigger function and test
```bash
firebase functions:shell
> expireMemberships()
```

#### **Step 3**: Open app and screenshot

#### **Step 4**: Change to 1 day from now
```javascript
// Firestore: membershipExpirationDate = November 23, 2025 11:59 PM
```

#### **Step 5**: Trigger function and test again

#### **Step 6**: Change to past date
```javascript
// Firestore: membershipExpirationDate = November 21, 2025 11:59 PM
```

#### **Step 7**: Trigger function and test expired notification

---

## 🔍 DEBUGGING / VERIFICATION

### Check if Cloud Function Created Notifications:
```javascript
// In Firebase Console → Firestore
// Look in "notifications" collection for documents with:
{
  "type": "membership_expiration",
  "userId": "your-user-id",
  "daysUntilExpiration": 3, // or 1
  "timestamp": "recent date"
}
```

### Check Android Logs:
```bash
adb logcat | findstr "WarmUpExerciseSelector\|MembershipExpiration\|NotificationHelper"
```

### Expected Log Messages:
```
D/MembershipExpiration: Membership expires in 3 days
D/MembershipExpiration: Created expiration notification for 3 days
D/NotificationHelper: Notification created successfully
```

---

## 📸 SCREENSHOT TARGETS

### **Screenshot 1**: "3 Days Before" Notification
- **Setup**: Expiration = Nov 25, 2025
- **Expected**: "Membership Expiring Soon - expires in 3 days"
- **Location**: Android notification tray

### **Screenshot 2**: "1 Day Before" Notification  
- **Setup**: Expiration = Nov 23, 2025
- **Expected**: "Membership Expires Tomorrow"
- **Location**: Android notification tray

### **Screenshot 3**: "Expired" Notification
- **Setup**: Expiration = Nov 21, 2025 (past)
- **Expected**: "Membership Expired"
- **Location**: Android notification tray

### **Screenshot 4**: Firestore Evidence
- **Show**: notifications collection with created notifications
- **Location**: Firebase Console

### **Screenshot 5**: Logcat Evidence
- **Show**: Console logs confirming notification creation
- **Location**: Android Studio or terminal

---

## ⚠️ IMPORTANT NOTES

### **Restore Original Data**:
After testing, remember to restore your user's actual membership expiration date!

### **Test User**:
Use a test user account if possible to avoid affecting real membership data.

### **Timezone**:
Remember that Cloud Function runs on Philippine time (GMT+8), so set your test dates accordingly.

### **Force Close App**:
Always force close and reopen the app to trigger the MembershipExpirationService.

---

## 🎯 SUCCESS CRITERIA

### **You'll know testing is successful when**:
- ✅ You see Android notifications in the notification tray
- ✅ Different messages for 7/3/1 days and expired states
- ✅ Logs show "Created expiration notification for X days"
- ✅ Firestore contains notification documents
- ✅ You have clear screenshots showing the notifications

---

**Follow this guide and you'll have all the screenshots you need to demonstrate the membership expiration notification system!** 📸

*Testing Guide Created: November 22, 2025*
*Target: Screenshot documentation of notification system*
