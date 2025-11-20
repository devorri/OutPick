package com.example.outpick.outfits;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import com.example.outpick.database.models.ClosetItem;
import com.example.outpick.database.models.Outfit;
import com.example.outpick.R;
import com.example.outpick.dialogs.CustomizeBottomSheet;
import com.example.outpick.MainActivity;
import com.example.outpick.common.adapters.ClosetListAdapter;
import com.example.outpick.common.adapters.OutfitPathAdapter;
import com.example.outpick.database.repositories.ClosetSnapshotRepository;
import com.example.outpick.database.repositories.OutfitRepository;
import com.example.outpick.database.repositories.UserOutfitRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.utils.ImageUploader;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Response;

public class OutfitCombinationActivity extends AppCompatActivity
        implements CustomizeBottomSheet.OnFiltersAppliedListener {

    private static final String TAG = "OutfitCombinationActivity";
    private static final int SNAPSHOT_DETAILS_REQUEST = 101;

    private RecyclerView recyclerView;
    private OutfitPathAdapter adapter;

    private String username = "Guest";
    private View bottomBar;
    private ImageButton btnMoreOptions;
    private View btnDeleteSelected, btnMoreSelected;

    private boolean isInMultiSelectMode = false;

    private List<ClosetContentItem> outfitItems = new ArrayList<>();
    private List<ClosetContentItem> filteredItems = new ArrayList<>();
    private SupabaseService supabaseService;
    private OutfitRepository outfitRepository;
    private UserOutfitRepository userOutfitRepository;
    private ClosetSnapshotRepository closetSnapshotRepository;
    private ImageUploader imageUploader;
    private String currentUserId;

    private Set<String> selectedCategories = new HashSet<>();
    private Set<String> selectedSeasons = new HashSet<>();
    private Set<String> selectedStyles = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outfit_combination);

        // Initialize Supabase and repositories
        supabaseService = SupabaseClient.getService();
        outfitRepository = new OutfitRepository(supabaseService);
        userOutfitRepository = new UserOutfitRepository(supabaseService, outfitRepository);
        closetSnapshotRepository = new ClosetSnapshotRepository(supabaseService);
        imageUploader = new ImageUploader(this);

        // ✅ GET CURRENT USER ID
        currentUserId = getCurrentUserId();

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

        // ✅ Initialize adapter BEFORE loading data
        adapter = new OutfitPathAdapter(this, filteredItems);
        recyclerView.setAdapter(adapter);

        // Load outfits for CURRENT USER only
        loadOutfitsForCurrentUser();

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

            List<ClosetContentItem> toDelete = new ArrayList<>();
            for (int pos : selected) {
                if (pos >= 0 && pos < filteredItems.size()) {
                    toDelete.add(filteredItems.get(pos));
                }
            }

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

    // ✅ GET CURRENT USER ID
    private String getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        if (userId == null) {
            Log.e(TAG, "No user ID found in SharedPreferences!");
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
        return userId;
    }

    // ✅ UPDATED: LOAD OUTFITS FOR CURRENT USER
    private void loadOutfitsForCurrentUser() {
        if (currentUserId == null) {
            Toast.makeText(this, "Please log in to view your outfits", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                List<Outfit> outfits = userOutfitRepository.getOutfitsForUser(currentUserId);

                Log.d(TAG, "Loaded " + outfits.size() + " outfits for user: " + currentUserId);

                // ✅ CONVERT Outfit objects to ClosetContentItem objects
                outfitItems.clear();
                for (Outfit outfit : outfits) {
                    // ✅ ADDED: Only show user's own outfits (not suggestions)
                    if (!outfit.isSuggestion()) {
                        ClosetContentItem item = new ClosetContentItem();
                        item.setType(ClosetContentItem.ItemType.SNAPSHOT);
                        item.setSnapshotPath(outfit.getImageUri());
                        item.setName(outfit.getName());
                        item.setCategory(outfit.getCategory());
                        item.setSeason(outfit.getSeason());
                        item.setStyle(outfit.getStyle());
                        outfitItems.add(item);

                        Log.d(TAG, "Converted user outfit: " + outfit.getName() + " - " + outfit.getImageUri());
                    }
                }

                runOnUiThread(() -> {
                    filteredItems.clear();
                    filteredItems.addAll(outfitItems);
                    adapter.notifyDataSetChanged();

                    if (outfitItems.isEmpty()) {
                        Toast.makeText(this, "No outfits found. Create some outfits first!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Loaded " + outfitItems.size() + " of your outfits", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading user outfits: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading outfits: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // ✅ LOAD USER CLOSETS FROM SUPABASE
    private List<ClosetItem> loadUserClosetsFromSupabase() {
        List<ClosetItem> userClosets = new ArrayList<>();

        try {
            Call<List<JsonObject>> call = supabaseService.getClosets();
            Response<List<JsonObject>> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                for (JsonObject jsonObject : response.body()) {
                    // Filter by current user
                    String userId = jsonObject.has("user_id") && !jsonObject.get("user_id").isJsonNull()
                            ? jsonObject.get("user_id").getAsString() : "";

                    if (currentUserId.equals(userId)) {
                        ClosetItem closet = new ClosetItem(
                                jsonObject.get("id").getAsString(),
                                jsonObject.get("name").getAsString(),
                                "",
                                jsonObject.get("image_uri").getAsString(),
                                ""
                        );
                        userClosets.add(closet);
                        Log.d(TAG, "Found user closet: " + closet.getName() + " - ID: " + closet.getId());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading user closets: " + e.getMessage());
        }

        Log.d(TAG, "Total user closets found: " + userClosets.size());
        return userClosets;
    }

    // ✅ ADD OUTFITS TO CLOSET
    private void addOutfitsToCloset(Set<Integer> selectedPositions, String closetId, String closetName) {
        new Thread(() -> {
            int addedCount = 0;

            for (int position : selectedPositions) {
                if (position >= 0 && position < filteredItems.size()) {
                    ClosetContentItem outfit = filteredItems.get(position);
                    String snapshotPath = outfit.getSnapshotPath(); // This is the outfit image URL

                    boolean success = closetSnapshotRepository.addOutfitToCloset(closetId, snapshotPath);
                    if (success) {
                        addedCount++;
                        Log.d(TAG, "✅ Added outfit to closet: " + outfit.getName() + " -> " + closetName);
                    } else {
                        Log.e(TAG, "❌ Failed to add outfit to closet: " + outfit.getName());
                    }
                }
            }

            final int finalCount = addedCount;
            runOnUiThread(() -> {
                exitMultiSelectMode();
                if (finalCount > 0) {
                    Toast.makeText(this,
                            "✅ Added " + finalCount + " outfit(s) to " + closetName,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this,
                            "❌ Failed to add outfits to " + closetName,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // ✅ UPDATED: SHOW ADD CLOSET BOTTOM SHEET
    private void showAddClosetBottomSheet() {
        BottomSheetDialog addDialog = new BottomSheetDialog(this);
        View addView = getLayoutInflater().inflate(R.layout.bottom_sheet_add_closet, null);
        addDialog.setContentView(addView);

        RecyclerView closetRecycler = addView.findViewById(R.id.bottomClosetRecyclerView);
        Button btnAddSelectedItems = addView.findViewById(R.id.btn_add_selected_items);

        // ✅ LOAD ACTUAL USER CLOSETS FROM SUPABASE
        List<ClosetItem> userClosets = loadUserClosetsFromSupabase();
        List<String> closetNames = new ArrayList<>();
        List<String> closetIds = new ArrayList<>();

        for (ClosetItem closet : userClosets) {
            closetNames.add(closet.getName());
            closetIds.add(closet.getId());
        }

        if (closetNames.isEmpty()) {
            Toast.makeText(this, "No closets found. Please create a closet first.", Toast.LENGTH_SHORT).show();
            addDialog.dismiss();
            return;
        }

        ClosetListAdapter closetListAdapter = new ClosetListAdapter(closetNames);
        closetRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        closetRecycler.setAdapter(closetListAdapter);

        btnAddSelectedItems.setOnClickListener(v -> {
            int selectedPosition = closetListAdapter.getSelectedPosition();
            if (selectedPosition == -1) {
                Toast.makeText(this, "Please select a closet", Toast.LENGTH_SHORT).show();
                return;
            }

            Set<Integer> selectedOutfitPositions = adapter.getSelectedItems();
            if (selectedOutfitPositions.isEmpty()) {
                Toast.makeText(this, "No outfits selected", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedClosetId = closetIds.get(selectedPosition);
            String selectedClosetName = closetNames.get(selectedPosition);

            // ✅ ADD SELECTED OUTFITS TO CLOSET
            addOutfitsToCloset(selectedOutfitPositions, selectedClosetId, selectedClosetName);

            addDialog.dismiss();
        });

        addDialog.show();
    }

    // ✅ UPDATED: Handle new outfit from SpecifyDetailsActivity
    private void handleNewSnapshotIntent() {
        String cloudImageUrl = getIntent().getStringExtra("cloud_image_url");
        String outfitName = getIntent().getStringExtra("outfitName");
        String event = getIntent().getStringExtra("event");
        String season = getIntent().getStringExtra("season");
        String style = getIntent().getStringExtra("style");

        if (cloudImageUrl != null) {
            // ✅ Create ClosetContentItem instead of Outfit
            ClosetContentItem newItem = new ClosetContentItem();
            newItem.setType(ClosetContentItem.ItemType.SNAPSHOT);
            newItem.setSnapshotPath(cloudImageUrl);
            newItem.setName(outfitName != null ? outfitName : "New Outfit");
            newItem.setCategory("General");
            newItem.setSeason(season != null ? season : "All-Season");
            newItem.setStyle(style != null ? style : "Casual");

            // Add to lists
            outfitItems.add(0, newItem);
            filteredItems.add(0, newItem);
            adapter.notifyItemInserted(0);
            recyclerView.scrollToPosition(0);

            getIntent().removeExtra("cloud_image_url");
            Toast.makeText(this, "New outfit added!", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadAndSaveOutfit(Uri localImageUri, String name, String event, String season, String style) {
        Toast.makeText(this, "Uploading outfit to cloud...", Toast.LENGTH_SHORT).show();

        String fileName = "outfit_" + System.currentTimeMillis() + ".jpg";

        imageUploader.uploadImage(localImageUri, "outfits", fileName, new ImageUploader.UploadCallback() {
            @Override
            public void onSuccess(String cloudImageUrl) {
                saveOutfitToSupabase(cloudImageUrl, name, event, season, style);

                runOnUiThread(() -> {
                    ClosetContentItem newItem = new ClosetContentItem();
                    newItem.setType(ClosetContentItem.ItemType.SNAPSHOT);
                    newItem.setSnapshotPath(cloudImageUrl);
                    newItem.setName(name != null ? name : "New Outfit");
                    newItem.setCategory("General");
                    newItem.setSeason(season != null ? season : "All-Season");
                    newItem.setStyle(style != null ? style : "Casual");

                    outfitItems.add(0, newItem);
                    filteredItems.add(0, newItem);
                    adapter.notifyItemInserted(0);
                    recyclerView.scrollToPosition(0);

                    Toast.makeText(OutfitCombinationActivity.this, "Outfit saved to cloud!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(OutfitCombinationActivity.this,
                            "Failed to upload outfit: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void saveOutfitToSupabase(String imageUrl, String name, String event, String season, String style) {
        new Thread(() -> {
            boolean outfitAdded = outfitRepository.addOutfit(
                    imageUrl,
                    name != null ? name : "New Outfit",
                    "General",
                    "",
                    "Unisex",
                    event != null ? event : "Casual",
                    season != null ? season : "All-Season",
                    style != null ? style : "Casual"
            );

            if (outfitAdded && currentUserId != null) {
                List<Outfit> allOutfits = outfitRepository.getAllOutfits();
                String newOutfitId = null;

                for (Outfit outfit : allOutfits) {
                    if (outfit.getImageUri().equals(imageUrl)) {
                        newOutfitId = outfit.getId();
                        break;
                    }
                }

                if (newOutfitId != null) {
                    boolean assigned = userOutfitRepository.assignOutfitToUser(
                            newOutfitId,
                            currentUserId,
                            "self"
                    );

                    if (assigned) {
                        Log.d(TAG, "Outfit successfully assigned to user: " + newOutfitId);
                    } else {
                        Log.e(TAG, "Failed to assign outfit to user");
                    }
                } else {
                    Log.e(TAG, "Could not find newly created outfit ID");
                }
            }

            runOnUiThread(() -> {
                if (!outfitAdded) {
                    Toast.makeText(this, "Failed to save outfit to database", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Outfit saved successfully!", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void deleteOutfitsFromSupabase(List<ClosetContentItem> itemsToDelete) {
        new Thread(() -> {
            int deletedCount = 0;

            for (ClosetContentItem item : itemsToDelete) {
                boolean success = deleteOutfitByImageUrl(item.getSnapshotPath());
                if (success) {
                    deletedCount++;
                    outfitItems.remove(item);
                    filteredItems.remove(item);
                }
            }

            final int finalCount = deletedCount;
            runOnUiThread(() -> {
                exitMultiSelectMode();
                adapter.notifyDataSetChanged();
                Toast.makeText(this, finalCount + " outfit(s) deleted.", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private boolean deleteOutfitByImageUrl(String imageUrl) {
        try {
            List<Outfit> allOutfits = outfitRepository.getAllOutfits();
            for (Outfit outfit : allOutfits) {
                if (outfit.getImageUri().equals(imageUrl)) {
                    if (currentUserId != null) {
                        userOutfitRepository.removeOutfitFromUser(outfit.getId(), currentUserId);
                    }
                    return outfitRepository.deleteOutfit(outfit.getId());
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting outfit: " + e.getMessage());
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
                showAddClosetBottomSheet(); // ✅ UPDATED: Now shows actual closets
            });
        }
    }

    @Override
    public void onFiltersApplied(List<String> categories, List<String> seasons, List<String> styles) {
        selectedCategories.clear();
        selectedSeasons.clear();
        selectedStyles.clear();

        if (categories != null) selectedCategories.addAll(categories);
        if (seasons != null) selectedSeasons.addAll(seasons);
        if (styles != null) selectedStyles.addAll(styles);

        filteredItems.clear();

        if (selectedCategories.isEmpty() && selectedSeasons.isEmpty() && selectedStyles.isEmpty()) {
            filteredItems.addAll(outfitItems);
        } else {
            for (ClosetContentItem item : outfitItems) {
                boolean matches = false;

                if (!selectedCategories.isEmpty() && item.getCategory() != null) {
                    for (String category : selectedCategories) {
                        if (item.getCategory().toLowerCase().contains(category.toLowerCase())) {
                            matches = true;
                            break;
                        }
                    }
                }

                if (!matches && !selectedSeasons.isEmpty() && item.getSeason() != null) {
                    for (String season : selectedSeasons) {
                        if (item.getSeason().toLowerCase().contains(season.toLowerCase())) {
                            matches = true;
                            break;
                        }
                    }
                }

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

    public void startSnapshotDetailsForResult(Intent intent, int position) {
        startActivityForResult(intent, SNAPSHOT_DETAILS_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SNAPSHOT_DETAILS_REQUEST && resultCode == RESULT_OK && data != null) {
            boolean updated = data.getBooleanExtra("snapshotUpdated", false);
            if (updated) {
                Toast.makeText(this, "Outfit updated successfully", Toast.LENGTH_SHORT).show();
                loadOutfitsForCurrentUser();
            }
        }
    }
}