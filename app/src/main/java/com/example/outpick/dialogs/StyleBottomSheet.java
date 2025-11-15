package com.example.outpick.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.outpick.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.HashSet;
import java.util.Set;

public class StyleBottomSheet extends BottomSheetDialogFragment {

    public interface StyleListener {
        void onStylesSelected(Set<String> styles);
    }

    private final StyleListener listener;
    private final Set<String> currentSelection;

    public StyleBottomSheet(Set<String> initialSelection, StyleListener listener) {
        this.listener = listener;
        this.currentSelection = initialSelection != null ? new HashSet<>(initialSelection) : new HashSet<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.bottom_sheet_style, container, false);

        // --- Back button ---
        ImageView btnBack = view.findViewById(R.id.btnBackStyle);
        btnBack.setOnClickListener(v -> dismiss());

        // --- CheckBoxes for styles ---
        int[] checkBoxIds = {
                R.id.checkCasual, R.id.checkSporty, R.id.checkCozy, R.id.checkStreetStyle,
                R.id.checkSmartCasual, R.id.checkSmart, R.id.checkFormal, R.id.checkClassic,
                R.id.checkElegant, R.id.checkFestive, R.id.checkLounge, R.id.checkPreppy, R.id.checkFeminine,
                R.id.checkMinimalist, R.id.checkAthleisure, R.id.checkBusinessCasual, R.id.checkChic, R.id.checkRomantic
        }; // <-- FIXED: Closing brace '}' and semicolon ';' added here

        for (int id : checkBoxIds) {
            CheckBox cb = view.findViewById(id);
            if (cb == null) continue; //

            //
            final String style = cb.getText().toString();

            cb.setChecked(currentSelection.contains(style));

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) currentSelection.add(style);
                else currentSelection.remove(style);
            });
        }

        // --- Apply button ---
        MaterialButton btnApply = view.findViewById(R.id.btnApplyStyle);
        btnApply.setOnClickListener(v -> {
            if (listener != null) listener.onStylesSelected(new HashSet<>(currentSelection));
            dismiss();
        });

        return view;
    }
}