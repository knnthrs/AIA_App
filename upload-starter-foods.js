const admin = require('firebase-admin');
const fs = require('fs');

// You'll need to download your Firebase service account key and put it here
// Get it from: Firebase Console > Project Settings > Service accounts > Generate new private key
const serviceAccount = require('./serviceAccountKey.json'); // Download this from Firebase Console

// Initialize Firebase Admin with your project details
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://YOUR-PROJECT-ID.firebaseio.com' // Replace with your Firebase project URL
});

const db = admin.firestore();

// Load the foods data
const foods = JSON.parse(fs.readFileSync('starter-foods-20.json', 'utf8'));

async function uploadFoods() {
  console.log('🚀 Starting to upload', foods.length, 'foods to Firebase...');

  let uploadCount = 0;
  const batchSize = 500; // Firestore batch limit

  for (let i = 0; i < foods.length; i += batchSize) {
    const batch = db.batch();
    const batchFoods = foods.slice(i, i + batchSize);

    batchFoods.forEach(food => {
      const docRef = db.collection('foods').doc(); // Auto-generate ID
      batch.set(docRef, food);
      uploadCount++;
    });

    try {
      await batch.commit();
      console.log(`✅ Uploaded batch ${Math.ceil((i + 1) / batchSize)}: ${batchFoods.length} foods`);
    } catch (error) {
      console.error(`❌ Error uploading batch ${Math.ceil((i + 1) / batchSize)}:`, error);
    }
  }

  console.log(`🎉 Successfully uploaded ${uploadCount} foods to Firebase!`);
  console.log('📱 Now open your app and check Food Recommendations!');

  // Verify upload
  const snapshot = await db.collection('foods').get();
  console.log(`✅ Verification: ${snapshot.size} foods now in database`);

  process.exit();
}

uploadFoods().catch(error => {
  console.error('❌ Upload failed:', error);
  process.exit(1);
});
