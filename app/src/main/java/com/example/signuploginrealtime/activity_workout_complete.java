package com.example.signuploginrealtime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class activity_workout_complete extends AppCompatActivity {

    private Button btnFinish, btnViewStreak;
    private SharedPreferences workoutPrefs;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_complete);

        btnFinish = findViewById(R.id.btn_end_session);
        btnViewStreak = findViewById(R.id.btn_view_streaks); // optional

        workoutPrefs = getSharedPreferences("workout_prefs", MODE_PRIVATE);

        // Initialize TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                tts.speak("Well done! Congratulations!", TextToSpeech.QUEUE_FLUSH, null, "well_done");
            }
        });

        // End session → save workout → go to MainActivity immediately
        btnFinish.setOnClickListener(v -> {
            saveWorkoutForToday();
            goToMain();
        });

        // Optional: view streak button
        if (btnViewStreak != null) {
            btnViewStreak.setOnClickListener(v -> {
                Intent intent = new Intent(activity_workout_complete.this, StreakCalendar.class);
                startActivity(intent);
            });
        }
    }

    private void saveWorkoutForToday() {
        String today = getCurrentDateString();
        String workoutDetails = "Gym session completed";

        StreakCalendar.saveWorkoutForDate(workoutPrefs, today, workoutDetails);

        Toast.makeText(this, "Workout recorded for today!", Toast.LENGTH_SHORT).show();
    }

    private String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void goToMain() {
        Intent intent = new Intent(activity_workout_complete.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
