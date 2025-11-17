package com.example.outpick.common;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.closet.ItemsAddingActivity;
import com.example.outpick.R;
import com.example.outpick.closet.YourClothesActivity;
import com.example.outpick.database.repositories.ClothingRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.utils.ImageUploader;
import com.google.android.flexbox.FlexboxLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PreviewImageActivity extends AppCompatActivity {

    private ImageView previewImageView, categoryArrow, seasonArrow, occasionArrow, backButton;
    private Button btnTops, btnBottoms, btnAccessories, btnOuterwear, btnFootwear, saveButton;

    private FlexboxLayout topsLayout, bottomLayout, accessoriesLayout,
            outerwearLayout, footwearLayout, seasonLayout, occasionLayout;

    private TextView selectedCategoryText, selectedSeasonText, selectedOccasionText;
    private LinearLayout categoryToggleLayout, mainCategoriesLayout, seasonToggleLayout, occasionToggleLayout;
    private ProgressBar progressBar;

    private String selectedMainCategory = "";
    private String selectedSubCategory = "";
    private Set<String> selectedSeasons = new HashSet<>();
    private Set<String> selectedOccasions = new HashSet<>();
    private Uri selectedImageUri = null;
    private String processedImagePath = null;

    private boolean fromItemsAdding = false;
    private boolean isCategoryExpanded = false;
    private boolean isSeasonExpanded = false;
    private boolean isOccasionExpanded = false;

    private SupabaseService supabaseService;
    private ClothingRepository clothingRepository;

    // NOTE: This API key should ideally be managed more securely (e.g., in resources or backend).
    private static final String REMOVE_BG_API_KEY = "Xuidn9aVNMWAZan8q9aUe1LC";
    private static final String REMOVE_BG_URL = "https://api.remove.bg/v1.0/removebg";
    private static final String TAG = "PreviewImageActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_image);

        // Initialize Supabase
        supabaseService = SupabaseClient.getService();
        clothingRepository = new ClothingRepository(supabaseService);

        fromItemsAdding = getIntent().getBooleanExtra("from_items_adding", false);

        initViews();
        loadImageFromIntent();

        if (selectedImageUri != null) {
            removeBackgroundWithRemoveBg(selectedImageUri);
        }

        setupCategoryButtons();
        setupSeasonToggle();
        setupOccasionToggle();
        setupSaveButton();
        setupBackButton();
    }

    private void initViews() {
        previewImageView = findViewById(R.id.previewImageView);

        btnTops = findViewById(R.id.btnTops);
        btnBottoms = findViewById(R.id.btnBottom);
        btnAccessories = findViewById(R.id.btnAccessories);
        btnOuterwear = findViewById(R.id.btnOuterwear);
        btnFootwear = findViewById(R.id.btnFootwear);
        saveButton = findViewById(R.id.saveButton);

        Button[] categoryButtons = {btnTops, btnBottoms, btnAccessories, btnOuterwear, btnFootwear};
        for (Button btn : categoryButtons) {
            btn.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            btn.setTextColor(getResources().getColor(android.R.color.black));
            btn.setTag(false);
        }

        topsLayout = findViewById(R.id.topsSubcategoryLayout);
        bottomLayout = findViewById(R.id.pantsSubcategoryLayout);
        accessoriesLayout = findViewById(R.id.accessoriesSubcategoryLayout);
        outerwearLayout = findViewById(R.id.outerwearSubcategoryLayout);
        footwearLayout = findViewById(R.id.footwearSubcategoryLayout);

        seasonLayout = findViewById(R.id.seasonSubcategoryLayout);
        occasionLayout = findViewById(R.id.occasionSubcategoryLayout);

        selectedCategoryText = findViewById(R.id.selectedCategoryText);
        selectedSeasonText = findViewById(R.id.selectedSeasonText);
        selectedOccasionText = findViewById(R.id.selectedOccasionText);

        categoryToggleLayout = findViewById(R.id.categoryToggleLayout);
        mainCategoriesLayout = findViewById(R.id.mainCategoriesLayout);
        seasonToggleLayout = findViewById(R.id.seasonToggleLayout);
        occasionToggleLayout = findViewById(R.id.occasionToggleLayout);

        categoryArrow = findViewById(R.id.categoryArrow);
        seasonArrow = findViewById(R.id.seasonArrow);
        occasionArrow = findViewById(R.id.occasionArrow);

        progressBar = findViewById(R.id.progressBar);

        backButton = findViewById(R.id.backButton);
    }

    private void setupBackButton() {
        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }
    }

    private void loadImageFromIntent() {
        // Debug: Log all intent extras
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                Log.d(TAG, "Intent extra - " + key + ": " + extras.get(key));
            }
        }

        // Check if we have a cloud URL (image already uploaded)
        String cloudImageUrl = getIntent().getStringExtra("cloud_image_url");
        String localImageUri = getIntent().getStringExtra("local_image_uri");

        try {
            // Priority 1: Use cloud URL if available (image already uploaded)
            if (cloudImageUrl != null && !cloudImageUrl.isEmpty()) {
                Log.d(TAG, "Loading from cloud URL: " + cloudImageUrl);
                selectedImageUri = Uri.parse(cloudImageUrl);
            }
            // Priority 2: Use local URI as fallback
            else if (localImageUri != null && !localImageUri.isEmpty()) {
                Log.d(TAG, "Loading from local URI: " + localImageUri);
                selectedImageUri = Uri.parse(localImageUri);
            }
            // Priority 3: Other possible parameter names (backward compatibility)
            else {
                Uri parcelableUri = getIntent().getParcelableExtra("imageUri");
                String imageUriStr = getIntent().getStringExtra("imageUri");
                String selectedImageUriStr = getIntent().getStringExtra("selected_image_uri");

                if (parcelableUri != null) {
                    selectedImageUri = parcelableUri;
                } else if (imageUriStr != null && !imageUriStr.isEmpty()) {
                    selectedImageUri = Uri.parse(imageUriStr);
                } else if (selectedImageUriStr != null && !selectedImageUriStr.isEmpty()) {
                    selectedImageUri = Uri.parse(selectedImageUriStr);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
        }

        if (selectedImageUri != null) {
            Log.d(TAG, "Successfully loaded image URI: " + selectedImageUri.toString());
            Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .into(previewImageView);
        } else {
            Log.e(TAG, "No image found in any parameter");
            Toast.makeText(this, "No image found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupCategoryButtons() {
        Button[] categoryButtons = {btnTops, btnBottoms, btnAccessories, btnOuterwear, btnFootwear};

        categoryToggleLayout.setOnClickListener(v -> {
            if (isCategoryExpanded) {
                mainCategoriesLayout.setVisibility(View.GONE);
                hideAllSubcategories();
                categoryArrow.setImageResource(R.drawable.ic_arrow_down);
                isCategoryExpanded = false;
            } else {
                mainCategoriesLayout.setVisibility(View.VISIBLE);
                categoryArrow.setImageResource(R.drawable.ic_arrow_up);
                isCategoryExpanded = true;
            }
        });

        for (Button btn : categoryButtons) {
            btn.setOnClickListener(v -> {
                String categoryName = "";
                if (btn.getId() == R.id.btnTops) categoryName = "Tops";
                else if (btn.getId() == R.id.btnBottom) categoryName = "Bottom";
                else if (btn.getId() == R.id.btnAccessories) categoryName = "Accessories";
                else if (btn.getId() == R.id.btnOuterwear) categoryName = "Outerwear";
                else if (btn.getId() == R.id.btnFootwear) categoryName = "Footwear";

                boolean isSelected = btn.getTag() != null && (boolean) btn.getTag();

                if (!isSelected) {
                    for (Button otherBtn : categoryButtons) {
                        otherBtn.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                        otherBtn.setTextColor(getResources().getColor(android.R.color.black));
                        otherBtn.setTag(false);
                        if (otherBtn != btn) otherBtn.setVisibility(View.GONE);
                    }
                    hideAllSubcategories();

                    btn.setBackgroundColor(getResources().getColor(android.R.color.black));
                    btn.setTextColor(getResources().getColor(android.R.color.white));
                    btn.setTag(true);

                    if (btn.getId() == R.id.btnTops) toggleCategory(categoryName, topsLayout);
                    else if (btn.getId() == R.id.btnBottom) toggleCategory(categoryName, bottomLayout);
                    else if (btn.getId() == R.id.btnAccessories) toggleCategory(categoryName, accessoriesLayout);
                    else if (btn.getId() == R.id.btnOuterwear) toggleCategory(categoryName, outerwearLayout);
                    else if (btn.getId() == R.id.btnFootwear) toggleCategory(categoryName, footwearLayout);

                } else {
                    btn.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                    btn.setTextColor(getResources().getColor(android.R.color.black));
                    btn.setTag(false);

                    hideAllSubcategories();
                    selectedMainCategory = "";
                    selectedSubCategory = "";
                    updateCategoryDisplay();

                    for (Button otherBtn : categoryButtons) {
                        otherBtn.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        setupSubcategoryClickListeners(topsLayout, "Tops");
        setupSubcategoryClickListeners(bottomLayout, "Bottom");
        setupSubcategoryClickListeners(accessoriesLayout, "Accessories");
        setupSubcategoryClickListeners(outerwearLayout, "Outerwear");
        setupSubcategoryClickListeners(footwearLayout, "Footwear");
    }

    private void toggleCategory(String category, FlexboxLayout layout) {
        hideAllSubcategories();
        layout.setVisibility(View.VISIBLE);
        selectedMainCategory = category;
        selectedSubCategory = "";
        clearSubcategorySelection(layout);
        updateCategoryDisplay();
    }

    private void hideAllSubcategories() {
        topsLayout.setVisibility(View.GONE);
        bottomLayout.setVisibility(View.GONE);
        accessoriesLayout.setVisibility(View.GONE);
        outerwearLayout.setVisibility(View.GONE);
        footwearLayout.setVisibility(View.GONE);
    }

    private void updateCategoryDisplay() {
        if (!selectedSubCategory.isEmpty()) {
            selectedCategoryText.setText(selectedSubCategory);
        } else if (!selectedMainCategory.isEmpty()) {
            selectedCategoryText.setText(selectedMainCategory);
        } else {
            selectedCategoryText.setText("Select Category");
        }
    }

    private void setupSeasonToggle() {
        seasonToggleLayout.setOnClickListener(v -> {
            if (isSeasonExpanded) {
                seasonLayout.setVisibility(View.GONE);
                seasonArrow.setImageResource(R.drawable.ic_arrow_down);
                isSeasonExpanded = false;
            } else {
                seasonLayout.setVisibility(View.VISIBLE);
                seasonArrow.setImageResource(R.drawable.ic_arrow_up);
                isSeasonExpanded = true;
            }
        });
        setupMultiSelectListeners(seasonLayout, selectedSeasons, selectedSeasonText);
    }

    private void setupOccasionToggle() {
        occasionToggleLayout.setOnClickListener(v -> {
            if (isOccasionExpanded) {
                occasionLayout.setVisibility(View.GONE);
                occasionArrow.setImageResource(R.drawable.ic_arrow_down);
                isOccasionExpanded = false;
            } else {
                occasionLayout.setVisibility(View.VISIBLE);
                occasionArrow.setImageResource(R.drawable.ic_arrow_up);
                isOccasionExpanded = true;
            }
        });
        setupMultiSelectListeners(occasionLayout, selectedOccasions, selectedOccasionText);
    }

    private void setupMultiSelectListeners(FlexboxLayout layout, Set<String> selectionSet, TextView displayText) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View view = layout.getChildAt(i);
            if (view instanceof TextView) {
                TextView chip = (TextView) view;
                chip.setOnClickListener(v -> {
                    String value = chip.getText().toString();
                    if (selectionSet.contains(value)) {
                        selectionSet.remove(value);
                        chip.setBackgroundResource(R.drawable.chip_background);
                    } else {
                        selectionSet.add(value);
                        chip.setBackgroundResource(R.drawable.selected_chip_border);
                    }
                    displayText.setText(String.join(", ", selectionSet));
                });
            }
        }
    }

    private void setupSubcategoryClickListeners(FlexboxLayout layout, String mainCategory) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View view = layout.getChildAt(i);
            if (view instanceof TextView) {
                TextView chip = (TextView) view;
                chip.setOnClickListener(v -> {
                    clearSubcategorySelection(layout);
                    chip.setBackgroundResource(R.drawable.selected_chip_border);
                    selectedMainCategory = mainCategory;
                    selectedSubCategory = chip.getText().toString();
                    updateCategoryDisplay();
                });
            }
        }
    }

    private void clearSubcategorySelection(FlexboxLayout layout) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View view = layout.getChildAt(i);
            if (view instanceof TextView) {
                view.setBackgroundResource(R.drawable.chip_background);
            }
        }
    }

    private void setupSaveButton() {
        saveButton.setOnClickListener(v -> {
            if (selectedImageUri == null) {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedMainCategory.isEmpty() || selectedSubCategory.isEmpty()
                    || selectedSeasons.isEmpty() || selectedOccasions.isEmpty()) {
                Toast.makeText(this, "Please select category, subcategory, season, and occasion", Toast.LENGTH_SHORT).show();
                return;
            }

            String seasonsStr = String.join(", ", selectedSeasons);
            String occasionsStr = String.join(", ", selectedOccasions);
            String fullCategory = selectedMainCategory + ">" + selectedSubCategory;

            // Upload image to cloud first, then save to database
            uploadImageAndSaveToSupabase(fullCategory, seasonsStr, occasionsStr);
        });
    }

    private void uploadImageAndSaveToSupabase(String category, String season, String occasion) {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);
        saveButton.setText("Uploading...");

        // Check if we already have a cloud URL (image was already uploaded)
        String cloudImageUrl = getIntent().getStringExtra("cloud_image_url");

        if (cloudImageUrl != null && !cloudImageUrl.isEmpty()) {
            Log.d(TAG, "Image already uploaded to cloud, using existing URL: " + cloudImageUrl);
            // Image was already uploaded in CreateOutfitActivity, just save to database
            saveItemToSupabase(category, season, occasion, cloudImageUrl);
        } else {
            // Image needs to be uploaded (coming from ItemsAddingActivity or other sources)
            Log.d(TAG, "Uploading image to cloud...");

            // Generate a unique filename
            String fileName = "clothing_" + category.toLowerCase().replace(">", "_") +
                    "_" + System.currentTimeMillis() + ".png";

            ImageUploader uploader = new ImageUploader(this);

            // Use the processed image if available, otherwise use original
            Uri imageToUpload = processedImagePath != null ? Uri.fromFile(new File(processedImagePath)) : selectedImageUri;

            uploader.uploadImage(imageToUpload, "clothing", fileName, new ImageUploader.UploadCallback() {
                @Override
                public void onSuccess(String newCloudImageUrl) {
                    // Image uploaded successfully, now save clothing item with cloud URL
                    saveItemToSupabase(category, season, occasion, newCloudImageUrl);
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        saveButton.setEnabled(true);
                        saveButton.setText("Save");
                        Toast.makeText(PreviewImageActivity.this,
                                "Failed to upload image: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        }
    }

    private void saveItemToSupabase(String category, String season, String occasion, String cloudImageUrl) {
        runOnUiThread(() -> {
            saveButton.setText("Saving...");
        });

        // Generate a name for the item
        String itemName = selectedSubCategory + " Item";

        // ✅ GET THE CURRENT USER'S ID FOR THEIR CLOSET
        String currentUserId = getCurrentUserId();

        // Use user ID for the clothing item
        new Thread(() -> {
            boolean success = clothingRepository.addClothingItemWithUserId(
                    itemName,
                    cloudImageUrl,
                    category,
                    season,
                    occasion,
                    currentUserId // ✅ PASS USER ID
            );

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                saveButton.setEnabled(true);
                saveButton.setText("Save");

                if (success) {
                    Toast.makeText(this, "Clothing item saved to your closet!", Toast.LENGTH_SHORT).show();

                    // Refresh the repository cache
                    clothingRepository.getAllClothing();

                    Intent intent = fromItemsAdding
                            ? new Intent(PreviewImageActivity.this, ItemsAddingActivity.class)
                            : new Intent(PreviewImageActivity.this, YourClothesActivity.class);

                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Failed to save item to cloud", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // ✅ ADD THIS METHOD TO GET CURRENT USER ID
    private String getCurrentUserId() {
        // Get from SharedPreferences or wherever you store user session
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        return prefs.getString("user_id", null);
    }

    private void removeBackgroundWithRemoveBg(Uri imageUri) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // First, copy the URI to a file for remove.bg API
        File imageFile = copyUriToFile(imageUri);
        if (imageFile == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image_file", imageFile.getName(),
                        RequestBody.create(MediaType.parse("image/*"), imageFile))
                .addFormDataPart("size", "auto")
                .build();

        Request request = new Request.Builder()
                .url(REMOVE_BG_URL)
                .addHeader("X-Api-Key", REMOVE_BG_API_KEY)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) {
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(PreviewImageActivity.this, "Failed to connect to remove.bg", Toast.LENGTH_SHORT).show();
                });
                Log.e(TAG, "remove.bg failure", e);
            }

            @Override public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        runOnUiThread(() -> {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            Toast.makeText(PreviewImageActivity.this,
                                    "remove.bg API error: " + response.code(), Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    byte[] imageBytes = response.body().bytes();
                    File cutoutFile = new File(getFilesDir(), "cutout_" + System.currentTimeMillis() + ".png");
                    try (FileOutputStream fos = new FileOutputStream(cutoutFile)) {
                        fos.write(imageBytes);
                    }

                    runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        if (bitmap != null) {
                            previewImageView.setImageBitmap(bitmap);
                            processedImagePath = cutoutFile.getAbsolutePath();
                        } else {
                            Glide.with(PreviewImageActivity.this)
                                    .load(selectedImageUri)
                                    .into(previewImageView);
                            Toast.makeText(PreviewImageActivity.this, "Could not decode processed image", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(PreviewImageActivity.this, "Processing error", Toast.LENGTH_SHORT).show();
                    });
                    Log.e(TAG, "remove.bg processing error", e);
                } finally {
                    if (response.body() != null) response.body().close();
                }
            }
        });
    }

    private File copyUriToFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File directory = new File(getFilesDir(), "temp_images");
            if (!directory.exists()) directory.mkdirs();

            String fileName = "temp_" + System.currentTimeMillis() + ".jpg";
            File imageFile = new File(directory, fileName);

            OutputStream outputStream = new FileOutputStream(imageFile);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            return imageFile;
        } catch (Exception e) {
            Log.e(TAG, "copyUriToFile failed", e);
            return null;
        }
    }
}