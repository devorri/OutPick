package com.example.outpick.closet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outpick.MainActivity;
import com.example.outpick.R;
import com.example.outpick.common.BaseDrawerActivity;
import com.example.outpick.common.PreviewImageActivity;
import com.example.outpick.common.adapters.ClothingAdapter;
import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.dialogs.FilterBottomSheetDialog;
import com.example.outpick.dialogs.SortBottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class YourClothesActivity extends BaseDrawerActivity implements FilterBottomSheetDialog.FilterListener {

    private static final int PICK_IMAGE_REQUEST = 101;
    private static final String PREFS_NAME = "sort_prefs";
    private static final String KEY_LAST_SORT = "last_sort";

    private ImageView backButton;
    private ImageButton moreOptionsButton;
    private ImageView filterButton;
    private RecyclerView recyclerView;
    private ClothingAdapter adapter;
    private ArrayList<ClothingItem> clothingItems = new ArrayList<>();
    private ArrayList<ClothingItem> allClothingItems = new ArrayList<>();
    private SupabaseService supabaseService;
    private LinearLayout bottomBar;
    private LinearLayout emptyStateLayout;
    private LinearLayout btnDelete;
    private TextView recentlyAddedBtn;
    private Button addItemButtonTop;
    private Button addItemButtonEmptyState;

    private boolean isSelectionMode = false;
    private String lastSelectedSort = "Recently added";
    private SharedPreferences sharedPreferences;

    // ✅ Track active filters globally
    private Set<String> activeSeasons = new HashSet<>();
    private Set<String> activeOccasions = new HashSet<>();

    // Category Tabs
    private LinearLayout tabAll, tabTops, tabBottoms, tabOuterwear, tabAccessories, tabFootwear;
    private TextView tabTextAll, tabTextTops, tabTextBottoms, tabTextOuterwear, tabTextAccessories, tabTextFootwear;
    private View underlineAll, underlineTops, underlineBottoms, underlineOuterwear, underlineAccessories, underlineFootwear;
    private String currentCategoryFilter = "All";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_your_clothes);

        // Initialize Supabase service
        supabaseService = SupabaseClient.getService();

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        lastSelectedSort = sharedPreferences.getString(KEY_LAST_SORT, "Recently added");

        setupDrawer(R.id.drawer_layout, R.id.nav_view);
        initViews();
        setupRecyclerView();
        setClickListeners();

        loadClothesFromSupabase();
        updateSortButtonText();
        applySorting();
        filterAndDisplay();
        updateCategoryTabVisibility();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Reload the database to reflect any updated item categories
        loadClothesFromSupabase();
        lastSelectedSort = sharedPreferences.getString(KEY_LAST_SORT, "Recently added");
        updateSortButtonText();
        applySorting();
        filterAndDisplay();
        updateCategoryTabVisibility(); // ✅ Ensure tabs refresh after category change
    }

    private void initViews() {
        backButton = findViewById(R.id.back_button);
        moreOptionsButton = findViewById(R.id.moreOptionsButton);
        filterButton = findViewById(R.id.buttonFilter);
        recyclerView = findViewById(R.id.recyclerView);
        bottomBar = findViewById(R.id.bottomActionBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        btnDelete = findViewById(R.id.btn_delete);
        recentlyAddedBtn = findViewById(R.id.recentlyAddedButton);
        addItemButtonTop = findViewById(R.id.addItemButtonTop);
        addItemButtonEmptyState = findViewById(R.id.addItemButtonEmptyState);

        tabAll = findViewById(R.id.tab_all);
        tabTops = findViewById(R.id.tab_tops);
        tabBottoms = findViewById(R.id.tab_bottoms);
        tabOuterwear = findViewById(R.id.tab_outerwear);
        tabAccessories = findViewById(R.id.tab_accessories);
        tabFootwear = findViewById(R.id.tab_footwear);

        underlineAll = findViewById(R.id.underline_all);
        underlineTops = findViewById(R.id.underline_tops);
        underlineBottoms = findViewById(R.id.underline_bottoms);
        underlineOuterwear = findViewById(R.id.underline_outerwear);
        underlineAccessories = findViewById(R.id.underline_accessories);
        underlineFootwear = findViewById(R.id.underline_footwear);

        tabTextAll = findViewById(R.id.tabText_all);
        tabTextTops = findViewById(R.id.tabText_tops);
        tabTextBottoms = findViewById(R.id.tabText_bottoms);
        tabTextOuterwear = findViewById(R.id.tabText_outerwear);
        tabTextAccessories = findViewById(R.id.tabText_accessories);
        tabTextFootwear = findViewById(R.id.tabText_footwear);
    }

    private void setupRecyclerView() {
        adapter = new ClothingAdapter(this, clothingItems);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new ClothingAdapter.OnItemClickListener() {
            @Override
            public void onClothingClick(ClothingItem item) {
                Intent intent = new Intent(YourClothesActivity.this, YourClothesDetailsActivity.class);
                intent.putExtra("item", item);
                startActivity(intent);
            }

            @Override
            public void onAddItemClick() {
                openImagePicker();
            }
        });
    }

    private void setClickListeners() {
        backButton.setOnClickListener(v -> navigateToMain());
        moreOptionsButton.setOnClickListener(v -> {
            if (!isSelectionMode) showMoreOptionsPopup();
            else exitSelectionMode();
        });
        addItemButtonTop.setOnClickListener(v -> openImagePicker());
        addItemButtonEmptyState.setOnClickListener(v -> openImagePicker());
        btnDelete.setOnClickListener(v -> {
            if (isSelectionMode) deleteSelectedItems();
        });
        filterButton.setOnClickListener(v -> {
            FilterBottomSheetDialog dialog = new FilterBottomSheetDialog();
            dialog.setFilterListener(this);
            dialog.show(getSupportFragmentManager(), "FilterBottomSheet");
        });
        recentlyAddedBtn.setOnClickListener(v -> {
            SortBottomSheetDialog sortDialog = new SortBottomSheetDialog();
            sortDialog.setCurrentSelection(lastSelectedSort);
            sortDialog.setOnSortAppliedListener(selectedOption -> {
                lastSelectedSort = selectedOption;
                sharedPreferences.edit().putString(KEY_LAST_SORT, lastSelectedSort).apply();
                updateSortButtonText();
                applySorting();
                filterAndDisplay();
            });
            sortDialog.show(getSupportFragmentManager(), "SortBottomSheet");
        });

        // ✅ Tab click logic (kept same but ensured live refresh)
        tabAll.setOnClickListener(v -> {
            currentCategoryFilter = "All";
            filterAndDisplay();
        });
        tabTops.setOnClickListener(v -> {
            currentCategoryFilter = "Tops";
            filterAndDisplay();
        });
        tabBottoms.setOnClickListener(v -> {
            currentCategoryFilter = "Bottoms";
            filterAndDisplay();
        });
        tabOuterwear.setOnClickListener(v -> {
            currentCategoryFilter = "Outerwear";
            filterAndDisplay();
        });
        tabAccessories.setOnClickListener(v -> {
            currentCategoryFilter = "Accessories";
            filterAndDisplay();
        });
        tabFootwear.setOnClickListener(v -> {
            currentCategoryFilter = "Footwear";
            filterAndDisplay();
        });
    }

    private void updateSortButtonText() {
        recentlyAddedBtn.setText(lastSelectedSort);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Clothing Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            File imageFile = copyImageToInternalStorage(selectedImageUri);
            if (imageFile != null) {
                Intent intent = new Intent(this, PreviewImageActivity.class);
                intent.putExtra("image_path", imageFile.getAbsolutePath());
                startActivity(intent);
            } else {
                showToast("Failed to save image.");
            }
        }
    }

    private File copyImageToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            File directory = new File(getFilesDir(), "images");
            if (!directory.exists()) directory.mkdirs();
            String fileName = "img_" + System.currentTimeMillis() + ".jpg";
            File imageFile = new File(directory, fileName);
            try (OutputStream outputStream = new FileOutputStream(imageFile)) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = inputStream.read(buffer)) > 0) outputStream.write(buffer, 0, length);
            }
            return imageFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadClothesFromSupabase() {
        Call<List<JsonObject>> call = supabaseService.getClothing();
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allClothingItems.clear();
                    for (JsonObject jsonItem : response.body()) {
                        ClothingItem item = convertJsonToClothingItem(jsonItem);
                        allClothingItems.add(item);
                    }
                    clothingItems.clear();
                    clothingItems.addAll(allClothingItems);
                    updateCategoryTabVisibility();
                    filterAndDisplay();
                } else {
                    showToast("Failed to load clothing items");
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                showToast("Network error: " + t.getMessage());
            }
        });
    }

    private ClothingItem convertJsonToClothingItem(JsonObject json) {
        ClothingItem item = new ClothingItem();
        if (json.has("id")) item.setId(json.get("id").getAsString());
        if (json.has("name")) item.setName(json.get("name").getAsString());
        if (json.has("category")) item.setCategory(json.get("category").getAsString());
        if (json.has("image_path")) item.setImagePath(json.get("image_path").getAsString());
        if (json.has("season")) item.setSeason(json.get("season").getAsString());
        if (json.has("occasion")) item.setOccasion(json.get("occasion").getAsString());
        if (json.has("closet_name")) item.setClosetName(json.get("closet_name").getAsString());
        if (json.has("is_favorite")) item.setFavorite(json.get("is_favorite").getAsBoolean());
        return item;
    }

    // ✅ FIXED: More robust tab visibility detection for category changes
    private void updateCategoryTabVisibility() {
        tabTops.setVisibility(View.GONE);
        tabBottoms.setVisibility(View.GONE);
        tabOuterwear.setVisibility(View.GONE);
        tabAccessories.setVisibility(View.GONE);
        tabFootwear.setVisibility(View.GONE);

        for (ClothingItem item : allClothingItems) {
            if (item.getCategory() == null) continue;
            String category = item.getCategory().toLowerCase(Locale.getDefault()).trim();

            if (category.contains("top")) tabTops.setVisibility(View.VISIBLE);
            else if (category.contains("bottom")) tabBottoms.setVisibility(View.VISIBLE);
            else if (category.contains("outerwear")) tabOuterwear.setVisibility(View.VISIBLE);
            else if (category.contains("accessor")) tabAccessories.setVisibility(View.VISIBLE);
            else if (category.contains("footwear") || category.contains("shoes")) tabFootwear.setVisibility(View.VISIBLE);
        }
    }

    // ✅ CENTRALIZED FILTERING LOGIC
    private void filterAndDisplay() {
        List<ClothingItem> filtered = new ArrayList<>();

        for (ClothingItem item : allClothingItems) {
            if (!isCategoryMatch(item.getCategory(), currentCategoryFilter)) continue;

            boolean seasonMatch = activeSeasons.isEmpty();
            boolean occasionMatch = activeOccasions.isEmpty();

            String[] seasonParts = (item.getSeason() != null ? item.getSeason() : "").split("[,;/]");
            String[] occasionParts = (item.getOccasion() != null ? item.getOccasion() : "").split("[,;/]");

            for (String part : seasonParts) {
                for (String sel : activeSeasons) {
                    if (part.trim().equalsIgnoreCase(sel.trim())) {
                        seasonMatch = true;
                        break;
                    }
                }
            }

            for (String part : occasionParts) {
                for (String sel : activeOccasions) {
                    if (part.trim().equalsIgnoreCase(sel.trim())) {
                        occasionMatch = true;
                        break;
                    }
                }
            }

            if (seasonMatch && occasionMatch) filtered.add(item);
        }

        clothingItems.clear();
        clothingItems.addAll(filtered);
        applySorting();
        updateCategoryTabUI(currentCategoryFilter);
        updateUI(clothingItems);
    }

    private boolean isCategoryMatch(String itemCategory, String tabCategory) {
        if (itemCategory == null) return false;
        if ("All".equalsIgnoreCase(tabCategory)) return true;

        String itemMainCategory;
        int separatorIndex = itemCategory.indexOf('>');
        if (separatorIndex != -1) {
            itemMainCategory = itemCategory.substring(0, separatorIndex).trim();
        } else {
            itemMainCategory = itemCategory.trim();
        }

        if ("Bottoms".equalsIgnoreCase(tabCategory)) {
            return itemMainCategory.equalsIgnoreCase("Bottom") || itemMainCategory.equalsIgnoreCase("Bottoms");
        }

        return tabCategory.equalsIgnoreCase(itemMainCategory);
    }

    private void updateCategoryTabUI(String activeCategory) {
        TextView[] texts = {tabTextAll, tabTextTops, tabTextBottoms, tabTextOuterwear, tabTextAccessories, tabTextFootwear};
        View[] underlines = {underlineAll, underlineTops, underlineBottoms, underlineOuterwear, underlineAccessories, underlineFootwear};
        String[] names = {"All", "Tops", "Bottoms", "Outerwear", "Accessories", "Footwear"};
        int active = 0xFF000000, inactive = 0xFF757575;
        for (int i = 0; i < names.length; i++) {
            boolean isActive = names[i].equalsIgnoreCase(activeCategory);
            texts[i].setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
            texts[i].setTextColor(isActive ? active : inactive);
            underlines[i].setVisibility(isActive ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void updateUI(ArrayList<ClothingItem> items) {
        boolean isEmpty = items == null || items.isEmpty();
        boolean isClosetEmpty = allClothingItems.isEmpty();
        recyclerView.setVisibility(isEmpty && !currentCategoryFilter.equals("All") ? View.GONE : View.VISIBLE);
        emptyStateLayout.setVisibility(isClosetEmpty ? View.VISIBLE : View.GONE);
        adapter.setShowAddTile(!isClosetEmpty);
        adapter.notifyDataSetChanged();
    }

    private void applySorting() {
        if (lastSelectedSort.equalsIgnoreCase("Recently added")) {
            // For String IDs, we can't sort by ID like before. We'll use insertion order.
            // In a real app, you might want to add a created_at timestamp to your Supabase table
            Collections.sort(clothingItems, (a, b) -> b.getId().compareTo(a.getId()));
        }
        adapter.setItems(clothingItems);
    }

    private void navigateToMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void showMoreOptionsPopup() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = LayoutInflater.from(this).inflate(R.layout.popup_more_options, null);
        TextView selectMultiple = v.findViewById(R.id.multipleSelectionOption);
        selectMultiple.setOnClickListener(x -> {
            isSelectionMode = true;
            adapter.setShowCheckboxes(true);
            bottomBar.setVisibility(View.VISIBLE);
            moreOptionsButton.setImageResource(R.drawable.ic_close);
            dialog.dismiss();
        });
        dialog.setContentView(v);
        dialog.show();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        adapter.setShowCheckboxes(false);
        bottomBar.setVisibility(View.GONE);
        moreOptionsButton.setImageResource(R.drawable.ic_more_vert);
        adapter.clearAllSelections();
    }

    private void deleteSelectedItems() {
        ArrayList<ClothingItem> selected = adapter.getSelectedItems();
        if (selected.isEmpty()) {
            showToast("No items selected");
            return;
        }

        // Delete from Supabase
        for (ClothingItem item : selected) {
            Call<Void> call = supabaseService.deleteClothing(item.getId());
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (!response.isSuccessful()) {
                        showToast("Failed to delete item: " + item.getName());
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    showToast("Network error deleting item: " + item.getName());
                }
            });
        }

        adapter.deleteSelectedItems();
        showToast(selected.size() + " item(s) deleted");
        exitSelectionMode();
        loadClothesFromSupabase();
        applySorting();
        filterAndDisplay();
        updateCategoryTabVisibility();
    }

    // ✅ FILTER CALLBACK — store selections and refresh list
    @Override
    public void onFilterApplied(Set<String> seasons, Set<String> occasions) {
        activeSeasons.clear();
        activeSeasons.addAll(seasons);
        activeOccasions.clear();
        activeOccasions.addAll(occasions);
        filterAndDisplay();
    }
}