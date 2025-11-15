package com.example.outpick.dialogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.outpick.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilterBottomSheetDialog extends BottomSheetDialogFragment {

    // ---- Season buttons ----
    private MaterialButton btnLaNiña, btnElNiño;
    private List<MaterialButton> seasonButtons = new ArrayList<>();
    private Set<String> selectedSeasons = new HashSet<>();

    // ---- Occasion buttons ----
    private MaterialButton btnDaily, btnWork, btnDate, btnFormal, btnTravel, btnHome,
            btnParty, btnSport, btnSpecial, btnSchool, btnBeach, btnEtc;
    private List<MaterialButton> occasionButtons = new ArrayList<>();
    private Set<String> selectedOccasions = new HashSet<>();

    private MaterialButton btnDone, btnClearFilter;
    private ImageView closeButton;

    // SharedPreferences for persistence
    private SharedPreferences prefs;

    // Listener
    private FilterListener filterListener;

    public interface FilterListener {
        void onFilterApplied(Set<String> seasons, Set<String> occasions);
    }

    public void setFilterListener(FilterListener listener) {
        this.filterListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.filter_bottom_sheet, container, false);
        prefs = requireContext().getSharedPreferences("FilterPrefs", Context.MODE_PRIVATE);

        // ---- Close button ----
        closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());

        // ---- Season buttons ----
        btnLaNiña = view.findViewById(R.id.btnLaNiña);
        btnElNiño = view.findViewById(R.id.btnElNiño);

        seasonButtons.add(btnLaNiña);
        seasonButtons.add(btnElNiño);

        for (MaterialButton btn : seasonButtons) {
            btn.setOnClickListener(v -> toggleButtonSelection(btn, selectedSeasons));
        }

        // ---- Occasion buttons ----
        btnDaily = view.findViewById(R.id.btnDaily);
        btnWork = view.findViewById(R.id.btnWork);
        btnDate = view.findViewById(R.id.btnDate);
        btnFormal = view.findViewById(R.id.btnFormal);
        btnTravel = view.findViewById(R.id.btnTravel);
        btnHome = view.findViewById(R.id.btnHome);
        btnParty = view.findViewById(R.id.btnParty);
        btnSport = view.findViewById(R.id.btnSport);
        btnSpecial = view.findViewById(R.id.btnSpecial);
        btnSchool = view.findViewById(R.id.btnSchool);
        btnBeach = view.findViewById(R.id.btnBeach);
        btnEtc = view.findViewById(R.id.btnEtc);

        occasionButtons.add(btnDaily);
        occasionButtons.add(btnWork);
        occasionButtons.add(btnDate);
        occasionButtons.add(btnFormal);
        occasionButtons.add(btnTravel);
        occasionButtons.add(btnHome);
        occasionButtons.add(btnParty);
        occasionButtons.add(btnSport);
        occasionButtons.add(btnSpecial);
        occasionButtons.add(btnSchool);
        occasionButtons.add(btnBeach);
        occasionButtons.add(btnEtc);

        for (MaterialButton btn : occasionButtons) {
            btn.setOnClickListener(v -> toggleButtonSelection(btn, selectedOccasions));
        }

        // ---- Done button ----
        btnDone = view.findViewById(R.id.btnDone);
        btnDone.setOnClickListener(v -> {
            saveSelections();
            if (filterListener != null) {
                filterListener.onFilterApplied(selectedSeasons, selectedOccasions);
            }
            dismiss();
        });

        // ---- Clear button ----
        btnClearFilter = view.findViewById(R.id.btnClearFilter);
        btnClearFilter.setOnClickListener(v -> clearAllSelections());

        // Restore previously saved selections
        loadSelections();

        return view;
    }

    // ---- Toggle multi-selection button ----
    private void toggleButtonSelection(MaterialButton button, Set<String> selectedSet) {
        String text = button.getText().toString();
        if (selectedSet.contains(text)) {
            selectedSet.remove(text);
            button.setBackgroundColor(getResources().getColor(android.R.color.white));
            button.setTextColor(getResources().getColor(android.R.color.black));
        } else {
            selectedSet.add(text);
            button.setBackgroundColor(getResources().getColor(android.R.color.black));
            button.setTextColor(getResources().getColor(android.R.color.white));
        }
    }

    // ---- Save selections to SharedPreferences ----
    private void saveSelections() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("selectedSeasons", selectedSeasons);
        editor.putStringSet("selectedOccasions", selectedOccasions);
        editor.apply();
    }

    // ---- Load saved selections ----
    private void loadSelections() {
        Set<String> savedSeasons = prefs.getStringSet("selectedSeasons", new HashSet<>());
        Set<String> savedOccasions = prefs.getStringSet("selectedOccasions", new HashSet<>());

        selectedSeasons.clear();
        selectedSeasons.addAll(savedSeasons);

        selectedOccasions.clear();
        selectedOccasions.addAll(savedOccasions);

        // Restore button states visually
        for (MaterialButton btn : seasonButtons) {
            updateButtonVisual(btn, selectedSeasons.contains(btn.getText().toString()));
        }
        for (MaterialButton btn : occasionButtons) {
            updateButtonVisual(btn, selectedOccasions.contains(btn.getText().toString()));
        }
    }

    // ---- Clear all selections ----
    private void clearAllSelections() {
        selectedSeasons.clear();
        selectedOccasions.clear();
        prefs.edit().clear().apply();

        for (MaterialButton btn : seasonButtons) {
            updateButtonVisual(btn, false);
        }
        for (MaterialButton btn : occasionButtons) {
            updateButtonVisual(btn, false);
        }
    }

    // ---- Update visual state of a button ----
    private void updateButtonVisual(MaterialButton button, boolean selected) {
        if (selected) {
            button.setBackgroundColor(getResources().getColor(android.R.color.black));
            button.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            button.setBackgroundColor(getResources().getColor(android.R.color.white));
            button.setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            getDialog().setCanceledOnTouchOutside(true);
        }
    }
}
