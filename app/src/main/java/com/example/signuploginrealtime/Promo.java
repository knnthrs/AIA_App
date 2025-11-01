package com.example.signuploginrealtime;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.firestore.ListenerRegistration;

public class Promo extends AppCompatActivity {
    private ImageView promoImageView;
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private ScaleGestureDetector scaleDetector;

    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 5f;

    private PointF last = new PointF();
    private PointF start = new PointF();

    private float imageWidth = 0;
    private float imageHeight = 0;
    private boolean isImageLoaded = false;
    private float initialFitScale = 1f;
    private ListenerRegistration promoListener;
    private String currentPromoUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promo);
        overridePendingTransition(0, 0);

        promoImageView = findViewById(R.id.promoImageView);
        ImageView backButton = findViewById(R.id.back_button);

        String promoUrl = getIntent().getStringExtra("promoUrl");
        currentPromoUrl = promoUrl;

        if (promoUrl == null || promoUrl.isEmpty()) {
            showPromoUnavailableDialog();
            return;
        }

        setupPromoChangeListener();


        if (promoUrl != null && !promoUrl.isEmpty()) {
            Glide.with(this)
                    .load(promoUrl)
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(R.drawable.badge_background)
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                            promoImageView.setImageDrawable(resource);
                            imageWidth = resource.getIntrinsicWidth();
                            imageHeight = resource.getIntrinsicHeight();
                            isImageLoaded = true;

                            if (promoImageView.getWidth() > 0 && promoImageView.getHeight() > 0) {
                                initializeImageMatrix();
                            } else {
                                promoImageView.getViewTreeObserver().addOnGlobalLayoutListener(
                                        new ViewTreeObserver.OnGlobalLayoutListener() {
                                            @Override
                                            public void onGlobalLayout() {
                                                promoImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                                initializeImageMatrix();
                                            }
                                        });
                            }
                        }

                        @Override
                        public void onLoadCleared(Drawable placeholder) {
                        }
                    });
        }

        backButton.setOnClickListener(v -> finish());
        setupZoom();

        // Migrate back handling to OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(0, 0);
            }
        });
    }

    private void initializeImageMatrix() {
        if (!isImageLoaded || imageWidth == 0 || imageHeight == 0) return;

        float viewWidth = promoImageView.getWidth();
        float viewHeight = promoImageView.getHeight();

        // Calculate scale to fit the entire image in the view
        float scaleX = viewWidth / imageWidth;
        float scaleY = viewHeight / imageHeight;
        float fitScale = Math.min(scaleX, scaleY);

        // Store the initial fit scale
        initialFitScale = fitScale;

        // Calculate position to center the image
        float scaledWidth = imageWidth * fitScale;
        float scaledHeight = imageHeight * fitScale;
        float translateX = (viewWidth - scaledWidth) / 2f;
        float translateY = (viewHeight - scaledHeight) / 2f;

        // Set initial matrix to fit and center the image
        matrix.reset();
        matrix.postScale(fitScale, fitScale);
        matrix.postTranslate(translateX, translateY);

        promoImageView.setImageMatrix(matrix);
    }

    private void setupZoom() {
        scaleDetector = new ScaleGestureDetector(this, new ScaleListener());

        promoImageView.setOnTouchListener(new View.OnTouchListener() {
            private int mode = 0;
            private static final int NONE = 0;
            private static final int DRAG = 1;
            private static final int ZOOM = 2;
            private long lastTapTime = 0;
            private static final long DOUBLE_TAP_TIMEOUT = 300;
            private float startX = 0;
            private float startY = 0;
            private boolean hasMoved = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isImageLoaded) return false;

                scaleDetector.onTouchEvent(event);
                PointF curr = new PointF(event.getX(), event.getY());

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        savedMatrix.set(matrix);
                        last.set(curr);
                        start.set(curr);
                        startX = curr.x;
                        startY = curr.y;
                        hasMoved = false;
                        mode = DRAG;
                        break;

                    case MotionEvent.ACTION_POINTER_DOWN:
                        savedMatrix.set(matrix);
                        last.set(curr);
                        start.set(curr);
                        mode = ZOOM;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (mode == DRAG) {
                            float dx = Math.abs(curr.x - startX);
                            float dy = Math.abs(curr.y - startY);
                            if (dx > 10 || dy > 10) {
                                hasMoved = true;
                            }

                            matrix.set(savedMatrix);
                            float deltaX = curr.x - start.x;
                            float deltaY = curr.y - start.y;
                            matrix.postTranslate(deltaX, deltaY);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        // Only trigger double tap if user didn't drag
                        if (!hasMoved && mode == DRAG) {
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastTapTime < DOUBLE_TAP_TIMEOUT) {
                                // Double tap detected
                                float[] values = new float[9];
                                matrix.getValues(values);
                                float currentScale = values[Matrix.MSCALE_X];

                                if (currentScale > initialFitScale * 1.1f) {
                                    // Zoomed in, zoom out to fit
                                    initializeImageMatrix();
                                } else {
                                    // Zoomed out, zoom in to 3x at tap point
                                    float viewWidth = promoImageView.getWidth();
                                    float viewHeight = promoImageView.getHeight();
                                    float scaledWidth = imageWidth * initialFitScale;
                                    float scaledHeight = imageHeight * initialFitScale;
                                    float translateX = (viewWidth - scaledWidth) / 2f;
                                    float translateY = (viewHeight - scaledHeight) / 2f;

                                    matrix.reset();
                                    matrix.postScale(initialFitScale, initialFitScale);
                                    matrix.postTranslate(translateX, translateY);

                                    float zoomScale = 3f;
                                    matrix.postScale(zoomScale, zoomScale, curr.x, curr.y);
                                    promoImageView.setImageMatrix(matrix);
                                }
                                lastTapTime = 0;
                            } else {
                                lastTapTime = currentTime;
                            }
                        }
                        mode = NONE;
                        break;

                    case MotionEvent.ACTION_POINTER_UP:
                        mode = NONE;
                        break;
                }

                promoImageView.setImageMatrix(matrix);
                return true;
            }
        });
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();

            float[] values = new float[9];
            matrix.getValues(values);
            float currentScale = values[Matrix.MSCALE_X];
            float newScale = currentScale * scaleFactor;

            // Allow zooming below initial fit scale down to MIN_SCALE
            float absoluteMinScale = Math.min(MIN_SCALE, initialFitScale);

            // Clamp scale between absoluteMinScale and MAX_SCALE
            if (newScale < absoluteMinScale) {
                scaleFactor = absoluteMinScale / currentScale;
            } else if (newScale > MAX_SCALE) {
                scaleFactor = MAX_SCALE / currentScale;
            }

            matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());

            return true;
        }
    }


    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    private void setupPromoChangeListener() {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        promoListener = db.collection("promotions")
                .document("latest")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        android.util.Log.w("Promo", "Listen failed", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        String newImageUrl = snapshot.getString("imageUrl");

                        // ✅ Check if promo was deleted or changed
                        if (newImageUrl == null || newImageUrl.isEmpty() ||
                                !newImageUrl.equals(currentPromoUrl)) {

                            android.util.Log.d("Promo", "Promo changed or deleted, closing activity");
                            showPromoUnavailableDialog();
                        }
                    } else {
                        // Document deleted
                        android.util.Log.d("Promo", "Promo document deleted");
                        showPromoUnavailableDialog();
                    }
                });
    }

    private void showPromoUnavailableDialog() {
        runOnUiThread(() -> {
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Promo Unavailable")
                    .setMessage("This promotion is no longer available.")
                    .setCancelable(false)
                    .setPositiveButton("OK", (d, which) -> {
                        finish();
                    })
                    .create();

            // ✅ Set rounded background
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
            }

            dialog.show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (promoListener != null) {
            promoListener.remove();
        }
    }
}