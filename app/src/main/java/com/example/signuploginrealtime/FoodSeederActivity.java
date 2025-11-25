package com.example.signuploginrealtime;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class FoodSeederActivity extends AppCompatActivity {

    private TextView statusText;
    private Button seedButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_seeder);

        statusText = findViewById(R.id.statusText);
        seedButton = findViewById(R.id.seedButton);
        db = FirebaseFirestore.getInstance();

        // Check authentication first
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            statusText.setText("❌ Not authenticated! Please login first.");
            seedButton.setEnabled(false);
            return;
        }

        android.util.Log.d("FoodSeeder", "User authenticated: " + FirebaseAuth.getInstance().getCurrentUser().getUid());
        statusText.setText("Ready to seed 500 foods. User authenticated.");

        seedButton.setOnClickListener(v -> seedFoods());
    }

    private void seedFoods() {
        try {
            // Read JSON from raw resources
            InputStream inputStream = getResources().openRawResource(R.raw.gym_foods_500);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            String json = new String(buffer, "UTF-8");

            // Parse JSON
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> foods = gson.fromJson(json, listType);

            statusText.setText("Found " + foods.size() + " foods. Starting upload...");

            // Upload in batches
            uploadBatch(foods, 0);

        } catch (Exception e) {
            statusText.setText("Error: " + e.getMessage());
            Toast.makeText(this, "Error reading foods: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void uploadBatch(List<Map<String, Object>> foods, int startIndex) {
        int batchSize = 50; // Larger batch for efficiency
        int endIndex = Math.min(startIndex + batchSize, foods.size());

        android.util.Log.d("FoodSeeder", "=== STARTING BATCH ===");
        android.util.Log.d("FoodSeeder", "Batch: " + startIndex + " to " + endIndex + " (total: " + foods.size() + ")");

        // Use Firestore batch write for better performance and reliability
        com.google.firebase.firestore.WriteBatch batch = db.batch();

        for (int i = startIndex; i < endIndex; i++) {
            Map<String, Object> food = foods.get(i);
            String docId = ((String) food.get("name")).replaceAll("[^a-zA-Z0-9]", "_").toLowerCase() + "_" + i;

            android.util.Log.d("FoodSeeder", "Adding to batch: #" + (i + 1) + " - " + food.get("name"));
            batch.set(db.collection("foods").document(docId), food);
        }

        android.util.Log.d("FoodSeeder", "Committing batch");
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("FoodSeeder", "✅ BATCH COMMIT SUCCESS: " + startIndex + " to " + endIndex + " (uploaded " + (endIndex - startIndex) + " foods)");
                    runOnUiThread(() -> {
                        statusText.setText("✅ Batch " + ((startIndex/batchSize) + 1) + " complete! (" + startIndex + "-" + endIndex + "). Continuing...");
                    });

                    // Continue with next batch
                    if (endIndex < foods.size()) {
                        android.util.Log.d("FoodSeeder", "📅 Next batch: " + endIndex + " to " + Math.min(endIndex + batchSize, foods.size()));
                        // Shorter delay
                        new android.os.Handler().postDelayed(() -> {
                            android.util.Log.d("FoodSeeder", "🚀 Starting next batch at index " + endIndex);
                            uploadBatch(foods, endIndex);
                        }, 1000);
                    } else {
                        android.util.Log.d("FoodSeeder", "🎉 ALL DONE! Total foods uploaded: " + foods.size());
                        runOnUiThread(() -> {
                            statusText.setText("✅ Upload complete! " + foods.size() + " foods uploaded.");
                            Toast.makeText(this, "All 500 foods uploaded successfully!", Toast.LENGTH_LONG).show();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FoodSeeder", "❌ BATCH COMMIT FAILED at " + startIndex + "-" + endIndex);
                    android.util.Log.e("FoodSeeder", "Error: " + e.getMessage());

                    // Cast to FirebaseFirestoreException to get error code
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                        com.google.firebase.firestore.FirebaseFirestoreException firestoreException =
                            (com.google.firebase.firestore.FirebaseFirestoreException) e;
                        android.util.Log.e("FoodSeeder", "Error code: " + firestoreException.getCode());
                    }

                    runOnUiThread(() -> {
                        statusText.setText("❌ Failed at batch " + ((startIndex/batchSize) + 1) + ": " + e.getMessage());
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                });
    }
}
