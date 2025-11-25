# Food Recommendation System - Quick Start

## 🚀 What Was Implemented

A complete food recommendation system with NO IMAGES to keep it simple for your capstone:

### ✅ Coach Features
- Add personalized food recommendations for specific clients
- Add general recommendations (available to all users)
- Edit/delete their recommendations
- Include nutrition data (calories, protein, carbs, fats)
- Add dietary tags (High Protein, Keto, Vegan, etc.)
- Write notes explaining why they recommend each food

### ✅ User Features  
- View personalized recommendations from their coach
- View general recommendations from nutrition database
- Smart filtering based on fitness goal (weight loss/muscle gain)
- Add foods to meal plan (Breakfast/Lunch/Dinner/Snack)
- View detailed nutrition breakdown
- See source badges (Coach Recommended vs Database)

## 📁 Files Created

### Java Classes (7 files)
1. `models/FoodRecommendation.java` - Food data model
2. `models/UserMealPlan.java` - Meal plan data model  
3. `CoachAddFoodActivity.java` - Add/edit food screen
4. `CoachFoodManagementActivity.java` - Coach's food list
5. `UserFoodRecommendationsActivity.java` - User's recommendations
6. `adapters/CoachFoodAdapter.java` - Coach RecyclerView
7. `adapters/UserFoodAdapter.java` - User RecyclerView

### XML Layouts (5 files)
1. `activity_coach_add_food.xml` - Add/edit food form
2. `activity_coach_food_management.xml` - Coach list screen
3. `activity_user_food_recommendations.xml` - User recommendations screen
4. `item_coach_food.xml` - Coach list item
5. `item_user_food.xml` - User list item (with chips)

### Other Files
- `bg_rounded_light.xml` - Background drawable
- `colors.xml` - Updated with new colors
- `seed_food_data.js` - Script to populate 20 sample foods
- `FOOD_RECOMMENDATION_GUIDE.md` - Full documentation

## 🔧 Next Steps to Complete Integration

### Step 1: Sync & Build Project
```bash
# In Android Studio:
1. Click "Sync Project with Gradle Files" (elephant icon)
2. Wait for sync to complete
3. Build → Make Project (Ctrl+F9)
```

### Step 2: Add Navigation from Coach App

In `coach_clients.java`, add a menu item to navigate to food management:

```java
// Add in your existing menu (drawer or toolbar)
menuFoodRecommendations.setOnClickListener(v -> {
    Intent intent = new Intent(coach_clients.this, CoachFoodManagementActivity.class);
    // For general recommendations:
    intent.putExtra("clientId", null);
    intent.putExtra("clientName", null);
    startActivity(intent);
});

// Or from client details, for personalized recommendations:
btnClientFoodRecs.setOnClickListener(v -> {
    Intent intent = new Intent(coach_clients.this, CoachFoodManagementActivity.class);
    intent.putExtra("clientId", selectedClient.getUserId());
    intent.putExtra("clientName", selectedClient.getFullName());
    startActivity(intent);
});
```

### Step 3: Add Navigation from User App

In `MainActivity.java` or `Profile.java`, add menu option:

```java
// In your profile menu or navigation drawer:
menuFoodRecommendations.setOnClickListener(v -> {
    startActivity(new Intent(MainActivity.this, UserFoodRecommendationsActivity.class));
});
```

### Step 4: Seed Initial Food Data

```bash
# In your project root folder:
npm install firebase-admin

# Edit seed_food_data.js line 9:
# Replace path-to-your-service-account-key.json with actual path
# Download from: Firebase Console > Project Settings > Service Accounts > Generate New Private Key

# Run once:
node seed_food_data.js
```

This will add 20 common foods to your database.

### Step 5: Test the Flow

**Coach Side:**
1. Open coach app
2. Navigate to Food Recommendations
3. Tap + button
4. Fill in food details (name, calories, protein, carbs, fats)
5. Select tags (e.g., High Protein, Keto)
6. Add notes (e.g., "Great for post-workout")
7. Submit

**User Side:**
1. Open user app
2. Go to Food Recommendations
3. See coach's recommendations at the top (green badge)
4. Tap a food to see full details
5. Tap "Add to Meal Plan"
6. Select meal type (Breakfast/Lunch/Dinner/Snack)
7. Verify it's saved

## 🎯 For Your Defense

### Panelist Questions & Answers

**Q: "Why no images?"**
A: "We focused on core functionality—nutrition data and personalized recommendations. Images would require storage costs and complicate the MVP. Text-based UI is faster and more maintainable."

**Q: "Where's the food data from?"**
A: "USDA FoodData Central for base foods—government-verified nutrition. Coaches can add custom foods based on their expertise and client needs."

**Q: "How do you ensure accuracy?"**
A: "Base foods are from USDA. Coach-added foods are their professional recommendations. Future: add admin approval workflow for quality control."

**Q: "Can it scale?"**
A: "Yes. Current: ~20 base foods + unlimited coach additions. Architecture supports adding full USDA API (350K+ foods) or Open Food Facts integration."

**Q: "How does it personalize?"**
A: "Three levels:
1. Coach-specific recommendations for individual clients
2. Smart filtering by user's fitness goal (weight loss = low cal, muscle gain = high protein)
3. General recommendations available to all"

### Demo Script (2 minutes)

1. **Show Coach Adding Food** (30 sec)
   - "Coach opens their food management"
   - "Adds 'Post-Workout Shake' - 180 cal, 30g protein"
   - "Selects tags: High Protein, Low Carb"
   - "Writes note: 'Drink within 30 min after workout'"
   - "Submits → immediately available"

2. **Show User Viewing** (45 sec)
   - "User opens food recommendations"
   - "Sees green 'Coach Recommended' badge"
   - "Taps to view details - sees macros breakdown"
   - "Adds to meal plan → selects 'Snack'"
   - "Saved to user's profile"

3. **Explain Smart Features** (45 sec)
   - "System filters by goal - weight loss users see low-cal"
   - "Coach recommendations show first (priority)"
   - "Tags help users find suitable foods (Keto, Vegan, etc.)"
   - "Meal plan tracking helps monitor daily nutrition"

## 🐛 Common Issues & Fixes

### Issue: R.layout errors after sync
**Fix:** Clean & rebuild project:
```
Build → Clean Project
Build → Rebuild Project
```

### Issue: No foods showing for user
**Check:**
1. Run `seed_food_data.js` to populate database
2. Verify Firestore rules allow read
3. Check user is authenticated

### Issue: Coach can't submit food
**Check:**
1. All required fields filled (name, calories, protein, carbs, fats)
2. Coach is authenticated
3. Check Logcat for permission errors

### Issue: Colors not found
**Fix:** Added to `colors.xml`:
- primary (#2196F3)
- background_color (#F5F5F5)
- text_primary (#212121)
- text_secondary (#757575)
- chip_background (#E3F2FD)

## 📊 Database Structure Overview

```
Firestore
├── foods (collection)
│   ├── {foodId}
│   │   ├── name: "Grilled Chicken"
│   │   ├── calories: 165
│   │   ├── protein: 31
│   │   ├── carbs: 0
│   │   ├── fats: 3.6
│   │   ├── tags: ["High Protein", "Low Carb"]
│   │   ├── coachId: "coach_uid" or null
│   │   ├── userId: "client_uid" or null
│   │   ├── notes: "Great for recovery"
│   │   ├── source: "Coach" or "USDA"
│   │   └── isVerified: true
│   
└── users (collection)
    └── {userId}
        └── mealPlan (subcollection)
            └── {mealId}
                ├── foodId: "food_doc_id"
                ├── foodName: "Grilled Chicken"
                ├── calories: 165
                ├── mealType: "lunch"
                ├── date: "2025-11-25"
                └── addedAt: timestamp
```

## ✅ Implementation Checklist

- [x] Data models created
- [x] Coach add/edit activity
- [x] Coach food management activity  
- [x] User recommendations activity
- [x] Adapters for both views
- [x] All XML layouts
- [x] Color resources
- [x] Background drawables
- [x] Seed script for sample data
- [x] Documentation
- [ ] **YOU DO: Add navigation from coach app**
- [ ] **YOU DO: Add navigation from user app**
- [ ] **YOU DO: Run seed script**
- [ ] **YOU DO: Test complete flow**

## 🎓 Estimated Time

- Implementation: ✅ Complete (~5 hours done by AI)
- Integration: ⏱️ ~30 minutes (add menu navigation)
- Seeding data: ⏱️ ~10 minutes
- Testing: ⏱️ ~20 minutes
- **Total: ~1 hour to finish and test**

## 💡 Tips for Success

1. **Keep it simple** - Don't add images/photos, focus on nutrition data
2. **Demo the personalization** - Show how coach recommendations appear first
3. **Explain the filtering** - Weight loss vs muscle gain logic impresses
4. **Emphasize scalability** - Current setup + future USDA API integration
5. **Have backup** - Keep seed script handy in case demo database is empty

---

**Status:** Core implementation COMPLETE ✅  
**Next:** Add navigation buttons and test (1 hour)  
**Ready for:** Capstone defense with minor integration work

Good luck with your defense! 🚀

