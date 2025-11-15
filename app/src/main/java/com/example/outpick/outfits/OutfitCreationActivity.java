package com.example.outpick.outfits;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.closet.ItemsAddingActivity;
import com.example.outpick.R;
import com.example.outpick.common.SpecifyDetailsActivity;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

public class OutfitCreationActivity extends AppCompatActivity {

    private ImageView backArrow;
    private MaterialButton addItemBtn;
    private FrameLayout workspace;
    private ImageView btnAddMoreItems;

    private LinearLayout globalControlButtons;
    private ImageButton btnZoomIn, btnZoomOut, btnClose;

    private ImageView selectedImageView = null;
    private View selectedItemContainer = null;
    private boolean controlsVisible = false;

    private static final int REQUEST_ADD_MORE_ITEMS = 101;
    private String username = "Guest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outfit_creation);

        String passedUsername = getIntent().getStringExtra("username");
        if (passedUsername != null && !passedUsername.trim().isEmpty()) {
            username = passedUsername;
        }

        backArrow = findViewById(R.id.back_arrow);
        addItemBtn = findViewById(R.id.add_item_button);
        workspace = findViewById(R.id.workspace);
        btnAddMoreItems = findViewById(R.id.btn_add_more_items);

        globalControlButtons = findViewById(R.id.global_control_buttons);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnClose = findViewById(R.id.btnClose);

        btnAddMoreItems.setVisibility(View.GONE);
        globalControlButtons.setVisibility(View.GONE);

        backArrow.setOnClickListener(v -> onBackPressed());

        addItemBtn.setOnClickListener(v -> {
            if ("Add Item".equals(addItemBtn.getText().toString())) {
                Intent intent = new Intent(this, ItemsAddingActivity.class);
                intent.putExtra("username", username);
                startActivity(intent);
            } else {
                goToSpecifyDetails(); // send snapshot to SpecifyDetailsActivity
            }
        });

        btnAddMoreItems.setOnClickListener(v -> {
            Intent intent = new Intent(this, ItemsAddingActivity.class);
            intent.putExtra("isAddingMore", true);
            intent.putExtra("username", username);
            startActivityForResult(intent, REQUEST_ADD_MORE_ITEMS);
        });

        btnZoomIn.setOnClickListener(v -> {
            if (selectedItemContainer != null) {
                selectedItemContainer.setScaleX(selectedItemContainer.getScaleX() * 1.1f);
                selectedItemContainer.setScaleY(selectedItemContainer.getScaleY() * 1.1f);
            }
        });

        btnZoomOut.setOnClickListener(v -> {
            if (selectedItemContainer != null) {
                selectedItemContainer.setScaleX(selectedItemContainer.getScaleX() * 0.9f);
                selectedItemContainer.setScaleY(selectedItemContainer.getScaleY() * 0.9f);
            }
        });

        btnClose.setOnClickListener(v -> {
            if (selectedItemContainer != null) {
                workspace.removeView(selectedItemContainer);
                hideControlPanel();
            }
        });

        workspace.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideControlPanel();
            }
            return false;
        });

        ArrayList<ClothingItem> selectedItems =
                (ArrayList<ClothingItem>) getIntent().getSerializableExtra("selected_items");

        if (selectedItems != null && !selectedItems.isEmpty()) {
            addItemBtn.setText("Save");
            for (ClothingItem item : selectedItems) {
                addDraggableImage(item.getImageUri());
            }
            btnAddMoreItems.setVisibility(View.VISIBLE);
        }
    }

    private void addDraggableImage(String imagePath) {
        workspace.post(() -> {
            LayoutInflater inflater = LayoutInflater.from(this);
            View itemContainer = inflater.inflate(R.layout.outfit_item_container, workspace, false);
            ImageView imageView = itemContainer.findViewById(R.id.outfitItemImage);
            View border = itemContainer.findViewById(R.id.itemBorder);

            Glide.with(this)
                    .load(new File(imagePath))
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .into(imageView);

            int size = 400;
            int centerX = (workspace.getWidth() - size) / 2;
            int centerY = (workspace.getHeight() - size) / 2;

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            params.leftMargin = Math.max(centerX, 0);
            params.topMargin = Math.max(centerY, 0);
            itemContainer.setLayoutParams(params);

            imageView.setOnTouchListener(new View.OnTouchListener() {
                float dX, dY;
                boolean isDragging = false;
                long touchDownTime;

                float initialDistance = 0f;
                float initialScaleX = 1f, initialScaleY = 1f;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            touchDownTime = System.currentTimeMillis();
                            dX = itemContainer.getX() - event.getRawX();
                            dY = itemContainer.getY() - event.getRawY();
                            isDragging = false;
                            return true;

                        case MotionEvent.ACTION_POINTER_DOWN:
                            if (event.getPointerCount() == 2) {
                                initialDistance = getFingerSpacing(event);
                                initialScaleX = itemContainer.getScaleX();
                                initialScaleY = itemContainer.getScaleY();
                            }
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            if (event.getPointerCount() == 1) {
                                float newX = event.getRawX() + dX;
                                float newY = event.getRawY() + dY;
                                itemContainer.setX(newX);
                                itemContainer.setY(newY);
                                isDragging = true;
                            } else if (event.getPointerCount() == 2) {
                                float newDistance = getFingerSpacing(event);
                                float scale = newDistance / initialDistance;
                                itemContainer.setScaleX(initialScaleX * scale);
                                itemContainer.setScaleY(initialScaleY * scale);
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                            long duration = System.currentTimeMillis() - touchDownTime;
                            if (!isDragging && duration < 200) {
                                toggleControls(itemContainer, imageView, border);
                            }
                            return true;
                    }
                    return false;
                }

                private float getFingerSpacing(MotionEvent event) {
                    float x = event.getX(0) - event.getX(1);
                    float y = event.getY(0) - event.getY(1);
                    return (float) Math.sqrt(x * x + y * y);
                }
            });

            workspace.addView(itemContainer);
        });
    }

    private void toggleControls(View container, ImageView imageView, View border) {
        if (selectedItemContainer == container && controlsVisible) {
            hideControlPanel();
        } else {
            if (selectedItemContainer != null && selectedItemContainer != container) {
                View oldBorder = selectedItemContainer.findViewById(R.id.itemBorder);
                if (oldBorder != null) oldBorder.setVisibility(View.GONE);
            }

            selectedItemContainer = container;
            selectedImageView = imageView;

            if (border != null) {
                border.setVisibility(View.VISIBLE);
                border.setBackgroundResource(R.drawable.item_border_dashed);
            }

            globalControlButtons.setVisibility(View.VISIBLE);
            controlsVisible = true;
        }
    }

    private void hideControlPanel() {
        globalControlButtons.setVisibility(View.GONE);
        if (selectedItemContainer != null) {
            View border = selectedItemContainer.findViewById(R.id.itemBorder);
            if (border != null) border.setVisibility(View.GONE);
        }
        selectedImageView = null;
        selectedItemContainer = null;
        controlsVisible = false;
    }

    /**
     * Instead of saving directly to DB and going to OutfitCombination,
     * we capture snapshot and send it to SpecifyDetailsActivity.
     */
    private void goToSpecifyDetails() {
        hideControlPanel();
        Bitmap snapshot = captureOutfitSnapshot();
        if (snapshot == null) {
            Toast.makeText(this, "❌ Failed to capture outfit", Toast.LENGTH_SHORT).show();
            return;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        snapshot.compress(Bitmap.CompressFormat.PNG, 90, stream);
        byte[] byteArray = stream.toByteArray();

        Intent intent = new Intent(this, SpecifyDetailsActivity.class);
        intent.putExtra("snapshot", byteArray);
        intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }

    private Bitmap captureOutfitSnapshot() {
        workspace.setDrawingCacheEnabled(true);
        workspace.buildDrawingCache();
        Bitmap original = Bitmap.createBitmap(workspace.getDrawingCache());
        workspace.setDrawingCacheEnabled(false);

        if (original == null) return null;

        int maxSize = 600;
        float scale = Math.min((float) maxSize / original.getWidth(), (float) maxSize / original.getHeight());
        int newW = Math.round(original.getWidth() * scale);
        int newH = Math.round(original.getHeight() * scale);

        return Bitmap.createScaledBitmap(original, newW, newH, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ADD_MORE_ITEMS && resultCode == RESULT_OK && data != null) {
            ArrayList<ClothingItem> newItems = (ArrayList<ClothingItem>) data.getSerializableExtra("selected_items");
            if (newItems != null && !newItems.isEmpty()) {
                for (ClothingItem item : newItems) {
                    addDraggableImage(item.getImageUri());
                }
                addItemBtn.setText("Save");
                btnAddMoreItems.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Are you sure you want to exit without saving?")
                .setMessage("Your changes will be lost if you exit.")
                .setPositiveButton("Exit", (dialog, which) -> super.onBackPressed())
                .setNegativeButton("Stay", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }
}
