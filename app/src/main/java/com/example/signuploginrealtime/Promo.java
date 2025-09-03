package com.example.signuploginrealtime;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class Promo extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promo);

        // Get the ImageView
        ImageView promoImageView = findViewById(R.id.promoImageView);

        // Get the URL passed from MainActivity
        String promoUrl = getIntent().getStringExtra("promoUrl");

        if (promoUrl != null && !promoUrl.isEmpty()) {
            // Load image using Glide
            Glide.with(this)
                    .load(promoUrl)
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(R.drawable.badge_background)
                    .into(promoImageView);
        }
    }
}

