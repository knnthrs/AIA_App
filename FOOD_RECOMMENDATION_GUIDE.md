# Food Recommendation System - Implementation Guide

## Overview
This document explains the food recommendation system implementation for your fitness app capstone project.

## Features Implemented

### For Coaches
1. **Add Personalized Food Recommendations**
   - Navigate to client details → "Food Recommendations"
   - Add foods specifically for a client OR general recommendations
   - Include macros (protein, carbs, fats), tags, and personal notes
   - Auto-approved (no admin verification needed)

2. **Edit Food Recommendations**
   - View all their recommendations
   - Edit nutrition info, tags, or notes
   - Delete outdated recommendations

3. **Access from Coach Dashboard**
   - From `coach_clients.java`, add button/menu item to access `CoachFoodManagementActivity`

### For Users
1. **View Personalized Recommendations**
   - See foods recommended by their coach (priority display)
   - See general recommendations from nutrition database
   - Filtered based on their fitness goal (weight loss/muscle gain)
   - Clear badges show source (Coach vs Database)

2. **Add to Meal Plan**
   - Tap "Add to Meal Plan" button
   - Select meal type (Breakfast/Lunch/Dinner/Snack)
   - Saved to `users/{userId}/mealPlan` subcollection

3. **View Food Details**
   - Tap card to see full nutrition breakdown
   - See macro percentages
   - Read coach's notes

## Data Structure

### Firestore Collections

#### `foods` Collection
```json
{
  "name": "Grilled Chicken Breast",
  "calories": 165,
  "protein": 31,
  "carbs": 0,
  "fats": 3.6,
  "servingSize": "100g",
  "tags": ["High Protein", "Low Carb", "Keto"],
  "coachId": "coach_uid_or_null",
  "userId": "client_uid_or_null", // null = general recommendation
  "notes": "Great for post-workout recovery",
  "source": "Coach" or "USDA",
  "isVerified": true,
  "createdAt": timestamp
}
```

#### `users/{userId}/mealPlan` Subcollection
```json
{
  "userId": "user_uid",
  "foodId": "food_doc_id",
  "foodName": "Grilled Chicken Breast",
  "calories": 165,
  "protein": 31,
  "carbs": 0,
  "fats": 3.6,
  "mealType": "lunch",
  "servingSize": "100g",
  "date": "2025-11-25",
  "addedAt": timestamp
}
```

## Smart Recommendation Logic

The system filters foods based on user's fitness goal:

- **Weight Loss**: Shows foods < 250 calories
- **Muscle Gain**: Shows foods with protein >= 15g
- **General Fitness**: Shows balanced foods (< 300 cal, >= 10g protein)

Priority order:
1. Coach's personalized recommendations (userId matches)
2. Coach's general recommendations
3. Database recommendations (filtered by goal)

## Setup Instructions

### 1. Seed Initial Food Data

```bash
# Install Firebase Admin SDK
npm install firebase-admin

# Update seed_food_data.js with your service account key path
# Download from Firebase Console > Project Settings > Service Accounts

# Run the script once
node seed_food_data.js
```

This will populate ~20 common foods in your database.

### 2. Add Menu Items

#### In Coach's Client Details Screen
Add a button/menu option:
```java
Button btnManageFoodRecommendations = findViewById(R.id.btnManageFoodRecommendations);
btnManageFoodRecommendations.setOnClickListener(v -> {
    Intent intent = new Intent(this, CoachFoodManagementActivity.class);
    intent.putExtra("clientId", selectedClient.getUserId());
    intent.putExtra("clientName", selectedClient.getFullName());
    startActivity(intent);
});
```

#### In User's Main Screen (Profile Menu)
Add a menu option:
```java
// In MainActivity.java or Profile.java
TextView menuFoodRecommendations = findViewById(R.id.menuFoodRecommendations);
menuFoodRecommendations.setOnClickListener(v -> {
    startActivity(new Intent(this, UserFoodRecommendationsActivity.class));
});
```

### 3. Update Firestore Rules

Already included in your current rules! The system supports:
- Coaches can read/write foods they created
- Users can read verified foods
- Users can write to their own mealPlan subcollection

## For Your Panelists

### Key Talking Points

**Q: Where does the food data come from?**
A: We use a combination of:
- USDA FoodData Central (government-verified nutrition database)
- Coach-curated recommendations (personalized for clients)

**Q: Can coaches customize recommendations?**
A: Yes! Coaches can:
- Add new foods with full nutrition info
- Personalize recommendations for specific clients
- Add notes explaining why they recommend each food
- Edit or remove recommendations anytime

**Q: How does it help users?**
A: The system:
- Filters foods based on user's fitness goal
- Prioritizes coach's personalized suggestions
- Allows easy meal planning (track daily nutrition)
- Shows clear nutrition breakdown for informed choices

**Q: Is it scalable?**
A: Yes! Current architecture supports:
- ~20 base foods (proven sufficient for capstone demo)
- Unlimited coach-added foods
- Future expansion: integrate full USDA API (350K+ foods)

### Demo Flow for Defense

1. **Show Coach Interface**
   - Open coach app → Navigate to client
   - Tap "Food Recommendations"
   - Add a food (e.g., "Post-Workout Shake" with custom macros)
   - Show how tags and notes work

2. **Show User Interface**
   - Open user app → Food Recommendations
   - Point out "Coach Recommended" badge
   - Tap a food to show details
   - Add to meal plan → Select "Breakfast"

3. **Explain Smart Filtering**
   - Show in code: `isGoodForGoal()` method
   - Explain weight loss users see low-cal foods
   - Muscle gain users see high-protein foods

## Files Created

### Java Classes
- `models/FoodRecommendation.java` - Food data model
- `models/UserMealPlan.java` - Meal plan data model
- `CoachAddFoodActivity.java` - Coach adds/edits food
- `CoachFoodManagementActivity.java` - Coach views their foods
- `UserFoodRecommendationsActivity.java` - User views recommendations
- `adapters/CoachFoodAdapter.java` - Coach's RecyclerView adapter
- `adapters/UserFoodAdapter.java` - User's RecyclerView adapter

### XML Layouts
- `activity_coach_add_food.xml` - Add/edit food form
- `activity_coach_food_management.xml` - Coach's food list
- `activity_user_food_recommendations.xml` - User's recommendations
- `item_coach_food.xml` - Coach list item
- `item_user_food.xml` - User list item (with chips)
- `bg_rounded_light.xml` - Background drawable

### Scripts
- `seed_food_data.js` - One-time database population

## Maintenance

### Adding More Base Foods
Edit `seed_food_data.js` and re-run, or add directly in Firebase Console.

### Future Enhancements
- Meal plan calendar view
- Daily nutrition tracking (total cals/macros)
- Barcode scanning (integrate Open Food Facts API)
- Recipe suggestions (multiple foods combined)
- Shopping list generator

## Troubleshooting

**Problem**: No foods showing for user
- Check Firestore: verify `isVerified: true` on foods
- Check rules: user must be authenticated
- Check filters: user's goal might be filtering all foods

**Problem**: Coach can't see their foods
- Verify `coachId` field matches coach's UID
- Check query in `loadFoods()` method

**Problem**: Can't add to meal plan
- Check Firestore rules for `users/{userId}/mealPlan`
- Verify user is authenticated

## Testing Checklist

- [ ] Coach can add general food recommendation
- [ ] Coach can add personalized food for specific client
- [ ] Coach can edit food details
- [ ] Coach can delete food
- [ ] User sees coach's personalized recommendations first
- [ ] User sees general recommendations
- [ ] User can add food to breakfast/lunch/dinner/snack
- [ ] Food details dialog shows correct macros
- [ ] Tags display properly as chips
- [ ] Source badges show correctly (Coach vs Database)

---

**Date**: November 25, 2025
**For**: Capstone Project Defense
**Implementation Time**: ~5 hours total

