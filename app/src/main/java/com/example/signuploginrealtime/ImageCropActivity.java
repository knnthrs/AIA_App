package com.example.signuploginrealtime;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageCropActivity extends AppCompatActivity {

    private ImageView imageView;
    private View cropOverlay;
    private CardView btnCancel, btnDone;

    private Bitmap originalBitmap;
    private Matrix matrix = new Matrix();
    private float scale = 1f;
    private float minScale = 1f;
    private float maxScale = 4f;

    // For touch gestures
    private float lastTouchX, lastTouchY;
    private float posX = 0f, posY = 0f;
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    // For pinch zoom
    private float oldDist = 1f;
    private float newDist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_image_crop);

        imageView = findViewById(R.id.crop_image_view);
        cropOverlay = findViewById(R.id.crop_overlay);
        btnCancel = findViewById(R.id.btn_cancel_crop);
        btnDone = findViewById(R.id.btn_done_crop);

        // Handle system insets for top and bottom bars
        View topBar = findViewById(R.id.top_bar);
        View bottomBar = findViewById(R.id.bottom_bar);

        ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    v.getPaddingLeft() + systemBars.left,
                    v.getPaddingTop() + systemBars.top,
                    v.getPaddingRight() + systemBars.right,
                    v.getPaddingBottom()
            );
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(bottomBar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    v.getPaddingLeft() + systemBars.left,
                    v.getPaddingTop(),
                    v.getPaddingRight() + systemBars.right,
                    v.getPaddingBottom() + systemBars.bottom
            );
            return insets;
        });

        // Get image URI from intent
        String imageUriString = getIntent().getStringExtra("imageUri");
        if (imageUriString == null) {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Uri imageUri = Uri.parse(imageUriString);
        loadImage(imageUri);

        // Set up touch listener for pan and zoom
        imageView.setOnTouchListener((v, event) -> handleTouch(event));

        btnCancel.setOnClickListener(v -> finish());

        btnDone.setOnClickListener(v -> {
            Bitmap croppedBitmap = cropImage();
            if (croppedBitmap != null) {
                // Convert bitmap to URI and return
                Uri croppedUri = bitmapToUri(croppedBitmap);
                Intent resultIntent = new Intent();
                resultIntent.putExtra("croppedImageUri", croppedUri.toString());
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Failed to crop image", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            originalBitmap = BitmapFactory.decodeStream(inputStream);

            if (originalBitmap != null) {
                imageView.setImageBitmap(originalBitmap);

                // Wait for layout to calculate initial scale
                imageView.post(() -> {
                    calculateInitialScale();
                    applyMatrix();
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void calculateInitialScale() {
        if (originalBitmap == null) return;

        int viewWidth = imageView.getWidth();
        int viewHeight = imageView.getHeight();
        int bitmapWidth = originalBitmap.getWidth();
        int bitmapHeight = originalBitmap.getHeight();

        // Calculate scale to fit the crop area (which is square in the center)
        int cropSize = Math.min(viewWidth, viewHeight);

        float scaleX = (float) cropSize / bitmapWidth;
        float scaleY = (float) cropSize / bitmapHeight;

        // Use the larger scale to ensure the image covers the crop area
        scale = Math.max(scaleX, scaleY);
        minScale = scale;

        // Center the image
        posX = (viewWidth - bitmapWidth * scale) / 2;
        posY = (viewHeight - bitmapHeight * scale) / 2;
    }

    private boolean handleTouch(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mode = DRAG;
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    mode = ZOOM;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;

                    posX += dx;
                    posY += dy;

                    lastTouchX = event.getX();
                    lastTouchY = event.getY();

                    applyMatrix();
                } else if (mode == ZOOM && event.getPointerCount() == 2) {
                    newDist = spacing(event);
                    if (newDist > 10f) {
                        float scaleFactor = newDist / oldDist;
                        float newScale = scale * scaleFactor;

                        if (newScale >= minScale && newScale <= maxScale) {
                            scale = newScale;

                            // Zoom relative to the center point between two fingers
                            float focusX = (event.getX(0) + event.getX(1)) / 2;
                            float focusY = (event.getY(0) + event.getY(1)) / 2;

                            posX = focusX - (focusX - posX) * scaleFactor;
                            posY = focusY - (focusY - posY) * scaleFactor;

                            applyMatrix();
                        }
                        oldDist = newDist;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
        }
        return true;
    }

    private void applyMatrix() {
        matrix.reset();
        matrix.postScale(scale, scale);
        matrix.postTranslate(posX, posY);
        imageView.setImageMatrix(matrix);
    }

    private float spacing(MotionEvent event) {
        if (event.getPointerCount() < 2) return 0;
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private Bitmap cropImage() {
        if (originalBitmap == null) return null;

        try {
            // Get crop area dimensions
            int viewWidth = imageView.getWidth();
            int viewHeight = imageView.getHeight();
            int cropSize = Math.min(viewWidth, viewHeight);

            // Calculate crop area position (centered)
            int cropLeft = (viewWidth - cropSize) / 2;
            int cropTop = (viewHeight - cropSize) / 2;

            // Create a bitmap of the visible area
            imageView.setDrawingCacheEnabled(true);
            imageView.buildDrawingCache();
            Bitmap viewBitmap = Bitmap.createBitmap(imageView.getDrawingCache());
            imageView.setDrawingCacheEnabled(false);

            // Crop the bitmap to the square area
            Bitmap croppedBitmap = Bitmap.createBitmap(
                    viewBitmap,
                    cropLeft,
                    cropTop,
                    cropSize,
                    cropSize
            );

            viewBitmap.recycle();

            // Optionally resize to a standard size (e.g., 800x800)
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, 800, 800, true);
            croppedBitmap.recycle();

            return resizedBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Uri bitmapToUri(Bitmap bitmap) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);

            String path = android.provider.MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    bitmap,
                    "cropped_profile_" + System.currentTimeMillis(),
                    null
            );

            return Uri.parse(path);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}