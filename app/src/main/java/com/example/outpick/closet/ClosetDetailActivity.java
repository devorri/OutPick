package com.example.outpick.closet;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

import java.util.ArrayList;
import java.util.List;

public class ClosetDetailActivity extends AppCompatActivity {

    private TextView closetNameText;
    private ImageButton backButton, btnMore;
    private RecyclerView recyclerView;
    private ClosetDetailAdapter adapter;
    private LinearLayout bottomActionBar;

    private List<ClosetContentItem> closetItems = new ArrayList<>();
    private List<ClosetContentItem> selectedItems = new ArrayList<>();
    private boolean isMultiSelect = false;
    private String closetName = "My Closet";
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

        // Get closet name from intent
        if (getIntent() != null && getIntent().hasExtra("closet_name")) {
            closetName = getIntent().getStringExtra("closet_name");
        }
        closetNameText.setText(closetName);

        // RecyclerView setup
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ClosetDetailAdapter(this, closetItems);
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

    // Crucial for refreshing the list if items were added/removed elsewhere
    @Override
    protected void onResume() {
        super.onResume();
        // Reload data just in case the data changed (e.g., deleted via notification)
        loadClosetItems();
    }

    // Handles back press: exit multi-select or finish activity
    @Override
    public void onBackPressed() {
        if (isMultiSelect) {
            exitMultiSelectMode();
        } else {
            super.onBackPressed();
        }
    }

    // Handles both the main back button and the back key functionality
    private void handleBackAction() {
        if (isMultiSelect) {
            exitMultiSelectMode();
        } else {
            finish();
        }
    }

    /** Load clothing items from Supabase using repository */
    private void loadClosetItems() {
        // Show loading state
        Toast.makeText(this, "Loading closet items...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            List<ClothingItem> clothingItems = clothingRepository.getClothingByCloset(closetName);

            runOnUiThread(() -> {
                closetItems.clear();

                if (clothingItems != null && !clothingItems.isEmpty()) {
                    // Convert ClothingItem to ClosetContentItem
                    for (ClothingItem clothingItem : clothingItems) {
                        ClosetContentItem closetItem = convertToClosetContentItem(clothingItem);
                        closetItems.add(closetItem);
                    }

                    adapter.notifyDataSetChanged();
                    Toast.makeText(ClosetDetailActivity.this,
                            "Loaded " + closetItems.size() + " items from " + closetName,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ClosetDetailActivity.this,
                            "No items found in " + closetName,
                            Toast.LENGTH_SHORT).show();
                }

                // Also load snapshots if you have them
                loadSnapshotsFromSupabase();
            });
        }).start();
    }

    private ClosetContentItem convertToClosetContentItem(ClothingItem clothingItem) {
        ClosetContentItem item = new ClosetContentItem();
        item.setClothingId(clothingItem.getId());
        item.setName(clothingItem.getName());
        item.setImageUri(clothingItem.getImagePath()); // This should be the Supabase Storage URL
        item.setCategory(clothingItem.getCategory());
        item.setType(ClosetContentItem.ItemType.CLOTHING);
        return item;
    }

    private void loadSnapshotsFromSupabase() {
        // If you have snapshots/outfits in your closet, load them here
        // This would depend on your Supabase table structure for snapshots
        // For now, we'll just log that this needs implementation
        // adapter.notifyDataSetChanged();
    }

    /** Show bottom sheet menu when 3-dot button clicked */
    private void showBottomOptionsMenu() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_closet_options_menu, null);
        bottomSheetDialog.setContentView(sheetView);

        // Select Multiple Items option
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
        adapter.notifyDataSetChanged();
    }

    private void exitMultiSelectMode() {
        isMultiSelect = false;
        selectedItems.clear();
        bottomActionBar.setVisibility(View.GONE);
        btnMore.setImageResource(R.drawable.ic_more_vert);
        adapter.notifyDataSetChanged();
    }

    /** Delete selected items from Supabase */
    private void deleteSelectedItems() {
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show deletion in progress
        Toast.makeText(this, "Deleting " + selectedItems.size() + " items...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            int deletedCount = 0;

            for (ClosetContentItem item : selectedItems) {
                if (item.getType() == ClosetContentItem.ItemType.CLOTHING) {
                    // Remove clothing item from Supabase using repository
                    boolean success = clothingRepository.deleteClothing(item.getClothingId());
                    if (success) {
                        deletedCount++;
                        // Remove from local list
                        closetItems.remove(item);
                    }
                } else if (item.getType() == ClosetContentItem.ItemType.SNAPSHOT) {
                    // Remove snapshot from closet in Supabase
                    // This would need implementation based on your snapshot structure
                    // For now, just remove from local list
                    closetItems.remove(item);
                    deletedCount++;
                }
            }

            final int finalDeletedCount = deletedCount;
            runOnUiThread(() -> {
                if (finalDeletedCount > 0) {
                    Toast.makeText(ClosetDetailActivity.this,
                            finalDeletedCount + " items removed from " + closetName,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ClosetDetailActivity.this,
                            "Failed to delete items",
                            Toast.LENGTH_SHORT).show();
                }

                exitMultiSelectMode();
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    /**
     * =============================
     * Adapter
     * =============================
     */
    private class ClosetDetailAdapter extends RecyclerView.Adapter<ClosetDetailAdapter.ViewHolder> {

        private final Context context;
        private final List<ClosetContentItem> items;

        ClosetDetailAdapter(Context context, List<ClosetContentItem> items) {
            this.context = context;
            this.items = items;
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

            // Load image using Glide - assuming item.getImageUri() contains Supabase Storage URL
            if (item.getType() == ClosetContentItem.ItemType.CLOTHING) {
                holder.itemImage.setBackgroundColor(Color.TRANSPARENT);
                Glide.with(context)
                        .load(item.getImageUri())
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(holder.itemImage);

            } else if (item.getType() == ClosetContentItem.ItemType.SNAPSHOT) {
                holder.itemImage.setBackgroundColor(Color.WHITE);
                Glide.with(context)
                        .load(item.getSnapshotPath())
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
                    } else {
                        selectedItems.add(item);
                    }
                    notifyItemChanged(position);
                } else {
                    // TODO: Add single-click logic here (e.g., launch detail activity)
                    // For now, show a simple toast
                    Toast.makeText(context, "Clicked: " + item.getName(), Toast.LENGTH_SHORT).show();
                }
            });

            // Long press to enter multi-select mode
            holder.itemView.setOnLongClickListener(v -> {
                if (!isMultiSelect) {
                    enterMultiSelectMode();
                    selectedItems.add(item);
                    notifyItemChanged(position);
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