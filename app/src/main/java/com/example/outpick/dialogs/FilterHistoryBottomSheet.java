package com.example.outpick.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.outpick.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilterHistoryBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_QUERY = "arg_query";
    private static final String ARG_STYLES = "arg_styles";
    private static final String ARG_EVENTS = "arg_events";
    private static final String ARG_SEASONS = "arg_seasons";

    private String lastQuery;
    private Set<String> selectedStyles = new HashSet<>();
    private Set<String> selectedEvents = new HashSet<>();
    private Set<String> selectedSeasons = new HashSet<>();

    private EditText etSearch;
    private LinearLayout searchContainer;
    private TextView allTextView;
    private MaterialButton btnApply;
    private ImageView btnBack;

    public interface OnFilterAppliedListener {
        void onFilterApplied(String query,
                             Set<String> selectedStyles,
                             Set<String> selectedEvents,
                             Set<String> selectedSeasons);
    }

    private OnFilterAppliedListener listener;

    public static FilterHistoryBottomSheet newInstance(String query,
                                                       Set<String> styles,
                                                       Set<String> events,
                                                       Set<String> seasons) {
        FilterHistoryBottomSheet fragment = new FilterHistoryBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        args.putStringArrayList(ARG_STYLES, styles == null ? null : new ArrayList<>(styles));
        args.putStringArrayList(ARG_EVENTS, events == null ? null : new ArrayList<>(events));
        args.putStringArrayList(ARG_SEASONS, seasons == null ? null : new ArrayList<>(seasons));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFilterAppliedListener) {
            listener = (OnFilterAppliedListener) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            lastQuery = getArguments().getString(ARG_QUERY);

            List<String> styles = getArguments().getStringArrayList(ARG_STYLES);
            selectedStyles = styles == null ? new HashSet<>() : new HashSet<>(styles);

            List<String> events = getArguments().getStringArrayList(ARG_EVENTS);
            selectedEvents = events == null ? new HashSet<>() : new HashSet<>(events);

            List<String> seasons = getArguments().getStringArrayList(ARG_SEASONS);
            selectedSeasons = seasons == null ? new HashSet<>() : new HashSet<>(seasons);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_filter_history, container, false);

        etSearch = view.findViewById(R.id.etSearch);
        searchContainer = view.findViewById(R.id.searchContainer);
        allTextView = view.findViewById(R.id.allTextView);
        btnApply = view.findViewById(R.id.btnApply);
        btnBack = view.findViewById(R.id.btnBack);

        if (lastQuery != null) {
            etSearch.setText(lastQuery);
        }

        // Toggle search bar visibility
        ImageView btnSearch = view.findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> {
            if (searchContainer.getVisibility() == View.GONE) searchContainer.setVisibility(View.VISIBLE);
            else searchContainer.setVisibility(View.GONE);
        });

        // --- Style buttons ---
        setupToggleButton(view, R.id.btnCasual, "Casual", selectedStyles);
        setupToggleButton(view, R.id.btnSporty, "Sporty", selectedStyles);
        setupToggleButton(view, R.id.btnCozy, "Cozy", selectedStyles);
        setupToggleButton(view, R.id.btnStreetstyle, "Streetstyle", selectedStyles);
        setupToggleButton(view, R.id.btnSmartcasual, "Smart Casual", selectedStyles);

        // --- Event buttons (fixed IDs to match XML) ---
        setupToggleButton(view, R.id.btnWorkformHome, "Work from Home", selectedEvents);
        setupToggleButton(view, R.id.btnWorkout, "Workout", selectedEvents);
        setupToggleButton(view, R.id.btnOffice, "Office", selectedEvents);
        setupToggleButton(view, R.id.btnDating, "Dating", selectedEvents);
        setupToggleButton(view, R.id.btnDailyroutine, "Daily Routine", selectedEvents);
        setupToggleButton(view, R.id.btnFormal, "Formal", selectedEvents);
        setupToggleButton(view, R.id.btnRelaxingathome, "Relaxing at Home", selectedEvents);
        setupToggleButton(view, R.id.btnParty, "Party", selectedEvents);
        setupToggleButton(view, R.id.btnTravelling, "Travelling", selectedEvents);
        setupToggleButton(view, R.id.btnBeach, "Beach", selectedEvents);

        // --- Season buttons ---
        setupToggleButton(view, R.id.btnElNino, "El Niño", selectedSeasons);
        setupToggleButton(view, R.id.btnLaNina, "La Niña", selectedSeasons);
        // Update "All" text for styles
        allTextView.setText(selectedStyles.isEmpty() ? "All" : TextUtils.join(", ", selectedStyles));

        // Apply button
        btnApply.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (listener != null) {
                listener.onFilterApplied(query,
                        new HashSet<>(selectedStyles),
                        new HashSet<>(selectedEvents),
                        new HashSet<>(selectedSeasons));
            }
            dismiss();
        });

        // Back button
        btnBack.setOnClickListener(v -> dismiss());

        // Open StyleBottomSheet when clicking "All"
        LinearLayout layoutStyleAll = view.findViewById(R.id.layoutStyleAll);
        layoutStyleAll.setOnClickListener(v -> {
            Set<String> pre = new HashSet<>(selectedStyles);
            StyleBottomSheetDialog dialog = new StyleBottomSheetDialog(
                    styles -> {
                        allTextView.setText(styles.isEmpty() ? "All" : TextUtils.join(", ", styles));
                        selectedStyles.clear();
                        selectedStyles.addAll(styles);
                        updateInlineButtons(view);
                    }, pre
            );
            dialog.show(getParentFragmentManager(), "StyleBottomSheet");
        });

        return view;
    }

    private void setupToggleButton(View parentView, int buttonId, String key, Set<String> set) {
        MaterialButton button = parentView.findViewById(buttonId);
        if (button == null) return;

        if (set.contains(key)) setButtonSelected(button, true);

        button.setOnClickListener(v -> {
            if (set.contains(key)) {
                set.remove(key);
                setButtonSelected(button, false);
            } else {
                set.add(key);
                setButtonSelected(button, true);
            }

            if (set == selectedStyles) {
                allTextView.setText(set.isEmpty() ? "All" : TextUtils.join(", ", set));
            }
        });
    }

    private void setButtonSelected(MaterialButton button, boolean selected) {
        if (selected) {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.black));
            button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        } else {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        }
    }

    private void updateInlineButtons(View parentView) {
        setButtonSelected(parentView.findViewById(R.id.btnCasual), selectedStyles.contains("Casual"));
        setButtonSelected(parentView.findViewById(R.id.btnSporty), selectedStyles.contains("Sporty"));
        setButtonSelected(parentView.findViewById(R.id.btnCozy), selectedStyles.contains("Cozy"));
        setButtonSelected(parentView.findViewById(R.id.btnStreetstyle), selectedStyles.contains("Streetstyle"));
        setButtonSelected(parentView.findViewById(R.id.btnSmartcasual), selectedStyles.contains("Smart Casual"));
    }
}
