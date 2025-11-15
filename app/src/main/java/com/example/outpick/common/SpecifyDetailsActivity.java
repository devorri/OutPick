package com.example.outpick.common;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.outpick.R;
import com.example.outpick.dialogs.StyleBottomSheetDialog;
import com.example.outpick.outfits.OutfitCombinationActivity;
import com.example.outpick.outfits.OutfitCreationActivity;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;

public class SpecifyDetailsActivity extends AppCompatActivity {

    private EditText etOutfitName;
    private MaterialButton saveButton;
    private ImageView backButton;
    private TextView allTextView;

    private byte[] snapshot;
    private Uri snapshotUri;

    // Multi-select lists
    private final ArrayList<String> selectedStyles = new ArrayList<>();
    private final ArrayList<String> selectedEvents = new ArrayList<>();
    private final ArrayList<String> selectedSeasons = new ArrayList<>();

    // Buttons references for syncing with All Text (only style buttons need to be fields)
    private Button btnCasual, btnSporty, btnCozy, btnStreetstyle, btnSmartcasual,
            btnSmart, btnClassic, btnFestive, btnFormal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_specify_details);

        // UI Initialization
        etOutfitName = findViewById(R.id.etOutfitName);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);
        allTextView = findViewById(R.id.allTextView);

        // Style buttons (these are kept as they are needed for syncing)
        btnCasual = findViewById(R.id.btnCasual);
        btnSporty = findViewById(R.id.btnSporty);
        btnCozy = findViewById(R.id.btnCozy);
        btnStreetstyle = findViewById(R.id.btnStreetstyle);
        btnSmartcasual = findViewById(R.id.btnSmartcasual);
        btnSmart = findViewById(R.id.btnSmart);
        btnClassic = findViewById(R.id.btnClassic);
        btnFestive = findViewById(R.id.btnFestive);
        btnFormal = findViewById(R.id.btnFormal);

        // Snapshot from previous activity
        snapshot = getIntent().getByteArrayExtra("snapshot");
        String uriString = getIntent().getStringExtra("snapshotImageUri");
        if (uriString != null) snapshotUri = Uri.parse(uriString);

        // Back button functionality
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(SpecifyDetailsActivity.this, OutfitCreationActivity.class);
            String username = getIntent().getStringExtra("username");
            if (username != null) intent.putExtra("username", username);
            startActivity(intent);
            finish();
        });

        // Event buttons setup (assuming these IDs are still present)
        setupEventButton(findViewById(R.id.btnWorkformHome), "Work from Home");
        setupEventButton(findViewById(R.id.btnWorkout), "Workout");
        setupEventButton(findViewById(R.id.btnOffice), "Office");
        setupEventButton(findViewById(R.id.btnDating), "Dating");
        setupEventButton(findViewById(R.id.btnDailyroutine), "Daily Routine");
        setupEventButton(findViewById(R.id.btnRelaxingathome), "Relaxing at Home");
        setupEventButton(findViewById(R.id.btnParty), "Party");
        setupEventButton(findViewById(R.id.btnTravelling), "Travelling");
        setupEventButton(findViewById(R.id.btnBeach), "Beach");

        // Season buttons setup - Only including IDs present in the layout
        setupSeasonButton(findViewById(R.id.btnSummer), "Summer");
        setupSeasonButton(findViewById(R.id.btnRainy), "Rainy");

        // Style buttons setup
        setupStyleToggle(btnCasual, "Casual");
        setupStyleToggle(btnSporty, "Sporty");
        setupStyleToggle(btnCozy, "Cozy");
        setupStyleToggle(btnStreetstyle, "Streetstyle");
        setupStyleToggle(btnSmartcasual, "Smart Casual");
        setupStyleToggle(btnSmart, "Smart");
        setupStyleToggle(btnClassic, "Classic");
        setupStyleToggle(btnFestive, "Festive");
        setupStyleToggle(btnFormal, "Formal");

        // Initialize All Text
        refreshAllText();

        // Listener for the "All" TextView to open the bottom sheet
        allTextView.setOnClickListener(v -> {
            // Note: StyleBottomSheetDialog class is assumed to be implemented elsewhere.
            StyleBottomSheetDialog dialog = new StyleBottomSheetDialog(
                    styles -> {
                        selectedStyles.clear();
                        selectedStyles.addAll(styles);

                        // Update buttons based on selection from the dialog
                        updateStyleButtons();

                        // Refresh All text display
                        refreshAllText();
                    },
                    new HashSet<>(selectedStyles) // pre-selected styles
            );
            dialog.show(getSupportFragmentManager(), "StyleBottomSheet");
        });

        // Save button functionality
        saveButton.setOnClickListener(v -> {
            String outfitName = etOutfitName.getText().toString().trim();
            if (TextUtils.isEmpty(outfitName)) {
                Toast.makeText(this, "Please enter an outfit name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Prepare data for the next activity
            String eventStr = selectedEvents.isEmpty() ? "" : TextUtils.join(", ", selectedEvents);
            String seasonStr = selectedSeasons.isEmpty() ? "" : TextUtils.join(", ", selectedSeasons);
            String styleStr = selectedStyles.isEmpty() ? "" : TextUtils.join(", ", selectedStyles);

            Intent intent = new Intent(SpecifyDetailsActivity.this, OutfitCombinationActivity.class);
            if (snapshot != null) intent.putExtra("snapshot", snapshot);
            else if (snapshotUri != null) intent.putExtra("snapshotImageUri", snapshotUri.toString());

            intent.putExtra("outfitName", outfitName);
            intent.putExtra("event", eventStr);
            intent.putExtra("season", seasonStr);
            intent.putExtra("style", styleStr);

            String username = getIntent().getStringExtra("username");
            if (username != null) intent.putExtra("username", username);

            startActivity(intent);
            finish();
        });
    }

    /** Toggle button logic for Events */
    private void setupEventButton(final Button button, final String eventName) {
        button.setOnClickListener(v -> {
            boolean selected = button.isSelected();
            button.setSelected(!selected);
            if (!selected) {
                // Select state: Black background, White text
                button.setBackgroundColor(getResources().getColor(android.R.color.black));
                button.setTextColor(getResources().getColor(android.R.color.white));
                if (!selectedEvents.contains(eventName)) selectedEvents.add(eventName);
            } else {
                // Deselect state: White background, Black text
                button.setBackgroundColor(getResources().getColor(android.R.color.white));
                button.setTextColor(getResources().getColor(android.R.color.black));
                selectedEvents.remove(eventName);
            }
        });
    }

    /** Toggle button logic for Seasons */
    private void setupSeasonButton(final Button button, final String seasonName) {
        button.setOnClickListener(v -> {
            boolean selected = button.isSelected();
            button.setSelected(!selected);
            if (!selected) {
                // Select state: Black background, White text
                button.setBackgroundColor(getResources().getColor(android.R.color.black));
                button.setTextColor(getResources().getColor(android.R.color.white));
                if (!selectedSeasons.contains(seasonName)) selectedSeasons.add(seasonName);
            } else {
                // Deselect state: White background, Black text
                button.setBackgroundColor(getResources().getColor(android.R.color.white));
                button.setTextColor(getResources().getColor(android.R.color.black));
                selectedSeasons.remove(seasonName);
            }
        });
    }

    /** Toggle button logic for Styles (updates All Text) */
    private void setupStyleToggle(final Button button, final String styleName) {
        button.setOnClickListener(v -> {
            boolean selected = button.isSelected();
            button.setSelected(!selected);

            if (!selected) {
                // Select state: Black background, White text
                button.setBackgroundColor(getResources().getColor(android.R.color.black));
                button.setTextColor(getResources().getColor(android.R.color.white));
                if (!selectedStyles.contains(styleName)) selectedStyles.add(styleName);
            } else {
                // Deselect state: White background, Black text
                button.setBackgroundColor(getResources().getColor(android.R.color.white));
                button.setTextColor(getResources().getColor(android.R.color.black));
                selectedStyles.remove(styleName);
            }

            // Update All text dynamically
            refreshAllText();
        });
    }

    /** Update All text display based on selectedStyles list */
    private void refreshAllText() {
        if (selectedStyles.isEmpty()) {
            allTextView.setText("All");
        } else if (selectedStyles.size() == 1) {
            allTextView.setText(selectedStyles.get(0));
        } else {
            // Join multiple selected styles with a comma and space
            allTextView.setText(TextUtils.join(", ", selectedStyles));
        }
    }

    /** Sync style buttons state to the currently selected styles list, used after bottom sheet dialog closes */
    private void updateStyleButtons() {
        syncButton(btnCasual, "Casual");
        syncButton(btnSporty, "Sporty");
        syncButton(btnCozy, "Cozy");
        syncButton(btnStreetstyle, "Streetstyle");
        syncButton(btnSmartcasual, "Smart Casual");
        syncButton(btnSmart, "Smart");
        syncButton(btnClassic, "Classic");
        syncButton(btnFestive, "Festive");
        syncButton(btnFormal, "Formal");
    }

    /** Helper function to set the state of a single style button */
    private void syncButton(Button button, String styleName) {
        if (selectedStyles.contains(styleName)) {
            button.setSelected(true);
            button.setBackgroundColor(getResources().getColor(android.R.color.black));
            button.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            button.setSelected(false);
            button.setBackgroundColor(getResources().getColor(android.R.color.white));
            button.setTextColor(getResources().getColor(android.R.color.black));
        }
    }
}
