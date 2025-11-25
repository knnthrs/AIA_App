const admin = require('firebase-admin');
const fs = require('fs');

// Initialize Firebase Admin SDK
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'fittrack-capstone'
});

const db = admin.firestore();

// Read the 500 foods JSON
const foods = JSON.parse(fs.readFileSync('gym-foods-500-final.json', 'utf8'));

async function uploadFoodsToFirestore() {
  console.log('🚀 Starting upload of 500 foods to Firestore...');
  console.log(`📊 Total foods to upload: ${foods.length}`);

  const batch = db.batch();
  let batchCount = 0;
  const BATCH_SIZE = 500; // Firestore batch limit is 500

  for (let i = 0; i < foods.length; i++) {
    const food = foods[i];
    // Use sanitized name as document ID
    const docId = food.name.replace(/[^a-zA-Z0-9]/g, '_').toLowerCase() + '_' + i;
    const docRef = db.collection('foods').doc(docId);
    batch.set(docRef, food);

    batchCount++;
    if (batchCount >= BATCH_SIZE || i === foods.length - 1) {
      try {
        await batch.commit();
        console.log(`✅ Committed batch of ${batchCount} foods`);
        batchCount = 0;
      } catch (error) {
        console.error('❌ Batch commit failed:', error);
        return;
      }
    }
  }

  console.log('\n🎉 UPLOAD COMPLETE!');
  console.log(`✅ Successfully uploaded ${foods.length} foods to Firestore!`);

  // Verify upload
  try {
    const snapshot = await db.collection('foods').limit(5).get();
    console.log(`✅ Verification: Foods collection has at least ${snapshot.size} documents`);
    snapshot.forEach((doc, index) => {
      if (index < 3) {
        console.log(`   ${index + 1}. ${doc.data().name}`);
      }
    });
  } catch (error) {
    console.error('❌ Verification failed:', error);
  }

  process.exit(0);
}

uploadFoodsToFirestore().catch(console.error);

