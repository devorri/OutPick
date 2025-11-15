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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StyleBottomSheetDialog extends BottomSheetDialogFragment {

    private final OnStyleSelectedListener listener;
    private final Set<String> preSelected = new HashSet<>();

    // Listener interface
    public interface OnStyleSelectedListener {
        void onStylesSelected(List<String> styles);
    }

    // Constructor accepts listener + optional pre-selected styles
    public StyleBottomSheetDialog(OnStyleSelectedListener listener, Set<String> preSelectedStyles) {
        this.listener = listener;
        if (preSelectedStyles != null) {
            this.preSelected.addAll(preSelectedStyles);
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.bottom_sheet_style, container, false);

        // Find all checkboxes
        CheckBox cbCasual = view.findViewById(R.id.checkCasual);
        CheckBox cbSporty = view.findViewById(R.id.checkSporty);
        CheckBox cbCozy = view.findViewById(R.id.checkCozy);
        CheckBox cbStreetStyle = view.findViewById(R.id.checkStreetStyle);
        CheckBox cbSmartCasual = view.findViewById(R.id.checkSmartCasual);
        CheckBox cbSmart = view.findViewById(R.id.checkSmart);
        CheckBox cbClassic = view.findViewById(R.id.checkClassic);
        CheckBox cbFestive = view.findViewById(R.id.checkFestive);
        CheckBox cbLounge = view.findViewById(R.id.checkLounge);
        CheckBox cbPreppy = view.findViewById(R.id.checkPreppy);
        CheckBox cbFeminine = view.findViewById(R.id.checkFeminine);
        CheckBox cbMinimalist = view.findViewById(R.id.checkMinimalist);
        CheckBox cbAthleisure = view.findViewById(R.id.checkAthleisure);
        CheckBox cbElegant = view.findViewById(R.id.checkElegant);
        CheckBox cbBusinessCasual = view.findViewById(R.id.checkBusinessCasual);
        CheckBox cbChic = view.findViewById(R.id.checkChic);
        CheckBox cbRomantic = view.findViewById(R.id.checkRomantic);
        CheckBox cbFormal = view.findViewById(R.id.checkFormal);

        // 🔹 Pre-check based on preSelected set
        if (preSelected.contains("Casual")) cbCasual.setChecked(true);
        if (preSelected.contains("Sporty")) cbSporty.setChecked(true);
        if (preSelected.contains("Cozy")) cbCozy.setChecked(true);
        if (preSelected.contains("Streetstyle") || preSelected.contains("Street Style")) cbStreetStyle.setChecked(true);
        if (preSelected.contains("Smart Casual")) cbSmartCasual.setChecked(true);
        if (preSelected.contains("Smart")) cbSmart.setChecked(true);
        if (preSelected.contains("Classic")) cbClassic.setChecked(true);
        if (preSelected.contains("Festive")) cbFestive.setChecked(true);
        if (preSelected.contains("Lounge")) cbLounge.setChecked(true);
        if (preSelected.contains("Preppy")) cbPreppy.setChecked(true);
        if (preSelected.contains("Feminine")) cbFeminine.setChecked(true);
        if (preSelected.contains("Minimalist")) cbMinimalist.setChecked(true);
        if (preSelected.contains("Athleisure")) cbAthleisure.setChecked(true);
        if (preSelected.contains("Elegant")) cbElegant.setChecked(true);
        if (preSelected.contains("Business Casual")) cbBusinessCasual.setChecked(true);
        if (preSelected.contains("Chic")) cbChic.setChecked(true);
        if (preSelected.contains("Romantic")) cbRomantic.setChecked(true);
        if (preSelected.contains("Formal")) cbFormal.setChecked(true);

        // 🔹 Buttons
        MaterialButton btnApply = view.findViewById(R.id.btnApplyStyle);
        ImageView btnBack = view.findViewById(R.id.btnBackStyle);
        btnBack.setOnClickListener(v -> dismiss());

        // 🔹 Apply selections
        btnApply.setOnClickListener(v -> {
            List<String> selected = new ArrayList<>();
            if (cbCasual.isChecked()) selected.add("Casual");
            if (cbSporty.isChecked()) selected.add("Sporty");
            if (cbCozy.isChecked()) selected.add("Cozy");
            if (cbStreetStyle.isChecked()) selected.add("Streetstyle");
            if (cbSmartCasual.isChecked()) selected.add("Smart Casual");
            if (cbSmart.isChecked()) selected.add("Smart");
            if (cbClassic.isChecked()) selected.add("Classic");
            if (cbFestive.isChecked()) selected.add("Festive");
            if (cbLounge.isChecked()) selected.add("Lounge");
            if (cbPreppy.isChecked()) selected.add("Preppy");
            if (cbFeminine.isChecked()) selected.add("Feminine");
            if (cbMinimalist.isChecked()) selected.add("Minimalist");
            if (cbAthleisure.isChecked()) selected.add("Athleisure");
            if (cbElegant.isChecked()) selected.add("Elegant");
            if (cbBusinessCasual.isChecked()) selected.add("Business Casual");
            if (cbChic.isChecked()) selected.add("Chic");
            if (cbRomantic.isChecked()) selected.add("Romantic");
            if (cbFormal.isChecked()) selected.add("Formal");

            if (listener != null) listener.onStylesSelected(selected);
            dismiss();
        });

        return view;
    }
}
