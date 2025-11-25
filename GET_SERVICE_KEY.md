# 🔑 GET YOUR FIREBASE SERVICE ACCOUNT KEY

## 📝 Quick Steps to Download Your Key:

1. **Go to Firebase Console**: https://console.firebase.google.com/u/1/project/fittrack-capstone

2. **Click the Settings gear** (⚙️) → **Project settings**

3. **Click "Service accounts" tab**

4. **Click "Generate new private key"** button

5. **Click "Generate key"** in the popup

6. **Save the downloaded file as `serviceAccountKey.json`** in your project folder:
   ```
   C:\Users\myrlen\AndroidStudioProjects\SignupLoginRealtime\serviceAccountKey.json
   ```

## ✅ Then run the upload:

```cmd
cd C:\Users\myrlen\AndroidStudioProjects\SignupLoginRealtime
npm install firebase-admin
node upload-500-foods-final.js
```

**Your script is already configured with your Firebase URL!** 🎉
