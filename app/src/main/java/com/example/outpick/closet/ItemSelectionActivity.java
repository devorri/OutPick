package com.example.outpick.closet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.LinearLayout;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.common.adapters.ClothingItemAdapter;
import com.example.outpick.database.repositories.ClothingRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity for displaying a grid of clothing items allowing the user to select one
 * to place on the TryOnActivity canvas. Updated for Supabase integration with Storage bucket.
 */
public class ItemSelectionActivity extends AppCompatActivity implements ClothingItemAdapter.OnItemClickListener {

    public static final String EXTRA_SELECTED_ITEM_JSON = "extra_selected_item_json";
    public static final String EXTRA_ITEM_CATEGORY = "extra_item_category";
    public static final String EXTRA_ITEM_LIST_JSON = "extra_item_list_json";
    public static final int REQUEST_CODE = 1;

    private List<ClothingItem> allItemsFromDB;
    private List<ClothingItem> filteredItemList;
    private TextView selectionTitle;
    private RecyclerView recyclerView;
    private ImageView backButton;
    private SupabaseService supabaseService;
    private ClothingRepository clothingRepository;

    // NOTE: This R.id.main_selection_layout is a placeholder ID and may need to be defined in a real resources file.
    private static final int R_ID_MAIN_SELECTION_LAYOUT = 1001; // Mocking R.id.main_selection_layout
    private static final int R_ID_BACK_BUTTON_SELECTION = 1002; // Mocking R.id.back_button_selection
    private static final int R_ID_SELECTION_TITLE = 1003;      // Mocking R.id.selection_title
    private static final int R_ID_RECYCLER_VIEW_ITEMS = 1004;  // Mocking R.id.recycler_view_items

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Supabase service and repository
        supabaseService = SupabaseClient.getService();
        clothingRepository = ClothingRepository.getInstance(supabaseService);

        // --- Placeholder UI Setup (mimicking activity_item_selection.xml) ---
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setId(R_ID_MAIN_SELECTION_LAYOUT);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.setPadding(32, 32, 32, 32); // Increased padding for better look

        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);

        backButton = new ImageView(this);
        backButton.setId(R_ID_BACK_BUTTON_SELECTION);
        backButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        backButton.setPadding(0, 0, 32, 0); // Spacing for the back button

        // Ensure the back button is tap-friendly
        backButton.setLayoutParams(new LinearLayout.LayoutParams(
                (int) (getResources().getDisplayMetrics().density * 48), // 48dp width
                (int) (getResources().getDisplayMetrics().density * 48)));// 48dp height

        selectionTitle = new TextView(this);
        selectionTitle.setId(R_ID_SELECTION_TITLE);
        selectionTitle.setTextSize(20);
        selectionTitle.setPadding(0, 0, 0, 32); // Padding below title

        headerLayout.addView(backButton);
        headerLayout.addView(selectionTitle, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        recyclerView = new RecyclerView(this);
        recyclerView.setId(R_ID_RECYCLER_VIEW_ITEMS);
        LinearLayout.LayoutParams recyclerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1.0f); // Fill remaining space

        rootLayout.addView(headerLayout);
        rootLayout.addView(recyclerView, recyclerParams);
        setContentView(rootLayout);
        // --- End Placeholder UI Setup ---

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Get category filter from intent
        String categoryFilter = getIntent().getStringExtra(EXTRA_ITEM_CATEGORY);

        if (categoryFilter == null) {
            Toast.makeText(this, "Error: Missing category filter.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Load clothing items from Supabase using repository
        loadClothingItemsFromSupabase(categoryFilter);

        // 5. Setup back button
        backButton.setOnClickListener(v -> finish());
    }

    /**
     * Load clothing items from Supabase using repository and filter by category
     */
    private void loadClothingItemsFromSupabase(String categoryFilter) {
        // Show loading state
        selectionTitle.setText("Loading " + categoryFilter + "...");

        new Thread(() -> {
            List<ClothingItem> items = clothingRepository.getAllClothing();

            runOnUiThread(() -> {
                if (items != null) {
                    allItemsFromDB = items;

                    // Filter the items based on category
                    filteredItemList = getFilteredItems(allItemsFromDB, categoryFilter);

                    // Update the screen title
                    selectionTitle.setText("Select " + categoryFilter + " (" + filteredItemList.size() + " items)");

                    if (filteredItemList.isEmpty()) {
                        Toast.makeText(ItemSelectionActivity.this,
                                "No " + categoryFilter + " items found in your closet.",
                                Toast.LENGTH_LONG).show();
                    }

                    // Setup RecyclerView
                    ClothingItemAdapter adapter = new ClothingItemAdapter(
                            ItemSelectionActivity.this,
                            filteredItemList,
                            ItemSelectionActivity.this
                    );
                    recyclerView.setLayoutManager(new GridLayoutManager(ItemSelectionActivity.this, 3)); // 3 items per row
                    recyclerView.setAdapter(adapter);

                    Log.d("ItemSelection", "Loaded " + filteredItemList.size() + " " + categoryFilter + " items");

                } else {
                    Toast.makeText(ItemSelectionActivity.this,
                            "Failed to load clothing items",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }).start();
    }

    /**
     * Filters the complete list of clothing items based on the requested category filter.
     * This method applies the special logic:
     * - "Tops" includes items categorized as "Tops" or "Outerwear".
     * - All other categories filter normally.
     * @param allItems The complete list of clothing items from the database.
     * @param filter The category string passed from TryOnActivity (e.g., "Tops").
     * @return A list of filtered ClothingItem objects.
     */
    private List<ClothingItem> getFilteredItems(List<ClothingItem> allItems, String filter) {
        if (allItems == null || allItems.isEmpty()) return new ArrayList<>();

        List<ClothingItem> list = new ArrayList<>();
        String filterLower = filter.toLowerCase(Locale.getDefault());

        for (ClothingItem item : allItems) {
            if (item.getCategory() == null) continue;

            // Extract the main category (e.g., "Bottom" from "Bottom>Leggings")
            String itemCategory = item.getCategory().toLowerCase(Locale.getDefault());
            String itemMainCategory;
            int separatorIndex = itemCategory.indexOf('>');
            if (separatorIndex != -1) {
                itemMainCategory = itemCategory.substring(0, separatorIndex);
            } else {
                itemMainCategory = itemCategory;
            }

            boolean matches = false;

            if (filterLower.equals("tops")) {
                // Special requirement: "Tops" filter includes "Tops" AND "Outerwear"
                if (itemMainCategory.equals("tops") || itemMainCategory.equals("outerwear")) {
                    matches = true;
                }
            } else if (filterLower.equals("bottoms")) {
                // Handle "Bottoms" UI tab mapping to "Bottom" DB category
                if (itemMainCategory.equals("bottom")) {
                    matches = true;
                }
            } else {
                // For all other categories (Footwear, Accessories, etc.)
                if (itemMainCategory.equals(filterLower)) {
                    matches = true;
                }
            }

            if (matches) {
                list.add(item);
            }
        }
        return list;
    }

    /**
     * Handles an item being clicked in the RecyclerView grid, returning it immediately.
     */
    @Override
    public void onItemClick(ClothingItem item) {
        // Log the selected item for debugging
        Log.d("ItemSelection", "Selected item: " + item.getName() +
                ", Image URL: " + item.getImagePath());

        // 1. Serialize the selected item back to JSON string
        Gson gson = new Gson();
        String selectedItemJson = gson.toJson(item);

        // 2. Send the selected item back to the calling activity (TryOnActivity)
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_SELECTED_ITEM_JSON, selectedItemJson);

        // 3. Set result and finish the activity
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any resources if needed
    }
}