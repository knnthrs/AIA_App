package com.example.signuploginrealtime;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class PayMongoPaymentActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private String paymentUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paymongo_payment);

        webView = findViewById(R.id.payment_webview);
        progressBar = findViewById(R.id.payment_progress);

        // Handle notch and system bars
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        // Apply window insets to avoid notch
        webView.setOnApplyWindowInsetsListener((v, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                android.graphics.Insets systemBars = insets.getInsets(
                        android.view.WindowInsets.Type.systemBars()
                );
                v.setPadding(
                        systemBars.left,
                        systemBars.top,
                        systemBars.right,
                        systemBars.bottom
                );
            }
            return insets;
        });

        paymentUrl = getIntent().getStringExtra("paymentUrl");

        if (paymentUrl == null || paymentUrl.isEmpty()) {
            Toast.makeText(this, "Invalid payment URL", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupWebView();
        webView.loadUrl(paymentUrl);
    }

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

                // Check if payment was successful or failed
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
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Handle success/failure redirects
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

    private void handlePaymentSuccess() {
        new AlertDialog.Builder(this)
                .setTitle("Payment Successful")
                .setMessage("Your membership payment was processed successfully!")
                .setCancelable(false)
                .setPositiveButton("Continue", (dialog, which) -> {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("paymentSuccess", true);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .show();
    }

    private void handlePaymentFailure() {
        new AlertDialog.Builder(this)
                .setTitle("Payment Failed")
                .setMessage("Your payment was not completed. Please try again.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("paymentSuccess", false);
                    setResult(RESULT_CANCELED, resultIntent);
                    finish();
                })
                .show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Payment?")
                .setMessage("Are you sure you want to cancel this payment?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("paymentSuccess", false);
                    setResult(RESULT_CANCELED, resultIntent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }
}