package com.example.outpick.outfits;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.closet.ItemSelectionActivity;
import com.example.outpick.R;
import com.example.outpick.utils.ResizableImageView;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class TryOnActivity extends AppCompatActivity {

    private static final String TAG = "TryOnActivity";

    // Main Canvas container to hold the mannequin and dynamic ResizableImageViews
    private FrameLayout tryOnCanvas;

    // Mannequin (Avatar) is the static background image
    private ImageView avatarImage;

    // Drawer layout reference
    private DrawerLayout drawerLayout;

    // References to the included category drawer layouts
    private View drawerTops, drawerBottoms, drawerFootwear, drawerAccessories;

    // Supabase service
    private SupabaseService supabaseService;
    private List<ClothingItem> allClothesItemsFromDB = new ArrayList<>();

    // Current user ID
    private String currentUserId = "";

    // --- Activity Result Launcher ---
    private ActivityResultLauncher<Intent> itemSelectionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_try_on);

        // --- Supabase setup ---
        supabaseService = SupabaseClient.getService();

        // --- Get current user ID ---
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", "");

        // --- View Initialization ---
        drawerLayout = findViewById(R.id.drawer_layout);
        tryOnCanvas = findViewById(R.id.try_on_frame);
        avatarImage = findViewById(R.id.avatarImage);

        // Handle system insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- Load Data and Setup UI ---
        loadClothesAndSetupUI();

        // --- Register the Activity Result Launcher ---
        setupItemSelectionLauncher();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-load data in case items were added/deleted via other activities
        loadClothesAndSetupUI();
    }

    /**
     * Loads all items from Supabase and updates the UI (drawers, etc.).
     */
    private void loadClothesAndSetupUI() {
        // Load clothes from Supabase instead of SQLite
        loadClothesFromSupabase();

        Button btnWear = findViewById(R.id.btnWearOutfit);
        Button btnRemoveAll = findViewById(R.id.btnRemoveAll);
        Button btnRemoveOptions = findViewById(R.id.btnRemoveOptions);
        ImageView btnOpenDrawer = findViewById(R.id.btnOpenDrawer);

        // --- Drawer View Initialization (Mapping the <include> IDs) ---
        drawerTops = findViewById(R.id.drawer_tops);
        drawerBottoms = findViewById(R.id.drawer_bottoms);
        drawerFootwear = findViewById(R.id.drawer_footwear);
        drawerAccessories = findViewById(R.id.drawer_accessories);

        // --- Initial View State ---
        if (avatarImage.getDrawable() == null) {
            avatarImage.setImageResource(R.drawable.avatar_front);
            avatarImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }

        // --- Touch Listener Setup ---
        tryOnCanvas.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return false;
        });

        // --- Click Listeners ---
        ImageView backButton = findViewById(R.id.back_button);
        if (backButton != null) backButton.setOnClickListener(v -> finish());

        if (btnOpenDrawer != null) {
            btnOpenDrawer.setOnClickListener(v -> {
                drawerLayout.openDrawer(findViewById(R.id.left_drawer_content));
            });
        }

        btnWear.setOnClickListener(v -> wearOutfit());
        btnRemoveAll.setOnClickListener(v -> removeAllClothes());
        btnRemoveOptions.setOnClickListener(v -> showRemoveOptions());

        // Setup listener and populate items for drawer categories using current DB data
        setupDrawerListeners();
    }

    /**
     * Loads clothing items from Supabase
     */
    private void loadClothesFromSupabase() {
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                // Get all clothing items for the current user
                Call<List<JsonObject>> call = supabaseService.getClothing();
                Response<List<JsonObject>> response = call.execute();

                if (response.isSuccessful() && response.body() != null) {
                    List<ClothingItem> clothingItems = new ArrayList<>();
                    Gson gson = new Gson();

                    for (JsonObject jsonObject : response.body()) {
                        // Convert JsonObject to ClothingItem
                        ClothingItem item = gson.fromJson(jsonObject, ClothingItem.class);
                        clothingItems.add(item);
                    }

                    runOnUiThread(() -> {
                        allClothesItemsFromDB = clothingItems;
                        // Refresh the drawer items after loading data
                        setupDrawerListeners();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(TryOnActivity.this, "Failed to load clothing items", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(TryOnActivity.this, "Error loading clothing: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    /**
     * Initializes the launcher to handle the result from ItemSelectionActivity.
     */
    private void setupItemSelectionLauncher() {
        itemSelectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String selectedItemJson = data.getStringExtra(ItemSelectionActivity.EXTRA_SELECTED_ITEM_JSON);
                            if (selectedItemJson != null) {
                                Gson gson = new Gson();
                                ClothingItem selectedItem = gson.fromJson(selectedItemJson, ClothingItem.class);
                                addClothingItemToMannequin(selectedItem);
                            }
                        }
                    }
                });
    }

    /**
     * Filters the main list based on required categories.
     */
    private List<ClothingItem> filterItemsByCategoriesForDrawer(List<String> categories) {
        List<ClothingItem> filteredList = new ArrayList<>();

        for (ClothingItem item : allClothesItemsFromDB) {
            String itemMainCategory = "";
            String itemCategory = item.getCategory() != null ? item.getCategory() : "";
            int separatorIndex = itemCategory.indexOf('>');
            if (separatorIndex != -1) {
                itemMainCategory = itemCategory.substring(0, separatorIndex);
            } else {
                itemMainCategory = itemCategory;
            }

            for (String requiredCategory : categories) {
                if ("Bottoms".equalsIgnoreCase(requiredCategory) && "Bottom".equalsIgnoreCase(itemMainCategory)) {
                    filteredList.add(item);
                    break;
                }
                if (requiredCategory.equalsIgnoreCase(itemMainCategory)) {
                    filteredList.add(item);
                    break;
                }
            }
        }
        return filteredList;
    }

    /**
     * Helper method to set up the icon, title, and click listener for a single category view.
     */
    private void setupCategoryView(View categoryView, int iconResId, String titleText, final String[] categories, final String selectionCategory) {
        if (categoryView != null) {
            ImageView icon = categoryView.findViewById(R.id.categoryIcon);
            TextView title = categoryView.findViewById(R.id.categoryTitle);
            Button seeAll = categoryView.findViewById(R.id.seeAllButton);
            LinearLayout itemsContainer = categoryView.findViewById(R.id.clothingItemsContainer);

            if (icon != null) icon.setImageResource(iconResId);
            if (title != null) title.setText(titleText);

            // 1. Populate the Horizontal List (Drawer Preview)
            List<String> categoryList = Arrays.asList(categories);
            List<ClothingItem> drawerItems = filterItemsByCategoriesForDrawer(categoryList);
            displayDrawerItems(itemsContainer, drawerItems);

            if (seeAll != null) {
                seeAll.setOnClickListener(v -> {
                    drawerLayout.closeDrawers();
                    launchItemSelector(selectionCategory);
                });
            }
        }
    }

    /**
     * Dynamically populates the HorizontalScrollView with item thumbnails.
     */
    private void displayDrawerItems(LinearLayout container, List<ClothingItem> items) {
        container.removeAllViews();

        int maxItems = Math.min(items.size(), 4);

        for (int i = 0; i < maxItems; i++) {
            final ClothingItem item = items.get(i);
            ImageView imageView = new ImageView(this);

            int size = (int) (getResources().getDisplayMetrics().density * 80);
            int margin = (int) (getResources().getDisplayMetrics().density * 8);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMarginEnd(margin);
            imageView.setLayoutParams(params);

            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            // ✅ FIXED: Use Glide to load cloud images from Supabase URLs
            if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
                Glide.with(this)
                        .load(item.getImagePath()) // Load from cloud URL (HTTPS)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_error)
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.ic_placeholder);
            }

            imageView.setOnClickListener(v -> {
                drawerLayout.closeDrawers();
                addClothingItemToMannequin(item);
            });

            container.addView(imageView);
        }
    }

    private void setupDrawerListeners() {
        setupCategoryView(drawerTops, R.drawable.ic_shirt, "Tops & Outerwear", new String[]{"Top", "Outerwear"}, "Tops");
        setupCategoryView(drawerBottoms, R.drawable.ic_pants, "Bottoms", new String[]{"Bottom"}, "Bottoms");
        setupCategoryView(drawerFootwear, R.drawable.ic_shoe, "Footwear", new String[]{"Footwear"}, "Footwear");
        setupCategoryView(drawerAccessories, R.drawable.ic_watch, "Accessories", new String[]{"Accessory"}, "Accessories");
    }

    /**
     * Launches the ItemSelectionActivity with ALL items from Supabase.
     */
    private void launchItemSelector(String selectionCategory) {
        if (allClothesItemsFromDB.isEmpty()) {
            Toast.makeText(this, "Your closet is empty. Add clothes first!", Toast.LENGTH_SHORT).show();
            return;
        }

        Gson gson = new Gson();
        String itemListJson = gson.toJson(allClothesItemsFromDB);

        Intent intent = new Intent(this, ItemSelectionActivity.class);
        intent.putExtra(ItemSelectionActivity.EXTRA_ITEM_CATEGORY, selectionCategory);
        intent.putExtra(ItemSelectionActivity.EXTRA_ITEM_LIST_JSON, itemListJson);

        itemSelectionLauncher.launch(intent);
    }

    /**
     * Creates and adds a ResizableImageView to the canvas for the selected item.
     */
    private void addClothingItemToMannequin(ClothingItem item) {
        ResizableImageView clothingView = new ResizableImageView(this);

        // ✅ FIXED: Use Glide to load cloud images in ResizableImageView
        if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
            Glide.with(this)
                    .load(item.getImagePath()) // Load from cloud URL (HTTPS)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .into(clothingView);
        } else {
            clothingView.setImageResource(R.drawable.ic_placeholder);
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = android.view.Gravity.CENTER;
        clothingView.setLayoutParams(params);

        clothingView.setTag(item.getId());
        clothingView.setContentDescription(item.getCategory());

        clothingView.resetTransformation();

        tryOnCanvas.addView(clothingView);
        clothingView.bringToFront();

        Toast.makeText(this, item.getName() + " (" + item.getCategory() + ") added. Drag and pinch to adjust.", Toast.LENGTH_LONG).show();
    }

    /**
     * Action for the "Wear Outfit" button.
     */
    private void wearOutfit() {
        Toast.makeText(this, "Outfit worn! Saving transformation data...", Toast.LENGTH_SHORT).show();
        // TODO: Save outfit to Supabase if needed
    }

    /**
     * Action for the "Remove All Clothes" button.
     */
    private void removeAllClothes() {
        List<View> toRemove = new ArrayList<>();

        for (int i = 0; i < tryOnCanvas.getChildCount(); i++) {
            View child = tryOnCanvas.getChildAt(i);
            if (child instanceof ResizableImageView) {
                toRemove.add(child);
            }
        }

        for (View view : toRemove) {
            tryOnCanvas.removeView(view);
        }

        Toast.makeText(this, "All clothes removed.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Action for the "Selective Remove" button.
     */
    private void showRemoveOptions() {
        final List<ResizableImageView> wornItems = new ArrayList<>();
        final List<String> options = new ArrayList<>();

        for (int i = 0; i < tryOnCanvas.getChildCount(); i++) {
            View child = tryOnCanvas.getChildAt(i);
            if (child instanceof ResizableImageView) {
                ResizableImageView item = (ResizableImageView) child;
                wornItems.add(item);
                options.add(item.getContentDescription().toString() + " (ID: " + item.getTag() + ")");
            }
        }

        if (wornItems.isEmpty()) {
            Toast.makeText(this, "Nothing to remove selectively.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select item to remove")
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    ResizableImageView selectedItem = wornItems.get(which);
                    tryOnCanvas.removeView(selectedItem);
                    Toast.makeText(this, selectedItem.getContentDescription().toString() + " removed.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}