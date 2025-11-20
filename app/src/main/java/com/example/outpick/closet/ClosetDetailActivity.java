package com.example.outpick.closet;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.outpick.R;
import com.example.outpick.database.models.ClosetContentItem;
import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.database.repositories.ClothingRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class ClosetDetailActivity extends AppCompatActivity {

    private static final String TAG = "ClosetDetailActivity";
    private TextView closetNameText;
    private ImageButton backButton, btnMore;
    private RecyclerView recyclerView;
    private ClosetDetailAdapter adapter;
    private LinearLayout bottomActionBar;

    private List<ClosetContentItem> closetItems = new ArrayList<>();
    private List<ClosetContentItem> selectedItems = new ArrayList<>();
    private boolean isMultiSelect = false;
    private String closetName = "My Closet";
    private String closetId = "";
    private SupabaseService supabaseService;
    private ClothingRepository clothingRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_closet_detail);

        // Initialize Supabase service and repository
        supabaseService = SupabaseClient.getService();
        clothingRepository = ClothingRepository.getInstance(supabaseService);

        // Views initialization
        closetNameText = findViewById(R.id.closetNameText);
        backButton = findViewById(R.id.backButton);
        btnMore = findViewById(R.id.btnMore);
        recyclerView = findViewById(R.id.clothingRecyclerView);
        bottomActionBar = findViewById(R.id.bottom_action_bar);

        // Get closet name and ID from intent
        if (getIntent() != null) {
            closetName = getIntent().getStringExtra("closet_name");
            closetId = getIntent().getStringExtra("closet_id");
            Log.d(TAG, "🔄 Received - Closet Name: " + closetName + ", Closet ID: " + closetId);
        }
        closetNameText.setText(closetName);

        // RecyclerView setup
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ClosetDetailAdapter(this, closetItems, selectedItems, isMultiSelect);
        recyclerView.setAdapter(adapter);

        // Load data initially
        loadClosetItems();

        // Listeners
        backButton.setOnClickListener(v -> handleBackAction());
        btnMore.setOnClickListener(v -> {
            if (isMultiSelect) {
                exitMultiSelectMode();
            } else {
                showBottomOptionsMenu();
            }
        });

        // Bottom bar Delete button
        LinearLayout btnDelete = bottomActionBar.findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(v -> deleteSelectedItems());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadClosetItems();
    }

    @Override
    public void onBackPressed() {
        if (isMultiSelect) {
            exitMultiSelectMode();
        } else {
            super.onBackPressed();
        }
    }

    private void handleBackAction() {
        if (isMultiSelect) {
            exitMultiSelectMode();
        } else {
            finish();
        }
    }

    /** Load clothing items AND outfits from Supabase for this specific closet */
    private void loadClosetItems() {
        Toast.makeText(this, "Loading " + closetName + "...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "🔄 Starting to load closet items for: " + closetName);

        new Thread(() -> {
            try {
                closetItems.clear();

                // 1. Load clothing items from this closet
                Log.d(TAG, "📥 Step 1: Loading clothing items...");
                List<ClothingItem> clothingItems = clothingRepository.getClothingByCloset(closetName);
                Log.d(TAG, "📊 Found " + (clothingItems != null ? clothingItems.size() : 0) + " clothing items");

                if (clothingItems != null && !clothingItems.isEmpty()) {
                    for (ClothingItem clothingItem : clothingItems) {
                        ClosetContentItem closetItem = convertToClosetContentItem(clothingItem);
                        closetItems.add(closetItem);
                        Log.d(TAG, "✅ Added clothing: " + clothingItem.getName());
                    }
                }

                // 2. Load outfits/snapshots from this closet
                Log.d(TAG, "📥 Step 2: Loading outfits from closet...");
                List<ClosetContentItem> closetOutfits = getOutfitsFromCloset();
                Log.d(TAG, "📊 Found " + (closetOutfits != null ? closetOutfits.size() : 0) + " outfits");

                if (closetOutfits != null && !closetOutfits.isEmpty()) {
                    closetItems.addAll(closetOutfits);
                    Log.d(TAG, "✅ Added " + closetOutfits.size() + " outfits to closet");
                }

                runOnUiThread(() -> {
                    adapter = new ClosetDetailAdapter(this, closetItems, selectedItems, isMultiSelect);
                    recyclerView.setAdapter(adapter);
                    String message = "Loaded " + closetItems.size() + " items from " + closetName;
                    Toast.makeText(ClosetDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "✅ " + message);
                });

            } catch (Exception e) {
                Log.e(TAG, "❌ Error loading closet: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(ClosetDetailActivity.this,
                            "Error loading closet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /** Get outfits from closet_snapshots table for this specific closet */
    private List<ClosetContentItem> getOutfitsFromCloset() {
        List<ClosetContentItem> outfits = new ArrayList<>();
        try {
            // First, make sure we have the closet ID
            if (closetId == null || closetId.isEmpty()) {
                Log.d(TAG, "🔍 Closet ID not provided, searching by name: " + closetName);
                closetId = getClosetIdByName(closetName);
                Log.d(TAG, "🔍 Found closet ID: " + closetId);
            }

            if (closetId != null && !closetId.isEmpty()) {
                // ✅ FIXED: Use executeGet with proper PostgREST filter syntax
                String filterUrl = "closet_snapshots?closet_id=eq." + closetId;
                Log.d(TAG, "📡 Fetching closet snapshots with URL: " + filterUrl);

                Call<List<JsonObject>> call = supabaseService.executeGet(filterUrl);
                Response<List<JsonObject>> response = call.execute();

                Log.d(TAG, "📡 Response code: " + response.code() + ", Success: " + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "📊 Found " + response.body().size() + " closet snapshots");
                    for (JsonObject json : response.body()) {
                        Log.d(TAG, "📄 Snapshot JSON: " + json.toString());
                        ClosetContentItem outfitItem = convertClosetSnapshotToContentItem(json);
                        if (outfitItem != null) {
                            outfits.add(outfitItem);
                            Log.d(TAG, "✅ Converted snapshot to outfit: " + outfitItem.getName());
                        }
                    }
                } else {
                    Log.e(TAG, "❌ Failed to get closet snapshots: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            Log.e(TAG, "❌ Error body: " + response.errorBody().string());
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Could not read error body");
                        }
                    }
                }
            } else {
                Log.e(TAG, "❌ No closet ID found for: " + closetName);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error getting outfits from closet: " + e.getMessage(), e);
        }
        return outfits;
    }

    /** Get closet ID by name */
    private String getClosetIdByName(String closetName) {
        try {
            Log.d(TAG, "🔍 Searching for closet ID by name: " + closetName);
            Call<List<JsonObject>> call = supabaseService.getClosets();
            Response<List<JsonObject>> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                Log.d(TAG, "📊 Found " + response.body().size() + " closets total");
                for (JsonObject json : response.body()) {
                    if (json.has("name") && !json.get("name").isJsonNull()) {
                        String name = json.get("name").getAsString();
                        Log.d(TAG, "🔍 Checking closet: " + name);
                        if (closetName.equals(name)) {
                            if (json.has("id") && !json.get("id").isJsonNull()) {
                                String id = json.get("id").getAsString();
                                Log.d(TAG, "✅ Found matching closet ID: " + id);
                                return id;
                            }
                        }
                    }
                }
                Log.e(TAG, "❌ No matching closet found for name: " + closetName);
            } else {
                Log.e(TAG, "❌ Failed to get closets list");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error getting closet ID: " + e.getMessage(), e);
        }
        return null;
    }

    /** Convert closet_snapshots JSON to ClosetContentItem */
    private ClosetContentItem convertClosetSnapshotToContentItem(JsonObject json) {
        try {
            ClosetContentItem item = new ClosetContentItem();

            // Get data from closet_snapshots
            if (json.has("id") && !json.get("id").isJsonNull()) {
                item.setClothingId("snapshot_" + json.get("id").getAsString());
            }

            // ✅ FIXED: Use snapshot_path instead of image_uri
            if (json.has("snapshot_path") && !json.get("snapshot_path").isJsonNull()) {
                String imagePath = json.get("snapshot_path").getAsString();
                item.setImageUri(imagePath);
                Log.d(TAG, "🖼️ Setting outfit image from snapshot_path: " + imagePath);
            }

            // Set item details
            item.setName("Outfit from " + closetName);
            item.setCategory("Outfit");
            item.setType(ClosetContentItem.ItemType.SNAPSHOT);

            Log.d(TAG, "✅ Successfully converted snapshot to content item: " + item.getName());
            return item;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error converting snapshot to content item: " + e.getMessage(), e);
            return null;
        }
    }

    private ClosetContentItem convertToClosetContentItem(ClothingItem clothingItem) {
        ClosetContentItem item = new ClosetContentItem();
        item.setClothingId(clothingItem.getId());
        item.setName(clothingItem.getName());
        item.setImageUri(clothingItem.getImagePath());
        item.setCategory(clothingItem.getCategory());
        item.setType(ClosetContentItem.ItemType.CLOTHING);
        return item;
    }

    /** Show bottom sheet menu when 3-dot button clicked */
    private void showBottomOptionsMenu() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_closet_options_menu, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView selectMultiple = sheetView.findViewById(R.id.selectMultipleOption);
        selectMultiple.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            enterMultiSelectMode();
        });

        bottomSheetDialog.show();
    }

    private void enterMultiSelectMode() {
        isMultiSelect = true;
        selectedItems.clear();
        bottomActionBar.setVisibility(View.VISIBLE);
        btnMore.setImageResource(R.drawable.ic_close);
        // ✅ FIXED: Recreate adapter with updated multi-select state
        adapter = new ClosetDetailAdapter(this, closetItems, selectedItems, isMultiSelect);
        recyclerView.setAdapter(adapter);
        Toast.makeText(this, "Multi-select mode enabled", Toast.LENGTH_SHORT).show();
    }

    private void exitMultiSelectMode() {
        isMultiSelect = false;
        selectedItems.clear();
        bottomActionBar.setVisibility(View.GONE);
        btnMore.setImageResource(R.drawable.ic_more_vert);
        // ✅ FIXED: Recreate adapter with updated multi-select state
        adapter = new ClosetDetailAdapter(this, closetItems, selectedItems, isMultiSelect);
        recyclerView.setAdapter(adapter);
        Toast.makeText(this, "Multi-select mode disabled", Toast.LENGTH_SHORT).show();
    }

    /** ✅ FIXED: Delete selected items from Supabase */
    private void deleteSelectedItems() {
        Log.d(TAG, "🗑️ Delete clicked - Selected items count: " + selectedItems.size());

        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Items")
                .setMessage("Are you sure you want to delete " + selectedItems.size() + " items from " + closetName + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Show deletion in progress
                    Toast.makeText(this, "Deleting " + selectedItems.size() + " items...", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "🗑️ Starting deletion of " + selectedItems.size() + " items");

                    new Thread(() -> {
                        int deletedCount = 0;
                        // ✅ FIXED: Create a copy to avoid concurrent modification
                        List<ClosetContentItem> itemsToDelete = new ArrayList<>(selectedItems);

                        for (ClosetContentItem item : itemsToDelete) {
                            if (item.getType() == ClosetContentItem.ItemType.CLOTHING) {
                                Log.d(TAG, "🗑️ Deleting clothing item: " + item.getClothingId() + " - " + item.getName());
                                boolean success = clothingRepository.deleteClothing(item.getClothingId());
                                if (success) {
                                    deletedCount++;
                                    closetItems.remove(item);
                                    selectedItems.remove(item); // ✅ Remove from selected list too
                                    Log.d(TAG, "✅ Successfully deleted clothing item: " + item.getName());
                                } else {
                                    Log.e(TAG, "❌ Failed to delete clothing item: " + item.getName());
                                }
                            } else if (item.getType() == ClosetContentItem.ItemType.SNAPSHOT) {
                                Log.d(TAG, "🗑️ Deleting closet snapshot: " + item.getClothingId() + " - " + item.getName());
                                boolean success = deleteOutfitFromCloset(item.getClothingId());
                                if (success) {
                                    deletedCount++;
                                    closetItems.remove(item);
                                    selectedItems.remove(item); // ✅ Remove from selected list too
                                    Log.d(TAG, "✅ Successfully deleted closet snapshot: " + item.getName());
                                } else {
                                    Log.e(TAG, "❌ Failed to delete closet snapshot: " + item.getName());
                                }
                            }
                        }

                        final int finalDeletedCount = deletedCount;
                        runOnUiThread(() -> {
                            if (finalDeletedCount > 0) {
                                String message = finalDeletedCount + " items removed from " + closetName;
                                Toast.makeText(ClosetDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "✅ " + message);

                                // Update the adapter
                                adapter.notifyDataSetChanged();
                            } else {
                                Toast.makeText(ClosetDetailActivity.this,
                                        "Failed to delete items. Please try again.", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "❌ No items were successfully deleted");
                            }

                            exitMultiSelectMode();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Delete outfit from closet_snapshots */
    private boolean deleteOutfitFromCloset(String snapshotId) {
        try {
            String cleanId = snapshotId.replace("snapshot_", "");
            Log.d(TAG, "🗑️ Deleting closet snapshot with ID: " + cleanId);

            // ✅ FIXED: Use executeDelete with proper PostgREST URL
            String deleteUrl = "closet_snapshots?id=eq." + cleanId;
            Log.d(TAG, "🗑️ Delete URL: " + deleteUrl);

            Call<Void> call = supabaseService.executeDelete(deleteUrl);
            Response<Void> response = call.execute();

            boolean success = response.isSuccessful();
            Log.d(TAG, "🗑️ Delete response - Code: " + response.code() + ", Success: " + success);

            if (!success) {
                Log.e(TAG, "❌ Delete failed with code: " + response.code());
                if (response.errorBody() != null) {
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e(TAG, "❌ Delete error: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "❌ Could not read error body", e);
                    }
                }
            } else {
                Log.d(TAG, "✅ Successfully deleted snapshot: " + cleanId);
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception deleting outfit from closet: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * ✅ FIXED: Adapter with proper selection handling
     */
    private class ClosetDetailAdapter extends RecyclerView.Adapter<ClosetDetailAdapter.ViewHolder> {

        private final Context context;
        private final List<ClosetContentItem> items;
        private final List<ClosetContentItem> selectedItems;
        private final boolean isMultiSelect;

        ClosetDetailAdapter(Context context, List<ClosetContentItem> items,
                            List<ClosetContentItem> selectedItems, boolean isMultiSelect) {
            this.context = context;
            this.items = items;
            this.selectedItems = selectedItems;
            this.isMultiSelect = isMultiSelect;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_clothing_image, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ClosetContentItem item = items.get(position);

            // Load image using Glide
            if (item.getImageUri() != null && !item.getImageUri().isEmpty()) {
                holder.itemImage.setBackgroundColor(Color.TRANSPARENT);
                Glide.with(context)
                        .load(item.getImageUri())
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(holder.itemImage);
            }

            // Multi-select overlay logic
            if (isMultiSelect && selectedItems.contains(item)) {
                holder.selectionOverlay.setVisibility(View.VISIBLE);
                holder.checkIcon.setVisibility(View.VISIBLE);
            } else {
                holder.selectionOverlay.setVisibility(View.GONE);
                holder.checkIcon.setVisibility(View.GONE);
            }

            // Item click handler
            holder.itemView.setOnClickListener(v -> {
                if (isMultiSelect) {
                    if (selectedItems.contains(item)) {
                        selectedItems.remove(item);
                        Log.d(TAG, "🔘 Deselected: " + item.getName() + " - Selected count: " + selectedItems.size());
                    } else {
                        selectedItems.add(item);
                        Log.d(TAG, "🔘 Selected: " + item.getName() + " - Selected count: " + selectedItems.size());
                    }
                    notifyItemChanged(position);

                    // Update delete button text
                    if (selectedItems.isEmpty()) {
                        Toast.makeText(context, "No items selected", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Clicked: " + item.getName(), Toast.LENGTH_SHORT).show();
                }
            });

            // Long press to enter multi-select mode
            holder.itemView.setOnLongClickListener(v -> {
                if (!isMultiSelect) {
                    enterMultiSelectMode();
                    selectedItems.add(item);
                    notifyItemChanged(position);
                    Log.d(TAG, "🔘 Long press selected: " + item.getName());
                    return true;
                }
                return false;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView itemImage, checkIcon;
            View selectionOverlay;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                itemImage = itemView.findViewById(R.id.itemImage);
                selectionOverlay = itemView.findViewById(R.id.selectionOverlay);
                checkIcon = itemView.findViewById(R.id.checkIcon);
            }
        }
    }
}