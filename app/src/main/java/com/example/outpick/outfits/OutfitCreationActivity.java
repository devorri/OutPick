package com.example.outpick.outfits;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.closet.ItemsAddingActivity;
import com.example.outpick.R;
import com.example.outpick.common.SpecifyDetailsActivity;
import com.example.outpick.common.adapters.ClothingAdapter;
import com.example.outpick.database.repositories.ClothingRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.utils.ImageUploader;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutfitCreationActivity extends AppCompatActivity {

    private static final String TAG = "OutfitCreationActivity";

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
    private ImageUploader imageUploader;
    private SupabaseService supabaseService;
    private ClothingRepository clothingRepository;
    private String currentUserId;

    // Closet browser elements
    private RecyclerView closetRecyclerView;
    private ClothingAdapter clothingAdapter;
    private List<ClothingItem> userClothingItems = new ArrayList<>();
    private LinearLayout closetBrowserLayout;
    private TextView closetTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outfit_creation);

        // Initialize services
        imageUploader = new ImageUploader(this);
        supabaseService = SupabaseClient.getService();
        clothingRepository = ClothingRepository.getInstance(supabaseService); // ✅ ADD THIS

        // Get current user ID
        currentUserId = getCurrentUserId();

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

        // Closet browser elements
        closetRecyclerView = findViewById(R.id.closetRecyclerView);
        closetBrowserLayout = findViewById(R.id.closetBrowserLayout);
        closetTitle = findViewById(R.id.closetTitle);

        btnAddMoreItems.setVisibility(View.GONE);
        globalControlButtons.setVisibility(View.GONE);
        closetBrowserLayout.setVisibility(View.VISIBLE);

        // Setup closet browser
        setupClosetBrowser();

        backArrow.setOnClickListener(v -> onBackPressed());

        addItemBtn.setOnClickListener(v -> {
            if ("Add Item".equals(addItemBtn.getText().toString())) {
                closetBrowserLayout.setVisibility(View.VISIBLE);
            } else {
                goToSpecifyDetails();
            }
        });

        btnAddMoreItems.setOnClickListener(v -> {
            closetBrowserLayout.setVisibility(View.VISIBLE);
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

    /**
     * Setup the closet browser to show user's existing clothes
     */
    private void setupClosetBrowser() {
        // Use your existing ClothingAdapter
        clothingAdapter = new ClothingAdapter(this, new ArrayList<>(userClothingItems));
        clothingAdapter.setShowCheckboxes(false); // Don't show checkboxes for single selection
        clothingAdapter.setShowAddTile(false); // Don't show add tile in the grid
        clothingAdapter.setOnItemClickListener(new ClothingAdapter.OnItemClickListener() {
            @Override
            public void onClothingClick(ClothingItem item) {
                // Add selected clothing item to workspace
                addDraggableImage(item.getImageUri());
                addItemBtn.setText("Save");
                btnAddMoreItems.setVisibility(View.VISIBLE);

                // Hide closet browser after selection
                closetBrowserLayout.setVisibility(View.GONE);

                Toast.makeText(OutfitCreationActivity.this,
                        "Added " + item.getName() + " to workspace", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAddItemClick() {
                // This won't be called since we set showAddTile to false
            }
        });

        closetRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        closetRecyclerView.setAdapter(clothingAdapter);

        // Add "Add New" button functionality
        TextView btnAddNewClothes = findViewById(R.id.btnAddNewClothes);
        btnAddNewClothes.setOnClickListener(v -> {
            // Open camera/gallery to add new clothes
            Intent intent = new Intent(OutfitCreationActivity.this, ItemsAddingActivity.class);
            intent.putExtra("username", username);
            startActivityForResult(intent, REQUEST_ADD_MORE_ITEMS);
        });

        // Load user's clothes from Supabase
        loadUserClothing();
    }

    /**
     * Load user's existing clothes from Supabase - FIXED VERSION
     */
    private void loadUserClothing() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "No user ID found - cannot load clothes");
            Toast.makeText(this, "Please log in to view your clothes", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Loading clothes for user: " + currentUserId);

        // Show loading
        closetTitle.setText("Loading your clothes...");

        new Thread(() -> {
            try {
                // ✅ USE THE SAME METHOD AS YourClothesActivity
                List<ClothingItem> items = clothingRepository.getClothingByUserId(currentUserId);

                runOnUiThread(() -> {
                    if (items != null && !items.isEmpty()) {
                        userClothingItems.clear();
                        userClothingItems.addAll(items);

                        // Update the adapter with new data
                        clothingAdapter.setItems(new ArrayList<>(userClothingItems));
                        closetTitle.setText("My Clothes (" + userClothingItems.size() + " items)");

                        Log.d(TAG, "✅ Successfully loaded " + userClothingItems.size() + " clothing items");
                        Toast.makeText(OutfitCreationActivity.this,
                                "Loaded " + userClothingItems.size() + " clothing items", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "❌ No clothes found for user");
                        closetTitle.setText("My Clothes (0 items)");
                        Toast.makeText(OutfitCreationActivity.this,
                                "No clothes found. Add some clothes first!", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "❌ Error loading clothes: " + e.getMessage());
                runOnUiThread(() -> {
                    closetTitle.setText("My Clothes (Error)");
                    Toast.makeText(OutfitCreationActivity.this,
                            "Error loading clothes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Get current user ID from SharedPreferences
     */
    private String getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        if (userId.isEmpty()) {
            Log.e(TAG, "No user ID found in SharedPreferences!");
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
        }
        return userId;
    }

    /**
     * Capture outfit and show closet selection
     */
    private void goToSpecifyDetails() {
        hideControlPanel();
        Bitmap snapshot = captureOutfitSnapshot();
        if (snapshot == null) {
            Toast.makeText(this, "❌ Failed to capture outfit", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show closet selection dialog before proceeding
        showClosetSelectionDialog(snapshot);
    }

    /**
     * Show dialog to select which closet to save the outfit to
     */
    private void showClosetSelectionDialog(Bitmap snapshot) {
        // Load user's closets first
        loadUserClosets(new ClosetSelectionCallback() {
            @Override
            public void onClosetsLoaded(List<com.example.outpick.database.models.ClosetItem> closets) {
                if (closets.isEmpty()) {
                    Toast.makeText(OutfitCreationActivity.this,
                            "Please create a closet first!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Show closet selection dialog
                showClosetDialog(closets, snapshot);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(OutfitCreationActivity.this,
                        "Failed to load closets: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Show dialog with closet options
     */
    private void showClosetDialog(List<com.example.outpick.database.models.ClosetItem> closets, Bitmap snapshot) {
        String[] closetNames = new String[closets.size()];
        for (int i = 0; i < closets.size(); i++) {
            closetNames[i] = closets.get(i).getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Save Outfit To Closet")
                .setItems(closetNames, (dialog, which) -> {
                    String selectedClosetId = closets.get(which).getId();
                    String selectedClosetName = closets.get(which).getName();

                    // Proceed to SpecifyDetailsActivity with closet info
                    proceedToSpecifyDetails(snapshot, selectedClosetId, selectedClosetName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Load user's closets from Supabase
     */
    private void loadUserClosets(ClosetSelectionCallback callback) {
        Call<List<JsonObject>> call = supabaseService.getClosets();
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<com.example.outpick.database.models.ClosetItem> userClosets = new ArrayList<>();

                    for (JsonObject jsonObject : response.body()) {
                        // Filter by current user
                        String userId = jsonObject.has("user_id") && !jsonObject.get("user_id").isJsonNull()
                                ? jsonObject.get("user_id").getAsString() : "";

                        if (currentUserId.equals(userId)) {
                            com.example.outpick.database.models.ClosetItem closet = new com.example.outpick.database.models.ClosetItem(
                                    jsonObject.get("id").getAsString(),
                                    jsonObject.get("name").getAsString(),
                                    "",
                                    jsonObject.get("image_uri").getAsString(),
                                    ""
                            );
                            userClosets.add(closet);
                        }
                    }

                    callback.onClosetsLoaded(userClosets);
                } else {
                    callback.onError("Failed to load closets");
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    interface ClosetSelectionCallback {
        void onClosetsLoaded(List<com.example.outpick.database.models.ClosetItem> closets);
        void onError(String error);
    }

    /**
     * Proceed to SpecifyDetailsActivity with closet information
     */
    private void proceedToSpecifyDetails(Bitmap snapshot, String closetId, String closetName) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        snapshot.compress(Bitmap.CompressFormat.PNG, 90, stream);
        byte[] byteArray = stream.toByteArray();

        Intent intent = new Intent(this, SpecifyDetailsActivity.class);
        intent.putExtra("snapshot", byteArray);
        intent.putExtra("username", username);
        intent.putExtra("selected_closet_id", closetId);
        intent.putExtra("selected_closet_name", closetName);
        startActivity(intent);
        finish();
    }

    private void addDraggableImage(String imagePath) {
        workspace.post(() -> {
            LayoutInflater inflater = LayoutInflater.from(this);
            View itemContainer = inflater.inflate(R.layout.outfit_item_container, workspace, false);
            ImageView imageView = itemContainer.findViewById(R.id.outfitItemImage);
            View border = itemContainer.findViewById(R.id.itemBorder);

            // Use Glide to handle both cloud URLs and local files
            if (imagePath != null && !imagePath.isEmpty()) {
                Glide.with(this)
                        .load(imagePath)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_error)
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.ic_placeholder);
            }

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

                // Reload clothes to show the newly added items
                loadUserClothing();
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