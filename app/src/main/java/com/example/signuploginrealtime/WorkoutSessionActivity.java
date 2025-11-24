package com.example.signuploginrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.util.Log; // Added for logging if needed
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import androidx.annotation.NonNull;
import com.example.signuploginrealtime.models.ExerciseInfo;
import java.util.List;
import java.util.Map;

public class WorkoutSessionActivity extends AppCompatActivity {

    private TextView tvExerciseName, tvExerciseDetails, tvExerciseTimer, tvNoImage;
    private TextView btnPrevious, btnSkip;
    private ImageView ivExerciseImage;
    private Button btnPause;
    private ProgressBar progressBar;

    private ArrayList<String> exerciseNames;
    private ArrayList<String> exerciseDetails;
    private ArrayList<String> exerciseImageUrls;
    private ArrayList<Integer> exerciseDurations;
    private ArrayList<Integer> exerciseRests;

    private int currentIndex = 0;
    private CountDownTimer timer;
    private CountDownTimer repTimer;
    private CountDownTimer readyTimer;
    private boolean isTimerRunning = false;
    private boolean isReadyCountdown = false;
    private long timeLeftMillis;
    private long workoutStartTime;

    private ImageView ivInfo;
    private TextView tvInstructions;
    private FrameLayout flInstructionsOverlay;

    private TextToSpeech tts;
    private int currentRepCount = 0;
    private boolean isRepetitionBased = false;
    private boolean isCounterPaused = false;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    private ArrayList<ExercisePerformanceData> performanceDataList;
    private long currentExerciseStartTimeMillis;
    private final java.util.Map<String, Runnable> utteranceCallbacks = new java.util.HashMap<>();

    private static final String TAG = "WorkoutSessionActivity"; // For logging
    private boolean isTTSReady = false;

    private TextView btnEquipmentMode, btnNoEquipmentMode;
    private Button btnSwitchWorkout;
    private boolean isNoEquipmentMode = false;
    private boolean hasBodyweightAlternative = false;
    private boolean isReplacingExercise = false;

    // Compact weight suggestion UI
    private android.widget.LinearLayout weightSuggestionContainer;
    private android.widget.EditText etWeightQuick;
    private android.widget.Spinner spinnerWeightUnitQuick;

    private ArrayList<String> originalExerciseNames;
    private ArrayList<String> originalExerciseDetails;
    private ArrayList<String> originalExerciseImageUrls;

    private ArrayList<Integer> completedSetsPerExercise; // Track completed sets for each exercise
    private ArrayList<Integer> totalSetsPerExercise; // Total sets needed for each exercise
    private long lastModeSwitch = 0;
    private static final long MODE_SWITCH_COOLDOWN = 500; // 500ms cooldown

    // Track weights used for each exercise
    private final java.util.Map<Integer, Double> exerciseWeights = new java.util.HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_session);

        performanceDataList = new ArrayList<>();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());
                isTTSReady = true;
                tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) {}
                    @Override public void onError(String utteranceId) {}
                    @Override
                    public void onDone(String utteranceId) {
                        runOnUiThread(() -> {
                            Runnable next = utteranceCallbacks.remove(utteranceId);
                            if (next != null) next.run();
                        });
                    }
                });
            } else {
                isTTSReady = false;
                Log.e(TAG, "TTS initialization failed");
            }
        });

        View rootView = findViewById(R.id.root_layout);
        rootView.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(
                    v.getPaddingLeft(),
                    insets.getSystemWindowInsetTop(),
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );
            return insets.consumeSystemWindowInsets();
        });

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // STEP 1: Initialize views
        tvExerciseName = findViewById(R.id.tv_exercise_name);
        tvExerciseDetails = findViewById(R.id.tv_exercise_details);
        tvExerciseTimer = findViewById(R.id.tv_exercise_timer);
        ivExerciseImage = findViewById(R.id.iv_exercise_image);
        tvNoImage = findViewById(R.id.tv_no_image);
        btnPause = findViewById(R.id.btn_pause);
        btnPrevious = findViewById(R.id.btn_previous);
        btnSkip = findViewById(R.id.btn_skip);
        progressBar = findViewById(R.id.progress_bar);
        ivInfo = findViewById(R.id.iv_info);
        tvInstructions = findViewById(R.id.tv_instructions);
        flInstructionsOverlay = findViewById(R.id.fl_instructions_overlay);

        // STEP 2: Load exercise data FIRST
        exerciseNames = getIntent().getStringArrayListExtra("exerciseNames");
        exerciseDetails = getIntent().getStringArrayListExtra("exerciseDetails");
        exerciseImageUrls = getIntent().getStringArrayListExtra("exerciseImageUrls");
        exerciseDurations = getIntent().getIntegerArrayListExtra("exerciseTimes");
        exerciseRests = getIntent().getIntegerArrayListExtra("exerciseRests");

        originalExerciseNames = new ArrayList<>(exerciseNames);
        originalExerciseDetails = new ArrayList<>(exerciseDetails);
        originalExerciseImageUrls = new ArrayList<>(exerciseImageUrls);

        // STEP 3: Initialize defaults
        if (exerciseDurations == null && exerciseNames != null) {
            exerciseDurations = new ArrayList<>();
            for (int i = 0; i < exerciseNames.size(); i++) exerciseDurations.add(30);
        }

        if (exerciseRests == null && exerciseNames != null) {
            exerciseRests = new ArrayList<>();
            for (int i = 0; i < exerciseNames.size(); i++) exerciseRests.add(60);
        }

        // STEP 4: Validate data
        if (exerciseNames == null || exerciseDetails == null || exerciseImageUrls == null
                || exerciseNames.size() != exerciseDetails.size()
                || exerciseNames.size() != exerciseImageUrls.size()) {
            Toast.makeText(this, "Exercise data invalid!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // STEP 5: Initialize equipment mode (NOW data is loaded)
        initializeEquipmentModeCard();
        initializeSetsTracking();

        // ✅ STEP 5.5: Initialize performance tracking
        performanceDataList = getIntent().hasExtra("performanceData")
                ? (ArrayList<ExercisePerformanceData>) getIntent().getSerializableExtra("performanceData")
                : new ArrayList<>();

        // STEP 6: Set time and index (DO THIS ONLY ONCE)
        workoutStartTime = getIntent().getLongExtra("workoutStartTime", System.currentTimeMillis());
        currentIndex = getIntent().getIntExtra("currentIndex", 0);

        // STEP 7: Show exercise (DO THIS ONLY ONCE AT THE END)
        showExercise(currentIndex);


        btnPause.setOnClickListener(v -> {
            if (btnPause.getText().equals("START")) {
                // Starting fresh after equipment mode switch
                startReadyCountdown();
                return;
            }

            if (isReadyCountdown) {
                if (isTimerRunning) {
                    stopAllTTS();
                    isTimerRunning = false;
                    btnPause.setText("RESUME");
                } else {
                    startReadyCountdown();
                }
            } else if (isRepetitionBased) {
                if (isCounterPaused) {
                    resumeRepCounter();
                } else {
                    pauseRepCounter();
                }
            } else {
                if (isTimerRunning) {
                    if (timer != null) timer.cancel();
                    isTimerRunning = false;
                    btnPause.setText("RESUME");
                } else {
                    startTimer((int) (timeLeftMillis / 1000));
                }
            }
        });

        btnPrevious.setOnClickListener(v -> moveToPreviousExercise());
        btnSkip.setOnClickListener(v -> skipCurrentExercise());

        ivInfo.setOnClickListener(v -> {
            if (exerciseDetails != null && currentIndex < exerciseDetails.size()) {
                String fullInstructions = getFullExerciseInstructions(currentIndex);
                showInstructionsDialog(fullInstructions);
            }
        });

        // Migrate back handling to OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitWorkoutDialog();
            }
        });
    }
    // MODIFIED: Added status parameter
    private void recordAndLogExercisePerformance(int actualRepsAchieved, int actualDurationAchievedSeconds, String status) {
        if (currentIndex < 0 || currentIndex >= exerciseNames.size()) {
            Log.e(TAG, "recordAndLogExercisePerformance: Invalid currentIndex: " + currentIndex);
            return;
        }

        String name = getCleanExerciseName(exerciseNames.get(currentIndex));
        int targetReps = 0;
        int targetDuration = 0;

        if (isRepetitionBased) {
            targetReps = getTargetReps(currentIndex);
        } else {
            if (exerciseDurations != null && currentIndex < exerciseDurations.size()) {
                targetDuration = exerciseDurations.get(currentIndex);
            }
        }

        ExercisePerformanceData performance = new ExercisePerformanceData(
                name,
                targetReps,
                actualRepsAchieved,
                targetDuration,
                actualDurationAchievedSeconds,
                status // MODIFIED: Pass status
        );

        // Apply stored weight if available
        Double storedWeight = exerciseWeights.get(currentIndex);
        if (storedWeight != null) {
            performance.setWeight(storedWeight);
            Log.d(TAG, "Applied weight to performance: " + storedWeight + "kg");
        }

        // Set exercise type based on name
        String exerciseName = name.toLowerCase();
        if (exerciseName.contains("run") || exerciseName.contains("jump") || exerciseName.contains("cardio")) {
            performance.setExerciseType("cardio");
        } else if (exerciseName.contains("stretch") || exerciseName.contains("yoga")) {
            performance.setExerciseType("flexibility");
        } else {
            performance.setExerciseType("strength");
        }

        Log.d(TAG, "Recording performance: " + performance.toString());
        performanceDataList.add(performance);
    }

    // FIXED: Updated showExercise method
    private void showExercise(int index) {
        String cleanExerciseName = getCleanExerciseName(exerciseNames.get(index));
        tvExerciseName.setText(cleanExerciseName);

        // Get current set number for this exercise
        int currentSet = completedSetsPerExercise.get(index) + 1;
        int totalSets = totalSetsPerExercise.get(index);

        String setsRepsInfo = extractSetsRepsInfo(exerciseNames.get(index), exerciseDetails.get(index));

        // Show set progress
        String displayText;
        if (totalSets > 1) {
            displayText = "Set " + currentSet + " of " + totalSets + " | " + setsRepsInfo;
        } else {
            displayText = setsRepsInfo;
        }
        tvExerciseDetails.setText(displayText);

        if (tvInstructions != null && exerciseDetails != null && index < exerciseDetails.size()) {
            String fullInstructions = getFullExerciseInstructions(index);
            tvInstructions.setText(fullInstructions);
            tvInstructions.setVisibility(View.GONE);
        }

        updateProgressBar();
        updateButtonStates();
        loadExerciseImage(index);

        // ✅ NEW: Auto-detect exercise type and set toggle position
        String currentExerciseName = exerciseNames.get(index);
        boolean exerciseRequiresEquipment = requiresEquipment(currentExerciseName);

        // Set initial toggle position based on exercise type
        isNoEquipmentMode = !exerciseRequiresEquipment;
        updateModeUI(isNoEquipmentMode);

        Log.d(TAG, "Exercise: " + currentExerciseName +
                " | Requires Equipment: " + exerciseRequiresEquipment +
                " | Mode: " + (isNoEquipmentMode ? "No Equipment" : "Equipment"));

        // Check if alternatives exist
        checkBodyweightAlternativeAvailability();

        // Update weight suggestion display
        updateWeightSuggestion();

        if (isTTSReady) {
            startReadyCountdown();
        } else {
            tvExerciseTimer.postDelayed(() -> {
                if (isTTSReady) {
                    startReadyCountdown();
                } else {
                    Log.w(TAG, "TTS not ready, starting countdown without voice");
                    startReadyCountdown();
                }
            }, 1000);
        }
    }



    private String getCleanExerciseName(String fullName) {
        String cleaned = fullName;
        cleaned = cleaned.replaceAll("(?i)\\s*(each\\s+side\\s*)?x\\s*\\d+.*$", "");
        cleaned = cleaned.replaceAll("(?i)\\s*\\d+\\s*sets?.*$", "");
        cleaned = cleaned.replaceAll("(?i)\\s*-\\s*\\d+.*$", "");
        return cleaned.trim();
    }

    private String extractSetsRepsInfo(String exerciseNameInput, String exerciseDetails) {
        if (exerciseDetails != null) {
            int sets = 1;
            int reps = 1;
            String[] lines = exerciseDetails.split("\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("Sets: ")) {
                    try {
                        sets = Integer.parseInt(line.substring(6).trim());
                    } catch (NumberFormatException e) { /* Keep default */ }
                } else if (line.startsWith("Reps: ")) {
                    try {
                        reps = Integer.parseInt(line.substring(6).trim());
                    } catch (NumberFormatException e) { /* Keep default */ }
                }
            }
            if (sets > 0 && reps > 0) {
                return sets + " sets x " + reps + " reps";
            }
        }
        return "Follow the instructions";
    }

    private String getFullExerciseInstructions(int index) {
        if (exerciseDetails != null && index < exerciseDetails.size()) {
            return exerciseDetails.get(index);
        }
        return "No instructions available";
    }

    private void loadExerciseImage(int index) {
        String imageUrl = exerciseImageUrls.get(index);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            tvNoImage.setVisibility(View.GONE);
            ivExerciseImage.setVisibility(View.VISIBLE);
            Glide.with(this).asGif().load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .dontTransform().override(800, 800)
                    .into(ivExerciseImage);
        } else {
            ivExerciseImage.setVisibility(View.GONE);
            tvNoImage.setVisibility(View.VISIBLE);
        }
    }

    // FIXED: Updated ready countdown method
    private void startReadyCountdown() {
        stopAllTTS(); // Stop any ongoing TTS first
        isReadyCountdown = true;
        isTimerRunning = true;
        btnPause.setText("PAUSE");
        tvExerciseTimer.setVisibility(View.INVISIBLE);

        // First speak exercise name with a longer delay before countdown
        String exerciseName = getCleanExerciseName(exerciseNames.get(currentIndex));
        speakAndThen("Get ready for " + exerciseName, () -> {
            // Add a longer delay to ensure the first speech is heard completely
            tvExerciseTimer.postDelayed(() -> startReadyCountDownFrom(3), 1000); // Increased delay
        });
    }
    private void startReadyCountDownFrom(int number) {
        if (number <= 0) {
            speakAndThen("Go!", this::startActualExercise); // changed "Start!" → "Go!"
            return;
        }

        // small delay between numbers so voices don’t overlap
        tvExerciseTimer.postDelayed(() ->
                speakAndThen(String.valueOf(number), () -> startReadyCountDownFrom(number - 1)), 800);
    }


    // Utility: speak text, then run action after finished
    private void speakAndThen(String text, Runnable onDone) {
        if (tts != null && isTTSReady) {
            String utteranceId = "UTTERANCE_" + System.currentTimeMillis();
            if (onDone != null) {
                utteranceCallbacks.put(utteranceId, onDone);
            }
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId);
        } else {
            // TTS not ready, just run the callback immediately
            Log.w(TAG, "TTS not ready for: " + text);
            if (onDone != null) {
                onDone.run();
            }
        }
    }


    // FIXED: Updated startActualExercise method
    private void startActualExercise() {
        currentExerciseStartTimeMillis = System.currentTimeMillis();
        isReadyCountdown = false;

        String exerciseName = exerciseNames.get(currentIndex).toLowerCase();
        String exerciseDetailText = (exerciseDetails != null && currentIndex < exerciseDetails.size())
                ? exerciseDetails.get(currentIndex).toLowerCase() : "";

        isRepetitionBased = exerciseName.contains("reps") || exerciseName.contains("repetition")
                || exerciseName.contains(" x ") || exerciseName.contains("sets")
                || exerciseDetailText.contains("reps:") || exerciseDetailText.contains("sets:")
                || exerciseDetailText.contains("repetition");

        if (exerciseDurations != null && currentIndex < exerciseDurations.size() && exerciseDurations.get(currentIndex) <= 0) {
            isRepetitionBased = true;
        }

        continueWithExercise();
    }

    /**
     * Continue with the exercise after weight suggestion (or if no suggestion needed)
     */
    private void continueWithExercise() {
        // FIXED: Show timer only when actual exercise starts
        tvExerciseTimer.setVisibility(View.VISIBLE);

        if (isRepetitionBased) {
            startRepCounter();
        } else {
            if (exerciseDurations != null && currentIndex < exerciseDurations.size()) {
                startTimer(exerciseDurations.get(currentIndex));
            } else {
                startTimer(30);
            }
        }
    }

    /**
     * Show/hide and update the compact weight suggestion button
     */
    private void updateWeightSuggestion() {
        String currentExerciseName = exerciseNames.get(currentIndex);

        // Show only for exercises that need EXTERNAL WEIGHT (not just equipment)
        if (!isNoEquipmentMode && requiresExternalWeight(currentExerciseName)) {
            weightSuggestionContainer.setVisibility(View.VISIBLE);

            // Get suggested weight
            double suggestedWeight = getSuggestedWeightValue(currentExerciseName);
            etWeightQuick.setText(String.valueOf((int) suggestedWeight));

            // Listen for changes and store weight
            etWeightQuick.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    saveWeightFromQuickInput();
                }
            });

            spinnerWeightUnitQuick.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    saveWeightFromQuickInput();
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        } else {
            weightSuggestionContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Check if exercise requires EXTERNAL WEIGHT (barbells, dumbbells, etc.)
     * Excludes bodyweight exercises that use equipment (dips, pull-ups, etc.)
     */
    private boolean requiresExternalWeight(String exerciseName) {
        String nameLower = exerciseName.toLowerCase();

        // EXCLUDE bodyweight exercises that use equipment
        String[] bodyweightOnEquipment = {
            "dip", "pull up", "pull-up", "pullup", "chin up", "chin-up", "chinup",
            "push up", "push-up", "pushup", "handstand", "muscle up", "muscle-up",
            "hanging", "leg raise", "knee raise", "toes to bar", "toe to bar",
            "pike", "plank", "hyperextension", "back extension",
            "inverted row", "australian pull", "incline push",
            "decline push", "box jump", "step up", "step-up", "box step"
        };

        for (String bodyweight : bodyweightOnEquipment) {
            if (nameLower.contains(bodyweight)) {
                return false; // No weight needed
            }
        }

        // INCLUDE exercises that need external weights
        String[] externalWeightKeywords = {
            "barbell", "dumbbell", "kettlebell", "ez bar", "ez-bar",
            "weight", "plate", "loaded", "weighted",
            "bench press", "squat", "deadlift", "row", "curl",
            "press", "shrug", "lunge", "overhead",
            "fly", "raise", "extension", "kickback",
            "preacher", "hammer", "wrist", "calf raise",
            "goblet", "sumo", "romanian", "rdl",
            "skull crusher", "tricep press", "close grip"
        };

        for (String keyword : externalWeightKeywords) {
            if (nameLower.contains(keyword)) {
                return true; // Needs weight
            }
        }

        return false; // Default: no weight needed
    }

    /**
     * Save weight from the compact input
     */
    private void saveWeightFromQuickInput() {
        try {
            String weightStr = etWeightQuick.getText().toString();
            if (!weightStr.isEmpty()) {
                double weight = Double.parseDouble(weightStr);
                String unit = spinnerWeightUnitQuick.getSelectedItem().toString();

                // Convert to kg if needed
                if ("lbs".equals(unit)) {
                    weight = weight * 0.453592;
                }

                storeExerciseWeight(currentIndex, weight);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid weight input", e);
        }
    }

    /**
     * Get suggested weight value (just the number, not the formatted string)
     */
    private double getSuggestedWeightValue(String exerciseName) {
        String nameLower = exerciseName.toLowerCase();

        // Get user fitness level
        String fitnessLevel = getSharedPreferences("UserProfile", MODE_PRIVATE)
                .getString("fitnessLevel", "Moderately Active");

        double multiplier = 1.0;
        if (fitnessLevel.contains("Sedentary") || fitnessLevel.contains("Lightly")) {
            multiplier = 0.7;
        } else if (fitnessLevel.contains("Very Active") || fitnessLevel.contains("Extremely")) {
            multiplier = 1.3;
        }

        // Base weights
        int baseWeight = 20; // Default

        if (nameLower.contains("bench press")) {
            baseWeight = 40;
        } else if (nameLower.contains("squat")) {
            baseWeight = 50;
        } else if (nameLower.contains("deadlift")) {
            baseWeight = 60;
        } else if (nameLower.contains("dumbbell") || nameLower.contains("curl")) {
            baseWeight = 10;
        } else if (nameLower.contains("row")) {
            baseWeight = 35;
        } else if (nameLower.contains("shoulder press") || nameLower.contains("overhead press")) {
            baseWeight = 30;
        }

        return baseWeight * multiplier;
    }

    /**
     * Store the weight used for this exercise
     */
    private void storeExerciseWeight(int exerciseIndex, double weightKg) {
        if (exerciseIndex >= 0 && exerciseIndex < performanceDataList.size()) {
            // Update the performance data with the weight
            performanceDataList.get(exerciseIndex).setWeight(weightKg);
        }
    }

    // FIXED: Updated startRepCounter method
    private void startRepCounter() {
        currentRepCount = 0;
        isCounterPaused = false;
        isTimerRunning = true;
        int targetReps = getTargetReps(currentIndex);

        // FIXED: Show reps counter immediately
        tvExerciseTimer.setVisibility(View.VISIBLE);
        tvExerciseTimer.setText("0/" + targetReps);
        btnPause.setText("PAUSE");

        startActualRepCounting(targetReps);
    }

    // FIXED: Updated startActualRepCounting method
    private void startActualRepCounting(int targetReps) {
        currentRepCount = 0;
        speakNextRep(targetReps);
    }

    private void speakNextRep(int targetReps) {
        if (isCounterPaused) {
            return; // Don't continue if paused
        }
        if (currentRepCount >= targetReps) {
            speakAndThen("Exercise complete!", () -> {
                recordAndLogExercisePerformance(currentRepCount, 0, "completed");
                tvExerciseTimer.postDelayed(this::moveToNextExercise, 2000);
            });
            return;
        }

        currentRepCount++;
        tvExerciseTimer.setText(currentRepCount + "/" + targetReps);

        String announcement;
        if (currentRepCount == targetReps - 2) {
            announcement = "Last 2!";
        } else {
            announcement = String.valueOf(currentRepCount);
        }


        speakAndThen(announcement, () -> {
            // small pause between reps
            tvExerciseTimer.postDelayed(() -> speakNextRep(targetReps), 1000);
        });
    }

    private int getTargetReps(int exerciseIndex) {
        if (exerciseNames != null && exerciseIndex < exerciseNames.size()){
            String exerciseName = exerciseNames.get(exerciseIndex);
            // Attempt to parse reps like "... x 10 ..." or "... 10 reps ..."
            if (exerciseName.toLowerCase().contains("x ")) {
                try {
                    String afterX = exerciseName.substring(exerciseName.toLowerCase().indexOf("x ") + 2).trim();
                    String[] parts = afterX.split("\\s+");
                    if (parts.length > 0) {
                        return Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
                    }
                } catch (Exception e) { /* Continue to next check */ }
            }
        }
        if (exerciseDetails != null && exerciseIndex < exerciseDetails.size()) {
            String details = exerciseDetails.get(exerciseIndex);
            if (details.contains("Reps: ")) {
                try {
                    String repsLine = details.substring(details.indexOf("Reps: ") + 6);
                    String repsStr = repsLine.split("\\n")[0].trim(); // Get first line after "Reps: "
                    return Integer.parseInt(repsStr.replaceAll("[^0-9]", ""));
                } catch (Exception e) { /* Fallback to default */ }
            }
        }
        return 15; // Default target reps
    }

    // FIXED: Updated pauseRepCounter method
    private void pauseRepCounter() {
        isCounterPaused = true;
        isTimerRunning = false;
        btnPause.setText("RESUME");
    }

    // FIXED: Updated resumeRepCounter method
    private void resumeRepCounter() {
        isCounterPaused = false;
        isTimerRunning = true;
        btnPause.setText("PAUSE");

        // Continue from where we left off
        int targetReps = getTargetReps(currentIndex);
        speakNextRep(targetReps);
    }
    private void updateProgressBar() {
        if (progressBar != null && exerciseNames != null && !exerciseNames.isEmpty()) {
            int progress = (int) (((float) (currentIndex + 1) / exerciseNames.size()) * 100);
            progressBar.setProgress(progress);
        }
    }

    private void updateButtonStates() {
        if (btnPrevious != null) {
            btnPrevious.setEnabled(currentIndex > 0);
            btnPrevious.setAlpha(currentIndex > 0 ? 1.0f : 0.5f);
        }
    }

    private void moveToPreviousExercise() {
        if (currentIndex <= 0) {
            Toast.makeText(this, "This is the first exercise", Toast.LENGTH_SHORT).show();
            return;
        }
        stopAllTTS(); // Stop TTS before switching
        cancelAllTimers();
        currentIndex--;
        showExercise(currentIndex);
    }
    private void skipCurrentExercise() {
        Log.d(TAG, "Skipping exercise: " + exerciseNames.get(currentIndex));
        stopAllTTS();
        recordAndLogExercisePerformance(0, 0, "skipped");
        cancelAllTimers();

        // Mark current set as completed even though skipped
        int completedSets = completedSetsPerExercise.get(currentIndex);
        completedSetsPerExercise.set(currentIndex, completedSets + 1);

        // Find next exercise with sets remaining
        int nextIndex = findNextExerciseWithSetsRemaining(currentIndex + 1);

        if (nextIndex == -1) {
            // All done - Go to feedback first, then summary
            markWorkoutCompletedInFirestore();
            Intent intent = new Intent(WorkoutSessionActivity.this, Activity_workout_feedback.class);
            intent.putExtra("workout_name", getIntent().getStringExtra("workout_name"));
            intent.putExtra("total_exercises", exerciseNames != null ? exerciseNames.size() : 0);
            intent.putExtra("workoutDuration", calculateWorkoutDuration()); // Duration in minutes
            intent.putExtra("performanceData", performanceDataList);
            startActivity(intent);
            finish();
            return;
        }

        // Go to rest timer
        Intent intent = new Intent(WorkoutSessionActivity.this, RestTimerActivity.class);
        intent.putExtra("nextIndex", nextIndex);
        intent.putStringArrayListExtra("exerciseNames", exerciseNames);
        intent.putStringArrayListExtra("exerciseDetails", exerciseDetails);
        intent.putStringArrayListExtra("exerciseImageUrls", exerciseImageUrls);
        intent.putIntegerArrayListExtra("exerciseTimes", exerciseDurations);
        intent.putIntegerArrayListExtra("exerciseRests", exerciseRests);
        intent.putIntegerArrayListExtra("completedSetsPerExercise", completedSetsPerExercise);
        intent.putIntegerArrayListExtra("totalSetsPerExercise", totalSetsPerExercise);
        intent.putExtra("workout_name", getIntent().getStringExtra("workout_name"));
        intent.putExtra("performanceData", performanceDataList);
        intent.putExtra("workoutStartTime", workoutStartTime);
        startActivity(intent);
        finish();
    }


    private void cancelAllTimers() {
        if (timer != null) timer.cancel();
        if (repTimer != null) repTimer.cancel();
        if (readyTimer != null) readyTimer.cancel();
        isTimerRunning = false;
        isReadyCountdown = false;
    }


    // FIXED: Updated startTimer method with enhanced TTS
    private void startTimer(int seconds) {
        if (timer != null) timer.cancel();
        timeLeftMillis = seconds * 1000L;
        final int totalDurationForThisExercise = seconds;

        // Show timer for timed exercises
        tvExerciseTimer.setVisibility(View.VISIBLE);
        tvExerciseTimer.setText(seconds + "s");

        timer = new CountDownTimer(timeLeftMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMillis = millisUntilFinished;
                int secondsLeft = (int) (millisUntilFinished / 1000);
                tvExerciseTimer.setText(secondsLeft + "s");

                // TTS announcements for timed exercises
                if (tts != null) {
                    switch (secondsLeft) {
                        case 10:
                            tts.speak("10 seconds left", TextToSpeech.QUEUE_ADD, null, "TEN_SEC");
                            break;
                        case 5:
                        case 4:
                        case 3:
                        case 2:
                        case 1:
                            tts.speak(String.valueOf(secondsLeft), TextToSpeech.QUEUE_ADD, null, "COUNT_" + secondsLeft);
                            break;
                    }


                }
            }

            @Override
            public void onFinish() {
                tvExerciseTimer.setText("DONE!");
                timeLeftMillis = 0;
                isTimerRunning = false;

                if (tts != null) {
                    tts.speak("Exercise complete! Well done!", TextToSpeech.QUEUE_ADD, null, "TIMER_COMPLETE");
                }

                recordAndLogExercisePerformance(0, totalDurationForThisExercise, "completed");
                tvExerciseTimer.postDelayed(() -> moveToNextExercise(), 2000);
            }
        }.start();

        isTimerRunning = true;
        btnPause.setText("PAUSE");
    }

    private void moveToNextExercise() {
        cancelAllTimers();

        // Mark current exercise set as completed
        int completedSets = completedSetsPerExercise.get(currentIndex);
        completedSetsPerExercise.set(currentIndex, completedSets + 1);

        // Record last exercise if needed
        if (!isReadyCountdown && isRepetitionBased) {
            recordAndLogExercisePerformance(currentRepCount, 0, "completed");
        }

        // Find next exercise that still has sets remaining
        int nextIndex = findNextExerciseWithSetsRemaining(currentIndex + 1);

        if (nextIndex == -1) {
            // No more exercises with remaining sets - workout complete!
            markWorkoutCompletedInFirestore();

            Intent intent = new Intent(WorkoutSessionActivity.this, Activity_workout_feedback.class);
            intent.putExtra("workout_name", getIntent().getStringExtra("workout_name"));
            intent.putExtra("total_exercises", exerciseNames != null ? exerciseNames.size() : 0);
            intent.putExtra("workoutDuration", calculateWorkoutDuration()); // Duration in minutes
            intent.putExtra("performanceData", performanceDataList);
            startActivity(intent);
            finish();
            return;
        }

        // Move to rest period before next exercise
        Intent intent = new Intent(WorkoutSessionActivity.this, RestTimerActivity.class);
        intent.putExtra("nextIndex", nextIndex);
        intent.putStringArrayListExtra("exerciseNames", exerciseNames);
        intent.putStringArrayListExtra("exerciseDetails", exerciseDetails);
        intent.putStringArrayListExtra("exerciseImageUrls", exerciseImageUrls);
        intent.putIntegerArrayListExtra("exerciseTimes", exerciseDurations);
        intent.putIntegerArrayListExtra("exerciseRests", exerciseRests);
        intent.putIntegerArrayListExtra("completedSetsPerExercise", completedSetsPerExercise);
        intent.putIntegerArrayListExtra("totalSetsPerExercise", totalSetsPerExercise);
        intent.putExtra("workout_name", getIntent().getStringExtra("workout_name"));
        intent.putExtra("performanceData", performanceDataList);
        intent.putExtra("workoutStartTime", workoutStartTime);
        startActivity(intent);
        finish();
    }


    // Helper method to find next exercise with remaining sets:
    private int findNextExerciseWithSetsRemaining(int startFrom) {
        // First, check from startFrom to end of list
        for (int i = startFrom; i < exerciseNames.size(); i++) {
            int completed = completedSetsPerExercise.get(i);
            int total = totalSetsPerExercise.get(i);
            if (completed < total) {
                return i; // Found an exercise with remaining sets
            }
        }

        // If we reached the end, loop back to beginning
        for (int i = 0; i < startFrom; i++) {
            int completed = completedSetsPerExercise.get(i);
            int total = totalSetsPerExercise.get(i);
            if (completed < total) {
                return i; // Found an exercise with remaining sets
            }
        }

        // All exercises completed all sets
        return -1;
    }

    // FIXED: Updated onPause method
    @Override
    protected void onPause() {
        super.onPause();

        if (isReadyCountdown && readyTimer != null && isTimerRunning) {
            readyTimer.cancel();
            isTimerRunning = false;
            btnPause.setText("RESUME");
            // Keep timer hidden during ready countdown pause
            tvExerciseTimer.setVisibility(View.INVISIBLE);
        } else if (isRepetitionBased && isTimerRunning) {
            pauseRepCounter();
            // Keep reps counter visible during pause
        } else if (!isRepetitionBased && timer != null && isTimerRunning) {
            timer.cancel();
            isTimerRunning = false;
            btnPause.setText("RESUME");
            // Keep time display visible during pause
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelAllTimers();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    // ✅ Calculate actual workout duration in minutes
    private int calculateWorkoutDuration() {
        long durationMillis = System.currentTimeMillis() - workoutStartTime;
        int minutes = (int) ((durationMillis / 1000) / 60);

        // Ensure minimum 1 minute
        minutes = Math.max(1, minutes);

        Log.d(TAG, "📊 Workout duration calculated: " + minutes + " minutes");
        Log.d(TAG, "📊 Start time: " + workoutStartTime + ", End time: " + System.currentTimeMillis());

        return minutes;
    }

    private void stopAllTTS() {
        if (tts != null && isTTSReady) {
            tts.stop(); // This stops all current and queued utterances
            utteranceCallbacks.clear(); // Clear all pending callbacks
        }
    }

    private void markWorkoutCompletedInFirestore() {
        if (currentUser != null) {
            String uid = currentUser.getUid();
            int currentWeek = getIntent().getIntExtra("currentWeek", 1);
            DocumentReference workoutRef = firestore.collection("users")
                    .document(uid)
                    .collection("currentWorkout")
                    .document("week_" + currentWeek);

            workoutRef.update("completed", true)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Workout marked as completed in Firestore."))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to mark workout completed", e));
        }
    }

    // 2. In onCreate() method, add this after other view initializations:
    private void initializeEquipmentModeCard() {
        btnEquipmentMode = findViewById(R.id.btn_equipment_mode);
        btnNoEquipmentMode = findViewById(R.id.btn_no_equipment_mode);
        btnSwitchWorkout = findViewById(R.id.btn_switch_workout);

        // Initialize compact weight suggestion UI
        weightSuggestionContainer = findViewById(R.id.weight_suggestion_container);
        etWeightQuick = findViewById(R.id.et_weight_quick);
        spinnerWeightUnitQuick = findViewById(R.id.spinner_weight_unit_quick);

        // Setup unit spinner with white text
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            this, R.layout.spinner_item_white, new String[]{"kg", "lbs"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWeightUnitQuick.setAdapter(adapter);

        // Start fresh each workout - default to Equipment mode
        updateModeUI(false);

        // Check if current exercise has bodyweight alternative
        checkBodyweightAlternativeAvailability();

        // Equipment Mode Click - REMOVED TOAST
        btnEquipmentMode.setOnClickListener(v -> {
            if (!isNoEquipmentMode) {
                return; // Already in equipment mode
            }
            switchToEquipmentMode();
        });

        // No Equipment Mode Click - REMOVED TOAST
        btnNoEquipmentMode.setOnClickListener(v -> {
            if (isNoEquipmentMode) {
                return; // Already in no equipment mode
            }
            switchToNoEquipmentMode();
        });

        // Switch Workout Button Click
        btnSwitchWorkout.setOnClickListener(v -> {
            switchToAnyAlternativeWorkout();
        });
    }


    // 4. Update UI to reflect current mode (SIMPLIFIED VERSION)
    private void updateModeUI(boolean noEquipmentMode) {
        if (noEquipmentMode) {
            // No Equipment Mode - Selected
            btnNoEquipmentMode.setSelected(true);
            btnEquipmentMode.setSelected(false);

            btnNoEquipmentMode.setTextColor(getResources().getColor(android.R.color.white));
            btnEquipmentMode.setTextColor(getResources().getColor(android.R.color.darker_gray));

            btnNoEquipmentMode.setBackgroundResource(R.drawable.selected_equipment_bg);
            btnEquipmentMode.setBackgroundResource(R.drawable.unselected_equipment_bg);

        } else {
            // Equipment Mode - Selected
            btnEquipmentMode.setSelected(true);
            btnNoEquipmentMode.setSelected(false);

            btnEquipmentMode.setTextColor(getResources().getColor(android.R.color.white));
            btnNoEquipmentMode.setTextColor(getResources().getColor(android.R.color.darker_gray));

            btnEquipmentMode.setBackgroundResource(R.drawable.selected_equipment_bg);
            btnNoEquipmentMode.setBackgroundResource(R.drawable.unselected_equipment_bg);
        }
    }

// Check if bodyweight alternative exists for current exercise
private void checkBodyweightAlternativeAvailability() {
    String currentExerciseName = exerciseNames.get(currentIndex);
    Log.d(TAG, "=== Checking alternatives for: " + currentExerciseName + " ===");

    // Always check Firebase for alternatives (both bodyweight AND equipment)
    DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

    dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            Log.d(TAG, "Firebase data loaded. Total exercises in DB: " + snapshot.getChildrenCount());

            List<String> currentTargetMuscles = null;

            // Find current exercise to get its target muscles
            for (DataSnapshot exerciseSnap : snapshot.getChildren()) {
                if (exerciseSnap.getKey() != null && exerciseSnap.getKey().matches("^[0-9]+$")) {
                    ExerciseInfo exercise = exerciseSnap.getValue(ExerciseInfo.class);
                    if (exercise != null && exercise.getName().equalsIgnoreCase(currentExerciseName)) {
                        currentTargetMuscles = exercise.getTargetMuscles();
                        Log.d(TAG, "✓ Found current exercise in Firebase");
                        Log.d(TAG, "  Target muscles: " + currentTargetMuscles);
                        break;
                    }
                }
            }

            if (currentTargetMuscles == null || currentTargetMuscles.isEmpty()) {
                Log.w(TAG, "✗ No target muscles found - showing Switch Workout button");
                hasBodyweightAlternative = false;
                runOnUiThread(() -> showSwitchWorkoutButton());
                return;
            }

            // Check for BOTH bodyweight AND equipment alternatives
            boolean foundBodyweightAlt = false;
            boolean foundEquipmentAlt = false;
            int bodyweightCount = 0;
            int equipmentCount = 0;

            for (DataSnapshot exerciseSnap : snapshot.getChildren()) {
                if (exerciseSnap.getKey() != null && exerciseSnap.getKey().matches("^[0-9]+$")) {
                    ExerciseInfo exercise = exerciseSnap.getValue(ExerciseInfo.class);

                    if (exercise != null && !exercise.getName().equalsIgnoreCase(currentExerciseName)) {
                        if (hasSameTargetMuscles(exercise.getTargetMuscles(), currentTargetMuscles)) {
                            if (isBodyweightExercise(exercise)) {
                                bodyweightCount++;
                                foundBodyweightAlt = true;
                            } else {
                                equipmentCount++;
                                foundEquipmentAlt = true;
                            }
                        }
                    }
                }
            }

            Log.d(TAG, "=== Search complete ===");
            Log.d(TAG, "Bodyweight alternatives: " + bodyweightCount);
            Log.d(TAG, "Equipment alternatives: " + equipmentCount);

            // Show toggle if EITHER type has alternatives
            boolean hasAnyAlternatives = foundBodyweightAlt || foundEquipmentAlt;
            hasBodyweightAlternative = hasAnyAlternatives;

            runOnUiThread(() -> {
                if (hasAnyAlternatives) {
                    Log.d(TAG, "→ Showing Equipment/No Equipment toggle");
                    showEquipmentToggleButtons();
                } else {
                    Log.d(TAG, "→ Showing Switch Workout button");
                    showSwitchWorkoutButton();
                }
            });
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Log.e(TAG, "✗ Database error checking alternatives", error.toException());
            hasBodyweightAlternative = false;
            runOnUiThread(() -> showSwitchWorkoutButton());
        }
    });
}

private void showEquipmentToggleButtons() {
    btnEquipmentMode.setVisibility(View.VISIBLE);
    btnNoEquipmentMode.setVisibility(View.VISIBLE);
    btnSwitchWorkout.setVisibility(View.GONE);
}

private void showSwitchWorkoutButton() {
    btnEquipmentMode.setVisibility(View.GONE);
    btnNoEquipmentMode.setVisibility(View.GONE);
    btnSwitchWorkout.setVisibility(View.VISIBLE);
}

// Switch to any alternative workout (not limited to bodyweight)
private void switchToAnyAlternativeWorkout() {
    // Debounce: prevent rapid switching
    if (System.currentTimeMillis() - lastModeSwitch < MODE_SWITCH_COOLDOWN) {
        return;
    }
    lastModeSwitch = System.currentTimeMillis();

    String currentExerciseName = exerciseNames.get(currentIndex);

    // RESET all states first
    stopAllTTS();
    cancelAllTimers();
    isTimerRunning = false;
    isReadyCountdown = false;
    isCounterPaused = false;
    currentRepCount = 0;

    // Fetch any alternative with same target muscles
    fetchAnyAlternativeExercise();
}

// Fetch ANY alternative exercise (not just bodyweight) from Firebase
private void fetchAnyAlternativeExercise() {
    DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
    String currentExerciseName = exerciseNames.get(currentIndex);

    dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            List<String> currentTargetMuscles = null;

            // Find current exercise to get its target muscles
            for (DataSnapshot exerciseSnap : snapshot.getChildren()) {
                if (exerciseSnap.getKey() != null && exerciseSnap.getKey().matches("^[0-9]+$")) {
                    ExerciseInfo exercise = exerciseSnap.getValue(ExerciseInfo.class);
                    if (exercise != null && exercise.getName().equalsIgnoreCase(currentExerciseName)) {
                        currentTargetMuscles = exercise.getTargetMuscles();
                        Log.d(TAG, "Found current exercise. Target muscles: " + currentTargetMuscles);
                        break;
                    }
                }
            }

            if (currentTargetMuscles == null || currentTargetMuscles.isEmpty()) {
                Log.w(TAG, "Cannot find muscle data for current exercise");
                runOnUiThread(() -> {
                    Toast.makeText(WorkoutSessionActivity.this,
                            "Cannot find alternative exercises",
                            Toast.LENGTH_SHORT).show();
                });
                return;
            }

            // Find ANY alternatives with SAME target muscles
            List<ExerciseInfo> alternatives = new ArrayList<>();

            for (DataSnapshot exerciseSnap : snapshot.getChildren()) {
                if (exerciseSnap.getKey() != null && exerciseSnap.getKey().matches("^[0-9]+$")) {
                    ExerciseInfo exercise = exerciseSnap.getValue(ExerciseInfo.class);

                    if (exercise != null
                            && !exercise.getName().equalsIgnoreCase(currentExerciseName)
                            && hasSameTargetMuscles(exercise.getTargetMuscles(), currentTargetMuscles)) {
                        alternatives.add(exercise);
                        Log.d(TAG, "Found alternative: " + exercise.getName()
                                + " (targets: " + exercise.getTargetMuscles() + ")");
                    }
                }
            }

            if (!alternatives.isEmpty()) {
                // Pick a random alternative
                ExerciseInfo replacement = alternatives.get(
                        (int) (Math.random() * alternatives.size())
                );

                Log.d(TAG, "Selected replacement: " + replacement.getName());

                runOnUiThread(() -> {
                    replaceExerciseAtIndex(currentIndex, replacement);
                    // Re-check if new exercise has bodyweight alternatives
                    checkBodyweightAlternativeAvailability();
                });
            } else {
                Log.w(TAG, "No alternatives found for muscles: " + currentTargetMuscles);
                runOnUiThread(() -> {
                    Toast.makeText(WorkoutSessionActivity.this,
                            "No alternative exercises available",
                            Toast.LENGTH_LONG).show();
                });
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Log.e(TAG, "Database error", error.toException());
            runOnUiThread(() -> {
                Toast.makeText(WorkoutSessionActivity.this,
                        "Failed to fetch alternatives",
                        Toast.LENGTH_SHORT).show();
            });
        }
    });
}

    // 5. Switch to Equipment Mode
    private void switchToEquipmentMode() {
        // Debounce: prevent rapid switching
        if (System.currentTimeMillis() - lastModeSwitch < MODE_SWITCH_COOLDOWN) {
            return;
        }
        lastModeSwitch = System.currentTimeMillis();

        if (!isNoEquipmentMode) {
            return; // Already in equipment mode
        }

        String currentExerciseName = exerciseNames.get(currentIndex);

        // Check if current exercise is already equipment-based
        if (requiresEquipment(currentExerciseName)) {
            // Restore original exercise if it was replaced
            if (originalExerciseNames != null && currentIndex < originalExerciseNames.size()) {
                exerciseNames.set(currentIndex, originalExerciseNames.get(currentIndex));
                exerciseDetails.set(currentIndex, originalExerciseDetails.get(currentIndex));
                exerciseImageUrls.set(currentIndex, originalExerciseImageUrls.get(currentIndex));
            }

            isNoEquipmentMode = false;
            updateModeUI(false);

            // RESET all states
            stopAllTTS();
            cancelAllTimers();
            isTimerRunning = false;
            isReadyCountdown = false;
            isCounterPaused = false;
            currentRepCount = 0;

            // Update display without auto-starting
            updateExerciseDisplay();
        } else {
            // ✅ NEW: Current exercise is bodyweight, fetch equipment-based alternative
            stopAllTTS();
            cancelAllTimers();
            isTimerRunning = false;
            isReadyCountdown = false;
            isCounterPaused = false;
            currentRepCount = 0;

            isNoEquipmentMode = false;
            updateModeUI(false);

            // Fetch equipment-based alternative
            fetchEquipmentBasedAlternative();
        }
    }



    // 6. Switch to No Equipment Mode
    private void switchToNoEquipmentMode() {
        // Debounce: prevent rapid switching
        if (System.currentTimeMillis() - lastModeSwitch < MODE_SWITCH_COOLDOWN) {
            return;
        }
        lastModeSwitch = System.currentTimeMillis();

        if (isNoEquipmentMode) {
            return; // Already in no equipment mode
        }

        String currentExerciseName = exerciseNames.get(currentIndex);

        // Check if current exercise requires equipment
        if (requiresEquipment(currentExerciseName)) {
            // RESET all states first
            stopAllTTS();
            cancelAllTimers();
            isTimerRunning = false;
            isReadyCountdown = false;
            isCounterPaused = false;
            currentRepCount = 0;

            isNoEquipmentMode = true;
            updateModeUI(true);

            // Fetch alternative with slight delay
            replaceCurrentExerciseWithAlternative();
        } else {
            // Current exercise is already bodyweight-friendly
            isNoEquipmentMode = true;
            updateModeUI(true);
        }
    }


    // 7. Check if exercise requires equipment
    private boolean requiresEquipment(String exerciseName) {
        String nameLower = exerciseName.toLowerCase();

        String[] equipmentKeywords = {
                // Weights & Bars
                "barbell", "dumbbell", "kettlebell", "ez bar", "ez-bar", "ezbar",
                "trap bar", "hex bar", "medicine ball", "weight plate", "plate",
                "olympic bar", "curl bar",

                // Machines
                "machine", "smith", "smith machine", "leg press", "chest press",
                "shoulder press", "hack squat", "leg extension", "leg curl",
                "calf raise machine", "pec deck", "fly machine", "lat pulldown",
                "cable", "pulley", "seated row", "chest fly", "cable crossover",
                "tricep pushdown", "bicep curl machine", "preacher", "preacher curl",

                // Equipment/Tools
                "lever", "band", "resistance band", "exercise band", "elastic band",
                "bosu", "bosu ball", "stability ball", "swiss ball", "exercise ball",
                "foam roller", "ab roller", "ab wheel", "trx", "suspension",
                "slam ball", "wall ball", "battle rope", "rope", "skierg", "ski erg",
                "rowing machine", "rower", "assault bike", "stationary bike",
                "treadmill", "elliptical",

                // Benches & Racks
                "bench", "incline bench", "decline bench", "flat bench",
                "rack", "power rack", "squat rack", "cage",
                "dip station", "dip bars", "pull-up bar", "chin-up bar",
                "roman chair", "hyperextension bench", "glute ham developer",
                "preacher bench", "scott bench",

                // Specialized Equipment
                "t-bar", "t-bar row", "landmine", "sled", "prowler",
                "tire", "sandbag", "weighted vest", "ankle weights",
                "wrist weights", "loading pin", "dip belt",

                // Cardio Equipment
                "bike", "cycling", "spin", "ergometer", "erg",
                "stepper", "stair climber", "versaclimber",

                // Olympic/Powerlifting
                "platform", "lifting platform", "bumper plate",

                // Other indicators
                "weighted", "loaded", "press machine", "curl machine",
                "extension machine", "row machine", "pulldown",
                "assisted", "counterbalance"
        };

        for (String keyword : equipmentKeywords) {
            if (nameLower.contains(keyword)) {
                return true;
            }
        }

        return false;
    }


    // Replace current exercise with no-equipment alternative
    private void replaceCurrentExerciseWithAlternative() {
        isReplacingExercise = true;

        // Pause current activity
        stopAllTTS();
        cancelAllTimers();
        fetchAlternativeExercise();
    }


    // Fetch alternative exercise from Firebase
    private void fetchAlternativeExercise() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String currentExerciseName = exerciseNames.get(currentIndex);

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> currentTargetMuscles = null;

                // Find current exercise to get its target muscles
                for (DataSnapshot exerciseSnap : snapshot.getChildren()) {
                    if (exerciseSnap.getKey() != null && exerciseSnap.getKey().matches("^[0-9]+$")) {
                        ExerciseInfo exercise = exerciseSnap.getValue(ExerciseInfo.class);
                        if (exercise != null && exercise.getName().equalsIgnoreCase(currentExerciseName)) {
                            currentTargetMuscles = exercise.getTargetMuscles();
                            Log.d(TAG, "Found current exercise. Target muscles: " + currentTargetMuscles);
                            break;
                        }
                    }
                }

                // If we couldn't find target muscles, revert silently
                if (currentTargetMuscles == null || currentTargetMuscles.isEmpty()) {
                    Log.w(TAG, "Cannot find muscle data for current exercise");
                    // REMOVED: Toast
                    isNoEquipmentMode = false;
                    updateModeUI(false);
                    isReplacingExercise = false;
                    return;
                }

                // Find bodyweight alternatives with SAME target muscles
                List<ExerciseInfo> alternatives = new ArrayList<>();

                for (DataSnapshot exerciseSnap : snapshot.getChildren()) {
                    if (exerciseSnap.getKey() != null && exerciseSnap.getKey().matches("^[0-9]+$")) {
                        ExerciseInfo exercise = exerciseSnap.getValue(ExerciseInfo.class);

                        if (exercise != null
                                && !exercise.getName().equalsIgnoreCase(currentExerciseName)
                                && isBodyweightExercise(exercise)
                                && hasSameTargetMuscles(exercise.getTargetMuscles(), currentTargetMuscles)) {
                            alternatives.add(exercise);
                            Log.d(TAG, "Found matching alternative: " + exercise.getName()
                                    + " (targets: " + exercise.getTargetMuscles() + ")");
                        }
                    }
                }

                if (!alternatives.isEmpty()) {
                    // Pick a random bodyweight alternative
                    ExerciseInfo replacement = alternatives.get(
                            (int) (Math.random() * alternatives.size())
                    );

                    Log.d(TAG, "Selected bodyweight replacement: " + replacement.getName());
                    replaceExerciseAtIndex(currentIndex, replacement);
                } else {
                    // ✅ NEW: Try to find ANY exercise with same target muscles (not just bodyweight)
                    Log.w(TAG, "No bodyweight alternatives found. Searching for any exercise with same muscles...");

                    List<ExerciseInfo> anyAlternatives = new ArrayList<>();

                    for (DataSnapshot exerciseSnap : snapshot.getChildren()) {
                        if (exerciseSnap.getKey() != null && exerciseSnap.getKey().matches("^[0-9]+$")) {
                            ExerciseInfo exercise = exerciseSnap.getValue(ExerciseInfo.class);

                            if (exercise != null
                                    && !exercise.getName().equalsIgnoreCase(currentExerciseName)
                                    && hasSameTargetMuscles(exercise.getTargetMuscles(), currentTargetMuscles)) {
                                anyAlternatives.add(exercise);
                            }
                        }
                    }

                    if (!anyAlternatives.isEmpty()) {
                        // Found alternatives (may include equipment-based exercises)
                        ExerciseInfo replacement = anyAlternatives.get(
                                (int) (Math.random() * anyAlternatives.size())
                        );

                        Log.d(TAG, "Selected alternative exercise (may use equipment): " + replacement.getName());

                        runOnUiThread(() -> {
                            Toast.makeText(WorkoutSessionActivity.this,
                                    "No bodyweight option found. Switched to: " + replacement.getName(),
                                    Toast.LENGTH_LONG).show();
                        });

                        replaceExerciseAtIndex(currentIndex, replacement);
                    } else {
                        // Truly no alternatives at all
                        Log.w(TAG, "No alternatives found for muscles: " + currentTargetMuscles);

                        runOnUiThread(() -> {
                            Toast.makeText(WorkoutSessionActivity.this,
                                    "No alternative exercises available. Please skip this exercise.",
                                    Toast.LENGTH_LONG).show();
                        });

                        isNoEquipmentMode = false;
                        updateModeUI(false);
                    }
                }

                isReplacingExercise = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error", error.toException());
                // REMOVED: Toast
                isNoEquipmentMode = false;
                updateModeUI(false);
                isReplacingExercise = false;
            }
        });
    }    // 11. Check if exercise is bodyweight/no equipment
    private boolean isBodyweightExercise(ExerciseInfo exercise) {
        if (exercise.getEquipments() == null || exercise.getEquipments().isEmpty()) {
            return true; // No equipment listed = bodyweight
        }

        // Check all equipment entries
        for (String equip : exercise.getEquipments()) {
            String equipLower = equip.toLowerCase().trim();

            // These are considered "bodyweight-friendly"
            if (equipLower.contains("body weight") ||
                    equipLower.contains("bodyweight") ||
                    equipLower.contains("body-weight") ||
                    equipLower.equals("none") ||
                    equipLower.equals("null") ||
                    equipLower.isEmpty()) {
                continue; // This equipment is OK
            }

            // These are acceptable minimal equipment (still bodyweight-based)
            if (equipLower.equals("mat") ||
                    equipLower.equals("floor") ||
                    equipLower.equals("wall") ||
                    equipLower.equals("chair") ||
                    equipLower.equals("towel") ||
                    equipLower.equals("pillow")) {
                continue; // This equipment is OK
            }

            // If we find any other equipment, it's NOT bodyweight
            return false;
        }

        // All equipment entries were bodyweight-friendly
        return true;
    }



// Replace exercise at index
    private void replaceExerciseAtIndex(int index, ExerciseInfo replacement) {
        if (index < 0 || index >= exerciseNames.size()) return;

        // CRITICAL: Stop all timers and TTS to prevent mixing
        stopAllTTS();
        cancelAllTimers();
        isTimerRunning = false;
        isReadyCountdown = false;
        isCounterPaused = false;
        currentRepCount = 0;

        // KEEP the original number of sets for this exercise
        int originalSets = totalSetsPerExercise.get(index);
        // ✅ UPDATE THE EXERCISE NAME
        exerciseNames.set(index, replacement.getName());

        StringBuilder details = new StringBuilder();
        details.append("Sets: ").append(originalSets).append("\n"); // Use original sets
        details.append("Reps: 12\n\n");
        details.append("Instructions:\n");

        if (replacement.getInstructions() != null && !replacement.getInstructions().isEmpty()) {
            details.append(String.join("\n", replacement.getInstructions()));
        } else {
            details.append("Follow proper form and technique.");
        }

        exerciseDetails.set(index, details.toString());

        String imageUrl = (replacement.getGifUrl() != null && !replacement.getGifUrl().isEmpty())
                ? replacement.getGifUrl() : "https://via.placeholder.com/150";
        exerciseImageUrls.set(index, imageUrl);

        if (exerciseDurations != null && index < exerciseDurations.size()) {
            exerciseDurations.set(index, 30);
        }

        if (exerciseRests != null && index < exerciseRests.size()) {
            exerciseRests.set(index, 45);
        }

        Log.d(TAG, "Replaced exercise with: " + replacement.getName());

        // Restart the exercise from the beginning (don't call showExercise which auto-starts)
        String cleanExerciseName = getCleanExerciseName(exerciseNames.get(currentIndex));
        tvExerciseName.setText(cleanExerciseName);

        int currentSet = completedSetsPerExercise.get(index) + 1;
        int totalSets = totalSetsPerExercise.get(index);
        String setsRepsInfo = extractSetsRepsInfo(exerciseNames.get(index), exerciseDetails.get(index));

        String displayText;
        if (totalSets > 1) {
            displayText = "Set " + currentSet + " of " + totalSets + " | " + setsRepsInfo;
        } else {
            displayText = setsRepsInfo;
        }
        tvExerciseDetails.setText(displayText);

        updateProgressBar();
        updateButtonStates();
        loadExerciseImage(index);

        // Check if bodyweight alternative exists for new exercise
        checkBodyweightAlternativeAvailability();

        // Reset states and wait for user to resume
        isTimerRunning = false;
        isCounterPaused = false;
        btnPause.setText("START");
        tvExerciseTimer.setText("Ready");
        tvExerciseTimer.setVisibility(View.VISIBLE);
    }


    // Check if two exercises have the same target muscles
    private boolean hasSameTargetMuscles(List<String> muscles1, List<String> muscles2) {
        if (muscles1 == null || muscles2 == null) return false;
        if (muscles1.isEmpty() || muscles2.isEmpty()) return false;

        // Convert to lowercase for comparison
        List<String> list1 = new ArrayList<>();
        List<String> list2 = new ArrayList<>();

        for (String m : muscles1) list1.add(m.toLowerCase().trim());
        for (String m : muscles2) list2.add(m.toLowerCase().trim());

        // Check if they share at least one target muscle
        for (String muscle : list1) {
            if (list2.contains(muscle)) {
                return true;
            }
        }

        return false;
    }

    private int extractTotalSets(String exerciseNameInput, String exerciseDetails) {
        if (exerciseDetails != null) {
            String[] lines = exerciseDetails.split("\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("Sets: ")) {
                    try {
                        return Integer.parseInt(line.substring(6).trim());
                    } catch (NumberFormatException e) {
                        // Continue to fallback
                    }
                }
            }
        }

        // Try to extract from exercise name (e.g., "Push-ups 3 sets x 10")
        if (exerciseNameInput != null) {
            String nameLower = exerciseNameInput.toLowerCase();
            if (nameLower.contains("sets")) {
                try {
                    String[] parts = nameLower.split("\\s+");
                    for (int i = 0; i < parts.length - 1; i++) {
                        if (parts[i + 1].contains("set")) {
                            return Integer.parseInt(parts[i].replaceAll("[^0-9]", ""));
                        }
                    }
                } catch (Exception e) {
                    // Continue to fallback
                }
            }
        }

        return 1; // Default to 1 set
    }



    private void initializeSetsTracking() {
        completedSetsPerExercise = new ArrayList<>();
        totalSetsPerExercise = new ArrayList<>();

        // Get existing tracking from intent if returning to workout
        ArrayList<Integer> existingCompleted = getIntent().getIntegerArrayListExtra("completedSetsPerExercise");
        ArrayList<Integer> existingTotal = getIntent().getIntegerArrayListExtra("totalSetsPerExercise");

        if (existingCompleted != null && existingTotal != null) {
            completedSetsPerExercise = existingCompleted;
            totalSetsPerExercise = existingTotal;
        } else {
            // First time - initialize for all exercises
            for (int i = 0; i < exerciseNames.size(); i++) {
                completedSetsPerExercise.add(0); // No sets completed yet
                int totalSets = extractTotalSets(exerciseNames.get(i), exerciseDetails.get(i));
                totalSetsPerExercise.add(totalSets);
            }
        }
    }

    private void showExitWorkoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle("⚠️ Exit Workout?");
        builder.setMessage("Are you sure you want to exit?\n\n" +
                "WARNING: Your workout progress will NOT be saved if you exit now.\n\n" +
                "All completed exercises and performance data will be lost.");

        builder.setPositiveButton("Yes, Exit", (dialog, which) -> {
            stopAllTTS();
            cancelAllTimers();
            finish();
        });

        builder.setNegativeButton("Continue Workout", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.setCancelable(true);

        AlertDialog dialog = builder.create();

        // Apply rounded background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }

        dialog.show();

        // Style the buttons
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.holo_green_dark));
    }


    private void showInstructionsDialog(String instructions) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedDialogStyle);
        builder.setTitle("📋 Exercise Instructions");

        // Format the instructions text
        String formattedInstructions = formatInstructionsText(instructions);
        builder.setMessage(formattedInstructions);

        builder.setPositiveButton("Got it", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.setCancelable(true);

        AlertDialog dialog = builder.create();

        // Apply rounded background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }

        dialog.show();

        // Style the button
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
    }

    private String formatInstructionsText(String instructions) {
        if (instructions == null || instructions.isEmpty()) {
            return "No instructions available";
        }

        StringBuilder formatted = new StringBuilder();
        String[] lines = instructions.split("\n");

        String sets = "";
        String reps = "";
        boolean instructionsSectionStarted = false;

        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("Sets:")) {
                sets = line.replace("Sets:", "").trim();
            } else if (line.startsWith("Reps:")) {
                reps = line.replace("Reps:", "").trim();
            } else if (line.equalsIgnoreCase("Instructions:")) {
                // Add sets and reps on one line before instructions
                if (!sets.isEmpty() || !reps.isEmpty()) {
                    formatted.append("Sets: ").append(sets.isEmpty() ? "N/A" : sets)
                            .append("  Reps: ").append(reps.isEmpty() ? "N/A" : reps)
                            .append("\n\n");
                }
                formatted.append("Instructions:\n");
                instructionsSectionStarted = true;
            } else if (!line.isEmpty() && instructionsSectionStarted) {
                // Add spacing between steps
                formatted.append(line).append("\n\n");
            }
        }

        return formatted.toString().trim();
    }

    private void fetchEquipmentBasedAlternative() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String currentExerciseName = exerciseNames.get(currentIndex);

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> currentTargetMuscles = null;

                // Find current exercise to get its target muscles
                for (DataSnapshot exerciseSnap : snapshot.getChildren()) {
                    if (exerciseSnap.getKey() != null && exerciseSnap.getKey().matches("^[0-9]+$")) {
                        ExerciseInfo exercise = exerciseSnap.getValue(ExerciseInfo.class);
                        if (exercise != null && exercise.getName().equalsIgnoreCase(currentExerciseName)) {
                            currentTargetMuscles = exercise.getTargetMuscles();
                            Log.d(TAG, "Found current exercise. Target muscles: " + currentTargetMuscles);
                            break;
                        }
                    }
                }

                if (currentTargetMuscles == null || currentTargetMuscles.isEmpty()) {
                    Log.w(TAG, "Cannot find muscle data for current exercise");
                    isNoEquipmentMode = true;
                    updateModeUI(true);
                    return;
                }

                // Find EQUIPMENT-BASED alternatives with SAME target muscles
                List<ExerciseInfo> equipmentAlternatives = new ArrayList<>();

                for (DataSnapshot exerciseSnap : snapshot.getChildren()) {
                    if (exerciseSnap.getKey() != null && exerciseSnap.getKey().matches("^[0-9]+$")) {
                        ExerciseInfo exercise = exerciseSnap.getValue(ExerciseInfo.class);

                        if (exercise != null
                                && !exercise.getName().equalsIgnoreCase(currentExerciseName)
                                && !isBodyweightExercise(exercise) // ✅ Must require equipment
                                && hasSameTargetMuscles(exercise.getTargetMuscles(), currentTargetMuscles)) {
                            equipmentAlternatives.add(exercise);
                            Log.d(TAG, "Found equipment alternative: " + exercise.getName());
                        }
                    }
                }

                if (!equipmentAlternatives.isEmpty()) {
                    // Pick a random equipment-based alternative
                    ExerciseInfo replacement = equipmentAlternatives.get(
                            (int) (Math.random() * equipmentAlternatives.size())
                    );

                    Log.d(TAG, "Selected equipment replacement: " + replacement.getName());
                    runOnUiThread(() -> replaceExerciseAtIndex(currentIndex, replacement));
                } else {
                    // No equipment alternatives found
                    Log.w(TAG, "No equipment alternatives found for muscles: " + currentTargetMuscles);

                    runOnUiThread(() -> {
                        Toast.makeText(WorkoutSessionActivity.this,
                                "No equipment-based alternatives available",
                                Toast.LENGTH_LONG).show();

                        // Revert to No Equipment mode
                        isNoEquipmentMode = true;
                        updateModeUI(true);
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error", error.toException());
                runOnUiThread(() -> {
                    isNoEquipmentMode = true;
                    updateModeUI(true);
                });
            }
        });
    }

    private void updateExerciseDisplay() {
        String cleanExerciseName = getCleanExerciseName(exerciseNames.get(currentIndex));
        tvExerciseName.setText(cleanExerciseName);

        int currentSet = completedSetsPerExercise.get(currentIndex) + 1;
        int totalSets = totalSetsPerExercise.get(currentIndex);
        String setsRepsInfo = extractSetsRepsInfo(exerciseNames.get(currentIndex), exerciseDetails.get(currentIndex));

        String displayText;
        if (totalSets > 1) {
            displayText = "Set " + currentSet + " of " + totalSets + " | " + setsRepsInfo;
        } else {
            displayText = setsRepsInfo;
        }
        tvExerciseDetails.setText(displayText);

        updateProgressBar();
        updateButtonStates();
        loadExerciseImage(currentIndex);

        // Wait for user to start
        btnPause.setText("START");
        tvExerciseTimer.setText("Ready");
        tvExerciseTimer.setVisibility(View.VISIBLE);
    }

}
