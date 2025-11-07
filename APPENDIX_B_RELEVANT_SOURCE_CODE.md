# APPENDIX B: RELEVANT SOURCE CODE

This section includes the key source code implementations for the major features of the system. The code snippets shown here represent the core functionality - note that some helper methods and boilerplate code have been omitted for brevity. Sensitive API keys have been removed for security.

---

## B.1 Authentication System

### B.1.1 User Registration (SignupActivity.java)

This method handles new user registration including input validation and creating accounts in Firebase Authentication.

```java
// User registration with Firebase Authentication
private void registerUser() {
    String email = signupEmail.getText().toString().trim();
    String password = signupPassword.getText().toString().trim();
    String fullname = signupFullname.getText().toString().trim();
    String phone = signupPhone.getText().toString().trim();

    // Validate all inputs
    if (!validateInputs(fullname, email, phone, password)) {
        return;
    }

    showLoadingState(true);

    // Create user account in Firebase Authentication
    mAuth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                String userId = firebaseUser.getUid();

                // Store user data in Firestore
                Map<String, Object> userData = new HashMap<>();
                userData.put("fullname", fullname);
                userData.put("email", email);
                userData.put("phone", phone);
                userData.put("userType", "user");
                userData.put("membershipPlanType", "None");
                userData.put("membershipActive", false);
                userData.put("createdAt", System.currentTimeMillis());

                db.collection("users").document(userId).set(userData)
                    .addOnSuccessListener(aVoid -> {
                        showLoadingState(false);
                        Toast.makeText(this, "Registration successful!", 
                            Toast.LENGTH_SHORT).show();
                        
                        // Navigate to profile setup
                        Intent intent = new Intent(SignupActivity.this, 
                            GenderSelection.class);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        showLoadingState(false);
                        Toast.makeText(this, "Error saving user data: " + 
                            e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            } else {
                showLoadingState(false);
                Toast.makeText(this, "Registration failed: " + 
                    task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
}

// Validate user inputs with regex patterns
private boolean validateInputs(String fullname, String email, 
                               String phone, String password) {
    if (fullname.isEmpty()) {
        signupFullname.setError("Full name is required");
        return false;
    }
    
    if (!EMAIL_PATTERN.matcher(email).matches()) {
        signupEmail.setError("Invalid email format");
        return false;
    }
    
    if (!PH_MOBILE_PATTERN.matcher(phone).matches()) {
        signupPhone.setError("Invalid Philippine mobile number");
        return false;
    }
    
    if (password.length() < MIN_PASSWORD_LENGTH) {
        signupPassword.setError("Password must be at least 8 characters");
        return false;
    }
    
    return true;
}
```

This registration system validates email and password formats using regex patterns before creating the account. After successful authentication, user data is stored in Firestore with default values for membership status.

---

### B.1.2 Password Reset (LoginActivity.java)

The password reset feature uses Firebase's built-in email reset functionality.

```java
// Show password reset dialog
private void showPasswordResetDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Reset Password");

    final EditText input = new EditText(this);
    input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
    input.setHint("Enter your email");
    builder.setView(input);

    builder.setPositiveButton("Send Reset Link", (dialog, which) -> {
        String email = input.getText().toString().trim();
        
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", 
                Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        
        // Send password reset email via Firebase
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                
                if (task.isSuccessful()) {
                    showPasswordResetSuccessDialog();
                } else {
                    Toast.makeText(this, "Error: " + 
                        task.getException().getMessage(), 
                        Toast.LENGTH_LONG).show();
                }
            });
    });

    builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
    builder.show();
}

// Show success dialog with password requirements
private void showPasswordResetSuccessDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Reset Link Sent!");
    builder.setMessage("A password reset link has been sent to your email.\n\n" +
        "Password Requirements:\n" +
        "• At least 8 characters\n" +
        "• One uppercase letter\n" +
        "• One lowercase letter\n" +
        "• One number\n" +
        "• One special character");
    builder.setPositiveButton("OK", null);
    builder.show();
}
```

The dialog validates the email input before calling Firebase's sendPasswordResetEmail method. After the link is sent, a confirmation dialog shows the password requirements that users need to follow when creating their new password.

---

## B.2 Membership Management System

### B.2.1 Payment Integration (SelectMembership.java)

The payment system integrates with PayMongo's API to generate payment links and process membership purchases.

```java
// Initiate PayMongo payment flow
private void initiatePayMongoPayment() {
    if (loadingProgress != null) {
        loadingProgress.setVisibility(View.VISIBLE);
    }
    confirmButtonCard.setEnabled(false);

    int amountInCents = (int) (selectedPrice * 100);

    // Execute payment creation on background thread
    executor.execute(() -> {
        try {
            String paymentLinkUrl = createPayMongoPaymentLink(amountInCents);

            runOnUiThread(() -> {
                if (loadingProgress != null) {
                    loadingProgress.setVisibility(View.GONE);
                }
                confirmButtonCard.setEnabled(true);

                if (paymentLinkUrl != null) {
                    // Launch PayMongo payment activity
                    Intent intent = new Intent(SelectMembership.this, 
                        PayMongoPaymentActivity.class);
                    intent.putExtra("paymentUrl", paymentLinkUrl);
                    intent.putExtra("packageId", selectedPackageId);
                    intent.putExtra("membershipPlanType", selectedPlanType);
                    intent.putExtra("months", selectedMonths);
                    intent.putExtra("sessions", selectedSessions);
                    intent.putExtra("price", selectedPrice);
                    startActivityForResult(intent, 100);
                } else {
                    Toast.makeText(SelectMembership.this,
                        "Failed to create payment link. Please try again.",
                        Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating payment", e);
            runOnUiThread(() -> {
                if (loadingProgress != null) {
                    loadingProgress.setVisibility(View.GONE);
                }
                confirmButtonCard.setEnabled(true);
                Toast.makeText(SelectMembership.this,
                    "Payment error: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            });
        }
    });
}

// Create PayMongo payment link via REST API
private String createPayMongoPaymentLink(int amountInCents) {
    try {
        URL url = new URL("https://api.paymongo.com/v1/links");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Basic " +
            android.util.Base64.encodeToString(
                (PAYMONGO_SECRET_KEY + ":").getBytes(),
                android.util.Base64.NO_WRAP
            ));
        conn.setDoOutput(true);

        // Build payment request JSON
        JSONObject data = new JSONObject();
        JSONObject attributes = new JSONObject();

        String description = generateTitleText(selectedPlanType, 
            selectedMonths, selectedDurationDays, selectedSessions);
        
        attributes.put("amount", amountInCents);
        attributes.put("description", description);
        attributes.put("remarks", "Membership: " + description);

        data.put("data", new JSONObject().put("attributes", attributes));

        Log.d(TAG, "PayMongo Request: " + data.toString());

        // Send request
        OutputStream os = conn.getOutputStream();
        os.write(data.toString().getBytes());
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();
        Log.d(TAG, "PayMongo Response Code: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK || 
            responseCode == HttpURLConnection.HTTP_CREATED) {
            
            BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Parse response to get checkout URL
            JSONObject jsonResponse = new JSONObject(response.toString());
            String checkoutUrl = jsonResponse.getJSONObject("data")
                .getJSONObject("attributes")
                .getString("checkout_url");

            Log.d(TAG, "Payment URL created: " + checkoutUrl);
            return checkoutUrl;
        } else {
            BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getErrorStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            Log.e(TAG, "PayMongo Error: " + response.toString());
        }
    } catch (Exception e) {
        Log.e(TAG, "Error creating PayMongo link", e);
    }
    return null;
}
```

The createPayMongoPaymentLink method makes a POST request to PayMongo's API using HttpURLConnection. The request includes authentication via Base64 encoding and sends payment details as JSON. Network operations run on a background thread to prevent blocking the UI, with results passed back to the main thread using runOnUiThread.

---

### B.2.2 Membership Activation (SelectMembership.java)

After a successful payment, the membership data needs to be saved to Firestore and the user's profile updated.

```java
// Save membership data after successful payment
private void saveNewMembershipData(String fullName, String paymentMethod) {
    Log.d(TAG, "Starting saveNewMembershipData...");

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

    if (currentUser == null) {
        Log.e(TAG, "Current user is NULL!");
        runOnUiThread(() -> {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            if (loadingProgress != null) loadingProgress.setVisibility(View.GONE);
        });
        return;
    }

    String userId = currentUser.getUid();
    
    // Generate membership timestamps
    Timestamp startTimestamp = Timestamp.now();
    Timestamp expirationTimestamp = getExpirationTimestamp(startTimestamp);

    Log.d(TAG, "Start Date: " + startTimestamp.toDate());
    Log.d(TAG, "Expiration Date: " + expirationTimestamp.toDate());

    // Prepare membership document
    Map<String, Object> membershipData = new HashMap<>();
    membershipData.put("fullname", fullName);
    membershipData.put("userId", userId);
    membershipData.put("email", currentUser.getEmail());
    membershipData.put("membershipPlanType", selectedPlanType);
    membershipData.put("months", selectedMonths);
    membershipData.put("sessions", selectedSessions);
    membershipData.put("price", selectedPrice);
    membershipData.put("membershipStatus", "active");
    membershipData.put("membershipStartDate", startTimestamp);
    membershipData.put("membershipExpirationDate", expirationTimestamp);
    membershipData.put("lastUpdated", Timestamp.now());

    // Add coach info if PT package selected
    if (selectedCoachId != null) {
        membershipData.put("coachId", selectedCoachId);
        membershipData.put("coachName", selectedCoachName);
    } else {
        membershipData.put("coachName", "No coach assigned");
    }

    // Save to memberships collection
    db.collection("memberships")
        .document(userId)
        .set(membershipData)
        .addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Membership document updated for user: " + userId);

            // Update user document
            Map<String, Object> userUpdate = new HashMap<>();
            userUpdate.put("membershipPlanType", selectedPlanType);
            userUpdate.put("membershipActive", true);
            userUpdate.put("membershipStatus", "active");
            userUpdate.put("membershipExpirationDate", expirationTimestamp);
            userUpdate.put("months", selectedMonths);
            userUpdate.put("sessions", selectedSessions);

            if (selectedCoachId != null) {
                userUpdate.put("coachId", selectedCoachId);
            }

            db.collection("users")
                .document(userId)
                .update(userUpdate)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "User document updated successfully");

                    // Add to history collection
                    addMembershipToHistory(fullName, userId, 
                        currentUser.getEmail(), paymentMethod, 
                        startTimestamp, expirationTimestamp);
                });
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Failed to update membership document: " + 
                e.getMessage(), e);
            runOnUiThread(() -> {
                if (loadingProgress != null) 
                    loadingProgress.setVisibility(View.GONE);
                Toast.makeText(SelectMembership.this,
                    "Failed to activate membership. Please try again.",
                    Toast.LENGTH_LONG).show();
            });
        });
}

// Calculate membership expiration date
private Timestamp getExpirationTimestamp(Timestamp startTimestamp) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(startTimestamp.toDate());

    if (selectedMonths > 0) {
        calendar.add(Calendar.MONTH, selectedMonths);
    } else {
        int days = (selectedDurationDays > 0) ? selectedDurationDays : 1;
        calendar.add(Calendar.DAY_OF_MONTH, days);
    }

    Date expirationDate = calendar.getTime();
    return new Timestamp(expirationDate);
}
```

The method updates multiple Firestore collections to maintain consistency - first the memberships collection, then the users collection, and finally adds a record to the history collection. The getExpirationTimestamp helper calculates when the membership should expire based on whether it's a monthly plan or a day-based plan.

---

## B.3 Payment Processing

### B.3.1 WebView Payment Handler (PayMongoPaymentActivity.java)

This activity loads the PayMongo payment page in a WebView and monitors URL changes to detect when payment is completed or cancelled.

```java
// Setup WebView for PayMongo payment page
private void setupWebView() {
    webView.getSettings().setJavaScriptEnabled(true);
    webView.getSettings().setDomStorageEnabled(true);
    webView.getSettings().setSupportZoom(true);
    webView.getSettings().setBuiltInZoomControls(false);

    webView.setWebViewClient(new WebViewClient() {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);

            // Detect payment success or failure from URL
            if (url.contains("success") || url.contains("paid")) {
                handlePaymentSuccess();
            } else if (url.contains("failed") || url.contains("cancelled")) {
                handlePaymentFailure();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, 
                                               WebResourceRequest request) {
            String url = request.getUrl().toString();
            
            // Handle success/failure URLs
            if (url.contains("success") || url.contains("paid")) {
                handlePaymentSuccess();
                return true;
            } else if (url.contains("failed") || url.contains("cancelled")) {
                handlePaymentFailure();
                return true;
            }
            
            return false;
        }
    });
}

// Handle successful payment
private void handlePaymentSuccess() {
    runOnUiThread(() -> {
        Toast.makeText(this, "Payment successful!", 
            Toast.LENGTH_SHORT).show();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("paymentSuccess", true);
        resultIntent.putExtra("paymentMethod", "PayMongo");
        setResult(RESULT_OK, resultIntent);
        finish();
    });
}

// Handle failed payment
private void handlePaymentFailure() {
    runOnUiThread(() -> {
        new AlertDialog.Builder(this)
            .setTitle("Payment Failed")
            .setMessage("Your payment was not successful. " +
                "Please try again or contact support.")
            .setPositiveButton("OK", (dialog, which) -> {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("paymentSuccess", false);
                setResult(RESULT_CANCELED, resultIntent);
                finish();
            })
            .setCancelable(false)
            .show();
    });
}
```

The WebView needs JavaScript enabled for the payment page to work. When a page loads, the URL is checked for keywords like "success" or "failed" to determine the payment outcome. Results are sent back to the calling activity using Intent extras.

---

## B.4 Workout Session Management

### B.4.1 Exercise Timer (WorkoutSessionActivity.java)

The timer system controls exercise duration and provides voice prompts using Android's text-to-speech engine.

```java
// Load and start exercise
private void loadExercise(int index) {
    if (index >= exerciseNames.size()) {
        finishWorkout();
        return;
    }

    currentIndex = index;
    currentExerciseStartTimeMillis = System.currentTimeMillis();

    // Update UI with exercise info
    tvExerciseName.setText(exerciseNames.get(index));
    tvExerciseDetails.setText(exerciseDetails.get(index));

    // Load exercise image with Glide
    String imageUrl = exerciseImageUrls.get(index);
    if (imageUrl != null && !imageUrl.isEmpty()) {
        Glide.with(this)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(ivExerciseImage);
        ivExerciseImage.setVisibility(View.VISIBLE);
        tvNoImage.setVisibility(View.GONE);
    } else {
        ivExerciseImage.setVisibility(View.GONE);
        tvNoImage.setVisibility(View.VISIBLE);
    }

    // Set timer duration
    timeLeftMillis = exerciseDurations.get(index) * 1000L;
    
    updateTimerDisplay();
    updateProgressBar();

    // Start timer and announce exercise
    startTimer();
    speak("Starting " + exerciseNames.get(index));
}

// Countdown timer with voice prompts
private void startTimer() {
    if (timer != null) {
        timer.cancel();
    }

    timer = new CountDownTimer(timeLeftMillis, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            timeLeftMillis = millisUntilFinished;
            updateTimerDisplay();

            // Voice countdown for last 5 seconds
            long secondsLeft = millisUntilFinished / 1000;
            if (secondsLeft <= 5 && secondsLeft > 0) {
                speak(String.valueOf(secondsLeft));
            }
        }

        @Override
        public void onFinish() {
            timeLeftMillis = 0;
            updateTimerDisplay();
            
            // Save performance data
            saveExercisePerformance();
            
            // Move to rest or next exercise
            startRestOrNext();
        }
    }.start();

    isTimerRunning = true;
    btnPause.setText("Pause");
}

// Text-to-speech helper
private void speak(String text) {
    if (tts != null && isTTSReady) {
        String utteranceId = "TTS_" + System.currentTimeMillis();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }
}

// Update timer display
private void updateTimerDisplay() {
    int minutes = (int) (timeLeftMillis / 1000) / 60;
    int seconds = (int) (timeLeftMillis / 1000) % 60;
    
    String timeFormatted = String.format(Locale.getDefault(), 
        "%02d:%02d", minutes, seconds);
    tvExerciseTimer.setText(timeFormatted);
}
```

The loadExercise method sets up each exercise by updating the UI elements and starting the countdown timer. Images are loaded using the Glide library for efficient caching. When the timer finishes, it automatically saves performance data and moves to either a rest period or the next exercise.

---

### B.4.2 Workout Completion (WorkoutSessionActivity.java)

When a workout session finishes, the system saves a summary to Firestore for tracking progress over time.

```java
// Save workout session to Firestore
private void finishWorkout() {
    if (currentUser == null) {
        Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        finish();
        return;
    }

    String userId = currentUser.getUid();
    long workoutDuration = System.currentTimeMillis() - workoutStartTime;
    
    // Prepare workout summary
    Map<String, Object> workoutData = new HashMap<>();
    workoutData.put("userId", userId);
    workoutData.put("completedAt", System.currentTimeMillis());
    workoutData.put("duration", workoutDuration);
    workoutData.put("exerciseCount", exerciseNames.size());
    workoutData.put("workoutType", getIntent().getStringExtra("workoutType"));

    // Save to Firestore
    firestore.collection("workoutHistory")
        .add(workoutData)
        .addOnSuccessListener(documentReference -> {
            Log.d(TAG, "Workout saved successfully: " + 
                documentReference.getId());
            
            // Show completion dialog
            showCompletionDialog(workoutDuration);
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Error saving workout", e);
            Toast.makeText(this, "Error saving workout history", 
                Toast.LENGTH_SHORT).show();
            finish();
        });
}

// Display workout completion summary
private void showCompletionDialog(long duration) {
    int minutes = (int) (duration / 1000) / 60;
    int seconds = (int) (duration / 1000) % 60;
    
    String message = String.format(Locale.getDefault(),
        "Duration: %d min %d sec\nExercises: %d",
        minutes, seconds, exerciseNames.size());

    new AlertDialog.Builder(this)
        .setTitle("Workout Complete!")
        .setMessage(message)
        .setPositiveButton("Finish", (dialog, which) -> {
            finish();
        })
        .setCancelable(false)
        .show();
}
```

The finishWorkout method calculates the total duration and creates a document in the workoutHistory collection. A dialog then displays the workout summary before returning to the main screen.

---

## B.5 Backend Automation (Cloud Functions)

### B.5.1 Membership Expiration Job (functions/index.js)

This Cloud Function runs automatically every day to check for and expire memberships that have passed their expiration date.

```javascript
const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

/**
 * Scheduled function that runs once per day at midnight (Philippine time)
 * Expires all memberships that have passed their expiration date
 */
exports.expireMemberships = functions.pubsub
  .schedule("every 24 hours")
  .timeZone("Asia/Manila")
  .onRun(async (context) => {
    const now = admin.firestore.Timestamp.now();
    const membershipsRef = admin.firestore().collection("memberships");

    // Query all active memberships that have expired
    const snapshot = await membershipsRef
      .where("membershipStatus", "==", "active")
      .where("membershipExpirationDate", "<", now)
      .get();

    if (snapshot.empty) {
      console.log("✅ No memberships to expire today.");
      return null;
    }

    // Batch update expired memberships
    const batch = admin.firestore().batch();
    snapshot.forEach((doc) => {
      batch.update(doc.ref, { 
        membershipStatus: "expired",
        lastUpdated: now
      });
    });

    await batch.commit();
    console.log(`✅ Expired ${snapshot.size} memberships.`);
    
    return null;
  });

/**
 * Triggered when a new workout is completed
 * Can be used to update user statistics or send notifications
 */
exports.onWorkoutCompleted = functions.firestore
  .document("workoutHistory/{workoutId}")
  .onCreate(async (snap, context) => {
    const workoutData = snap.data();
    const userId = workoutData.userId;

    // Query recent workouts for this user
    const recentWorkouts = await admin.firestore()
      .collection("workoutHistory")
      .where("userId", "==", userId)
      .orderBy("completedAt", "desc")
      .limit(7)
      .get();

    // Check for 7-day workout streak
    if (recentWorkouts.size >= 7) {
      const notificationData = {
        userId: userId,
        title: "7-Day Streak!",
        message: "Congratulations on completing 7 workouts!",
        type: "achievement",
        timestamp: admin.firestore.Timestamp.now(),
        isRead: false
      };

      await admin.firestore()
        .collection("notifications")
        .add(notificationData);
    }

    return null;
  });
```

The function uses Cloud Pub/Sub to schedule execution at midnight Philippine time. It queries for all active memberships with expiration dates in the past, then uses batch updates to change their status to "expired" efficiently. The onWorkoutCompleted trigger checks if a user has completed 7 workouts in a row and creates an achievement notification.

---

## B.6 Firestore Security Rules (firestore.rules)

The security rules control who can read and write data in each Firestore collection.

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Check if user is authenticated
    function isSignedIn() {
      return request.auth != null;
    }
    
    // Check if user owns the resource
    function isOwner(userId) {
      return isSignedIn() && request.auth.uid == userId;
    }
    
    // Check if user is a coach
    function isCoach() {
      return isSignedIn() && 
        get(/databases/$(database)/documents/users/$(request.auth.uid))
        .data.userType == 'coach';
    }
    
    // Check if user is admin
    function isAdmin() {
      return isSignedIn() && 
        get(/databases/$(database)/documents/users/$(request.auth.uid))
        .data.userType == 'admin';
    }

    // Users collection
    match /users/{userId} {
      allow read: if isSignedIn();
      allow create: if isSignedIn();
      allow update: if isOwner(userId) || isCoach() || isAdmin();
      allow delete: if isAdmin();
    }

    // Memberships collection
    match /memberships/{membershipId} {
      allow read: if isSignedIn();
      allow write: if isOwner(membershipId) || isAdmin();
    }

    // Workout history collection
    match /workoutHistory/{workoutId} {
      allow read: if isSignedIn();
      allow create: if isSignedIn();
      allow update, delete: if isOwner(resource.data.userId) || isAdmin();
    }

    // Packages collection - anyone can view, only admin can modify
    match /packages/{packageId} {
      allow read: if true;
      allow write: if isAdmin();
    }

    // Payment history subcollection
    match /users/{userId}/paymentHistory/{paymentId} {
      allow read: if isOwner(userId) || isAdmin();
      allow create: if isOwner(userId);
      allow update, delete: if isAdmin();
    }

    // Coaches collection
    match /coaches/{coachId} {
      allow read: if isSignedIn();
      allow write: if isCoach() || isAdmin();
    }

    // Notifications collection
    match /notifications/{notificationId} {
      allow read: if isSignedIn() && 
        resource.data.userId == request.auth.uid;
      allow create: if isSignedIn();
      allow update: if isOwner(resource.data.userId);
      allow delete: if isOwner(resource.data.userId) || isAdmin();
    }
  }
}
```

The rules use helper functions to check user roles and ownership. This keeps the actual permission rules readable while reusing common checks. For example, workout history can be read by anyone logged in, created by any user, but only modified or deleted by the owner or an admin.

---

## B.7 Gradle Dependencies (app/build.gradle.kts)

The build configuration specifies which libraries and versions the app uses.

```kotlin
android {
    namespace = "com.example.signuploginrealtime"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.signuploginrealtime"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Firebase BOM manages versions
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    
    // Firebase services
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-database")
    
    // UI libraries
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.cardview:cardview:1.0.0")
    
    // Image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    
    // QR Code scanning
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
```

Using Firebase BOM (Bill of Materials) means we don't need to specify version numbers for individual Firebase libraries - they're all automatically compatible with each other. The minimum SDK is set to 26 (Android 8.0) to support the modern features we're using like notification channels and improved background processing.

---

## B.8 Workout Generation System

### B.8.1 Personalized Workout Generation (WorkoutList.java)

The workout generation system creates custom workout plans based on user profile data including fitness level, health issues, and weekly progress.

```java
// Generate personalized workout from available exercises
private void generateWorkout(List<ExerciseInfo> availableExercises) {
    if (currentUser == null) {
        Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        return;
    }

    String uid = currentUser.getUid();
    DocumentReference userDocRef = firestore.collection("users").document(uid);

    // Get difficulty multiplier from user preferences
    float savedMultiplier = workoutPrefs.getFloat("workout_difficulty_multiplier", 1.0f);
    final double difficultyMultiplier = WorkoutAdjustmentHelper.getDifficultyMultiplier(savedMultiplier);

    Log.d(TAG, "Using difficulty multiplier: " + difficultyMultiplier);

    userDocRef.get().addOnSuccessListener(userSnapshot -> {
        if (!userSnapshot.exists()) {
            Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
            return;
        }

        updateUserProfileFromFirestore(userSnapshot);

        // Check for multiplier in Firestore (if adjusted on another device)
        Double firestoreMultiplier = userSnapshot.getDouble("workoutDifficultyMultiplier");
        final double finalMultiplier = (firestoreMultiplier != null) ? 
            firestoreMultiplier : difficultyMultiplier;

        if (firestoreMultiplier != null && firestoreMultiplier != savedMultiplier) {
            // Sync local preference with Firestore
            workoutPrefs.edit()
                .putFloat("workout_difficulty_multiplier", firestoreMultiplier.floatValue())
                .apply();
        }

        Long profileLastModified = userSnapshot.getLong("profileLastModified");

        // Reference to current week's workout
        DocumentReference workoutRef = firestore.collection("users")
                .document(uid)
                .collection("currentWorkout")
                .document("week_" + userProfile.getCurrentWeek());

        workoutRef.get().addOnSuccessListener(workoutSnapshot -> {
            boolean shouldRegenerate = false;

            // Check if workout needs regeneration
            if (!workoutSnapshot.exists()) {
                Log.d(TAG, "No existing workout found. Generating new workout.");
                shouldRegenerate = true;
            } else if (Boolean.TRUE.equals(workoutSnapshot.getBoolean("completed"))) {
                Log.d(TAG, "Previous workout completed. Generating new workout.");
                shouldRegenerate = true;
            } else {
                Long workoutCreatedAt = workoutSnapshot.getLong("createdAt");

                // Check if profile was updated after workout creation
                if (profileLastModified != null && workoutCreatedAt != null
                        && profileLastModified > workoutCreatedAt) {
                    Log.d(TAG, "Profile changed after workout creation. Regenerating workout.");
                    Toast.makeText(this, "Your profile has changed. Generating new personalized workout...",
                            Toast.LENGTH_LONG).show();
                    shouldRegenerate = true;
                } else {
                    // Check if difficulty was adjusted
                    Long lastAdjustmentTime = workoutPrefs.getLong("last_adjustment_timestamp", 0);
                    if (lastAdjustmentTime > 0 && workoutCreatedAt != null 
                            && lastAdjustmentTime > workoutCreatedAt) {
                        Log.d(TAG, "Difficulty adjusted after workout creation. Regenerating workout.");
                        Toast.makeText(this, "Workout difficulty adjusted. Generating new workout...",
                                Toast.LENGTH_LONG).show();
                        shouldRegenerate = true;
                    } else {
                        // Load existing workout
                        Log.d(TAG, "Loading existing workout (profile unchanged).");
                        currentWorkoutExercises = workoutSnapshot
                            .toObject(WorkoutWrapper.class)
                            .toWorkoutExercises();
                        showExercises(currentWorkoutExercises);
                        startWorkoutButton.setEnabled(true);
                        return;
                    }
                }
            }

            if (shouldRegenerate) {
                // Convert to model format
                com.example.signuploginrealtime.models.UserProfile modelProfile = 
                    convertToModel(userProfile);

                // Generate base workout based on user profile
                Workout baseWorkout = AdvancedWorkoutDecisionMaker.generatePersonalizedWorkout(
                        availableExercises, modelProfile);

                // Apply weekly progression
                Workout progressedWorkout = WorkoutProgression.generateProgressiveWorkout(
                        baseWorkout,
                        userProfile.getCurrentWeek(),
                        modelProfile
                );

                // Apply difficulty adjustment
                Workout finalWorkout = progressedWorkout;
                if (finalMultiplier != 1.0) {
                    Log.d(TAG, "Applying difficulty adjustment: " + finalMultiplier);
                    finalWorkout = WorkoutAdjustmentHelper.adjustWorkoutDifficulty(
                            progressedWorkout,
                            finalMultiplier
                    );
                }

                // Save and display the generated workout
                currentWorkoutExercises = finalWorkout.getExercises();
                saveWorkoutToFirestore(finalWorkout);
                showExercises(currentWorkoutExercises);
                startWorkoutButton.setEnabled(true);
            }
        });
    });
}
```

The system checks multiple conditions before deciding whether to generate a new workout or load an existing one. It regenerates when the profile changes, difficulty is adjusted, or the previous week's workout is completed. The generation process involves three stages: base workout creation, weekly progression, and difficulty adjustment.

---

### B.8.2 Exercise Filtering Logic (AdvancedWorkoutDecisionMaker.java)

The workout decision maker filters exercises based on fitness level and health conditions to ensure safe workouts.

```java
// Filter exercises based on user's fitness level and health issues
private static List<ExerciseInfo> filterExercisesByFitnessLevel(
        List<ExerciseInfo> exercises,
        UserProfile userProfile) {

    List<ExerciseInfo> suitable = new ArrayList<>();
    String level = userProfile.getFitnessLevel().toLowerCase();

    for (ExerciseInfo exercise : exercises) {
        if (exercise == null || exercise.getName() == null) continue;

        String nameLower = exercise.getName().toLowerCase();
        List<String> equipments = exercise.getEquipments();

        boolean isSafe = true;

        // SEDENTARY: Very restrictive filtering
        if (level.equals("sedentary")) {
            // Block all plyometric/explosive movements
            if (nameLower.contains("jump") || nameLower.contains("hop") ||
                    nameLower.contains("burpee") || nameLower.contains("plyometric") ||
                    nameLower.contains("explosive") || nameLower.contains("box") ||
                    nameLower.contains("sprint") || nameLower.contains("mountain climber")) {
                isSafe = false;
            }

            // Block advanced bodyweight exercises
            if (nameLower.contains("pull up") || nameLower.contains("chin up") ||
                    nameLower.contains("muscle up") || nameLower.contains("handstand") ||
                    nameLower.contains("pistol") || nameLower.contains("dragon flag")) {
                isSafe = false;
            }

            // Block barbell exercises
            if (equipments != null && equipments.contains("barbell")) {
                isSafe = false;
            }

            // Block loaded squats/lunges
            if ((nameLower.contains("squat") || nameLower.contains("lunge")) &&
                    equipments != null && !equipments.contains("body weight")) {
                isSafe = false;
            }
        }

        // LIGHTLY ACTIVE: Moderate restrictions
        else if (level.equals("lightly active")) {
            if (nameLower.contains("burpee") || nameLower.contains("box jump") ||
                    nameLower.contains("plyometric") || nameLower.contains("explosive") ||
                    nameLower.contains("muscle up") || nameLower.contains("handstand") ||
                    nameLower.contains("pistol squat")) {
                isSafe = false;
            }

            // Block olympic lifts
            if (nameLower.contains("clean") || nameLower.contains("snatch") ||
                    nameLower.contains("jerk")) {
                isSafe = false;
            }
        }

        // HEALTH ISSUE FILTERING
        if (userProfile.getHealthIssues() != null) {
            for (String issue : userProfile.getHealthIssues()) {
                issue = issue.toLowerCase();

                // Knee issues
                if (issue.contains("knee")) {
                    if (nameLower.contains("squat") || nameLower.contains("lunge") ||
                            nameLower.contains("jump") || nameLower.contains("leg press")) {
                        isSafe = false;
                    }
                }

                // Back/spine issues
                if (issue.contains("back") || issue.contains("spine")) {
                    if (nameLower.contains("deadlift") || 
                            nameLower.contains("good morning") ||
                            nameLower.contains("bent over") || 
                            nameLower.contains("hyperextension")) {
                        isSafe = false;
                    }
                }

                // Shoulder issues
                if (issue.contains("shoulder")) {
                    if (nameLower.contains("overhead press") || 
                            nameLower.contains("snatch") ||
                            nameLower.contains("handstand")) {
                        isSafe = false;
                    }
                }

                // Wrist issues
                if (issue.contains("wrist")) {
                    if (nameLower.contains("push up") || nameLower.contains("plank") ||
                            nameLower.contains("handstand")) {
                        isSafe = false;
                    }
                }
            }
        }

        if (isSafe) {
            suitable.add(exercise);
        }
    }

    return suitable.isEmpty() ? exercises : suitable;
}

// Generate workout with filtered exercises
public static Workout generatePersonalizedWorkout(
        List<ExerciseInfo> availableExercises,
        UserProfile userProfile) {

    // Filter exercises based on fitness level and health
    List<ExerciseInfo> suitableExercises = filterExercisesByFitnessLevel(
            availableExercises,
            userProfile
    );

    // Prioritize equipment for sedentary users
    if (userProfile.getFitnessLevel().toLowerCase().equals("sedentary") &&
            suitableExercises.size() > 10) {

        List<ExerciseInfo> prioritized = new ArrayList<>();

        // Priority 1: Bodyweight exercises
        for (ExerciseInfo e : suitableExercises) {
            if (e.getEquipments() != null &&
                    e.getEquipments().contains("body weight")) {
                prioritized.add(e);
            }
        }

        // Priority 2: Resistance bands
        for (ExerciseInfo e : suitableExercises) {
            if (e.getEquipments() != null &&
                    e.getEquipments().contains("band") &&
                    !prioritized.contains(e)) {
                prioritized.add(e);
            }
        }

        // Use prioritized list if we have enough exercises
        if (prioritized.size() >= 5) {
            suitableExercises = prioritized;
        }
    }

    // Create workout plan with selected exercises
    Workout workout = new Workout();
    workout.setExercises(selectBalancedExercises(suitableExercises, userProfile));
    
    return workout;
}
```

The filtering system uses multiple layers of safety checks. For sedentary users, it blocks high-impact movements and advanced exercises. It also respects health conditions - if someone has knee problems, squats and lunges are automatically excluded. The prioritization system ensures beginners start with bodyweight exercises before progressing to equipment-based training.

---

## B.9 Admin Web Dashboard (JavaScript)

The administrative web dashboard provides gym staff with real-time monitoring and management capabilities. Built with vanilla JavaScript and Firebase SDK, it connects to the same Firestore database as the mobile app.

### B.9.1 Admin Authentication (login.js)

The login system verifies that only authorized administrators can access the dashboard.

```javascript
// Admin login with role verification
import { auth, firestore } from "./firebaseSDK.js";
import { onAuthStateChanged, signInWithEmailAndPassword, 
         collection, query, where, getDocs } from "./firebaseSDK.js";

const loginForm = document.getElementById("login-form");
const emailInput = document.getElementById("email");
const passwordInput = document.getElementById("password");
const loginButton = loginForm.querySelector('button[type="submit"]');

// Check if user has admin privileges
async function isAdmin(email) {
    try {
        const adminCol = collection(firestore, "admin");
        const q = query(adminCol, 
            where("email", "==", email), 
            where("userType", "==", "admin")
        );
        const querySnapshot = await getDocs(q);
        
        return !querySnapshot.empty;
    } catch (error) {
        console.error("Error checking admin status:", error);
        return false;
    }
}

// Show loading state on login button
function setLoggingIn(isLoggingIn) {
    const spinner = loginButton.querySelector(".btn-spinner");
    const txt = loginButton.querySelector(".btn-text");
    
    if (isLoggingIn) {
        loginButton.dataset.origText = txt.textContent;
        txt.textContent = "Logging in";
        loginButton.disabled = true;
        if (spinner) spinner.classList.remove("hidden");
    } else {
        if (loginButton.dataset.origText) 
            txt.textContent = loginButton.dataset.origText;
        loginButton.disabled = false;
        if (spinner) spinner.classList.add("hidden");
    }
}

// Monitor authentication state
onAuthStateChanged(auth, async (user) => {
    if (user) {
        console.log("User logged in, checking admin status...");
        
        if (await isAdmin(user.email)) {
            console.log("Admin verified, redirecting to dashboard.");
            showToast("Signed in successfully.", "success");
            window.location.href = "Dashboard.html";
        } else {
            console.warn("User is not an admin. Signing out:", user.email);
            await signOut(auth);
            showToast("Access denied: Administrator privileges required.", "error");
        }
    }
});

// Handle login form submission
loginForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const email = emailInput.value;
    const password = passwordInput.value;
    
    setLoggingIn(true);
    
    try {
        await signInWithEmailAndPassword(auth, email, password);
        // onAuthStateChanged will handle redirect
    } catch (error) {
        console.error("Login Error:", error.code, error.message);
        
        if (error.code === "auth/invalid-credential" || 
            error.code === "auth/wrong-password" || 
            error.code === "auth/user-not-found") {
            showToast("Invalid email or password.", "error");
        } else {
            showToast("An unexpected error occurred. Please try again.", "error");
        }
        setLoggingIn(false);
    }
});
```

The authentication flow checks two things: first, Firebase Authentication validates the credentials, then the system queries the admin collection to verify the user has admin privileges. Non-admin users are automatically signed out even if they have valid credentials.

---

### B.9.2 Member Management (membership.js)

The membership page displays all users and coaches in a sortable table with real-time updates.

```javascript
// Real-time member and coach listing
import { firestore, collection, onSnapshot } from "../firebaseSDK.js";

const membershipTableBody = document.getElementById("membershipTableBody");
let membershipData = {};

// Sort state management
let sortState = {
    active: { column: "fullname", direction: "asc" },
    fullname: "asc",
    type: "asc",
    status: "asc",
};

// Load members with real-time updates
function loadMembersRealtime() {
    const usersCol = collection(firestore, "users");
    const coachesCol = collection(firestore, "coaches");
    const membershipsCol = collection(firestore, "memberships");
    
    let usersData = [];
    let coachesData = [];
    
    const renderTable = () => {
        if (!membershipTableBody) {
            console.error("Table body element not found");
            return;
        }
        
        const allMembers = [...usersData, ...coachesData];
        membershipTableBody.innerHTML = "";
        
        if (allMembers.length === 0) {
            document.getElementById("noMembersMessage").style.display = "block";
            return;
        }
        
        // Apply sorting
        allMembers.sort((a, b) => {
            let valA, valB;
            
            switch (sortState.active.column) {
                case "type":
                    valA = a.isCoach ? "coach" : "member";
                    valB = b.isCoach ? "coach" : "member";
                    break;
                case "status":
                    const membershipA = membershipData[a.docId] || {};
                    const membershipB = membershipData[b.docId] || {};
                    valA = a.isCoach ? "active" : 
                        (membershipA.membershipStatus || "inactive").toLowerCase();
                    valB = b.isCoach ? "active" : 
                        (membershipB.membershipStatus || "inactive").toLowerCase();
                    break;
                default: // fullname
                    valA = (a.fullname || "").toLowerCase();
                    valB = (b.fullname || "").toLowerCase();
                    break;
            }
            
            let comparison = 0;
            if (valA > valB) {
                comparison = 1;
            } else if (valA < valB) {
                comparison = -1;
            }
            
            return sortState.active.direction === "desc" ? 
                comparison * -1 : comparison;
        });
        
        // Render table rows
        allMembers.forEach((member) => {
            const docId = member.docId;
            const isCoach = member.isCoach;
            const userType = isCoach ? "coach" : member.userType || "Member";
            
            const membership = membershipData[docId] || {};
            const membershipType = isCoach ? "None" : 
                membership.membershipPlanType || "None";
            
            let status;
            if (isCoach) {
                status = "active";
            } else {
                const memberStatus = (membership.membershipStatus || "").toLowerCase();
                status = memberStatus === "active" ? "active" : "inactive";
            }
            
            const statusColor = status === "active" ? "#22c55e" : "#ef4444";
            const profilePictureUrl = member.profilePictureUrl || "Images/blankProfile.png";
            
            const row = document.createElement("tr");
            row.innerHTML = `
                <td class="profile-col">
                    <img src="${profilePictureUrl}" alt="Profile Picture">
                </td>
                <td>${userType}</td>
                <td>${member.fullname || ""}</td>
                <td>${member.email || ""}</td>
                <td>${member.phone || ""}</td>
                <td class="membership-cell" data-user-id="${docId}">
                    ${membershipType}
                </td>
                <td style="color: ${statusColor}; font-weight: 500;">
                    ${status}
                </td>
                <td>
                    <button class="archive-btn" title="Archive" 
                        data-id="${docId}" data-email="${member.email || ""}">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" 
                             fill="currentColor" viewBox="0 0 16 16">
                            <path d="M0 2a1 1 0 0 1 1-1h14a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1v7.5a2.5 2.5 0 0 1-2.5 2.5h-9A2.5 2.5 0 0 1 1 12.5V5a1 1 0 0 1-1-1V2zm2 3v7.5A1.5 1.5 0 0 0 3.5 14h9a1.5 1.5 0 0 0 1.5-1.5V5H2zm13-3H1v2h14V2zM5 7.5a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5z"/>
                        </svg>
                    </button>
                </td>
            `;
            
            // Add click handler to open edit modal
            row.addEventListener("click", (e) => {
                if (!e.target.classList.contains("archive-btn") && 
                    !e.target.closest(".archive-btn")) {
                    openEditModal(docId, member, isCoach, membershipData);
                }
            });
            
            membershipTableBody.appendChild(row);
        });
        
        setupArchiveButtons();
    };
    
    // Subscribe to real-time updates for users
    const unsubscribeUsers = onSnapshot(usersCol, (snapshot) => {
        usersData = snapshot.docs.map((doc) => ({
            ...doc.data(),
            docId: doc.id,
            isCoach: false
        }));
        renderTable();
    });
    
    // Subscribe to real-time updates for coaches
    const unsubscribeCoaches = onSnapshot(coachesCol, (snapshot) => {
        coachesData = snapshot.docs.map((doc) => ({
            ...doc.data(),
            docId: doc.id,
            isCoach: true
        }));
        renderTable();
    });
    
    // Subscribe to real-time updates for memberships
    const unsubscribeMemberships = onSnapshot(membershipsCol, (snapshot) => {
        membershipData = {};
        snapshot.docs.forEach((doc) => {
            membershipData[doc.id] = doc.data();
        });
        renderTable();
    });
    
    // Cleanup listeners on page unload
    window.addEventListener("beforeunload", () => {
        unsubscribeUsers();
        unsubscribeCoaches();
        unsubscribeMemberships();
    });
}

// Initialize when page loads
document.addEventListener("DOMContentLoaded", () => {
    onAuthStateChanged(auth, (user) => {
        if (user) {
            loadMembersRealtime();
        } else {
            window.location.href = "Login.html";
        }
    });
});
```

The system uses three separate Firestore listeners (users, coaches, memberships) that all trigger the same render function when data changes. This means the table updates automatically when memberships are purchased, users register, or coaches are added - no page refresh needed.

---

### B.9.3 Dashboard Analytics (cards.js)

The dashboard displays real-time metrics using Firestore snapshot listeners.

```javascript
// Real-time dashboard metrics
import { firestore, collection, query, where, onSnapshot } from "../firebaseSDK.js";

// Initialize metrics cards on page load
document.addEventListener("DOMContentLoaded", () => {
    initMetricsCards();
});

function initMetricsCards() {
    // Track active memberships
    const membersRef = collection(firestore, "users");
    const activeQuery = query(membersRef, 
        where("membershipStatus", "==", "active")
    );
    
    onSnapshot(activeQuery, (snapshot) => {
        const activeMembershipsCount = snapshot.size;
        document.querySelector("#box1 .metric-value").textContent = 
            activeMembershipsCount;
    }, (error) => {
        console.error("Error monitoring active memberships:", error);
    });
    
    // Track today's check-ins
    const today = new Date();
    const todayStr = today.toISOString().split("T")[0]; // YYYY-MM-DD format
    
    const attendanceRef = collection(firestore, "attendance");
    const todayQuery = query(attendanceRef, 
        where("date", "==", todayStr)
    );
    
    onSnapshot(todayQuery, (snapshot) => {
        const todayCheckins = snapshot.size;
        document.querySelector("#box2 .metric-value").textContent = todayCheckins;
    }, (error) => {
        console.error("Error monitoring today's check-ins:", error);
    });
    
    // Track new members this month
    trackNewMembersThisMonth();
}

// Count new members who joined this month
function trackNewMembersThisMonth() {
    const usersRef = collection(firestore, "users");
    
    onSnapshot(usersRef, (snapshot) => {
        const today = new Date();
        const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
        let newMembersThisMonth = 0;
        
        snapshot.forEach((doc) => {
            const data = doc.data();
            
            if (data.profileCompletedAt) {
                let profileDate;
                
                // Handle Firestore Timestamp
                if (typeof data.profileCompletedAt.toDate === "function") {
                    profileDate = data.profileCompletedAt.toDate();
                }
                // Handle string date format
                else if (typeof data.profileCompletedAt === "string") {
                    const dateParts = data.profileCompletedAt
                        .split(" at ")[0]
                        .split(" ");
                    
                    if (dateParts.length >= 3) {
                        const day = parseInt(dateParts[1].replace(",", ""));
                        const month = dateParts[0];
                        const year = parseInt(dateParts[2]);
                        
                        const months = ["January", "February", "March", "April", 
                                       "May", "June", "July", "August", 
                                       "September", "October", "November", "December"];
                        const monthIndex = months.indexOf(month);
                        
                        if (monthIndex !== -1) {
                            profileDate = new Date(year, monthIndex, day);
                        }
                    }
                }
                
                // Check if profile was completed this month
                if (profileDate && 
                    profileDate.getMonth() === today.getMonth() && 
                    profileDate.getFullYear() === today.getFullYear()) {
                    newMembersThisMonth++;
                }
            }
        });
        
        document.querySelector("#box3 .metric-value").textContent = 
            newMembersThisMonth;
    }, (error) => {
        console.error("Error monitoring new members:", error);
    });
}
```

The dashboard uses Firestore's onSnapshot listeners to get real-time updates. When a member checks in or purchases a membership, the metrics update immediately without requiring a page refresh. The new members calculation handles both Firestore Timestamp objects and string date formats for compatibility.

