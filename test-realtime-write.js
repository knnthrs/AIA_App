const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://fittrack-capstone-default-rtdb.firebaseio.com'
});

const db = admin.database();

db.ref('test_upload').set({ hello: "world", time: Date.now() })
  .then(() => {
    console.log('✅ Test write succeeded!');
    process.exit(0);
  })
  .catch((err) => {
    console.error('❌ Test write failed:', err);
    process.exit(1);
  });

