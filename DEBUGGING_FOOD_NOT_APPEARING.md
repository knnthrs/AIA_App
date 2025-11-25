# 🔍 DEBUGGING: Food Added But Not Appearing

## Current Status
- ✅ Coach successfully added food ("Food added successfully")
- ❌ User can't see the food

## Possible Issues

1. **Field name mismatch** - `isVerified` field might be saved differently
2. **Coach/User ID mismatch** - Food saved with wrong coachId
3. **Query filtering too strict** - Food being filtered out incorrectly
4. **Cache issue** - Old query cached

---

## 🧪 TESTING STEPS

### Step 1: Check Coach Side (See What Was Saved)

1. **Open Coach App**
2. **Go to Sidebar → Food Recommendations**
3. **Check if the food appears in coach's list**
4. **Open Logcat**, filter: `CoachFoodMgmt`

**Send me this output!** It will show:
```
D/CoachFoodMgmt: === LOADING COACH FOODS ===
D/CoachFoodMgmt: coachId: [id]
D/CoachFoodMgmt: Query returned X foods from coach
D/CoachFoodMgmt: Doc ID: [id], data: {name=..., coachId=..., isVerified=...}
D/CoachFoodMgmt: ✅ Added general food: [name]
```

### Step 2: Check User Side (See What It's Querying)

1. **Open User App**
2. **Go to Food Recommendations**
3. **Open Logcat**, filter: `FoodRecommendations`

**Send me this output!** It will show:
```
D/FoodRecommendations: Loading foods for userId: [id], coachId: [id]
D/FoodRecommendations: General query returned X total foods
D/FoodRecommendations: === Document ID: [id] ===
D/FoodRecommendations: Raw data: {calories=180, name=Test Food, ...}
D/FoodRecommendations: Parsed food: name=Test Food, coachId=[id], userId=null, isVerified=true
```

---

## 🎯 What I Need From You

**Do both steps above and copy-paste BOTH Logcat outputs:**

1. **Coach Logcat** (filter: `CoachFoodMgmt`)
   - From when coach opens Food Recommendations list
   
2. **User Logcat** (filter: `FoodRecommendations`)
   - From when user opens Food Recommendations

**These logs will tell me**:
- ✅ Is the food actually saved in Firestore?
- ✅ What are ALL the fields in the food document?
- ✅ Is `isVerified` true or false?
- ✅ Is `coachId` matching user's coach?
- ✅ Why is the food being filtered out?

---

## 🔧 Quick Checks You Can Do

### Check 1: Firebase Console
1. Go to Firebase Console
2. Firestore Database
3. Look at `foods` collection
4. Click on the food document
5. **Take a screenshot** and send it

Look for:
- `name`: "Test Food" or whatever you entered
- `coachId`: Should match coach's UID
- `userId`: Should be `null` for general
- `isVerified`: Should be `true`
- `calories`, `protein`, `carbs`, `fats`: Should have values

### Check 2: User's Coach ID
In User Logcat, check:
```
D/FoodRecommendations: coachId: [some-id]
```

Does this match the `coachId` in the food document?

---

## 🐛 Common Issues & Solutions

### Issue 1: Field Name Mismatch
**Symptom**: Raw data shows `verified: true` instead of `isVerified: true`

**Solution**: I'll add `@PropertyName` annotation

### Issue 2: CoachId Mismatch
**Symptom**: Food's coachId ≠ User's coachId

**Solution**: Food was saved with wrong coachId

### Issue 3: isVerified is false
**Symptom**: `isVerified: false` in raw data

**Solution**: Code needs to properly set it to true

### Issue 4: Query Returns 0
**Symptom**: "General query returned 0 total foods"

**Solution**: Firestore rules blocking or query issue

---

## ⚡ QUICK ACTION

1. **Rebuild app** (Clean → Rebuild)
2. **Open Coach app** → Food Recommendations
3. **Copy Logcat** (filter: CoachFoodMgmt)
4. **Open User app** → Food Recommendations  
5. **Copy Logcat** (filter: FoodRecommendations)
6. **Paste BOTH logs here**

I'll analyze them and fix the exact issue! 🔍

