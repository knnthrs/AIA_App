# 📋 Food Recommendation System - Checklist

## ✅ What's Already Done

- [x] Created `MealPlanAdapter.java` for displaying meal plan items
- [x] Created `badge_background.xml` for item count badges
- [x] Updated `UserFoodRecommendationsActivity.java` with dual-card system
- [x] Updated `activity_user_food_recommendations.xml` with two-card layout
- [x] Implemented meal plan loading from Firestore
- [x] Implemented add to meal plan with meal type selection
- [x] Implemented remove from meal plan with confirmation
- [x] Implemented duplicate prevention
- [x] Implemented real-time updates and item counts
- [x] Implemented empty states for both cards
- [x] Added goal-based food filtering
- [x] Added support for coach personalized and general recommendations

---

## 📝 TODO - What You Need to Do

### 1. ✨ **Add Navigation to Food Recommendations** (REQUIRED)

You need to add a way for users to access the Food Recommendations screen. Here are your options:

#### Option A: Add to Bottom Navigation Menu
If you have a bottom navigation, add a new item:
```xml
<!-- In your menu XML file -->
<item
    android:id="@+id/nav_food"
    android:icon="@drawable/ic_food"
    android:title="Food Plan"/>
```

Then handle the click:
```java
// In MainActivity
bottomNav.setOnItemSelectedListener(item -> {
    if (item.getItemId() == R.id.nav_food) {
        startActivity(new Intent(this, UserFoodRecommendationsActivity.class));
        return true;
    }
    // ... other menu items
});
```

#### Option B: Add a Button/Card in MainActivity
Add a clickable card or button:
```xml
<!-- In activity_main.xml -->
<androidx.cardview.widget.CardView
    android:id="@+id/cardFoodRecommendations"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="16dp"
    app:cardCornerRadius="12dp">
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="16dp"
        android:gravity="center_vertical">
        
        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="🍽️ My Food Plan"
            android:textSize="18sp"
            android:textStyle="bold"/>
            
        <ImageView
            android:layout_width="24dp"
            android:layout_height="24dp"
            android:src="@drawable/ic_arrow_right"/>
    </LinearLayout>
</androidx.cardview.widget.CardView>
```

Then add click listener:
```java
// In MainActivity onCreate()
CardView cardFood = findViewById(R.id.cardFoodRecommendations);
cardFood.setOnClickListener(v -> {
    Intent intent = new Intent(MainActivity.this, UserFoodRecommendationsActivity.class);
    startActivity(intent);
});
```

### 2. 🧪 **Test the System**

- [ ] Navigate to Food Recommendations screen
- [ ] Verify you see both cards (My Meal Plan and Suggested Foods)
- [ ] Try adding a food from suggestions to meal plan
- [ ] Verify the food appears in My Meal Plan card
- [ ] Try adding the same food again (should show duplicate error)
- [ ] Try removing a food from meal plan
- [ ] Verify item counts update correctly

### 3. 👨‍🏫 **Have Coach Add Recommendations**

Ask your coach to:
- [ ] Add a general food recommendation (no user selected)
- [ ] Add a personalized food recommendation for you
- [ ] Verify both foods appear in your recommendations

### 4. 📊 **(Optional) Upload 500 Foods Database**

If you want the full 500+ food database:
- [ ] Upload to Firestore `foods` collection, OR
- [ ] Upload to Realtime Database `foods` node
- [ ] Verify foods appear in recommendations

### 5. 🔒 **Verify Firestore Rules**

Your current rules should already work, but double-check:
```javascript
// In firestore.rules
match /users/{userId}/mealPlan/{mealId} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}
```

---

## 🐛 Common Issues & Solutions

### Issue: "Can't see Food Recommendations button/menu"
**Solution**: You need to add navigation (see TODO #1 above)

### Issue: "No foods showing in recommendations"
**Solutions**:
- Ask coach to add and verify foods
- Upload 500 foods database
- Check if foods have `isVerified: true`
- Check if `userId` is null for general recommendations

### Issue: "Can't add food to meal plan"
**Solutions**:
- Check Firestore rules allow write to `mealPlan` subcollection
- Check user is authenticated
- Check logcat for permission errors

### Issue: "Duplicate prevention not working"
**Solutions**:
- Check that `foodName`, `mealType`, and `date` are set correctly
- Look at logcat for "MealPlanAdd" logs
- Verify Firestore query is working

### Issue: "App crashes when opening Food Recommendations"
**Solutions**:
- Check logcat for stack trace
- Verify all views are in XML layout
- Check Firebase is initialized
- Verify user is authenticated

---

## 📱 How It Should Look

```
┌─────────────────────────────────────────┐
│  Food Recommendations              [←]  │
└─────────────────────────────────────────┘
┌─────────────────────────────────────────┐
│  💪 Personalized Nutrition              │
│  🎯 Goal: Muscle Gain                   │
│  Showing high-protein foods...          │
└─────────────────────────────────────────┘
┌─────────────────────────────────────────┐
│  📋 My Meal Plan          [3 items]     │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │ Grilled Chicken          [DELETE] │ │
│  │ 100g                               │ │
│  │ 165 cal | P:31g | C:0g | F:4g     │ │
│  └────────────────────────────────────┘ │
│  ┌────────────────────────────────────┐ │
│  │ Brown Rice               [DELETE] │ │
│  └────────────────────────────────────┘ │
└─────────────────────────────────────────┘
┌─────────────────────────────────────────┐
│  🌟 Suggested Foods       [12 items]    │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │ Salmon Fillet        [+]  [ℹ️]    │ │
│  │ 100g                               │ │
│  │ 206 cal | P:22g | C:0g | F:12g    │ │
│  │ 🏷️ High Protein, Omega-3          │ │
│  └────────────────────────────────────┘ │
│  ┌────────────────────────────────────┐ │
│  │ Sweet Potato          [+]  [ℹ️]   │ │
│  └────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

---

## ✅ Ready to Test!

Once you add navigation (TODO #1), your food recommendation system is **100% complete and ready to use**!

Just run the app, navigate to Food Recommendations, and start adding foods to your meal plan! 🎉

