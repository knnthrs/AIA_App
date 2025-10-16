package com.example.signuploginrealtime;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.graphics.Matrix;
import android.graphics.PointF;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class Promo extends AppCompatActivity {
    private ImageView promoImageView;
    private Matrix matrix = new Matrix();
    private float scale = 1f;
    private ScaleGestureDetector scaleDetector;

    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 5f;

    private PointF last = new PointF();
    private PointF start = new PointF();
    private float[] matrixValues = new float[9];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promo);

        // Get the ImageView
        promoImageView = findViewById(R.id.promoImageView);

        // Get the back button
        ImageView backButton = findViewById(R.id.back_button);

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

        // Set back button click listener
        backButton.setOnClickListener(v -> finish());

        // Setup zoom functionality
        setupZoom();
    }

    private void setupZoom() {
        scaleDetector = new ScaleGestureDetector(this, new ScaleListener());

        promoImageView.setOnTouchListener(new View.OnTouchListener() {
            private int mode = 0;
            private static final int NONE = 0;
            private static final int DRAG = 1;
            private static final int ZOOM = 2;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleDetector.onTouchEvent(event);

                PointF curr = new PointF(event.getX(), event.getY());

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        last.set(curr);
                        start.set(last);
                        mode = DRAG;
                        break;

                    case MotionEvent.ACTION_POINTER_DOWN:
                        last.set(curr);
                        start.set(last);
                        mode = ZOOM;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (mode == DRAG && scale > MIN_SCALE) {
                            float deltaX = curr.x - last.x;
                            float deltaY = curr.y - last.y;
                            matrix.postTranslate(deltaX, deltaY);
                            limitDrag();
                            last.set(curr.x, curr.y);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        mode = NONE;
                        break;
                }

                promoImageView.setImageMatrix(matrix);
                return true;
            }
        });
    }

    private void limitDrag() {
        matrix.getValues(matrixValues);
        float transX = matrixValues[Matrix.MTRANS_X];
        float transY = matrixValues[Matrix.MTRANS_Y];

        float width = promoImageView.getDrawable().getIntrinsicWidth() * scale;
        float height = promoImageView.getDrawable().getIntrinsicHeight() * scale;

        float viewWidth = promoImageView.getWidth();
        float viewHeight = promoImageView.getHeight();

        float minTransX = viewWidth - width;
        float minTransY = viewHeight - height;

        if (width < viewWidth) {
            transX = (viewWidth - width) / 2;
        } else {
            if (transX > 0) transX = 0;
            if (transX < minTransX) transX = minTransX;
        }

        if (height < viewHeight) {
            transY = (viewHeight - height) / 2;
        } else {
            if (transY > 0) transY = 0;
            if (transY < minTransY) transY = minTransY;
        }

        matrixValues[Matrix.MTRANS_X] = transX;
        matrixValues[Matrix.MTRANS_Y] = transY;
        matrix.setValues(matrixValues);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float newScale = scale * scaleFactor;

            if (newScale > MAX_SCALE) {
                scaleFactor = MAX_SCALE / scale;
            } else if (newScale < MIN_SCALE) {
                scaleFactor = MIN_SCALE / scale;
            }

            scale *= scaleFactor;
            scale = Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));

            matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            limitDrag();

            return true;
        }
    }
}