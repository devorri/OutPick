package com.example.outpick.dialogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.outpick.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.HashSet;
import java.util.Set;

public class ItemFilterBottomSheetDialog extends BottomSheetDialogFragment {

    // ---- Occasion Buttons ----
    private MaterialButton btnDaily, btnWork, btnDate, btnFormal, btnTravel,
            btnHome, btnParty, btnSport, btnSpecial, btnSchool, btnBeach;

    // ---- Season Buttons (Updated names to match XML: El Niño and La Niña) ----
    private MaterialButton btnLaNina, btnElNino;

    // ---- Buttons and Icons ----
    private MaterialButton btnApply;
    private ImageView closeIcon;
    private TextView resetAllText;

    // ---- Data Storage ----
    private final Set<String> selectedOccasions = new HashSet<>();
    private final Set<String> selectedSeasons = new HashSet<>();

    private FilterListener listener;
    private SharedPreferences preferences;

    private static final String PREF_NAME = "ItemFilterPrefs";
    private static final String KEY_OCCASIONS = "SelectedOccasions";
    private static final String KEY_SEASONS = "SelectedSeasons";
    private static final String KEY_SESSION_ID = "AppSessionID";

    public interface FilterListener {
        void onFilterApplied(Set<String> occasions, Set<String> seasons);
    }

    public void setFilterListener(FilterListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.item_filter_bottom_sheet, container, false);
        preferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // ---- Setup UI ----
        closeIcon = view.findViewById(R.id.closeIcon);
        resetAllText = view.findViewById(R.id.resetAllText);
        btnApply = view.findViewById(R.id.btnApplyFilter);

        // ---- Initialize Buttons ----
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

        // Mapped to the new IDs from item_filter_bottom_sheet.xml
        btnLaNina = view.findViewById(R.id.btnLaNina);
        btnElNino = view.findViewById(R.id.btnElNino);

        // ---- Close BottomSheet ----
        closeIcon.setOnClickListener(v -> dismiss());

        // ---- Reset All ----
        resetAllText.setVisibility(View.GONE);
        resetAllText.setOnClickListener(v -> resetAllFilters());

        // ---- Reset if app restarted ----
        checkIfAppWasRestarted();

        // ---- Load Saved Filters ----
        loadSavedFilters();

        // ---- Setup all filter buttons (Occasions remain the same) ----
        setupOccasionButton(btnDaily, "Daily");
        setupOccasionButton(btnWork, "Work");
        setupOccasionButton(btnDate, "Date");
        setupOccasionButton(btnFormal, "Formal");
        setupOccasionButton(btnTravel, "Travel");
        setupOccasionButton(btnHome, "Home");
        setupOccasionButton(btnParty, "Party");
        setupOccasionButton(btnSport, "Sport");
        setupOccasionButton(btnSpecial, "Special");
        setupOccasionButton(btnSchool, "School");
        setupOccasionButton(btnBeach, "Beach");

        // ---- Setup Season Buttons (Updated keys to match new labels) ----
        setupSeasonButton(btnLaNina, "La Niña");
        setupSeasonButton(btnElNino, "El Niño");

        // ---- Apply Button ----
        btnApply.setOnClickListener(v -> {
            saveFilters();
            if (listener != null) {
                listener.onFilterApplied(new HashSet<>(selectedOccasions), new HashSet<>(selectedSeasons));
            }
            dismiss();
        });

        updateResetVisibility();
        return view;
    }

    // ================= BUTTON LOGIC =================

    private void setupOccasionButton(MaterialButton button, String occasion) {
        setButtonSelected(button, selectedOccasions.contains(occasion));
        button.setOnClickListener(v -> {
            if (selectedOccasions.contains(occasion)) {
                selectedOccasions.remove(occasion);
                setButtonSelected(button, false);
            } else {
                selectedOccasions.add(occasion);
                setButtonSelected(button, true);
            }
            updateResetVisibility();
        });
    }

    private void setupSeasonButton(MaterialButton button, String season) {
        setButtonSelected(button, selectedSeasons.contains(season));
        button.setOnClickListener(v -> {
            if (selectedSeasons.contains(season)) {
                selectedSeasons.remove(season);
                setButtonSelected(button, false);
            } else {
                selectedSeasons.add(season);
                setButtonSelected(button, true);
            }
            updateResetVisibility();
        });
    }

    private void setButtonSelected(MaterialButton button, boolean selected) {
        if (selected) {
            // Using ContextCompat for color resources to be safer across Android versions
            button.setBackgroundColor(button.getContext().getResources().getColor(android.R.color.black, button.getContext().getTheme()));
            button.setTextColor(button.getContext().getResources().getColor(android.R.color.white, button.getContext().getTheme()));
        } else {
            button.setBackgroundColor(button.getContext().getResources().getColor(android.R.color.white, button.getContext().getTheme()));
            button.setTextColor(button.getContext().getResources().getColor(android.R.color.black, button.getContext().getTheme()));
        }
    }

    private void updateResetVisibility() {
        boolean anySelected = !selectedOccasions.isEmpty() || !selectedSeasons.isEmpty();
        resetAllText.setVisibility(anySelected ? View.VISIBLE : View.GONE);
    }

    // ================= RESET LOGIC =================

    private void resetAllFilters() {
        selectedOccasions.clear();
        selectedSeasons.clear();
        resetAllButtons();
        preferences.edit().clear().apply();
        resetAllText.setVisibility(View.GONE);
    }

    private void resetAllButtons() {
        MaterialButton[] allButtons = {
                btnDaily, btnWork, btnDate, btnFormal, btnTravel,
                btnHome, btnParty, btnSport, btnSpecial, btnSchool, btnBeach,
                btnLaNina, btnElNino // Updated button references
        };
        for (MaterialButton b : allButtons) setButtonSelected(b, false);
    }

    // ================= SAVE / LOAD =================

    private void saveFilters() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(KEY_OCCASIONS, selectedOccasions);
        editor.putStringSet(KEY_SEASONS, selectedSeasons);
        editor.putString(KEY_SESSION_ID, getCurrentSessionId());
        editor.apply();
    }

    private void loadSavedFilters() {
        selectedOccasions.clear();
        selectedSeasons.clear();

        Set<String> savedOccasions = preferences.getStringSet(KEY_OCCASIONS, new HashSet<>());
        Set<String> savedSeasons = preferences.getStringSet(KEY_SEASONS, new HashSet<>());

        selectedOccasions.addAll(savedOccasions);
        selectedSeasons.addAll(savedSeasons);
    }

    // ================= APP RESTART DETECTION =================

    private void checkIfAppWasRestarted() {
        String savedSessionId = preferences.getString(KEY_SESSION_ID, "");
        String currentSessionId = getCurrentSessionId();

        // If session changed (app fully killed and restarted)
        if (!savedSessionId.equals(currentSessionId)) {
            preferences.edit().clear().apply(); // reset all filters
        }

        // Save current session ID
        preferences.edit().putString(KEY_SESSION_ID, currentSessionId).apply();
    }

    private String getCurrentSessionId() {
        // Each process has a unique ID; resets when app is killed
        return String.valueOf(android.os.Process.myPid());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        saveFilters();
    }
}
