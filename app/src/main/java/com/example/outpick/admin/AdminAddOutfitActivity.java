package com.example.outpick.admin;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.outpick.R;

import java.io.File;
import java.io.FileOutputStream;

/**
 * AdminAddOutfitActivity:
 * Admin can arrange outfit items on a workspace,
 * then save a snapshot and proceed to outfit details.
 */
public class AdminAddOutfitActivity extends AppCompatActivity {

    private FrameLayout outfitWorkspace;
    private Button btnChooseItems;
    private Button btnSaveOutfit;
    private ImageButton btnBack;

    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_outfit);

        outfitWorkspace = findViewById(R.id.outfitWorkspace);
        btnChooseItems = findViewById(R.id.btnChooseItems);
        btnSaveOutfit = findViewById(R.id.btnSaveOutfit);
        btnBack = findViewById(R.id.btnBack);

        setupGalleryLauncher();

        btnBack.setOnClickListener(v -> finish());

        btnChooseItems.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        // ✅ Capture workspace image and go to details activity
        btnSaveOutfit.setOnClickListener(v -> {
            int count = outfitWorkspace.getChildCount();
            if (count == 0) {
                Toast.makeText(this, "Please add at least one item first.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Capture workspace snapshot
            Uri snapshotUri = captureWorkspaceSnapshot();
            if (snapshotUri == null) {
                Toast.makeText(this, "Failed to capture outfit image.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Proceed to outfit details screen
            Toast.makeText(this, "Proceeding to outfit details...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, AdminAddOutfitDetailsActivity.class);
            intent.putExtra("snapshotImageUri", snapshotUri.toString());
            startActivity(intent);
        });
    }

    /**
     * Launch gallery to choose clothing items.
     */
    private void setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        Toast.makeText(this, "Selected " + uris.size() + " items. Tap and drag them!", Toast.LENGTH_LONG).show();
                        for (Uri uri : uris) {
                            addItemToWorkspace(uri);
                        }
                    } else {
                        Toast.makeText(this, "No items selected.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Add an image to the workspace, draggable and resizable.
     */
    private void addItemToWorkspace(Uri uri) {
        final int initialSizePx = (int) (200 * getResources().getDisplayMetrics().density);

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new FrameLayout.LayoutParams(initialSizePx, initialSizePx));
        imageView.setImageURI(uri);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {}

        outfitWorkspace.post(() -> {
            imageView.setX(outfitWorkspace.getWidth() / 2f - initialSizePx / 2f);
            imageView.setY(outfitWorkspace.getHeight() / 2f - initialSizePx / 2f);
        });

        imageView.setOnTouchListener(new OutfitItemTouchListener(this));
        outfitWorkspace.addView(imageView);
    }

    /**
     * ✅ Captures the workspace layout as a Bitmap, fills white background,
     * saves it internally, and returns a content Uri to the saved file.
     */
    private Uri captureWorkspaceSnapshot() {
        try {
            // Create bitmap from the workspace view
            Bitmap bitmap = Bitmap.createBitmap(outfitWorkspace.getWidth(), outfitWorkspace.getHeight(), Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

            // ✅ Fill background white before drawing workspace content
            canvas.drawColor(android.graphics.Color.WHITE);

            // Draw workspace content
            outfitWorkspace.draw(canvas);

            // Save to internal storage
            File imageFile = new File(getFilesDir(), "outfit_snapshot_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            return Uri.fromFile(imageFile);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Custom touch listener for drag + pinch zoom.
     */
    private static class OutfitItemTouchListener implements View.OnTouchListener {
        private final ScaleGestureDetector scaleDetector;
        private final GestureDetector gestureDetector;
        private float scaleFactor = 1.0f;
        private float lastX, lastY;
        private boolean isScaling = false;
        private View targetView;

        public OutfitItemTouchListener(Context context) {
            scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
            gestureDetector = new GestureDetector(context, new GestureListener());
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            targetView = view;
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.bringToFront();
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!isScaling && event.getPointerCount() == 1) {
                        float dx = event.getRawX() - lastX;
                        float dy = event.getRawY() - lastY;
                        view.setX(view.getX() + dx);
                        view.setY(view.getY() + dy);
                        lastX = event.getRawX();
                        lastY = event.getRawY();
                    }
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    isScaling = true;
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    isScaling = false;
                    break;
            }
            return true;
        }

        private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (targetView == null) return false;
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 5.0f));
                targetView.setScaleX(scaleFactor);
                targetView.setScaleY(scaleFactor);
                return true;
            }
        }

        private class GestureListener extends GestureDetector.SimpleOnGestureListener {
            @Override
            public void onLongPress(MotionEvent e) {
                if (targetView != null) {
                    Toast.makeText(targetView.getContext(), "Long press on item!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
