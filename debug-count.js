const admin = require('firebase-admin');
const fs = require('fs');
const serviceAccount = require('./serviceAccountKey.json');

fs.writeFileSync('debug.log', 'Starting Firebase init...\n');

try {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    projectId: 'fittrack-capstone'
  });
  fs.appendFileSync('debug.log', 'Firebase initialized successfully.\n');
} catch (e) {
  fs.appendFileSync('debug.log', 'Firebase init error: ' + e.message + '\n');
}

const db = admin.firestore();

async function countFoods() {
  fs.appendFileSync('debug.log', 'Starting count...\n');
  try {
    const snapshot = await db.collection('foods').get();
    fs.appendFileSync('debug.log', `Total documents: ${snapshot.size}\n`);
    if (snapshot.size > 0) {
      let count = 0;
      snapshot.forEach((doc) => {
        if (count < 5) {
          fs.appendFileSync('debug.log', `  ${count + 1}. ${doc.id}: ${doc.data().name || 'No name'}\n`);
          count++;
        }
      });
    }
  } catch (error) {
    fs.appendFileSync('debug.log', 'Error: ' + error.message + '\n');
  }
}

countFoods().then(() => {
  fs.appendFileSync('debug.log', 'Done.\n');
  process.exit(0);
}).catch((err) => {
  fs.appendFileSync('debug.log', 'Unhandled error: ' + err.message + '\n');
  process.exit(1);
});

