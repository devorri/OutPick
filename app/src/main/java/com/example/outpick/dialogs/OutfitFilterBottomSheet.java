package com.example.outpick.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.outpick.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.HashSet;
import java.util.Set;

public class OutfitFilterBottomSheet extends BottomSheetDialogFragment {

    public interface FilterListener {
        void onFilterApplied(
                Set<String> selectedCategories,
                Set<String> selectedGenders, // Retained in interface
                Set<String> selectedEvents,
                Set<String> selectedSeasons,
                Set<String> selectedStyles,
                String searchKeyword
        );
    }

    private final FilterListener listener;

    // Filter sets are stored internally in lowercase for consistent comparison with button text
    private final Set<String> selectedCategories;
    private final Set<String> selectedGenders; // Retained internally, but UI doesn't modify it
    private final Set<String> selectedEvents;
    private final Set<String> selectedSeasons;
    private final Set<String> selectedStyles;
    private String searchKeyword;

    private EditText etSearch;
    private TextView tvSearchLabel; // Added reference for the search label
    private TextView tvStylesDisplay; // Reference to the TextView that displays selected styles

    /**
     * Constructor fixed to accept 7 content arguments (5 Sets + 1 String + the Categories Set)
     * to match the call signature expected by OutfitSuggestionActivity.
     */
    public OutfitFilterBottomSheet(
            FilterListener listener,
            Set<String> selectedCategories, // Arg 2 (The previously missing set)
            Set<String> selectedGenders,    // Arg 3 (Set 1)
            Set<String> selectedEvents,     // Arg 4 (Set 2)
            Set<String> selectedSeasons,    // Arg 5 (Set 3)
            Set<String> selectedStyles,     // Arg 6 (Set 4)
            String searchKeyword            // Arg 7 (String 1)
    ) {
        this.listener = listener;

        // Initialize selectedCategories from arguments, fixing the length mismatch error.
        this.selectedCategories = selectedCategories != null ? toLowerTrimSet(selectedCategories) : new HashSet<>();

        this.selectedGenders = selectedGenders != null ? toLowerTrimSet(selectedGenders) : new HashSet<>();
        this.selectedEvents = selectedEvents != null ? toLowerTrimSet(selectedEvents) : new HashSet<>();
        this.selectedSeasons = selectedSeasons != null ? toLowerTrimSet(selectedSeasons) : new HashSet<>();
        this.selectedStyles = selectedStyles != null ? toLowerTrimSet(selectedStyles) : new HashSet<>();
        this.searchKeyword = searchKeyword != null ? searchKeyword : "";
    }

    // Helper to ensure initial data is consistent (lowercased)
    private Set<String> toLowerTrimSet(Set<String> inputSet) {
        Set<String> newSet = new HashSet<>();
        for (String item : inputSet) {
            if (item != null) {
                newSet.add(item.trim().toLowerCase());
            }
        }
        return newSet;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // NOTE: R.layout.outfit_filter_suggestion must be a layout resource that contains all the IDs referenced below.
        View view = inflater.inflate(R.layout.outfit_filter_suggestion, container, false);

        // --- Back Button ---
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> dismiss());

        // --- Search Bar ---
        etSearch = view.findViewById(R.id.etSearch);
        tvSearchLabel = view.findViewById(R.id.tvSearchLabel); // Initialize Search Label

        etSearch.setText(searchKeyword);
        // Initial visibility is set by XML, but we explicitly hide it here if not already
        etSearch.setVisibility(View.GONE);
        tvSearchLabel.setVisibility(View.GONE);

        // --- Styles Selection (The text we need to update) ---
        // Using the correct ID from the provided XML: tvAllCategories
        tvStylesDisplay = view.findViewById(R.id.tvAllCategories);
        updateStyleTextView(); // Initialize the text view

        // --- QUICK STYLE BUTTONS (Linked to selectedStyles) ---
        int[] styleButtonIds = {
                R.id.btnCasual, R.id.btnSporty, R.id.btnCozy,
                R.id.btnStreetstyle, R.id.btnSmartcasual
        };
        // Pass tvStylesDisplay to setupToggleGroup to update it when quick styles are clicked
        setupToggleGroup(view, styleButtonIds, selectedStyles, tvStylesDisplay);

        // --- Category Buttons (Empty in original but kept for structure) ---
        int[] categoryButtonIds = {};
        setupToggleGroup(view, categoryButtonIds, selectedCategories, null);


        // --- Event Buttons ---
        int[] eventButtonIds = {
                R.id.btnWorkfromHome, R.id.btnWorkout, R.id.btnOffice,
                R.id.btnDating, R.id.btnDailyRoutine, R.id.btnFormal,
                R.id.btnRelaxingAtHome, R.id.btnParty, R.id.btnTravelling,
                R.id.btnBeach
        };
        setupToggleGroup(view, eventButtonIds, selectedEvents, null);

        // --- Season Buttons (El Niño and La Niña) ---
        int[] seasonButtonIds = {
                R.id.btnElNino,
                R.id.btnLaNina
        };
        setupToggleGroup(view, seasonButtonIds, selectedSeasons, null);

        // --- Styles Selection Click Listener (for StyleBottomSheet) ---
        LinearLayout layoutStyleAll = view.findViewById(R.id.layoutStyleAll);
        if (layoutStyleAll != null) {
            layoutStyleAll.setOnClickListener(v -> {
                // Pass the current selection to the sub-sheet
                StyleBottomSheet styleSheet = new StyleBottomSheet(selectedStyles, styles -> {
                    selectedStyles.clear();
                    // Ensure styles from sub-sheet are lowercased and trimmed before adding
                    selectedStyles.addAll(toLowerTrimSet(styles));
                    updateStyleTextView(); // Update after returning from sub-sheet
                });
                if (getParentFragmentManager() != null) {
                    styleSheet.show(getParentFragmentManager(), "StyleBottomSheet");
                }
            });
        }


        // --- Apply Button ---
        MaterialButton btnApply = view.findViewById(R.id.btnApply);
        btnApply.setOnClickListener(v -> {
            searchKeyword = etSearch.getText().toString().trim();
            if (listener != null) {
                // Pass the sets as they are (already lowercased for filtering)
                listener.onFilterApplied(
                        selectedCategories,
                        selectedGenders,
                        selectedEvents,
                        selectedSeasons,
                        selectedStyles,
                        searchKeyword
                );
            }
            dismiss();
        });

        // --- Toggle visibility for search section ---
        ImageButton btnSearchIcon = view.findViewById(R.id.btnSearch);

        btnSearchIcon.setOnClickListener(v -> {
            boolean isVisible = etSearch.getVisibility() == View.VISIBLE;
            // Toggle visibility for both the EditText and the Label
            etSearch.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            tvSearchLabel.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        });

        return view;
    }

    // ---------------- Helper Methods ----------------

    private void updateStyleTextView() {
        if (tvStylesDisplay != null) {
            // Capitalize the first letter of each style name for display
            if (selectedStyles.isEmpty()) {
                tvStylesDisplay.setText("All");
            } else {
                Set<String> displayStyles = new HashSet<>();
                for (String style : selectedStyles) {
                    if (style != null && !style.isEmpty()) {
                        // Capitalize only the first letter of the string (e.g., "smart casual" remains "Smart casual")
                        String capitalizedStyle = style.substring(0, 1).toUpperCase() + style.substring(1);
                        displayStyles.add(capitalizedStyle);
                    }
                }
                tvStylesDisplay.setText(String.join(", ", displayStyles));
            }
        }
    }


    /**
     * Sets up click listeners for a group of MaterialButtons.
     */
    private void setupToggleGroup(View root, int[] buttonIds, Set<String> selectedSet, @Nullable TextView textViewToUpdate) {
        for (int id : buttonIds) {
            MaterialButton btn = root.findViewById(id);
            if (btn == null) continue; // Safety check

            // Convert button text to lowercase for the filter key (e.g., "El Niño" -> "el niño")
            final String key = btn.getText().toString().trim().toLowerCase();

            updateCategoryButtonUI(btn, selectedSet.contains(key));
            btn.setOnClickListener(v -> {
                if (selectedSet.contains(key)) selectedSet.remove(key);
                else selectedSet.add(key);
                updateCategoryButtonUI(btn, selectedSet.contains(key));

                // If a TextView is provided (i.e., for Styles), update it.
                if (textViewToUpdate != null) {
                    updateStyleTextView();
                }
            });
        }
    }

    private void updateCategoryButtonUI(MaterialButton button, boolean selected) {
        if (selected) {
            button.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.black));
            button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        } else {
            button.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.white));
            button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        }
    }
}
