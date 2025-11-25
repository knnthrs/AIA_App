const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

console.log('🔄 Initializing Firebase Admin...');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'fittrack-capstone'
});

const db = admin.firestore();
console.log('✅ Firebase Admin initialized.');

async function countFoods() {
  console.log('🔍 Querying foods collection...');
  try {
    const snapshot = await db.collection('foods').get();
    console.log(`📊 Total documents in 'foods' collection: ${snapshot.size}`);
    if (snapshot.size > 0) {
      console.log('📋 Sample documents:');
      let count = 0;
      snapshot.forEach((doc) => {
        if (count < 5) { // Show first 5
          console.log(`  ${count + 1}. ${doc.id}: ${doc.data().name || 'No name'}`);
          count++;
        }
      });
    } else {
      console.log('⚠️ No documents found in foods collection.');
    }
  } catch (error) {
    console.error('❌ Error counting foods:', error.message);
    console.error('Full error:', error);
  }
  process.exit(0);
}

console.log('🚀 Starting count...');
countFoods().catch((err) => {
  console.error('💥 Unhandled error:', err);
  process.exit(1);
});
