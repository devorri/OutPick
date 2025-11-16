package com.example.outpick.closet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.R;
import com.example.outpick.common.PreviewImageActivity;
import com.example.outpick.common.adapters.SelectableClothingAdapter;
import com.example.outpick.database.repositories.ClothingRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.dialogs.ItemFilterBottomSheetDialog;
import com.example.outpick.outfits.OutfitCreationActivity;
import com.example.outpick.utils.FileUtils;
import com.example.outpick.utils.ImageUploader;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ItemsAddingActivity extends AppCompatActivity {

    private static final int GALLERY_REQUEST_CODE = 101;

    private RecyclerView recyclerView;
    private SelectableClothingAdapter adapter;
    private Button buttonContinue;
    private boolean isAddingMore = false;

    private Uri imageUri;
    private ImageButton filterButton;

    // --- Category Tabs ---
    private MaterialButton btnAll, btnTop, btnBottom, btnOuterwear, btnAccessories, btnFootwear;

    private List<ClothingItem> allItems;
    private List<ClothingItem> filteredByOccasionSeason; // Keeps current filter results

    // Supabase service
    private SupabaseService supabaseService;
    private ClothingRepository clothingRepository;
    private ImageUploader imageUploader;

    // --- Camera permission launcher ---
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) openCamera();
                else Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            });

    // --- Take picture launcher ---
    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), (ActivityResultCallback<Boolean>) result -> {
                if (result) {
                    // Upload to Supabase Storage first, then proceed to preview
                    uploadImageToSupabaseStorage(imageUri, "camera_capture");
                } else {
                    Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show();
                }
            });

    // --- Gallery launcher ---
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    // Upload to Supabase Storage first, then proceed to preview
                    uploadImageToSupabaseStorage(uri, "gallery_selection");
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items_adding);

        // Initialize Supabase
        supabaseService = SupabaseClient.getService();
        clothingRepository = ClothingRepository.getInstance(supabaseService);
        imageUploader = new ImageUploader(this);

        // --- Back Button ---
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // --- Add Item Button ---
        findViewById(R.id.btn_add_item).setOnClickListener(v -> showAddItemBottomSheet());

        // --- Filter Button ---
        filterButton = findViewById(R.id.filterButton);
        filterButton.setOnClickListener(v -> showFilterBottomSheet());

        // --- RecyclerView Setup ---
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        buttonContinue = findViewById(R.id.buttonContinue);
        buttonContinue.setVisibility(View.GONE);

        isAddingMore = getIntent().getBooleanExtra("isAddingMore", false);

        // --- Load items from Supabase ---
        allItems = new ArrayList<>();
        filteredByOccasionSeason = new ArrayList<>();
        loadItemsFromSupabase();

        adapter = new SelectableClothingAdapter(filteredByOccasionSeason,
                selectedItems -> buttonContinue.setVisibility(selectedItems.isEmpty() ? View.GONE : View.VISIBLE));
        recyclerView.setAdapter(adapter);

        // --- Continue Button ---
        buttonContinue.setOnClickListener(v -> {
            List<ClothingItem> selectedItems = adapter.getSelectedItems();
            if (selectedItems == null || selectedItems.isEmpty()) {
                Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isAddingMore) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_items", new ArrayList<>(selectedItems));
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Intent intent = new Intent(ItemsAddingActivity.this, OutfitCreationActivity.class);
                intent.putExtra("selected_items", new ArrayList<>(selectedItems));
                startActivity(intent);
                finish();
            }
        });

        // --- Category Tabs ---
        btnAll = findViewById(R.id.btn_all);
        btnTop = findViewById(R.id.btn_top);
        btnBottom = findViewById(R.id.btn_bottom);
        btnOuterwear = findViewById(R.id.btn_outerwear);
        btnAccessories = findViewById(R.id.btn_accessories);
        btnFootwear = findViewById(R.id.btn_footwear);

        // Initially hide all category tabs except "All"
        btnTop.setVisibility(View.GONE);
        btnBottom.setVisibility(View.GONE);
        btnOuterwear.setVisibility(View.GONE);
        btnAccessories.setVisibility(View.GONE);
        btnFootwear.setVisibility(View.GONE);

        // Tab listeners
        btnAll.setOnClickListener(v -> {
            adapter.updateItems(filteredByOccasionSeason);
            setActiveTab(btnAll);
        });

        btnTop.setOnClickListener(v -> {
            filterByCategoryTab("Top");
            setActiveTab(btnTop);
        });

        btnBottom.setOnClickListener(v -> {
            filterByCategoryTab("Bottom");
            setActiveTab(btnBottom);
        });

        btnOuterwear.setOnClickListener(v -> {
            filterByCategoryTab("Outerwear");
            setActiveTab(btnOuterwear);
        });

        btnAccessories.setOnClickListener(v -> {
            filterByCategoryTab("Accessories");
            setActiveTab(btnAccessories);
        });

        btnFootwear.setOnClickListener(v -> {
            filterByCategoryTab("Footwear");
            setActiveTab(btnFootwear);
        });

        setActiveTab(btnAll);
    }

    private void loadItemsFromSupabase() {
        // Use the repository to load items
        new Thread(() -> {
            List<ClothingItem> items = clothingRepository.getAllClothing();
            runOnUiThread(() -> {
                allItems.clear();
                allItems.addAll(items);
                filteredByOccasionSeason.clear();
                filteredByOccasionSeason.addAll(allItems);

                adapter.updateItems(filteredByOccasionSeason);
                checkAndShowCategoryTabs(filteredByOccasionSeason);
                setActiveTab(btnAll);

                Toast.makeText(ItemsAddingActivity.this, "Loaded " + allItems.size() + " items", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    // === Filter RecyclerView by category ===
    private void filterByCategoryTab(String category) {
        List<ClothingItem> filtered = new ArrayList<>();
        for (ClothingItem item : filteredByOccasionSeason) {
            if (item.getCategory() != null &&
                    item.getCategory().toLowerCase().contains(category.toLowerCase())) {
                filtered.add(item);
            }
        }
        adapter.updateItems(filtered);
    }

    // === Active tab style ===
    private void setActiveTab(MaterialButton activeBtn) {
        MaterialButton[] tabs = {btnAll, btnTop, btnBottom, btnOuterwear, btnAccessories, btnFootwear};

        // Define colors
        ColorStateList blackColor = ColorStateList.valueOf(Color.BLACK);
        ColorStateList whiteColor = ColorStateList.valueOf(Color.WHITE);

        for (MaterialButton tab : tabs) {
            if (tab.getVisibility() == View.VISIBLE) {
                // Inactive State: White background, Black text
                tab.setBackgroundTintList(whiteColor);
                tab.setTextColor(blackColor);
            }
        }

        // Active State: Black background, White text
        activeBtn.setBackgroundTintList(blackColor);
        activeBtn.setTextColor(whiteColor);
    }

    // === Show only tabs that exist in the items ===
    private void checkAndShowCategoryTabs(List<ClothingItem> items) {
        Set<String> categoriesInItems = new HashSet<>();
        for (ClothingItem item : items) {
            String cat = item.getCategory();
            if (cat != null) {
                String lc = cat.toLowerCase();
                if (lc.contains("top")) categoriesInItems.add("Top");
                if (lc.contains("bottom")) categoriesInItems.add("Bottom");
                if (lc.contains("outerwear")) categoriesInItems.add("Outerwear");
                if (lc.contains("accessories")) categoriesInItems.add("Accessories");
                if (lc.contains("footwear")) categoriesInItems.add("Footwear");
            }
        }

        btnTop.setVisibility(categoriesInItems.contains("Top") ? View.VISIBLE : View.GONE);
        btnBottom.setVisibility(categoriesInItems.contains("Bottom") ? View.VISIBLE : View.GONE);
        btnOuterwear.setVisibility(categoriesInItems.contains("Outerwear") ? View.VISIBLE : View.GONE);
        btnAccessories.setVisibility(categoriesInItems.contains("Accessories") ? View.VISIBLE : View.GONE);
        btnFootwear.setVisibility(categoriesInItems.contains("Footwear") ? View.VISIBLE : View.GONE);
    }

    // --- Apply Filters with OR logic (occasion OR season) ---
    private void applyFilters(Set<String> selectedOccasions, Set<String> selectedSeasons) {
        List<ClothingItem> filteredList = new ArrayList<>();

        // If nothing selected, show all
        if ((selectedOccasions == null || selectedOccasions.isEmpty()) &&
                (selectedSeasons == null || selectedSeasons.isEmpty())) {
            filteredByOccasionSeason = new ArrayList<>(allItems);
            adapter.updateItems(filteredByOccasionSeason);
            checkAndShowCategoryTabs(filteredByOccasionSeason);
            setActiveTab(btnAll);
            return;
        }

        Set<String> lowerOccasions = new HashSet<>();
        if (selectedOccasions != null) {
            for (String o : selectedOccasions) lowerOccasions.add(o.toLowerCase(Locale.ROOT));
        }

        Set<String> lowerSeasons = new HashSet<>();
        if (selectedSeasons != null) {
            for (String s : selectedSeasons) lowerSeasons.add(s.toLowerCase(Locale.ROOT));
        }

        for (ClothingItem item : allItems) {
            Set<String> itemOccasions = new HashSet<>();
            Set<String> itemSeasons = new HashSet<>();

            if (item.getOccasion() != null) {
                for (String o : item.getOccasion().split(",")) {
                    itemOccasions.add(o.trim().toLowerCase(Locale.ROOT));
                }
            }
            if (item.getSeason() != null) {
                for (String s : item.getSeason().split(",")) {
                    itemSeasons.add(s.trim().toLowerCase(Locale.ROOT));
                }
            }

            // OR Logic → Matches if either category intersects
            boolean matchesOccasion = !Collections.disjoint(lowerOccasions, itemOccasions);
            boolean matchesSeason = !Collections.disjoint(lowerSeasons, itemSeasons);

            if (matchesOccasion || matchesSeason) {
                filteredList.add(item);
            }
        }

        filteredByOccasionSeason = filteredList;
        adapter.updateItems(filteredByOccasionSeason);
        checkAndShowCategoryTabs(filteredByOccasionSeason);
        setActiveTab(btnAll);
    }

    // --- Add Item Bottom Sheet ---
    private void showAddItemBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.popup_add_item, null);
        bottomSheetDialog.setContentView(sheetView);

        ImageView closeIcon = sheetView.findViewById(R.id.icon_close);
        LinearLayout galleryBtn = sheetView.findViewById(R.id.btn_gallery);
        LinearLayout cameraBtn = sheetView.findViewById(R.id.btn_camera);

        closeIcon.setOnClickListener(v -> bottomSheetDialog.dismiss());
        galleryBtn.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            openGallery();
        });
        cameraBtn.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            checkCameraPermission();
        });

        bottomSheetDialog.show();
    }

    // --- Filter Bottom Sheet ---
    private void showFilterBottomSheet() {
        ItemFilterBottomSheetDialog dialog = new ItemFilterBottomSheetDialog();
        dialog.setFilterListener(this::applyFilters);
        dialog.show(getSupportFragmentManager(), "ItemFilterBottomSheet");
    }

    // --- Camera & Gallery ---
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private void openCamera() {
        try {
            File imageFile = createImageFile();
            imageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
            takePictureLauncher.launch(imageUri);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timeStamp;
        File storageDir = getExternalFilesDir("Pictures");
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    /**
     * Upload image to Supabase Storage before proceeding to preview
     */
    private void uploadImageToSupabaseStorage(Uri imageUri, String source) {
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

        String fileName = "clothing_item_" + System.currentTimeMillis() + ".jpg";

        imageUploader.uploadImage(imageUri, "clothing", fileName, new ImageUploader.UploadCallback() {
            @Override
            public void onSuccess(String publicImageUrl) {
                runOnUiThread(() -> {
                    Toast.makeText(ItemsAddingActivity.this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show();

                    // Proceed to PreviewImageActivity with the public URL
                    Intent intent = new Intent(ItemsAddingActivity.this, PreviewImageActivity.class);
                    intent.putExtra("imageUrl", publicImageUrl); // Public URL from Supabase Storage
                    intent.putExtra("imageUri", imageUri.toString()); // Local URI as backup
                    intent.putExtra("from_items_adding", true);
                    startActivity(intent);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ItemsAddingActivity.this,
                            "Failed to upload image: " + error, Toast.LENGTH_LONG).show();

                    // Fallback: proceed with local URI if upload fails
                    Intent intent = new Intent(ItemsAddingActivity.this, PreviewImageActivity.class);
                    intent.putExtra("imageUri", imageUri.toString());
                    intent.putExtra("from_items_adding", true);
                    startActivity(intent);
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload items from Supabase when activity resumes
        loadItemsFromSupabase();
    }
}