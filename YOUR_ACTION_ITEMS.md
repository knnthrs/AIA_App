# YOUR ACTION ITEMS - Food Recommendation Integration

## ✅ What's Already Done (by AI)

- [x] 7 Java classes created
- [x] 5 XML layouts created  
- [x] Data models (FoodRecommendation, UserMealPlan)
- [x] Coach add/edit food activity
- [x] Coach food management activity
- [x] User recommendations activity
- [x] Both RecyclerView adapters
- [x] Colors and drawables
- [x] Seed script for 20 sample foods
- [x] Complete documentation

## 📝 What YOU Need to Do (30-60 minutes)

### Task 1: Sync Project (2 minutes)
```
1. Open Android Studio
2. Click "Sync Project with Gradle Files" (🐘 icon in toolbar)
3. Wait for sync to complete
4. If errors, click "Build" → "Clean Project" then "Rebuild Project"
```

### Task 2: Add Navigation Button in Coach App (10 minutes)

**Option A: Add to coach_clients.java (Client List Screen)**

Find your existing menu/toolbar in `coach_clients.java` and add:

```java
// Add this button to your layout first (in activity_coach_clients.xml):
<Button
    android:id="@+id/btnFoodRecommendations"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Manage Food Recommendations"
    android:layout_margin="16dp"/>

// Then in coach_clients.java onCreate():
Button btnFoodRecommendations = findViewById(R.id.btnFoodRecommendations);
btnFoodRecommendations.setOnClickListener(v -> {
    Intent intent = new Intent(coach_clients.this, CoachFoodManagementActivity.class);
    intent.putExtra("clientId", null); // null = general recommendations
    intent.putExtra("clientName", null);
    startActivity(intent);
});
```

**Option B: Add menu item in drawer/toolbar**

```java
// If you have a navigation drawer or menu:
case R.id.menu_food_recommendations:
    Intent intent = new Intent(coach_clients.this, CoachFoodManagementActivity.class);
    intent.putExtra("clientId", null);
    intent.putExtra("clientName", null);
    startActivity(intent);
    return true;
```

### Task 3: Add Navigation Button in User App (10 minutes)

**Option A: Add to Profile.java**

Find your profile menu items and add:

```java
// Add this to activity_profile.xml in your menu section:
<TextView
    android:id="@+id/menuFoodRecommendations"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="🍎 Food Recommendations"
    android:padding="16dp"
    android:textSize="16sp"
    android:clickable="true"
    android:focusable="true"
    android:background="?attr/selectableItemBackground"/>

// Then in Profile.java:
TextView menuFoodRecommendations = findViewById(R.id.menuFoodRecommendations);
menuFoodRecommendations.setOnClickListener(v -> {
    startActivity(new Intent(Profile.this, UserFoodRecommendationsActivity.class));
});
```

**Option B: Add to MainActivity navigation drawer**

```java
// If you have a drawer menu in MainActivity:
case R.id.menu_food_recommendations:
    startActivity(new Intent(MainActivity.this, UserFoodRecommendationsActivity.class));
    return true;
```

### Task 4: Seed Initial Food Data (15 minutes)

**Step 1: Get Firebase Admin Key**
```
1. Go to Firebase Console (console.firebase.google.com)
2. Select your project
3. Click ⚙️ (Settings) → Project Settings
4. Go to "Service Accounts" tab
5. Click "Generate New Private Key"
6. Save the JSON file to your project root folder
7. Rename it to: serviceAccountKey.json
```

**Step 2: Install Dependencies**
```bash
# Open Command Prompt in your project root
cd C:\Users\myrlen\AndroidStudioProjects\SignupLoginRealtime
npm install firebase-admin
```

**Step 3: Update seed script**
```javascript
// Open seed_food_data.js
// Change line 9 from:
const serviceAccount = require('./path-to-your-service-account-key.json');
// To:
const serviceAccount = require('./serviceAccountKey.json');
```

**Step 4: Run the script**
```bash
node seed_food_data.js
```

You should see: `Successfully seeded 20 food items!`

### Task 5: Test the Complete Flow (20 minutes)

**Test Coach Side:**
1. ✅ Build and run coach app
2. ✅ Navigate to "Food Recommendations"
3. ✅ Tap + button (FAB)
4. ✅ Fill in details:
   - Name: "Protein Shake"
   - Calories: 180
   - Protein: 30, Carbs: 3, Fats: 1.5
   - Check tags: High Protein, Low Carb
   - Notes: "Great post-workout"
5. ✅ Tap "Submit Food"
6. ✅ Verify it appears in list
7. ✅ Try editing (tap ✏️ icon)
8. ✅ Try deleting (tap 🗑️ icon, confirm)

**Test User Side:**
1. ✅ Build and run user app
2. ✅ Navigate to "Food Recommendations"  
3. ✅ Verify you see:
   - Foods with green "Coach Recommended" badge (if coach added any)
   - Foods with blue "Nutrition Database" badge (from seed script)
4. ✅ Tap a food card → see details dialog
5. ✅ Tap "Add to Meal Plan"
6. ✅ Select "Breakfast"
7. ✅ Verify toast: "Added to breakfast meal plan"

**Check Firestore:**
1. ✅ Go to Firebase Console
2. ✅ Open Firestore Database
3. ✅ Verify `foods` collection has ~20+ items
4. ✅ Verify `users/{userId}/mealPlan` has entries

## 🐛 Troubleshooting

### Error: Cannot resolve symbol 'CoachFoodAdapter'
**Fix:** Sync project with Gradle files (🐘 icon)

### Error: Cannot find layout activity_coach_add_food
**Fix:** Clean and rebuild project:
```
Build → Clean Project
Build → Rebuild Project
```

### Error: No foods showing in user app
**Check:**
1. Did you run `seed_food_data.js`?
2. Check Firestore - are there documents in `foods` collection?
3. Check Logcat for permission errors

### Error: Can't add food (coach app)
**Check:**
1. All required fields filled?
2. Coach authenticated? (Check currentUser != null)
3. Check Logcat for Firestore errors

### Error: Can't add to meal plan (user app)
**Check:**
1. User authenticated?
2. Check Firestore rules allow write to `users/{userId}/mealPlan`
3. Check Logcat for errors

## 📸 Screenshots to Take (for Defense)

Take these screenshots to show during your presentation:

1. **Coach adding food** - Form with filled details
2. **Coach food list** - RecyclerView with multiple foods
3. **User recommendations** - List with green/blue badges
4. **Food details dialog** - Full nutrition breakdown
5. **Add to meal plan** - Meal type selection dialog
6. **Firestore data** - Show foods collection structure

## 🎤 Practice Your Demo Script

**Coach Flow (30 seconds):**
> "As a coach, I can add personalized food recommendations. Here I'm adding a post-workout shake with specific macros. I can tag it as 'High Protein' and add a note explaining why I recommend it. Once submitted, it's immediately available to my clients."

**User Flow (30 seconds):**
> "Users see recommendations from their coach first, marked with a green badge. They can tap to see full nutrition details including macro percentages. With one tap, they add it to their meal plan, choosing the meal type. This helps them track daily nutrition aligned with their fitness goals."

**Technical Explanation (30 seconds):**
> "The system uses smart filtering based on user goals. Weight loss users see low-calorie options, muscle gain users see high-protein foods. We query Firestore with priority: personalized coach recommendations first, then general recommendations, then database foods. All nutrition data comes from USDA FoodData Central, plus coach expertise."

## ✅ Final Checklist Before Defense

- [ ] Project synced and builds without errors
- [ ] Navigation buttons added (coach and user apps)
- [ ] Seed script run successfully (20 foods in database)
- [ ] Tested coach adding food
- [ ] Tested user viewing recommendations
- [ ] Tested user adding to meal plan
- [ ] Screenshots taken
- [ ] Demo script practiced
- [ ] Can explain data source (USDA + Coach)
- [ ] Can explain personalization logic
- [ ] Can answer scalability questions
- [ ] Firestore rules understood

## 🎯 Expected Timeline

- Task 1 (Sync): 2 minutes
- Task 2 (Coach nav): 10 minutes
- Task 3 (User nav): 10 minutes  
- Task 4 (Seed data): 15 minutes
- Task 5 (Testing): 20 minutes
- Screenshots: 5 minutes
- **Total: ~60 minutes**

## 💪 You Got This!

Everything is implemented and ready. Just add the navigation buttons, seed the data, and test. The hard work is done—you just need to wire it together!

**Questions? Check:**
- `FOOD_RECOMMENDATION_GUIDE.md` - Full documentation
- `FOOD_RECOMMENDATION_QUICK_START.md` - Setup guide
- `FOOD_RECOMMENDATION_VISUAL_GUIDE.md` - Architecture diagrams

Good luck with your capstone! 🚀🎓

