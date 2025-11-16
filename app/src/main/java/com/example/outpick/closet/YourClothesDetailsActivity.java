package com.example.outpick.closet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.outpick.R;
import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.database.repositories.ClothingRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.android.flexbox.FlexboxLayout;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class YourClothesDetailsActivity extends AppCompatActivity {

    private ImageView previewImageView, categoryArrow, seasonArrow, occasionArrow, backButton;
    private Button btnTops, btnBottom, btnAccessories, btnOuterwear, btnFootwear, saveButton;

    private FlexboxLayout topsLayout, bottomLayout, accessoriesLayout,
            outerwearLayout, footwearLayout, seasonLayout, occasionLayout;

    private TextView selectedCategoryText, selectedSeasonText, selectedOccasionText;
    private LinearLayout categoryToggleLayout, mainCategoriesLayout, seasonToggleLayout, occasionToggleLayout;

    private String selectedMainCategory = "";
    private String selectedSubCategory = "";
    private Set<String> selectedSeasons = new HashSet<>();
    private Set<String> selectedOccasions = new HashSet<>();

    private boolean isCategoryExpanded = false;
    private boolean isSeasonExpanded = false;
    private boolean isOccasionExpanded = false;

    private ClothingItem clothingItem;
    private SupabaseService supabaseService;
    private ClothingRepository clothingRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_your_clothes_details);

        // Initialize Supabase service and repository
        supabaseService = SupabaseClient.getService();
        clothingRepository = ClothingRepository.getInstance(supabaseService);

        initViews();

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, YourClothesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        clothingItem = (ClothingItem) getIntent().getSerializableExtra("item");
        if (clothingItem != null) {
            // Load image from Supabase Storage URL
            if (clothingItem.getImagePath() != null && !clothingItem.getImagePath().isEmpty()) {
                Glide.with(this)
                        .load(clothingItem.getImagePath())
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(previewImageView);
            } else if (clothingItem.getImageUri() != null && !clothingItem.getImageUri().isEmpty()) {
                // Fallback to local URI if imagePath is not available
                Glide.with(this)
                        .load(clothingItem.getImageUri())
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(previewImageView);
            }

            // Detect category & highlight main + sub
            if (clothingItem.getCategory() != null && !clothingItem.getCategory().isEmpty()) {
                detectAndSetCategory(clothingItem.getCategory());
                highlightMainCategoryButton(selectedMainCategory);
            }

            // Preselect seasons & occasions
            if (clothingItem.getSeason() != null && !clothingItem.getSeason().isEmpty()) {
                selectedSeasons = new HashSet<>(Arrays.asList(clothingItem.getSeason().split(", ")));
                selectedSeasonText.setText(clothingItem.getSeason());
                preselectChips(seasonLayout, selectedSeasons);
            }

            if (clothingItem.getOccasion() != null && !clothingItem.getOccasion().isEmpty()) {
                selectedOccasions = new HashSet<>(Arrays.asList(clothingItem.getOccasion().split(", ")));
                selectedOccasionText.setText(clothingItem.getOccasion());
                preselectChips(occasionLayout, selectedOccasions);
            }

            // Highlight subcategory chip
            if (!selectedSubCategory.isEmpty()) {
                highlightSubcategoryChip(selectedMainCategory, selectedSubCategory);
            }
        }

        setupCategoryButtons();
        setupSeasonToggle();
        setupOccasionToggle();
        setupSaveButton();
    }

    private void initViews() {
        previewImageView = findViewById(R.id.previewImageView);

        btnTops = findViewById(R.id.btnTops);
        btnBottom = findViewById(R.id.btnBottom);
        btnAccessories = findViewById(R.id.btnAccessories);
        btnOuterwear = findViewById(R.id.btnOuterwear);
        btnFootwear = findViewById(R.id.btnFootwear);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);

        Button[] categoryButtons = {btnTops, btnBottom, btnAccessories, btnOuterwear, btnFootwear};
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
    }

    private void detectAndSetCategory(String category) {
        if (isInLayout(topsLayout, category)) {
            selectedMainCategory = "Tops";
            selectedSubCategory = category;
        } else if (isInLayout(bottomLayout, category)) {
            selectedMainCategory = "Bottom";
            selectedSubCategory = category;
        } else if (isInLayout(accessoriesLayout, category)) {
            selectedMainCategory = "Accessories";
            selectedSubCategory = category;
        } else if (isInLayout(outerwearLayout, category)) {
            selectedMainCategory = "Outerwear";
            selectedSubCategory = category;
        } else if (isInLayout(footwearLayout, category)) {
            selectedMainCategory = "Footwear";
            selectedSubCategory = category;
        } else {
            // Handle categories that might be in "Main > Sub" format
            if (category.contains(">")) {
                String[] parts = category.split(">");
                if (parts.length >= 2) {
                    selectedMainCategory = parts[0].trim();
                    selectedSubCategory = parts[1].trim();
                }
            } else {
                selectedMainCategory = category;
                selectedSubCategory = "";
            }
        }
        updateCategoryDisplay();
    }

    private void setupCategoryButtons() {
        Button[] categoryButtons = {btnTops, btnBottom, btnAccessories, btnOuterwear, btnFootwear};

        categoryToggleLayout.setOnClickListener(v -> {
            if (isCategoryExpanded) {
                mainCategoriesLayout.setVisibility(View.GONE);
                hideAllSubcategories();
                categoryArrow.setImageResource(R.drawable.ic_arrow_down);
                isCategoryExpanded = false;
            } else {
                mainCategoriesLayout.setVisibility(View.VISIBLE);
                highlightMainCategoryButton(selectedMainCategory);
                categoryArrow.setImageResource(R.drawable.ic_arrow_up);
                isCategoryExpanded = true;
            }
        });

        for (Button btn : categoryButtons) {
            btn.setOnClickListener(v -> {
                boolean isSelected = btn.getTag() != null && (boolean) btn.getTag();

                if (!isSelected) {
                    for (Button otherBtn : categoryButtons) {
                        otherBtn.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                        otherBtn.setTextColor(getResources().getColor(android.R.color.black));
                        otherBtn.setTag(false);
                        if (otherBtn != btn) otherBtn.setVisibility(View.GONE);
                        getLayoutForButton(otherBtn).setVisibility(View.GONE);
                    }

                    btn.setBackgroundColor(getResources().getColor(android.R.color.black));
                    btn.setTextColor(getResources().getColor(android.R.color.white));
                    btn.setTag(true);

                    toggleCategory(getCategoryName(btn.getId()), getLayoutForButton(btn));
                } else {
                    btn.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                    btn.setTextColor(getResources().getColor(android.R.color.black));
                    btn.setTag(false);
                    getLayoutForButton(btn).setVisibility(View.GONE);

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
        updateCategoryDisplay();
    }

    private void hideAllSubcategories() {
        topsLayout.setVisibility(View.GONE);
        bottomLayout.setVisibility(View.GONE);
        accessoriesLayout.setVisibility(View.GONE);
        outerwearLayout.setVisibility(View.GONE);
        footwearLayout.setVisibility(View.GONE);
    }

    private void highlightMainCategoryButton(String mainCategory) {
        if (mainCategory == null || mainCategory.isEmpty()) return;

        Button[] buttons = {btnTops, btnBottom, btnAccessories, btnOuterwear, btnFootwear};
        for (Button btn : buttons) {
            if (getCategoryName(btn.getId()).equals(mainCategory)) {
                btn.setBackgroundColor(getResources().getColor(android.R.color.black));
                btn.setTextColor(getResources().getColor(android.R.color.white));
                btn.setTag(true);
                getLayoutForButton(btn).setVisibility(View.VISIBLE);
            } else {
                btn.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                btn.setTextColor(getResources().getColor(android.R.color.black));
                btn.setTag(false);
                getLayoutForButton(btn).setVisibility(View.GONE);
            }
        }
    }

    private void updateCategoryDisplay() {
        if (!selectedSubCategory.isEmpty()) {
            selectedCategoryText.setText(selectedMainCategory + " > " + selectedSubCategory);
        } else {
            selectedCategoryText.setText(selectedMainCategory);
        }
    }

    private String getCategoryName(int id) {
        if (id == R.id.btnTops) return "Tops";
        if (id == R.id.btnBottom) return "Bottom";
        if (id == R.id.btnAccessories) return "Accessories";
        if (id == R.id.btnOuterwear) return "Outerwear";
        if (id == R.id.btnFootwear) return "Footwear";
        return "";
    }

    private FlexboxLayout getLayoutForButton(Button btn) {
        if (btn.getId() == R.id.btnTops) return topsLayout;
        if (btn.getId() == R.id.btnBottom) return bottomLayout;
        if (btn.getId() == R.id.btnAccessories) return accessoriesLayout;
        if (btn.getId() == R.id.btnOuterwear) return outerwearLayout;
        if (btn.getId() == R.id.btnFootwear) return footwearLayout;
        return null;
    }

    private void setupSeasonToggle() {
        seasonToggleLayout.setOnClickListener(v -> {
            isSeasonExpanded = !isSeasonExpanded;
            seasonLayout.setVisibility(isSeasonExpanded ? View.VISIBLE : View.GONE);
            seasonArrow.setImageResource(isSeasonExpanded ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
        });
        setupMultiSelectListeners(seasonLayout, selectedSeasons, selectedSeasonText);
    }

    private void setupOccasionToggle() {
        occasionToggleLayout.setOnClickListener(v -> {
            isOccasionExpanded = !isOccasionExpanded;
            occasionLayout.setVisibility(isOccasionExpanded ? View.VISIBLE : View.GONE);
            occasionArrow.setImageResource(isOccasionExpanded ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
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
            if (view instanceof TextView) view.setBackgroundResource(R.drawable.chip_background);
        }
    }

    private void preselectChips(FlexboxLayout layout, Set<String> values) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View view = layout.getChildAt(i);
            if (view instanceof TextView) {
                TextView chip = (TextView) view;
                if (values.contains(chip.getText().toString())) {
                    chip.setBackgroundResource(R.drawable.selected_chip_border);
                }
            }
        }
    }

    private void highlightSubcategoryChip(String mainCategory, String subCategory) {
        FlexboxLayout layout = getLayoutByName(mainCategory);
        if (layout == null) return;
        for (int i = 0; i < layout.getChildCount(); i++) {
            View view = layout.getChildAt(i);
            if (view instanceof TextView) {
                TextView chip = (TextView) view;
                if (chip.getText().toString().equals(subCategory)) {
                    chip.setBackgroundResource(R.drawable.selected_chip_border);
                    break;
                }
            }
        }
    }

    private FlexboxLayout getLayoutByName(String mainCategory) {
        switch (mainCategory) {
            case "Tops": return topsLayout;
            case "Bottom": return bottomLayout;
            case "Accessories": return accessoriesLayout;
            case "Outerwear": return outerwearLayout;
            case "Footwear": return footwearLayout;
        }
        return null;
    }

    private boolean isInLayout(FlexboxLayout layout, String value) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View view = layout.getChildAt(i);
            if (view instanceof TextView) {
                if (((TextView) view).getText().toString().equals(value)) return true;
            }
        }
        return false;
    }

    private void setupSaveButton() {
        saveButton.setOnClickListener(v -> {
            if (clothingItem == null) return;

            if (selectedMainCategory.isEmpty()) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedSeasons.isEmpty() || selectedOccasions.isEmpty()) {
                Toast.makeText(this, "Please select at least one season and occasion", Toast.LENGTH_SHORT).show();
                return;
            }

            String finalCategory = !selectedSubCategory.isEmpty()
                    ? selectedMainCategory + " > " + selectedSubCategory
                    : selectedMainCategory;

            // Update the clothing item object
            clothingItem.setCategory(finalCategory);
            clothingItem.setSeason(String.join(", ", selectedSeasons));
            clothingItem.setOccasion(String.join(", ", selectedOccasions));

            // Show loading state
            saveButton.setEnabled(false);
            saveButton.setText("Saving...");

            // Update clothing item in Supabase using repository
            updateClothingItemInSupabase();
        });
    }

    private void updateClothingItemInSupabase() {
        new Thread(() -> {
            boolean success = clothingRepository.updateClothingItem(clothingItem);

            runOnUiThread(() -> {
                saveButton.setEnabled(true);
                saveButton.setText("Save");

                if (success) {
                    Toast.makeText(YourClothesDetailsActivity.this, "Item updated successfully!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(YourClothesDetailsActivity.this, YourClothesActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(YourClothesDetailsActivity.this, "Failed to update item", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}