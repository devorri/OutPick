package com.example.outpick.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.outpick.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CustomizeBottomSheet extends BottomSheetDialogFragment {

    // Category buttons
    private MaterialButton btnWorkformHome, btnWorkout, btnOffice, btnDating,
            btnDailyroutine, btnRelaxingathome, btnParty, btnTravelling, btnBeach, btnApply;

    // Season buttons (UPDATED NAMES)
    private MaterialButton btnElNino, btnLaNina; // Used to be btnSummer and btnRainy

    // Style buttons
    private MaterialButton btnCasual, btnSporty, btnCozy, btnStreetstyle,
            btnSmartcasual, btnSmart, btnFormal;

    // Groups
    private MaterialButton[] categoryButtons;
    private MaterialButton[] seasonButtons;
    private MaterialButton[] styleButtons;

    // Selected lists
    private final List<String> selectedCategories = new ArrayList<>();
    private final List<String> selectedSeasons = new ArrayList<>();
    private final List<String> selectedStyles = new ArrayList<>();

    // Listener
    public interface OnFiltersAppliedListener {
        void onFiltersApplied(List<String> categories,
                              List<String> seasons,
                              List<String> styles);
    }

    private OnFiltersAppliedListener filtersListener;

    public void setOnFiltersAppliedListener(OnFiltersAppliedListener listener) {
        this.filtersListener = listener;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_customize, container, false);

        // --- Category buttons (No change) ---
        btnWorkformHome = view.findViewById(R.id.btnWorkformHome);
        btnWorkout = view.findViewById(R.id.btnWorkout);
        btnOffice = view.findViewById(R.id.btnOffice);
        btnDating = view.findViewById(R.id.btnDating);
        btnDailyroutine = view.findViewById(R.id.btnDailyroutine);
        btnRelaxingathome = view.findViewById(R.id.btnRelaxingathome);
        btnParty = view.findViewById(R.id.btnParty);
        btnTravelling = view.findViewById(R.id.btnTravelling);
        btnBeach = view.findViewById(R.id.btnBeach);
        btnApply = view.findViewById(R.id.btnApply);

        // --- Season buttons (UPDATED: Only Summer/El Niño and Rainy/La Niña remain) ---
        // Assuming R.id.btnSummer now corresponds to "El Niño" and R.id.btnRainy to "La Niña"
        // You will need to make sure your bottom_sheet_customize.xml is updated to reflect these name changes and hide the other buttons.
        btnElNino = view.findViewById(R.id.btnSummer); // R.id.btnSummer should display "El Niño"
        btnLaNina = view.findViewById(R.id.btnRainy);  // R.id.btnRainy should display "La Niña"

        // --- Style buttons (No change) ---
        btnCasual = view.findViewById(R.id.btnCasual);
        btnSporty = view.findViewById(R.id.btnSporty);
        btnCozy = view.findViewById(R.id.btnCozy);
        btnStreetstyle = view.findViewById(R.id.btnStreetstyle);
        btnSmartcasual = view.findViewById(R.id.btnSmartcasual);
        btnSmart = view.findViewById(R.id.btnSmart);
        btnFormal = view.findViewById(R.id.btnFormal);

        // Groups
        categoryButtons = new MaterialButton[]{
                btnWorkformHome, btnWorkout, btnOffice, btnDating,
                btnDailyroutine, btnRelaxingathome, btnParty, btnTravelling, btnBeach
        };
        // UPDATED: Only El Niño and La Niña
        seasonButtons = new MaterialButton[]{btnElNino, btnLaNina};
        styleButtons = new MaterialButton[]{btnCasual, btnSporty, btnCozy,
                btnStreetstyle, btnSmartcasual, btnSmart, btnFormal};

        // --- Setup button logic ---
        setupButtons(categoryButtons, selectedCategories);
        setupButtons(seasonButtons, selectedSeasons);
        setupButtons(styleButtons, selectedStyles);

        // Restore previous selection states
        restoreButtonStates();

        // --- Style row click to open StyleBottomSheet ---
        LinearLayout layoutStyleAll = view.findViewById(R.id.layoutStyleAll);
        TextView txtStyleAll = view.findViewById(R.id.txtStyleAll);
        updateAllText(txtStyleAll, selectedStyles);

        layoutStyleAll.setOnClickListener(v -> {
            // NOTE: StyleBottomSheet must be defined elsewhere and accept a HashSet of selected items
            // and a callback (Consumer or similar interface) for when new styles are selected.
            StyleBottomSheet styleSheet = new StyleBottomSheet(new HashSet<>(selectedStyles), selected -> {
                selectedStyles.clear();
                selectedStyles.addAll(selected);
                updateAllText(txtStyleAll, selectedStyles);
                // Restore button states to reflect the selection from the StyleBottomSheet
                restoreButtonStates();
            });
            styleSheet.show(getParentFragmentManager(), "StyleBottomSheet");
        });

        // --- Apply button ---
        btnApply.setOnClickListener(v -> {
            if (filtersListener != null) {
                filtersListener.onFiltersApplied(
                        new ArrayList<>(selectedCategories),
                        new ArrayList<>(selectedSeasons),
                        new ArrayList<>(selectedStyles)
                );
            }
            dismiss();
        });

        // --- Close button ---
        ImageView btnClose = view.findViewById(R.id.btn_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        return view;
    }

    private void setupButtons(MaterialButton[] buttons, List<String> list) {
        for (MaterialButton button : buttons) {
            if (button == null) continue; // Safety check
            // Use the actual text on the button, which should be "El Niño" or "La Niña" if XML is updated
            String value = button.getText().toString().trim();
            button.setOnClickListener(v -> {
                toggleButton(button, value, list);
                if (buttons == styleButtons) {
                    // Update the "All Styles" TextView when a style button is clicked
                    TextView txtStyleAll = getView() != null ? getView().findViewById(R.id.txtStyleAll) : null;
                    if (txtStyleAll != null) updateAllText(txtStyleAll, selectedStyles);
                }
            });
        }
    }

    private void toggleButton(MaterialButton button, String value, List<String> list) {
        if (list.contains(value)) {
            list.remove(value);
            highlightButton(button, false);
        } else {
            list.add(value);
            highlightButton(button, true);
        }
    }

    private void highlightButton(MaterialButton button, boolean selected) {
        if (selected) {
            button.setBackgroundTintList(getResources().getColorStateList(android.R.color.black, null));
            button.setTextColor(getResources().getColor(android.R.color.white, null));
        } else {
            // Use white background for unselected, assuming XML provides the stroke for MaterialButton
            button.setBackgroundTintList(getResources().getColorStateList(android.R.color.white, null));
            button.setTextColor(getResources().getColor(android.R.color.black, null));
        }
    }

    private void restoreButtonStates() {
        for (MaterialButton button : categoryButtons) {
            if (button != null) highlightButton(button, selectedCategories.contains(button.getText().toString().trim()));
        }
        for (MaterialButton button : seasonButtons) {
            if (button != null) highlightButton(button, selectedSeasons.contains(button.getText().toString().trim()));
        }
        for (MaterialButton button : styleButtons) {
            if (button != null) highlightButton(button, selectedStyles.contains(button.getText().toString().trim()));
        }
    }

    private void updateAllText(TextView txt, List<String> selected) {
        if (selected.isEmpty()) {
            txt.setText("All");
        } else {
            // Updated to show a brief summary if too many are selected
            String display = String.join(", ", selected);
            if (display.length() > 30 && selected.size() > 1) {
                display = selected.size() + " selected";
            }
            txt.setText(display);
        }
    }

    // --- Preset selections ---
    public void setSelectedCategories(List<String> categories) {
        selectedCategories.clear();
        if (categories != null) selectedCategories.addAll(categories);
    }

    public void setSelectedSeasons(List<String> seasons) {
        selectedSeasons.clear();
        if (seasons != null) selectedSeasons.addAll(seasons);
    }

    public void setSelectedStyles(List<String> styles) {
        selectedStyles.clear();
        if (styles != null) selectedStyles.addAll(styles);
    }
}
