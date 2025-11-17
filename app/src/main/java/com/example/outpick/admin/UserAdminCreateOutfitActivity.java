package com.example.outpick.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.outpick.R;
import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.database.models.Outfit;
import com.example.outpick.database.repositories.ClothingRepository;
import com.example.outpick.database.repositories.OutfitRepository;
import com.example.outpick.database.repositories.UserOutfitRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

public class UserAdminCreateOutfitActivity extends AppCompatActivity {

    private ImageButton btnBack, btnWorkspace, btnMenu;
    private String targetUserId;
    private String targetUsername;
    private String adminUserId;

    private ClothingRepository clothingRepository;
    private OutfitRepository outfitRepository;
    private UserOutfitRepository userOutfitRepository;

    private RecyclerView recyclerViewOutfitItems;
    private ClothingAdapter clothingAdapter;
    private List<ClothingItem> selectedClothes = new ArrayList<>();
    private boolean isMultiSelectMode = false;

    private static final String TAG = "UserAdminCreateOutfit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_admin_create_outfit);

        // ✅ GET THE TARGET USER ID AND USERNAME FROM INTENT
        Intent intent = getIntent();
        targetUserId = intent.getStringExtra("user_id");
        targetUsername = intent.getStringExtra("username");

        Log.d(TAG, "Creating outfit for - User ID: " + targetUserId + ", Username: " + targetUsername);

        if (targetUserId == null || targetUserId.isEmpty()) {
            Toast.makeText(this, "Error: No user selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize repositories
        SupabaseService supabaseService = SupabaseClient.getService();
        clothingRepository = new ClothingRepository(supabaseService);
        outfitRepository = new OutfitRepository(supabaseService);
        userOutfitRepository = new UserOutfitRepository(supabaseService, outfitRepository);

        // Get admin user ID
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        adminUserId = prefs.getString("user_id", null);

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        btnWorkspace = findViewById(R.id.btn_workspace);
        btnMenu = findViewById(R.id.btnMenu);
        recyclerViewOutfitItems = findViewById(R.id.recyclerViewOutfitItems);

        // Setup RecyclerView
        setupClothesGrid();

        // Back button
        btnBack.setOnClickListener(v -> onBackPressed());

        // Workspace button - show which user we're working with
        btnWorkspace.setOnClickListener(v ->
                Toast.makeText(this, "Creating outfit for: " + targetUsername, Toast.LENGTH_SHORT).show()
        );

        // Menu button
        btnMenu.setOnClickListener(v -> {
            if (selectedClothes.isEmpty()) {
                showSelectMultipleBottomSheet();
            } else {
                createAndAssignOutfit();
            }
        });

        // ✅ Load ONLY this user's clothes
        loadUserClothes();

        // ✅ Show message that we're creating outfit for this user
        Toast.makeText(this, "Select clothes from " + targetUsername + "'s closet", Toast.LENGTH_LONG).show();
    }

    private void setupClothesGrid() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        recyclerViewOutfitItems.setLayoutManager(layoutManager);

        // Initialize adapter with empty list first
        clothingAdapter = new ClothingAdapter(new ArrayList<>(), new ClothingAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ClothingItem item, boolean isSelected) {
                onClothingItemSelected(item, isSelected);
            }
        });
        recyclerViewOutfitItems.setAdapter(clothingAdapter);
    }

    private void loadUserClothes() {
        new Thread(() -> {
            try {
                Log.d(TAG, "🔄 Loading clothes for user: " + targetUsername + " (ID: " + targetUserId + ")");

                // ✅ Use the fixed getClothingByUserId method
                List<ClothingItem> userClothes = clothingRepository.getClothingByUserId(targetUserId);

                runOnUiThread(() -> {
                    if (userClothes != null && !userClothes.isEmpty()) {
                        clothingAdapter.updateClothes(userClothes);
                        Toast.makeText(this, "✅ Loaded " + userClothes.size() + " items from " + targetUsername, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "✅ SUCCESS: Loaded " + userClothes.size() + " items for user: " + targetUsername);
                    } else {
                        Toast.makeText(this, "❌ No clothing items found for " + targetUsername, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "❌ No clothes found for user: " + targetUsername);

                        // Debug: Check what's in the database
                        new Thread(() -> {
                            try {
                                List<ClothingItem> allClothes = clothingRepository.getAllClothing();
                                Log.d(TAG, "Total clothes in DB: " + allClothes.size());
                                for (ClothingItem item : allClothes) {
                                    Log.d(TAG, "All items - Name: " + item.getName() + " | User ID: " + item.getUserId());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error checking all clothes: " + e.getMessage());
                            }
                        }).start();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "❌ Error loading user clothes: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(this, "❌ Error loading clothes: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void showSelectMultipleBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this)
                .inflate(R.layout.bottom_closet_options_menu, null);
        bottomSheetDialog.setContentView(view);

        TextView selectMultipleOption = view.findViewById(R.id.selectMultipleOption);

        selectMultipleOption.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            enableMultiSelectionMode();
        });

        bottomSheetDialog.show();
    }

    private void enableMultiSelectionMode() {
        isMultiSelectMode = true;
        clothingAdapter.enableMultiSelection();
        Toast.makeText(this, "Multi-selection enabled. Select items for the outfit.", Toast.LENGTH_SHORT).show();
    }

    public void onClothingItemSelected(ClothingItem item, boolean isSelected) {
        if (isSelected) {
            selectedClothes.add(item);
        } else {
            selectedClothes.remove(item);
        }
        updateSelectionUI();
    }

    private void updateSelectionUI() {
        if (selectedClothes.size() > 0) {
            btnMenu.setImageResource(android.R.drawable.ic_menu_save);
            Toast.makeText(this, selectedClothes.size() + " items selected - Click menu to create outfit", Toast.LENGTH_SHORT).show();
        } else {
            btnMenu.setImageResource(R.drawable.ic_more_vert);
        }
    }

    private void createAndAssignOutfit() {
        if (selectedClothes.isEmpty()) {
            Toast.makeText(this, "Please select at least one clothing item", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Creating outfit for " + targetUsername + "...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "🔄 START: Creating outfit for user: " + targetUsername + " (ID: " + targetUserId + ")");

        new Thread(() -> {
            try {
                String outfitName = "Outfit for " + targetUsername;
                String outfitDescription = "Custom outfit created by admin with " + selectedClothes.size() + " items from " + targetUsername + "'s closet";
                String outfitImageUri = selectedClothes.get(0).getImagePath();

                Log.d(TAG, "📝 Outfit Details:");
                Log.d(TAG, "  - Name: " + outfitName);
                Log.d(TAG, "  - Description: " + outfitDescription);
                Log.d(TAG, "  - Image URI: " + outfitImageUri);
                Log.d(TAG, "  - Selected clothes count: " + selectedClothes.size());

                // STEP 1: Create outfit
                Log.d(TAG, "🎯 STEP 1: Creating outfit in database...");
                boolean outfitCreated = outfitRepository.addOutfit(
                        outfitImageUri,
                        outfitName,
                        "Custom",
                        outfitDescription,
                        "Unisex",
                        "Casual",
                        "All-Season",
                        "Mixed"
                );

                Log.d(TAG, "✅ STEP 1 RESULT: Outfit creation = " + outfitCreated);

                if (outfitCreated) {
                    // STEP 2: Get the outfit ID
                    Log.d(TAG, "🎯 STEP 2: Getting latest outfit ID...");
                    String outfitId = getLatestOutfitId();
                    Log.d(TAG, "✅ STEP 2 RESULT: Outfit ID = " + outfitId);

                    if (outfitId != null) {
                        // STEP 3: Assign to user
                        Log.d(TAG, "🎯 STEP 3: Assigning outfit to user...");
                        Log.d(TAG, "  - User ID: " + targetUserId);
                        Log.d(TAG, "  - Outfit ID: " + outfitId);
                        Log.d(TAG, "  - Admin ID: " + adminUserId);

                        boolean assigned = userOutfitRepository.assignOutfitToUser(
                                outfitId,
                                targetUserId,
                                adminUserId
                        );

                        Log.d(TAG, "✅ STEP 3 RESULT: Assignment = " + assigned);

                        runOnUiThread(() -> {
                            if (assigned) {
                                Toast.makeText(this, "🎉 Outfit created and assigned to " + targetUsername, Toast.LENGTH_LONG).show();
                                Log.d(TAG, "✅ SUCCESS: Outfit created and assigned!");
                                finish();
                            } else {
                                Toast.makeText(this, "Outfit created but failed to assign to user", Toast.LENGTH_LONG).show();
                                Log.e(TAG, "❌ FAILED: Outfit created but assignment failed");
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Failed to get outfit ID after creation", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "❌ FAILED: Could not retrieve outfit ID");
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to create outfit", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "❌ FAILED: Outfit creation failed");
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ ERROR in createAndAssignOutfit: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Error creating outfit: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String getLatestOutfitId() {
        try {
            List<Outfit> outfits = outfitRepository.getAllOutfits();
            Log.d(TAG, "📊 Total outfits in database: " + (outfits != null ? outfits.size() : "null"));

            if (outfits != null && !outfits.isEmpty()) {
                String latestId = outfits.get(outfits.size() - 1).getId();
                Log.d(TAG, "🔍 Latest outfit ID found: " + latestId);
                return latestId;
            } else {
                Log.e(TAG, "❌ No outfits found in database");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error getting latest outfit ID: " + e.getMessage(), e);
            return null;
        }
    }

    // Clothing Adapter Class
    public static class ClothingAdapter extends RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder> {
        private List<ClothingItem> clothes;
        private OnItemClickListener listener;
        private boolean isMultiSelectMode = false;
        private List<ClothingItem> selectedItems = new ArrayList<>();

        public interface OnItemClickListener {
            void onItemClick(ClothingItem item, boolean isSelected);
        }

        public ClothingAdapter(List<ClothingItem> clothes, OnItemClickListener listener) {
            this.clothes = clothes;
            this.listener = listener;
        }

        public void updateClothes(List<ClothingItem> newClothes) {
            this.clothes = newClothes;
            notifyDataSetChanged();
        }

        public void enableMultiSelection() {
            isMultiSelectMode = true;
            selectedItems.clear();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ClothingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_clothing, parent, false);
            return new ClothingViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ClothingViewHolder holder, int position) {
            ClothingItem item = clothes.get(position);
            holder.bind(item, isMultiSelectMode, selectedItems.contains(item));
        }

        @Override
        public int getItemCount() {
            return clothes.size();
        }

        class ClothingViewHolder extends RecyclerView.ViewHolder {
            private ImageView imageView;
            private TextView textView;
            private View selectionOverlay;

            public ClothingViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.clothingImage);
                textView = itemView.findViewById(R.id.clothingName);
                selectionOverlay = itemView.findViewById(R.id.selectionOverlay);

                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        ClothingItem item = clothes.get(position);
                        if (isMultiSelectMode) {
                            boolean isSelected = !selectedItems.contains(item);
                            if (isSelected) {
                                selectedItems.add(item);
                            } else {
                                selectedItems.remove(item);
                            }
                            notifyItemChanged(position);
                            listener.onItemClick(item, isSelected);
                        }
                    }
                });
            }

            public void bind(ClothingItem item, boolean multiSelectMode, boolean isSelected) {
                if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(item.getImagePath())
                            .placeholder(R.drawable.ic_gallery)
                            .into(imageView);
                }

                textView.setText(item.getName());

                if (multiSelectMode) {
                    selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
                } else {
                    selectionOverlay.setVisibility(View.GONE);
                }
            }
        }
    }
}