// Run this script once to populate initial food database
// Usage: node seed_food_data.js

const admin = require('firebase-admin');

// Initialize Firebase Admin (you'll need to add your service account key)
// Download from Firebase Console > Project Settings > Service Accounts
const serviceAccount = require('./path-to-your-service-account-key.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// Base food recommendations (USDA-inspired data)
const baseFoods = [
  // Proteins
  {
    name: "Grilled Chicken Breast",
    calories: 165,
    protein: 31,
    carbs: 0,
    fats: 3.6,
    servingSize: "100g",
    tags: ["High Protein", "Low Carb", "Keto"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },
  {
    name: "Salmon Fillet",
    calories: 206,
    protein: 22,
    carbs: 0,
    fats: 13,
    servingSize: "100g",
    tags: ["High Protein", "Keto", "Omega-3"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },
  {
    name: "Egg (Boiled)",
    calories: 155,
    protein: 13,
    carbs: 1.1,
    fats: 11,
    servingSize: "2 eggs",
    tags: ["High Protein", "Keto", "Low Carb"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },
  {
    name: "Tuna (Canned in Water)",
    calories: 116,
    protein: 26,
    carbs: 0,
    fats: 0.8,
    servingSize: "100g",
    tags: ["High Protein", "Low Calorie", "Low Carb"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },
  {
    name: "Greek Yogurt (Plain)",
    calories: 59,
    protein: 10,
    carbs: 3.6,
    fats: 0.4,
    servingSize: "100g",
    tags: ["High Protein", "Low Calorie", "Dairy-Free"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },

  // Carbs
  {
    name: "Brown Rice (Cooked)",
    calories: 112,
    protein: 2.6,
    carbs: 24,
    fats: 0.9,
    servingSize: "100g",
    tags: ["High Fiber", "Gluten-Free"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },
  {
    name: "Sweet Potato (Baked)",
    calories: 90,
    protein: 2,
    carbs: 21,
    fats: 0.2,
    servingSize: "100g",
    tags: ["High Fiber", "Gluten-Free", "Vegan"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },
  {
    name: "Oatmeal (Cooked)",
    calories: 71,
    protein: 2.5,
    carbs: 12,
    fats: 1.5,
    servingSize: "100g",
    tags: ["High Fiber", "Vegan"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },
  {
    name: "Whole Wheat Bread",
    calories: 247,
    protein: 13,
    carbs: 41,
    fats: 3.4,
    servingSize: "2 slices",
    tags: ["High Fiber"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },
  {
    name: "Quinoa (Cooked)",
    calories: 120,
    protein: 4.4,
    carbs: 21,
    fats: 1.9,
    servingSize: "100g",
    tags: ["High Protein", "High Fiber", "Gluten-Free", "Vegan"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },

  // Fruits
  {
    name: "Apple",
    calories: 52,
    protein: 0.3,
    carbs: 14,
    fats: 0.2,
    servingSize: "1 medium",
    tags: ["Low Calorie", "High Fiber", "Vegan"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },
  {
    name: "Banana",
    calories: 89,
    protein: 1.1,
    carbs: 23,
    fats: 0.3,
    servingSize: "1 medium",
    tags: ["High Fiber", "Vegan"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },
  {
    name: "Blueberries",
    calories: 57,
    protein: 0.7,
    carbs: 14,
    fats: 0.3,
    servingSize: "100g",
    tags: ["Low Calorie", "High Fiber", "Vegan"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },
  {
    name: "Avocado",
    calories: 160,
    protein: 2,
    carbs: 9,
    fats: 15,
    servingSize: "1/2 avocado",
    tags: ["Keto", "High Fiber", "Vegan"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },

  // Vegetables
  {
    name: "Broccoli (Steamed)",
    calories: 35,
    protein: 2.4,
    carbs: 7,
    fats: 0.4,
    servingSize: "100g",
    tags: ["Low Calorie", "High Fiber", "Vegan"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },
  {
    name: "Spinach (Raw)",
    calories: 23,
    protein: 2.9,
    carbs: 3.6,
    fats: 0.4,
    servingSize: "100g",
    tags: ["Low Calorie", "High Fiber", "Vegan"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },
  {
    name: "Carrots (Raw)",
    calories: 41,
    protein: 0.9,
    carbs: 10,
    fats: 0.2,
    servingSize: "100g",
    tags: ["Low Calorie", "High Fiber", "Vegan"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },

  // Snacks
  {
    name: "Almonds",
    calories: 164,
    protein: 6,
    carbs: 6,
    fats: 14,
    servingSize: "28g (23 almonds)",
    tags: ["High Protein", "Keto", "Vegan"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },
  {
    name: "Peanut Butter",
    calories: 188,
    protein: 8,
    carbs: 7,
    fats: 16,
    servingSize: "2 tbsp",
    tags: ["High Protein", "Keto"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  },
  {
    name: "Protein Shake (Whey)",
    calories: 120,
    protein: 24,
    carbs: 3,
    fats: 1.5,
    servingSize: "1 scoop",
    tags: ["High Protein", "Low Carb"],
    source: "USDA",
    isVerified: true,
    coachId: null,
    userId: null
  }
];

async function seedFoodData() {
  console.log("Starting food data seeding...");

  const batch = db.batch();

  for (const food of baseFoods) {
    const docRef = db.collection('foods').doc();
    batch.set(docRef, {
      ...food,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
  }

  await batch.commit();
  console.log(`Successfully seeded ${baseFoods.length} food items!`);
  process.exit(0);
}

seedFoodData().catch(error => {
  console.error("Error seeding data:", error);
  process.exit(1);
});

