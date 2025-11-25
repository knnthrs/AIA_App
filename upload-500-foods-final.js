const admin = require('firebase-admin');
const fs = require('fs');

// Download your Firebase service account key and save as serviceAccountKey.json
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://fittrack-capstone-default-rtdb.firebaseio.com' // Your actual Realtime Database URL
});

const db = admin.database(); // Changed to Realtime Database

// Load the complete 500 foods database
const foods = JSON.parse(fs.readFileSync('gym-foods-500-final.json', 'utf8'));

async function upload500FoodsToRealtimeDB() {
  console.log('🚀 Starting upload of 500 gym foods to Firebase Realtime Database...');
  console.log(`📊 Total foods to upload: ${foods.length}`);

  try {
    // Create a foods object with auto-generated keys
    const foodsData = {};

    foods.forEach((food, index) => {
      // Use push() style keys or simple indices
      const key = `food_${String(index + 1).padStart(3, '0')}`; // food_001, food_002, etc.
      foodsData[key] = food;
    });

    console.log('📦 Uploading all foods to Realtime Database...');

    // Upload all foods at once to /foods reference
    await db.ref('foods').set(foodsData);

    console.log('\n🎉 UPLOAD COMPLETE!');
    console.log(`✅ Successfully uploaded ${foods.length} foods to Firebase Realtime Database!`);

    // Verify upload
    console.log('\n🔍 Verifying upload...');
    const snapshot = await db.ref('foods').once('value');
    const uploadedFoods = snapshot.val();
    const count = Object.keys(uploadedFoods || {}).length;

    console.log(`✅ Verification: ${count} foods now in Realtime Database`);

    // Show sample foods
    console.log('\n📋 Sample foods in database:');
    const sampleKeys = Object.keys(uploadedFoods).slice(0, 5);
    sampleKeys.forEach((key, index) => {
      const food = uploadedFoods[key];
      console.log(`   ${index + 1}. ${food.name} - ${food.calories} cal, ${food.protein}g protein`);
    });

    console.log('\n📱 Now you need to update your app to read from Realtime Database!');
    console.log('🎯 Path: /foods/');

  } catch (error) {
    console.error('\n❌ Upload failed:', error);
    console.log('💡 Make sure:');
    console.log('   1. serviceAccountKey.json is in the project folder');
    console.log('   2. Firebase Realtime Database URL is correct');
    console.log('   3. Realtime Database rules allow writing');
  }

  process.exit();
}

// Run the upload
upload500FoodsToRealtimeDB();
