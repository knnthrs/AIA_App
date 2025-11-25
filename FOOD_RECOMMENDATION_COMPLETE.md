# Food Recommendation System - Complete Implementation

## 🎉 What Was Implemented

I've successfully created a comprehensive food recommendation system with the following features:

### ✅ 1. **Two-Card Layout**
- **My Meal Plan Card**: Shows foods you've added to your daily meal plan
- **Suggested Foods Card**: Shows personalized food recommendations from your coach and general nutrition database

### ✅ 2. **Meal Plan Management**
- Add foods from recommendations to your meal plan by selecting meal type (Breakfast, Lunch, Dinner, Snack)
- Remove foods from your meal plan with confirmation dialog
- Real-time updates when adding/removing foods
- Shows count of items in each section

### ✅ 3. **Food Recommendations**
Two types of recommendations:
- **Personalized**: Foods specifically recommended by your coach for you
- **General**: Foods recommended by coach for all users OR foods from the 500+ food database

### ✅ 4. **Smart Filtering**
Foods are filtered based on your fitness goal:
- **Weight Loss**: Low-calorie (<250 cal) and high-protein foods
- **Muscle Gain**: High-protein foods (≥12g protein)
- **General Fitness**: Balanced nutrition
- **Endurance**: Carb-rich and lean protein
- **Strength Training**: Protein-rich and calorie-dense foods

### ✅ 5. **Duplicate Prevention**
- System checks if a food already exists in your meal plan before adding
- Shows friendly error message if duplicate is detected

### ✅ 6. **Coach Functionality**
Coaches can:
- Add **general** food recommendations (userId = null) - visible to all users
- Add **personalized** food recommendations (userId = specific user ID) - visible only to that user
- All coach recommendations must be verified (isVerified = true) to appear

---

## 📁 Files Created/Modified

### New Files:
1. **`MealPlanAdapter.java`** - Adapter for displaying meal plan items
2. **`badge_background.xml`** - Drawable for item count badges

### Modified Files:
1. **`UserFoodRecommendationsActivity.java`** - Complete rewrite with dual-card system
2. **`activity_user_food_recommendations.xml`** - Updated layout with two cards

---

## 🔧 How It Works

### User Flow:
1. User opens Food Recommendations screen
2. Sees two cards:
   - **Top Card**: My Meal Plan (foods already added)
   - **Bottom Card**: Suggested Foods (recommendations)
3. User can:
   - Click "+" button on any suggested food
   - Select meal type (Breakfast/Lunch/Dinner/Snack)
   - Food is added to meal plan
   - Meal plan card updates automatically
4. User can remove foods from meal plan by clicking the delete icon

### Coach Flow:
1. Coach opens Food Management screen
2. Coach can add food recommendations:
   - **For specific user**: Select user from dropdown, food appears only for that user
   - **For all users (general)**: Leave user dropdown empty, food appears for everyone

---

## 🎨 UI Design

### My Meal Plan Card:
```
┌─────────────────────────────────────┐
│ 📋 My Meal Plan          [2 items]  │
├─────────────────────────────────────┤
│ ┌─────────────────────────────────┐ │
│ │ Grilled Chicken      [DELETE] │ │
│ │ 100g                            │ │
│ │ 165 cal | P: 31g | C: 0g | F:4g│ │
│ └─────────────────────────────────┘ │
│ ┌─────────────────────────────────┐ │
│ │ Brown Rice           [DELETE] │ │
│ │ 1 cup                           │ │
│ │ 218 cal | P: 5g | C: 46g | F:2g│ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

### Suggested Foods Card:
```
┌─────────────────────────────────────┐
│ 🌟 Suggested Foods       [5 items]  │
├─────────────────────────────────────┤
│ ┌─────────────────────────────────┐ │
│ │ Salmon Fillet        [+] [i]   │ │
│ │ 100g                            │ │
│ │ 206 cal | P: 22g | C: 0g | F:12g│ │
│ │ Tags: High Protein              │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

---

## 🔥 Firebase Structure

### Firestore Collections:

#### 1. `foods` collection (Coach recommendations)
```javascript
{
  name: "Grilled Chicken",
  calories: 165,
  protein: 31,
  carbs: 0,
  fats: 4,
  coachId: "coach123",
  userId: null,  // null = general, or specific userId = personalized
  isVerified: true,
  source: "Coach",
  servingSize: "100g",
  tags: ["High Protein", "Low Carb"],
  notes: "Great for muscle building",
  createdAt: Timestamp
}
```

#### 2. `users/{userId}/mealPlan` subcollection
```javascript
{
  userId: "user123",
  foodId: "food456",
  foodName: "Grilled Chicken",
  calories: 165,
  protein: 31,
  carbs: 0,
  fats: 4,
  mealType: "Lunch",  // Breakfast, Lunch, Dinner, Snack
  servingSize: "100g",
  date: "2025-11-25",  // yyyy-MM-dd format
  addedAt: Timestamp
}
```

#### 3. Realtime Database `foods` node (500+ food database)
```json
{
  "foods": {
    "-N123abc": {
      "name": "Chicken Breast",
      "calories": 165,
      "protein": 31,
      ...
    }
  }
}
```

---

## 🚀 How to Use (For You)

### To Navigate to Food Recommendations:
You need to add a menu item or button in your MainActivity. Example:

```java
// In MainActivity.java
ImageView btnFoodRecommendations = findViewById(R.id.btnFoodRecommendations);
btnFoodRecommendations.setOnClickListener(v -> {
    Intent intent = new Intent(MainActivity.this, UserFoodRecommendationsActivity.class);
    startActivity(intent);
});
```

Or add it to your bottom navigation menu.

---

## 📝 TODO for You

1. **Add navigation button/menu item** in MainActivity to open UserFoodRecommendationsActivity
2. **Test the flow**:
   - Add foods from recommendations to meal plan
   - Remove foods from meal plan
   - Check if duplicate prevention works
3. **Ask your coach** to add some food recommendations through the coach app
4. **Optional**: Upload the 500 foods database to either Realtime DB or Firestore for more variety

---

## 🎯 Key Features Summary

✅ Separate cards for meal plan and recommendations  
✅ Add foods from recommendations with meal type selection  
✅ Remove foods from meal plan  
✅ Duplicate prevention  
✅ Real-time updates  
✅ Coach can add general or personalized recommendations  
✅ Smart filtering based on fitness goals  
✅ Item counts displayed  
✅ Empty states for both cards  
✅ Clean, intuitive UI  

---

## 🐛 Troubleshooting

### "No foods showing in recommendations"
- Check if coach has added and verified foods
- Check if 500 foods are uploaded to Realtime DB or Firestore
- Check Firebase security rules

### "Can't add food to meal plan"
- Check Firestore rules allow write to `users/{userId}/mealPlan`
- Check user is authenticated

### "Duplicate not prevented"
- Make sure `foodName`, `mealType`, and `date` fields are being set correctly
- Check Firestore query is working (check logs)

---

**All done! Your food recommendation system with meal plan management is ready to use! 🎉**

