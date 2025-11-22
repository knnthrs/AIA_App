# 📸 STEP-BY-STEP SCREENSHOT GUIDE

## 🎯 Get Screenshots of Membership Expiration Notifications

Follow these exact steps to trigger and capture the notifications:

---

## ⚡ QUICK START (15-Minute Method)

### **STEP 1: Find Your User ID**
1. Open your Android app
2. Go to Android Studio → Logcat
3. Filter for your app
4. Look for your user ID in the logs, OR:
5. Firebase Console → Authentication → Users → Copy your User UID

### **STEP 2: Backup Current Membership Data**
1. **Firebase Console** → **Firestore Database**
2. **Collections** → **memberships** → **[Your User ID]**
3. **Screenshot the current data** (so you can restore it later)
4. **Note down**:
   - Current `membershipExpirationDate`
   - Current `membershipStatus`

---

## 📸 TEST 1: "3 Days Before Expiration" Notification

### **Setup (2 minutes)**:
1. **Firebase Console** → **Firestore** → **memberships** → **[Your User ID]**
2. **Click "Edit Document"**
3. **Find** `membershipExpirationDate` field
4. **Change it to**: `November 25, 2025 at 11:59:00 PM GMT+8`
5. **Ensure** `membershipStatus` is `"active"`
6. **Click "Update"**

### **Trigger Cloud Function (1 minute)**:
```bash
# Open terminal in your project folder
firebase functions:shell

# Once shell opens, type:
expireMemberships()

# Wait for completion, should see:
# "✅ Created X expiration notifications for 3 days warning"
```

### **Test App (1 minute)**:
1. **Force close** your app completely
2. **Reopen** the app
3. **Wait 3-5 seconds**

### **Expected Result**:
📱 **Android notification should appear**:
- **Title**: "Membership Expiring Soon"  
- **Message**: "Your membership expires in 3 days. Don't miss out - renew today!"

### **📸 SCREENSHOT THIS** (from Android notification tray)

---

## 📸 TEST 2: "1 Day Before Expiration" Notification

### **Setup (30 seconds)**:
1. **Firebase Console** → **Firestore** → **memberships** → **[Your User ID]**
2. **Edit** `membershipExpirationDate` 
3. **Change to**: `November 23, 2025 at 11:59:00 PM GMT+8`
4. **Update**

### **Trigger (30 seconds)**:
```bash
# In Firebase shell (already open):
expireMemberships()
```

### **Test App (30 seconds)**:
1. **Force close** app
2. **Reopen** app
3. **Wait for notification**

### **Expected Result**:
📱 **Android notification**:
- **Title**: "Membership Expires Tomorrow"
- **Message**: "Your membership expires tomorrow! Renew now to avoid service interruption."

### **📸 SCREENSHOT THIS**

---

## 📸 TEST 3: "Expired Membership" Notification

### **Setup (30 seconds)**:
1. **Firebase Console** → **Firestore** → **memberships** → **[Your User ID]**
2. **Edit** `membershipExpirationDate`
3. **Change to**: `November 21, 2025 at 11:59:00 PM GMT+8` (yesterday)
4. **Update**

### **Trigger (30 seconds)**:
```bash
# In Firebase shell:
expireMemberships()
```

### **Test App (30 seconds)**:
1. **Force close** app  
2. **Reopen** app
3. **Wait for notification**

### **Expected Result**:
📱 **Android notification**:
- **Title**: "Membership Expired"
- **Message**: "Your membership has expired. Please renew to continue accessing all features."

### **📸 SCREENSHOT THIS**

---

## 📱 HOW TO TAKE PERFECT SCREENSHOTS

### **Method 1: Android Device**
1. **When notification appears**
2. **Pull down** from top of screen to open notification tray
3. **Press** `Volume Down + Power Button` simultaneously
4. **Screenshot will be saved** to Gallery/Photos

### **Method 2: Android Emulator** 
1. **When notification appears**
2. **Click camera icon** in emulator toolbar
3. **OR use** `Ctrl + S` (Windows) / `Cmd + S` (Mac)

### **Method 3: ADB Command**
```bash
# Capture screenshot via ADB
adb shell screencap -p /sdcard/notification_test.png
adb pull /sdcard/notification_test.png ./screenshots/
```

---

## 📋 VERIFICATION CHECKLIST

### **For Each Test, Verify**:
- ✅ **Notification appears** in Android notification tray
- ✅ **Correct title and message** as specified above
- ✅ **Clear screenshot** captured
- ✅ **Log message** appears: `"Created expiration notification for X days"`

### **Check Logs**:
```bash
# Monitor logs while testing
adb logcat | findstr "MembershipExpiration"

# Should see:
# D/MembershipExpiration: Membership expires in 3 days
# D/MembershipExpiration: Created expiration notification for 3 days
```

### **Check Firestore**:
1. **Firebase Console** → **Firestore** → **notifications collection**
2. **Look for new documents** with:
   - `type: "membership_expiration"`
   - `userId: [your-user-id]`
   - `daysUntilExpiration: 3` (or 1)

---

## 📸 BONUS SCREENSHOTS

### **Screenshot 4: Firestore Evidence**
1. **Firebase Console** → **Firestore** → **notifications**
2. **Show the notification documents** created by the Cloud Function
3. **📸 Screenshot the Firestore data**

### **Screenshot 5: Cloud Function Logs**
1. **Firebase Console** → **Functions** → **expireMemberships** → **Logs**
2. **Show the logs** indicating notifications were created
3. **📸 Screenshot the function execution logs**

### **Screenshot 6: Android Logcat**
```bash
adb logcat | findstr "MembershipExpiration" > test_logs.txt
```
4. **📸 Screenshot the log output** showing notification creation

---

## 🔄 RESTORE ORIGINAL DATA

### **IMPORTANT: After Testing**
1. **Firebase Console** → **Firestore** → **memberships** → **[Your User ID]**
2. **Restore** the original `membershipExpirationDate` you noted down
3. **Restore** the original `membershipStatus`
4. **Update** to save

---

## 🎯 EXPECTED SCREENSHOT RESULTS

After following this guide, you should have:

1. **📸 Screenshot 1**: "Membership Expiring Soon" (3 days) notification
2. **📸 Screenshot 2**: "Membership Expires Tomorrow" (1 day) notification  
3. **📸 Screenshot 3**: "Membership Expired" notification
4. **📸 Screenshot 4**: Firestore notifications collection data
5. **📸 Screenshot 5**: Cloud Function execution logs
6. **📸 Screenshot 6**: Android app logs showing notification creation

---

## ⚠️ TROUBLESHOOTING

### **If No Notification Appears**:
1. **Check notification permissions** - ensure app can show notifications
2. **Check logs** - look for error messages in Logcat
3. **Verify Cloud Function** - check if it executed successfully
4. **Try manual trigger** - force close and reopen app multiple times

### **If Wrong Message**:
1. **Double-check dates** - ensure you set the correct expiration dates
2. **Verify timezone** - remember GMT+8 for Philippine time
3. **Check membershipStatus** - should be "active" for tests 1-2

### **If Cloud Function Fails**:
```bash
# Check function deployment
firebase functions:config:get

# Redeploy if needed  
firebase deploy --only functions:expireMemberships
```

---

**🎯 You now have a complete testing plan to get all the screenshots you need!**

*Follow these steps exactly and you'll have perfect demonstration screenshots of your membership expiration notification system.* 📸

---

*Screenshot Testing Guide: November 22, 2025*
*Target: Complete visual documentation*
