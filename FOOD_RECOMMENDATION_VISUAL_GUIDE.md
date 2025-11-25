# Food Recommendation System - Visual Overview

## 📱 User Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER JOURNEY                            │
└─────────────────────────────────────────────────────────────────┘

User Opens App
      ↓
Main Menu / Profile
      ↓
[Food Recommendations] button
      ↓
UserFoodRecommendationsActivity
      ↓
┌─────────────────────────────────────┐
│  🟢 Coach Recommended (Priority)    │
│  ┌───────────────────────────────┐  │
│  │ Post-Workout Shake            │  │
│  │ 180 cal | P: 30g | C: 3g     │  │
│  │ 💬 Drink within 30 min        │  │
│  │ [+ Add to Meal Plan]          │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
      ↓
┌─────────────────────────────────────┐
│  🔵 Nutrition Database              │
│  ┌───────────────────────────────┐  │
│  │ Grilled Chicken Breast        │  │
│  │ 165 cal | P: 31g | C: 0g     │  │
│  │ #HighProtein #Keto           │  │
│  │ [+ Add to Meal Plan]          │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
      ↓
Tap "Add to Meal Plan"
      ↓
┌─────────────────────────────────────┐
│  Select Meal Type:                  │
│  ○ Breakfast                        │
│  ○ Lunch                            │
│  ○ Dinner                           │
│  ○ Snack                            │
└─────────────────────────────────────┘
      ↓
Saved to Firestore:
users/{userId}/mealPlan/{mealId}
      ↓
✅ Toast: "Added to lunch meal plan"
```

## 👨‍🏫 Coach Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        COACH JOURNEY                            │
└─────────────────────────────────────────────────────────────────┘

Coach Opens App
      ↓
Client List / Dashboard
      ↓
[Food Recommendations] button
      ↓
CoachFoodManagementActivity
      ↓
┌─────────────────────────────────────┐
│  My Food Recommendations            │
│  ┌───────────────────────────────┐  │
│  │ Protein Shake      [✏️] [🗑️]  │  │
│  │ 180 cal | P: 30g              │  │
│  │ For: John Doe                 │  │
│  └───────────────────────────────┘  │
│                                     │
│  [+] Add New Food (FAB)             │
└─────────────────────────────────────┘
      ↓
Tap [+] FAB
      ↓
CoachAddFoodActivity
      ↓
┌─────────────────────────────────────┐
│  Add Food Recommendation            │
│  ┌───────────────────────────────┐  │
│  │ Food Name: _______________    │  │
│  │ Calories: ___  Protein: ___   │  │
│  │ Carbs: ___     Fats: ___      │  │
│  │                               │  │
│  │ Dietary Tags:                 │  │
│  │ ☑ High Protein                │  │
│  │ ☑ Low Carb                    │  │
│  │ ☐ Keto                        │  │
│  │                               │  │
│  │ Notes: __________________     │  │
│  │ _________________________     │  │
│  │                               │  │
│  │ [Submit Food]                 │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
      ↓
Saved to Firestore:
foods/{foodId}
      ↓
✅ Toast: "Food added successfully"
      ↓
Client can now see it in their recommendations!
```

## 🗂️ Data Flow Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      FIREBASE FIRESTORE                         │
└─────────────────────────────────────────────────────────────────┘

foods (collection)
├── {foodId1}
│   ├── name: "Grilled Chicken"
│   ├── calories: 165
│   ├── coachId: null           ← General recommendation
│   ├── userId: null
│   └── source: "USDA"
│
├── {foodId2}
│   ├── name: "Protein Shake"
│   ├── calories: 180
│   ├── coachId: "coach123"    ← Coach-specific
│   ├── userId: null           ← Available to all clients
│   └── source: "Coach"
│
└── {foodId3}
    ├── name: "Post-Workout Meal"
    ├── calories: 350
    ├── coachId: "coach123"    ← Personalized for 1 client
    ├── userId: "user456"      ← Only this user sees it
    └── source: "Coach"

users (collection)
└── {user456}
    ├── name: "John Doe"
    ├── fitnessGoal: "Muscle Gain"
    └── mealPlan (subcollection)
        ├── {meal1}
        │   ├── foodId: "foodId2"
        │   ├── foodName: "Protein Shake"
        │   ├── mealType: "breakfast"
        │   └── date: "2025-11-25"
        └── {meal2}
            ├── foodId: "foodId3"
            ├── mealType: "lunch"
            └── date: "2025-11-25"
```

## 🎯 Smart Filtering Logic

```
User Opens Food Recommendations
      ↓
Load User Profile
      ↓
Get fitnessGoal
      ↓
┌─────────────────────────────────────┐
│   QUERY PRIORITY                    │
└─────────────────────────────────────┘
      ↓
1️⃣ PERSONALIZED (Coach + User)
   Query: coachId == myCoach
          userId == myId
          isVerified == true
      ↓
2️⃣ GENERAL COACH (Coach only)
   Query: coachId == myCoach
          userId == null
          isVerified == true
      ↓
3️⃣ DATABASE (No coach)
   Query: userId == null
          coachId == null
          isVerified == true
      ↓
┌─────────────────────────────────────┐
│   FILTER BY FITNESS GOAL            │
└─────────────────────────────────────┘
      ↓
if goal == "Weight Loss":
   filter: calories < 250
      ↓
if goal == "Muscle Gain":
   filter: protein >= 15g
      ↓
if goal == "General Fitness":
   filter: calories < 300 AND protein >= 10g
      ↓
DISPLAY RESULTS (sorted by priority)
```

## 🏗️ Class Relationships

```
┌─────────────────────────────────────┐
│   FoodRecommendation.java           │
│   (Model)                           │
├─────────────────────────────────────┤
│ - String name                       │
│ - int calories                      │
│ - double protein, carbs, fats       │
│ - List<String> tags                 │
│ - String coachId, userId            │
│ - String notes, source              │
│ - boolean isVerified                │
├─────────────────────────────────────┤
│ + isGoodForGoal(String goal)        │
│ + getProteinPercentage()            │
│ + getCarbsPercentage()              │
│ + getFatsPercentage()               │
└─────────────────────────────────────┘
          ↑
          │ used by
          │
┌─────────────────────────────────────┐
│   CoachAddFoodActivity              │
│   (Coach adds/edits)                │
├─────────────────────────────────────┤
│ - EditText etFoodName, etc.         │
│ - LinearLayout tagsContainer        │
│ - List<String> selectedTags         │
├─────────────────────────────────────┤
│ + validateInputs()                  │
│ + submitFood()                      │
│ + loadFoodData()                    │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│   CoachFoodManagementActivity       │
│   (Coach views list)                │
├─────────────────────────────────────┤
│ - RecyclerView recyclerView         │
│ - CoachFoodAdapter adapter          │
│ - List<FoodRecommendation> foodList │
├─────────────────────────────────────┤
│ + loadFoods()                       │
│ + editFood(food)                    │
│ + deleteFood(food)                  │
└─────────────────────────────────────┘
          │
          │ uses
          ↓
┌─────────────────────────────────────┐
│   CoachFoodAdapter                  │
│   (RecyclerView adapter)            │
├─────────────────────────────────────┤
│ - List<FoodRecommendation> foodList │
│ - OnFoodActionListener listener     │
├─────────────────────────────────────┤
│ + onBindViewHolder()                │
│ + FoodViewHolder (inner class)      │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│   UserFoodRecommendationsActivity   │
│   (User views recommendations)      │
├─────────────────────────────────────┤
│ - RecyclerView recyclerView         │
│ - UserFoodAdapter adapter           │
│ - List<FoodRecommendation> foodList │
├─────────────────────────────────────┤
│ + loadUserProfile()                 │
│ + loadFoodRecommendations()         │
│ + addToMealPlan(food, mealType)     │
│ + showFoodDetailsDialog(food)       │
└─────────────────────────────────────┘
          │
          │ uses
          ↓
┌─────────────────────────────────────┐
│   UserFoodAdapter                   │
│   (RecyclerView adapter)            │
├─────────────────────────────────────┤
│ - List<FoodRecommendation> foodList │
│ - OnFoodActionListener listener     │
├─────────────────────────────────────┤
│ + onBindViewHolder()                │
│ + FoodViewHolder (inner class)      │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│   UserMealPlan.java                 │
│   (Model for saved meals)           │
├─────────────────────────────────────┤
│ - String userId, foodId             │
│ - String foodName                   │
│ - int calories                      │
│ - String mealType, date             │
│ - Timestamp addedAt                 │
└─────────────────────────────────────┘
```

## 🎨 UI Component Hierarchy

```
CoachAddFoodActivity
│
├── Header (Back button + Title)
├── Client Name TextView (if personalized)
├── Food Name TextInputLayout
├── Calories TextInputLayout
├── Macros Row (Protein, Carbs, Fats)
├── Serving Size TextInputLayout
├── Tags Section
│   └── CheckBoxes (10 tags)
├── Notes TextInputLayout
├── Submit Button
└── ProgressBar

─────────────────────────────────────

CoachFoodManagementActivity
│
├── Header (Back button + Title)
├── RecyclerView
│   └── item_coach_food.xml
│       ├── Food Name
│       ├── Calories Badge
│       ├── Macros Text
│       ├── Tags
│       ├── Notes (italic)
│       ├── Edit Button
│       └── Delete Button
├── Empty State (icon + text)
├── ProgressBar
└── FAB (Add New Food)

─────────────────────────────────────

UserFoodRecommendationsActivity
│
├── Header (Back button + Title)
├── Info Card ("Personalized Nutrition")
├── RecyclerView
│   └── item_user_food.xml
│       ├── Food Name + Calories
│       ├── Source Badge (🟢 Coach / 🔵 Database)
│       ├── Macros Grid (3 columns)
│       ├── ChipGroup (tags)
│       ├── Coach Notes (with 💬 icon)
│       └── "Add to Meal Plan" Button
├── Empty State
└── ProgressBar
```

## 🔐 Firestore Security Rules (Already in place)

```javascript
// foods collection
match /foods/{foodId} {
  // Anyone authenticated can read verified foods
  allow read: if request.auth != null && resource.data.isVerified == true;
  
  // Coaches can create their own foods
  allow create: if request.auth != null && 
                   request.resource.data.coachId == request.auth.uid;
  
  // Coaches can update/delete their own foods
  allow update, delete: if request.auth != null && 
                           resource.data.coachId == request.auth.uid;
}

// users/{userId}/mealPlan subcollection
match /users/{userId}/mealPlan/{mealId} {
  // Users can read their own meal plans
  allow read: if request.auth != null && request.auth.uid == userId;
  
  // Users can add to their own meal plan
  allow create: if request.auth != null && 
                   request.auth.uid == userId &&
                   request.resource.data.userId == userId;
  
  // Users can update/delete their meal plans
  allow update, delete: if request.auth != null && request.auth.uid == userId;
}
```

---

**This visual guide shows exactly how everything connects!**  
Use this during your defense to explain the architecture clearly. 🎓

