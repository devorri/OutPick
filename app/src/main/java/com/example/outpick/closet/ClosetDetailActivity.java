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
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_closet_detail);

        // Initialize Supabase service
        supabaseService = SupabaseClient.getService();

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
        adapter.notifyDataSetChanged();
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

    /** Load both clothes + snapshots from Supabase */
    private void loadClosetItems() {
        // Get clothing items for this closet
        Call<List<JsonObject>> call = supabaseService.getClothingByCloset(closetName);
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    closetItems.clear();
                    for (JsonObject jsonItem : response.body()) {
                        ClosetContentItem item = convertJsonToClosetContentItem(jsonItem, ClosetContentItem.ItemType.CLOTHING);
                        closetItems.add(item);
                    }
                    adapter.notifyDataSetChanged();

                    // Also load snapshots if you have them
                    loadSnapshotsFromSupabase();
                } else {
                    Toast.makeText(ClosetDetailActivity.this, "Failed to load closet items", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Toast.makeText(ClosetDetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSnapshotsFromSupabase() {
        // If you have snapshots/outfits in your closet, load them here
        // This would depend on your Supabase table structure for snapshots
        // For now, we'll just update the adapter
        adapter.notifyDataSetChanged();
    }

    private ClosetContentItem convertJsonToClosetContentItem(JsonObject json, ClosetContentItem.ItemType type) {
        ClosetContentItem item = new ClosetContentItem();
        if (json.has("id")) item.setClothingId(json.get("id").getAsString());
        if (json.has("name")) item.setName(json.get("name").getAsString());
        if (json.has("image_path")) item.setImageUri(json.get("image_path").getAsString());
        if (json.has("category")) item.setCategory(json.get("category").getAsString());
        item.setType(type);
        return item;
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

        for (ClosetContentItem item : selectedItems) {
            if (item.getType() == ClosetContentItem.ItemType.CLOTHING) {
                // Remove clothing item from Supabase
                deleteClothingItemFromSupabase(item.getClothingId());
            } else if (item.getType() == ClosetContentItem.ItemType.SNAPSHOT) {
                // Remove snapshot from closet in Supabase
                deleteSnapshotFromCloset(item.getSnapshotPath());
            }

            // Remove from local list
            closetItems.remove(item);
        }

        Toast.makeText(this, selectedItems.size() + " items removed from " + closetName, Toast.LENGTH_SHORT).show();
        exitMultiSelectMode();
        adapter.notifyDataSetChanged();
    }

    private void deleteClothingItemFromSupabase(String clothingId) {
        Call<Void> call = supabaseService.deleteClothing(clothingId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(ClosetDetailActivity.this, "Failed to delete clothing item", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ClosetDetailActivity.this, "Network error deleting clothing item", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteSnapshotFromCloset(String snapshotPath) {
        // This would depend on your Supabase table structure for snapshots
        // For now, we'll just show a message
        Toast.makeText(this, "Snapshot removal from Supabase needs implementation", Toast.LENGTH_SHORT).show();
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

            if (item.getType() == ClosetContentItem.ItemType.CLOTHING) {
                holder.itemImage.setBackgroundColor(Color.TRANSPARENT);
                Glide.with(context)
                        .load(item.getImageUri())
                        .placeholder(R.drawable.ic_placeholder)
                        .into(holder.itemImage);

            } else if (item.getType() == ClosetContentItem.ItemType.SNAPSHOT) {
                holder.itemImage.setBackgroundColor(Color.WHITE);
                Glide.with(context)
                        .load(item.getSnapshotPath())
                        .placeholder(R.drawable.ic_placeholder)
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
                }
                // TODO: Add single-click logic here (e.g., launch detail activity)
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