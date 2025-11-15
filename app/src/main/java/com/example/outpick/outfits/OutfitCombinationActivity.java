package com.example.outpick.outfits;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outpick.database.models.ClosetContentItem;
import com.example.outpick.database.models.Outfit;
import com.example.outpick.R;
import com.example.outpick.dialogs.CustomizeBottomSheet;
import com.example.outpick.MainActivity;
import com.example.outpick.common.adapters.ClosetListAdapter;
import com.example.outpick.common.adapters.OutfitPathAdapter;
import com.example.outpick.database.repositories.OutfitRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutfitCombinationActivity extends AppCompatActivity
        implements CustomizeBottomSheet.OnFiltersAppliedListener {

    private static final int SNAPSHOT_DETAILS_REQUEST = 101;

    private RecyclerView recyclerView;
    private OutfitPathAdapter adapter;

    private String username = "Guest";
    private View bottomBar;
    private ImageButton btnMoreOptions;
    private View btnDeleteSelected, btnMoreSelected;

    private boolean isInMultiSelectMode = false;

    // Data lists updated to use custom item objects
    private List<ClosetContentItem> outfitItems = new ArrayList<>();
    private List<ClosetContentItem> filteredItems = new ArrayList<>();
    private SupabaseService supabaseService;
    private OutfitRepository outfitRepository;

    // Sets to store the active filters from the bottom sheet
    private Set<String> selectedCategories = new HashSet<>();
    private Set<String> selectedSeasons = new HashSet<>();
    private Set<String> selectedStyles = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outfit_combination);

        // Initialize Supabase
        supabaseService = SupabaseClient.getService();
        outfitRepository = new OutfitRepository(supabaseService);

        // Get username if passed
        String passedUsername = getIntent().getStringExtra("username");
        if (passedUsername != null && !passedUsername.trim().isEmpty()) {
            username = passedUsername;
        }

        // UI references
        ImageView backArrow = findViewById(R.id.back_arrow);
        btnMoreOptions = findViewById(R.id.btnMoreOptions);
        bottomBar = findViewById(R.id.bottomBar);
        btnDeleteSelected = bottomBar.findViewById(R.id.btn_delete);
        btnMoreSelected = bottomBar.findViewById(R.id.btn_more);

        // RecyclerView setup
        recyclerView = findViewById(R.id.outfit_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Load outfits from Supabase
        loadOutfitsFromSupabase();

        adapter = new OutfitPathAdapter(this, filteredItems);
        recyclerView.setAdapter(adapter);

        // Back navigation
        backArrow.setOnClickListener(v -> {
            Intent intent = new Intent(OutfitCombinationActivity.this, MainActivity.class);
            intent.putExtra("username", username);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Customize button
        ImageButton customizeButton = findViewById(R.id.button_customize);
        if (customizeButton != null) {
            customizeButton.setOnClickListener(v -> showCustomizeBottomSheet());
        }

        // Top-right more options button
        btnMoreOptions.setOnClickListener(v -> {
            if (isInMultiSelectMode) {
                exitMultiSelectMode();
            } else {
                showBottomOptionsDialog();
            }
        });

        // Delete selected outfits
        btnDeleteSelected.setOnClickListener(v -> {
            Set<Integer> selected = adapter.getSelectedItems();
            if (selected.isEmpty()) {
                Toast.makeText(this, "No items selected to delete", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a list of items to be deleted
            List<ClosetContentItem> toDelete = new ArrayList<>();
            for (int pos : selected) {
                if (pos >= 0 && pos < filteredItems.size()) {
                    toDelete.add(filteredItems.get(pos));
                }
            }

            // Delete from Supabase
            deleteOutfitsFromSupabase(toDelete);
        });

        // More options (when multiple items selected)
        btnMoreSelected.setOnClickListener(v -> {
            Set<Integer> selected = adapter.getSelectedItems();
            if (selected.isEmpty()) {
                Toast.makeText(this, "No items selected for this action", Toast.LENGTH_SHORT).show();
                return;
            }
            showMoreOptionsBottomSheet();
        });

        // Handle new snapshot from SpecifyDetailsActivity
        handleNewSnapshotIntent();
    }

    // Helper to load outfits from Supabase
    private void loadOutfitsFromSupabase() {
        new Thread(() -> {
            try {
                List<Outfit> outfits = outfitRepository.getAllOutfits();
                outfitItems.clear();

                for (Outfit outfit : outfits) {
                    // Convert Outfit to ClosetContentItem
                    ClosetContentItem item = new ClosetContentItem();
                    item.setSnapshotPath(outfit.getImageUri());
                    item.setName(outfit.getName());
                    item.setCategory(outfit.getCategory());
                    item.setSeason(outfit.getSeason());
                    item.setStyle(outfit.getStyle());
                    outfitItems.add(item);
                }

                runOnUiThread(() -> {
                    filteredItems.clear();
                    filteredItems.addAll(outfitItems);
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading outfits: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // Helper for new snapshot from intent
    private void handleNewSnapshotIntent() {
        byte[] byteArray = getIntent().getByteArrayExtra("snapshot");
        String outfitName = getIntent().getStringExtra("outfitName");
        String event = getIntent().getStringExtra("event");
        String season = getIntent().getStringExtra("season");
        String style = getIntent().getStringExtra("style");

        if (byteArray != null) {
            String savedPath = saveSnapshotToFile(byteArray);
            if (savedPath != null) {
                // Save to Supabase
                saveOutfitToSupabase(savedPath, outfitName, event, season, style);

                ClosetContentItem newItem = new ClosetContentItem(savedPath);
                outfitItems.add(0, newItem);
                filteredItems.add(0, newItem);

                adapter.notifyItemInserted(0);
                recyclerView.scrollToPosition(0);

                getIntent().removeExtra("snapshot");
            }
        }
    }

    private void saveOutfitToSupabase(String imagePath, String name, String event, String season, String style) {
        new Thread(() -> {
            boolean success = outfitRepository.addOutfit(
                    imagePath,
                    name != null ? name : "New Outfit",
                    "General", // Default category
                    "", // Description
                    "Unisex", // Default gender
                    event != null ? event : "Casual",
                    season != null ? season : "All-Season",
                    style != null ? style : "Casual"
            );

            runOnUiThread(() -> {
                if (!success) {
                    Toast.makeText(this, "Failed to save outfit to cloud", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private String saveSnapshotToFile(byte[] snapshotBytes) {
        try {
            File dir = new File(getFilesDir(), "outfits");
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, "outfit_" + System.currentTimeMillis() + ".png");
            Bitmap bitmap = BitmapFactory.decodeByteArray(snapshotBytes, 0, snapshotBytes.length);

            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void deleteOutfitsFromSupabase(List<ClosetContentItem> itemsToDelete) {
        new Thread(() -> {
            int deletedCount = 0;

            for (ClosetContentItem item : itemsToDelete) {
                // We need to find the outfit ID from Supabase first
                // This is a simplified approach - you might need to store outfit IDs
                boolean success = deleteOutfitByPath(item.getSnapshotPath());
                if (success) {
                    deletedCount++;
                    // Remove from local lists
                    outfitItems.remove(item);
                    filteredItems.remove(item);
                }
            }

            final int finalCount = deletedCount;
            runOnUiThread(() -> {
                exitMultiSelectMode();
                adapter.notifyDataSetChanged();
                Toast.makeText(this, finalCount + " items deleted.", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private boolean deleteOutfitByPath(String imagePath) {
        try {
            // First, find the outfit by image path
            List<Outfit> allOutfits = outfitRepository.getAllOutfits();
            for (Outfit outfit : allOutfits) {
                if (outfit.getImageUri().equals(imagePath)) {
                    // Delete from Supabase
                    return outfitRepository.deleteOutfit(outfit.getId());
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showBottomOptionsDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_options_menu, null);
        dialog.setContentView(view);
        dialog.show();

        TextView selectMultipleImages = view.findViewById(R.id.selectMultipleImages);
        selectMultipleImages.setOnClickListener(v -> {
            dialog.dismiss();
            enterMultiSelectMode();
        });
    }

    private void enterMultiSelectMode() {
        isInMultiSelectMode = true;
        adapter.enableMultiSelect();
        bottomBar.setVisibility(View.VISIBLE);
        btnMoreOptions.setImageResource(R.drawable.ic_close);
    }

    private void exitMultiSelectMode() {
        isInMultiSelectMode = false;
        adapter.disableMultiSelect();
        bottomBar.setVisibility(View.GONE);
        btnMoreOptions.setImageResource(R.drawable.ic_more_vert);
    }

    // === CustomizeBottomSheet with preloaded selections ===
    private void showCustomizeBottomSheet() {
        CustomizeBottomSheet bottomSheet = new CustomizeBottomSheet();
        bottomSheet.setSelectedCategories(new ArrayList<>(selectedCategories));
        bottomSheet.setSelectedSeasons(new ArrayList<>(selectedSeasons));
        bottomSheet.setSelectedStyles(new ArrayList<>(selectedStyles));

        bottomSheet.setOnFiltersAppliedListener(this);
        bottomSheet.show(getSupportFragmentManager(), "CustomizeBottomSheet");
    }

    private void showMoreOptionsBottomSheet() {
        BottomSheetDialog moreDialog = new BottomSheetDialog(this);
        View moreView = getLayoutInflater().inflate(R.layout.bottom_sheet_more_options, null);
        moreDialog.setContentView(moreView);
        moreDialog.show();

        TextView optionAddToCloset = moreView.findViewById(R.id.option_add_closet);

        if (optionAddToCloset != null) {
            optionAddToCloset.setOnClickListener(view -> {
                moreDialog.dismiss();
                showAddClosetBottomSheet();
            });
        }
    }

    // === Apply OR filters (categories OR seasons OR styles) ===
    @Override
    public void onFiltersApplied(List<String> categories, List<String> seasons, List<String> styles) {
        selectedCategories.clear();
        selectedSeasons.clear();
        selectedStyles.clear();

        if (categories != null) selectedCategories.addAll(categories);
        if (seasons != null) selectedSeasons.addAll(seasons);
        if (styles != null) selectedStyles.addAll(styles);

        filteredItems.clear();

        // If no filters selected across all groups, show all outfits
        if (selectedCategories.isEmpty() && selectedSeasons.isEmpty() && selectedStyles.isEmpty()) {
            filteredItems.addAll(outfitItems);
        } else {
            // Apply client-side filtering
            for (ClosetContentItem item : outfitItems) {
                boolean matches = false;

                // Check category
                if (!selectedCategories.isEmpty() && item.getCategory() != null) {
                    for (String category : selectedCategories) {
                        if (item.getCategory().toLowerCase().contains(category.toLowerCase())) {
                            matches = true;
                            break;
                        }
                    }
                }

                // Check season
                if (!matches && !selectedSeasons.isEmpty() && item.getSeason() != null) {
                    for (String season : selectedSeasons) {
                        if (item.getSeason().toLowerCase().contains(season.toLowerCase())) {
                            matches = true;
                            break;
                        }
                    }
                }

                // Check style
                if (!matches && !selectedStyles.isEmpty() && item.getStyle() != null) {
                    for (String style : selectedStyles) {
                        if (item.getStyle().toLowerCase().contains(style.toLowerCase())) {
                            matches = true;
                            break;
                        }
                    }
                }

                if (matches) {
                    filteredItems.add(item);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void showAddClosetBottomSheet() {
        BottomSheetDialog addDialog = new BottomSheetDialog(this);
        View addView = getLayoutInflater().inflate(R.layout.bottom_sheet_add_closet, null);
        addDialog.setContentView(addView);

        RecyclerView closetRecycler = addView.findViewById(R.id.bottomClosetRecyclerView);
        Button btnAddSelectedItems = addView.findViewById(R.id.btn_add_selected_items);

        // TODO: Load closets from Supabase instead of MainActivity
        ArrayList<String> closetNames = new ArrayList<>(); // Placeholder
        // closetNames = loadClosetsFromSupabase();

        ClosetListAdapter closetListAdapter = new ClosetListAdapter(closetNames);
        closetRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        closetRecycler.setAdapter(closetListAdapter);

        btnAddSelectedItems.setOnClickListener(v -> {
            String selectedCloset = closetListAdapter.getSelectedCloset();
            if (selectedCloset == null) {
                Toast.makeText(this, "Please select a closet", Toast.LENGTH_SHORT).show();
                return;
            }

            Set<Integer> selectedPositions = adapter.getSelectedItems();
            if (selectedPositions.isEmpty()) {
                Toast.makeText(this, "No snapshot selected", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: Implement Supabase closet functionality
            Toast.makeText(this, "Add to closet functionality needs Supabase implementation", Toast.LENGTH_SHORT).show();
            addDialog.dismiss();
            exitMultiSelectMode();
        });

        addDialog.show();
    }

    // === SnapshotDetailsActivity result handling ===
    public void startSnapshotDetailsForResult(Intent intent, int position) {
        startActivityForResult(intent, SNAPSHOT_DETAILS_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SNAPSHOT_DETAILS_REQUEST && resultCode == RESULT_OK && data != null) {
            boolean updated = data.getBooleanExtra("snapshotUpdated", false);
            String snapshotId = data.getStringExtra("snapshotId");

            if (updated && snapshotId != null) {
                Toast.makeText(this, "The Snapshot has been updated", Toast.LENGTH_SHORT).show();

                // Reload outfits to reflect changes
                loadOutfitsFromSupabase();
            }
        }
    }
}