# 🚀 MANUAL UPLOAD VIA FIREBASE CONSOLE (EASIEST WAY)

Since command line upload is having issues, let's use Firebase Console directly - it's actually easier!

## ✅ STEP-BY-STEP MANUAL UPLOAD:

### **1. Open Firebase Console**
- Go to: https://console.firebase.google.com/u/1/project/fittrack-capstone/database/fittrack-capstone-default-rtdb/data
- You should see your Realtime Database

### **2. Add Foods Manually**
1. **Click the "+" button** next to the root node
2. **Name**: Type `foods`
3. **Value**: Click the import icon (📁)
4. **Select file**: Choose `starter-foods-20.json` from your project folder
5. **Click Import**

### **3. Verify Upload**
- You should see `foods` → `0`, `1`, `2`... with 20 food entries
- Click on any food to see: name, calories, protein, etc.

---

## 🎯 ALTERNATIVE: Copy/Paste Method

If import doesn't work:

1. **Open `starter-foods-20.json`** in Notepad
2. **Copy all content** (Ctrl+A, Ctrl+C)
3. **In Firebase Console**: Click "+" → Name: `foods` → Value: Paste the JSON
4. **Click Add**

---

## 📱 TEST YOUR APP

After upload:

1. **Build your Android app**
2. **Go to Food Recommendations**
3. **You should see 20 foods with filtering!**

Expected result:
```
💪 Personalized Nutrition
🎯 Goal: Muscle Gain
Showing high-protein foods (≥12g protein) to build muscle mass

✅ Found 15 perfect matches out of 20 foods shown
🎯 75% match your Muscle Gain goal
```

---

## ✅ FILES YOU NEED:

- ✅ `starter-foods-20.json` (your 20 foods - ready!)
- ✅ `UserFoodRecommendationsActivityRealtimeDB.java` (copy to your app)
- ✅ Update MainActivity to use new activity

**The manual method is actually faster than command line!** 🎉

**Go to Firebase Console and import the JSON file!** 🚀
