# Quick Start Guide - Food Recommendations

## 🎯 What You Asked For
You wanted:
1. ✅ A food recommendation card with all suggested foods by coach
2. ✅ A separate meal plan card with foods you've added
3. ✅ Ability to click "Add" button to move foods from recommendations to meal plan
4. ✅ Coach can add general meal recommendations (visible to all users)
5. ✅ Coach can add specific meal recommendations (visible to specific user only)

## ✅ What I Built

### 1. **Two-Card Layout**
- **Top Card**: 📋 My Meal Plan - Shows your selected foods for today
- **Bottom Card**: 🌟 Suggested Foods - Shows recommendations from coach and database

### 2. **How It Works**

#### For Users:
1. Open Food Recommendations screen
2. Browse suggested foods in the bottom card
3. Click the "+" button on any food
4. Select meal type (Breakfast, Lunch, Dinner, or Snack)
5. Food appears in "My Meal Plan" card at the top
6. Click delete icon to remove foods from your plan

#### For Coaches:
In Coach Food Management screen:
- **General Recommendation**: Leave "User" dropdown empty → All users see this food
- **Personalized Recommendation**: Select a specific user → Only that user sees this food

## 📱 How to Navigate to Food Recommendations

You need to add a button or menu item in MainActivity. Here's an example:

```java
// In your MainActivity or wherever you want the button
ImageView btnFoodRecs = findViewById(R.id.btnFoodRecs);
btnFoodRecs.setOnClickListener(v -> {
    Intent intent = new Intent(MainActivity.this, UserFoodRecommendationsActivity.class);
    startActivity(intent);
});
```

Or add to your navigation menu.

## 🔒 Firestore Rules Update

Make sure your Firestore rules allow users to read/write their meal plan:

```javascript
match /users/{userId}/mealPlan/{mealId} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}
```

This should already be covered by your existing subcollection rules.

## 🎨 Features

✅ **Smart Filtering**: Foods are filtered based on your fitness goal  
✅ **Duplicate Prevention**: Can't add the same food twice to same meal  
✅ **Real-time Updates**: Meal plan updates immediately when you add/remove  
✅ **Item Counts**: See how many foods in each section  
✅ **Empty States**: Helpful messages when sections are empty  
✅ **Goal-Based**: Shows foods that match your fitness goal  

## 🧪 Test It

1. **As User**:
   - Open Food Recommendations
   - Add a few foods to your meal plan
   - Try to add the same food twice (should show error)
   - Remove a food from meal plan
   - Check counts update correctly

2. **As Coach**:
   - Add a general food recommendation (no user selected)
   - Add a personalized food for a specific user
   - Verify both foods appear correctly

## 📊 Data Structure

### General Recommendation (Coach):
```
foods collection → document
├─ name: "Chicken Breast"
├─ coachId: "coach123"
├─ userId: null  ← NULL means general
└─ isVerified: true
```

### Personalized Recommendation (Coach):
```
foods collection → document
├─ name: "Special Diet Food"
├─ coachId: "coach123"
├─ userId: "user456"  ← Specific user ID
└─ isVerified: true
```

### User's Meal Plan:
```
users/{userId}/mealPlan → document
├─ foodName: "Chicken Breast"
├─ mealType: "Lunch"
├─ date: "2025-11-25"
└─ ... (nutrition info)
```

---

**That's it! Your food recommendation system is ready! 🎉**

