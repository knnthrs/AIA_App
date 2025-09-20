package com.example.signuploginrealtime;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.TextView;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;


public class Client_workouts_details extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_client_workouts_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // 🔹 Get data from Intent
        String name = getIntent().getStringExtra("client_name");
        String email = getIntent().getStringExtra("client_email");
        String status = getIntent().getStringExtra("client_status");

        // 🔹 Find your TextViews (matching IDs from your XML)
        TextView clientName = findViewById(R.id.client_name);
        TextView clientWeight = findViewById(R.id.client_weight);
        TextView clientHeight = findViewById(R.id.client_height);
        TextView clientGoal = findViewById(R.id.client_goal);
        TextView workoutFrequency = findViewById(R.id.workout_frequency);

        // 🔹 Update UI (example: only name/status for now)
        clientName.setText(getIntent().getStringExtra("client_name"));
        clientWeight.setText(getIntent().getStringExtra("client_weight"));
        clientHeight.setText(getIntent().getStringExtra("client_height"));
        clientGoal.setText(getIntent().getStringExtra("client_goal"));
        workoutFrequency.setText(getIntent().getStringExtra("client_activity"));

    }
}