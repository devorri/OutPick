package com.example.outpick.admin;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.core.content.FileProvider;

import com.example.outpick.R;
import com.example.outpick.utils.ImageUploader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

            // Show loading
            btnSaveOutfit.setEnabled(false);
            btnSaveOutfit.setText("Capturing...");

            // Capture workspace snapshot
            captureAndUploadWorkspaceSnapshot();
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

        try {
            // Use ContentResolver to load the image safely
            imageView.setImageURI(uri);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            // Take persistable URI permission
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            Toast.makeText(this, "Cannot access this image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        } catch (Exception e) {
            Toast.makeText(this, "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        // Position the image in the center of workspace
        outfitWorkspace.post(() -> {
            imageView.setX(outfitWorkspace.getWidth() / 2f - initialSizePx / 2f);
            imageView.setY(outfitWorkspace.getHeight() / 2f - initialSizePx / 2f);
        });

        imageView.setOnTouchListener(new OutfitItemTouchListener(this));
        outfitWorkspace.addView(imageView);
    }

    /**
     * ✅ Captures the workspace layout as a Bitmap, uploads to Supabase Storage,
     * and proceeds to details activity with the public URL.
     */
    private void captureAndUploadWorkspaceSnapshot() {
        try {
            // Ensure workspace is laid out
            outfitWorkspace.measure(
                    View.MeasureSpec.makeMeasureSpec(outfitWorkspace.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(outfitWorkspace.getHeight(), View.MeasureSpec.EXACTLY)
            );
            outfitWorkspace.layout(0, 0, outfitWorkspace.getMeasuredWidth(), outfitWorkspace.getMeasuredHeight());

            // Create bitmap from the workspace view
            Bitmap bitmap = Bitmap.createBitmap(
                    outfitWorkspace.getWidth(),
                    outfitWorkspace.getHeight(),
                    Bitmap.Config.ARGB_8888
            );
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

            // ✅ Fill background white before drawing workspace content
            canvas.drawColor(android.graphics.Color.WHITE);

            // Draw workspace content
            outfitWorkspace.draw(canvas);

            // Save to internal storage using FileProvider for secure URI sharing
            File imageFile = new File(getFilesDir(), "outfit_snapshot_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.flush();
            }

            // Use FileProvider to create a content URI
            Uri snapshotUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    imageFile
            );

            // Upload to Supabase Storage
            uploadSnapshotToCloud(snapshotUri);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error capturing snapshot: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            btnSaveOutfit.setEnabled(true);
            btnSaveOutfit.setText("Save Outfit");
        }
    }

    /**
     * Upload the snapshot to Supabase Storage and proceed to details
     */
    private void uploadSnapshotToCloud(Uri snapshotUri) {
        btnSaveOutfit.setText("Uploading...");

        ImageUploader uploader = new ImageUploader(this);
        String fileName = "outfit_snapshot_" + System.currentTimeMillis() + ".jpg";

        uploader.uploadImage(snapshotUri, "outfits", fileName, new ImageUploader.UploadCallback() {
            @Override
            public void onSuccess(String publicImageUrl) {
                runOnUiThread(() -> {
                    // Proceed to outfit details screen with the PUBLIC URL
                    Toast.makeText(AdminAddOutfitActivity.this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(AdminAddOutfitActivity.this, AdminAddOutfitDetailsActivity.class);
                    intent.putExtra("snapshotImageUrl", publicImageUrl); // Public URL from cloud storage
                    intent.putExtra("snapshotImageUri", snapshotUri.toString()); // Local URI as backup
                    startActivity(intent);

                    btnSaveOutfit.setEnabled(true);
                    btnSaveOutfit.setText("Save Outfit");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(AdminAddOutfitActivity.this,
                            "Failed to upload image: " + error, Toast.LENGTH_LONG).show();

                    // Fallback: proceed with local URI if upload fails
                    Intent intent = new Intent(AdminAddOutfitActivity.this, AdminAddOutfitDetailsActivity.class);
                    intent.putExtra("snapshotImageUri", snapshotUri.toString());
                    startActivity(intent);

                    btnSaveOutfit.setEnabled(true);
                    btnSaveOutfit.setText("Save Outfit");
                });
            }
        });
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