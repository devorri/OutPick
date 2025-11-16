package com.example.outpick.admin;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.outpick.outfits.OutfitSuggestionActivity;
import com.example.outpick.R;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.dialogs.StyleBottomSheetDialog;
import com.example.outpick.utils.ImageUploader;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminAddOutfitDetailsActivity extends AppCompatActivity {

    private EditText etOutfitName, etDescription;
    private MaterialButton saveButton;
    private ImageButton backButton;
    private TextView allTextView;
    private Button maleButton, femaleButton;

    private String cloudImageUrl; // Cloud storage URL
    private Uri localSnapshotUri; // Local URI as backup

    private String selectedGender = "";

    // Multi-select lists
    private final ArrayList<String> selectedStyles = new ArrayList<>();
    private final ArrayList<String> selectedEvents = new ArrayList<>();
    private final ArrayList<String> selectedSeasons = new ArrayList<>();

    // Style buttons
    private Button btnCasual, btnSporty, btnCozy, btnStreetstyle, btnSmartcasual,
            btnSmart, btnClassic, btnFestive, btnFormal;

    // Season buttons
    private Button btnElNiño, btnLaNiña;

    private SupabaseService supabaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_outfit_details);

        // Initialize Supabase service
        supabaseService = SupabaseClient.getService();

        // UI Init
        etOutfitName = findViewById(R.id.etOutfitName);
        etDescription = findViewById(R.id.etDescription);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);
        allTextView = findViewById(R.id.allTextView);
        maleButton = findViewById(R.id.btnMale);
        femaleButton = findViewById(R.id.btnFemale);

        // Style buttons
        btnCasual = findViewById(R.id.btnCasual);
        btnSporty = findViewById(R.id.btnSporty);
        btnCozy = findViewById(R.id.btnCozy);
        btnStreetstyle = findViewById(R.id.btnStreetstyle);
        btnSmartcasual = findViewById(R.id.btnSmartcasual);
        btnSmart = findViewById(R.id.btnSmart);
        btnClassic = findViewById(R.id.btnClassic);
        btnFestive = findViewById(R.id.btnFestive);
        btnFormal = findViewById(R.id.btnFormal);

        // Season buttons (updated)
        btnElNiño = findViewById(R.id.btnElNiño);
        btnLaNiña = findViewById(R.id.btnLaNiña);

        // Retrieve image URLs - prefer cloud URL, fallback to local URI
        cloudImageUrl = getIntent().getStringExtra("snapshotImageUrl");
        String localUriString = getIntent().getStringExtra("snapshotImageUri");
        if (localUriString != null) {
            localSnapshotUri = Uri.parse(localUriString);
        }

        // Back button
        backButton.setOnClickListener(v -> finish());

        // Gender selection
        maleButton.setOnClickListener(v -> selectGender("Male"));
        femaleButton.setOnClickListener(v -> selectGender("Female"));

        // Event buttons
        setupEventButton(findViewById(R.id.btnWorkformHome), "Work from Home");
        setupEventButton(findViewById(R.id.btnWorkout), "Workout");
        setupEventButton(findViewById(R.id.btnOffice), "Office");
        setupEventButton(findViewById(R.id.btnDating), "Dating");
        setupEventButton(findViewById(R.id.btnDailyroutine), "Daily Routine");
        setupEventButton(findViewById(R.id.btnRelaxingathome), "Relaxing at Home");
        setupEventButton(findViewById(R.id.btnParty), "Party");
        setupEventButton(findViewById(R.id.btnTravelling), "Travelling");
        setupEventButton(findViewById(R.id.btnBeach), "Beach");

        // Season buttons
        setupSeasonButton(btnElNiño, "El Niño");
        setupSeasonButton(btnLaNiña, "La Niña");

        // Style buttons
        setupStyleToggle(btnCasual, "Casual");
        setupStyleToggle(btnSporty, "Sporty");
        setupStyleToggle(btnCozy, "Cozy");
        setupStyleToggle(btnStreetstyle, "Streetstyle");
        setupStyleToggle(btnSmartcasual, "Smart Casual");
        setupStyleToggle(btnSmart, "Smart");
        setupStyleToggle(btnClassic, "Classic");
        setupStyleToggle(btnFestive, "Festive");
        setupStyleToggle(btnFormal, "Formal");

        // Initialize "All" text
        refreshAllText();

        // "All" text click → bottom sheet
        allTextView.setOnClickListener(v -> {
            StyleBottomSheetDialog dialog = new StyleBottomSheetDialog(
                    styles -> {
                        selectedStyles.clear();
                        selectedStyles.addAll(styles);
                        updateStyleButtons();
                        refreshAllText();
                    },
                    new HashSet<>(selectedStyles)
            );
            dialog.show(getSupportFragmentManager(), "StyleBottomSheet");
        });

        // Save button
        saveButton.setOnClickListener(v -> saveOutfitDetails());
    }

    /** Gender selection */
    private void selectGender(String gender) {
        selectedGender = gender;
        if (gender.equals("Male")) {
            maleButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
            maleButton.setTextColor(Color.WHITE);
            femaleButton.setBackgroundColor(Color.WHITE);
            femaleButton.setTextColor(Color.BLACK);
        } else {
            femaleButton.setBackgroundColor(Color.parseColor("#FF69B4"));
            femaleButton.setTextColor(Color.WHITE);
            maleButton.setBackgroundColor(Color.WHITE);
            maleButton.setTextColor(Color.BLACK);
        }
    }

    /** Toggle button logic for Events */
    private void setupEventButton(final Button button, final String eventName) {
        button.setOnClickListener(v -> {
            boolean selected = button.isSelected();
            button.setSelected(!selected);
            if (!selected) {
                button.setBackgroundColor(Color.BLACK);
                button.setTextColor(Color.WHITE);
                if (!selectedEvents.contains(eventName)) selectedEvents.add(eventName);
            } else {
                button.setBackgroundColor(Color.WHITE);
                button.setTextColor(Color.BLACK);
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
                button.setBackgroundColor(Color.BLACK);
                button.setTextColor(Color.WHITE);
                if (!selectedSeasons.contains(seasonName)) selectedSeasons.add(seasonName);
            } else {
                button.setBackgroundColor(Color.WHITE);
                button.setTextColor(Color.BLACK);
                selectedSeasons.remove(seasonName);
            }
        });
    }

    /** Toggle button logic for Styles */
    private void setupStyleToggle(final Button button, final String styleName) {
        button.setOnClickListener(v -> {
            boolean selected = button.isSelected();
            button.setSelected(!selected);
            if (!selected) {
                button.setBackgroundColor(Color.BLACK);
                button.setTextColor(Color.WHITE);
                if (!selectedStyles.contains(styleName)) selectedStyles.add(styleName);
            } else {
                button.setBackgroundColor(Color.WHITE);
                button.setTextColor(Color.BLACK);
                selectedStyles.remove(styleName);
            }
            refreshAllText();
        });
    }

    /** Update "All" text */
    private void refreshAllText() {
        if (selectedStyles.isEmpty()) {
            allTextView.setText("All");
        } else if (selectedStyles.size() == 1) {
            allTextView.setText(selectedStyles.get(0));
        } else {
            allTextView.setText(TextUtils.join(", ", selectedStyles));
        }
    }

    /** Sync style buttons after bottom sheet */
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

    private void syncButton(Button button, String styleName) {
        if (selectedStyles.contains(styleName)) {
            button.setSelected(true);
            button.setBackgroundColor(Color.BLACK);
            button.setTextColor(Color.WHITE);
        } else {
            button.setSelected(false);
            button.setBackgroundColor(Color.WHITE);
            button.setTextColor(Color.BLACK);
        }
    }

    /** Save outfit details to Supabase */
    private void saveOutfitDetails() {
        String outfitName = etOutfitName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Validation
        if (outfitName.isEmpty()) {
            Toast.makeText(this, "Please enter an outfit name.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedGender.isEmpty()) {
            Toast.makeText(this, "Please select a gender.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedStyles.isEmpty()) {
            Toast.makeText(this, "Please select at least one style.", Toast.LENGTH_SHORT).show();
            return;
        }

        // If we don't have a cloud URL but have a local URI, upload it first
        if (cloudImageUrl == null && localSnapshotUri != null) {
            uploadLocalImageThenSave(outfitName, description);
        } else {
            // We already have a cloud URL or no image at all
            saveOutfitToDatabase(outfitName, description);
        }
    }

    /** Upload local image to cloud storage, then save outfit */
    private void uploadLocalImageThenSave(String outfitName, String description) {
        saveButton.setEnabled(false);
        saveButton.setText("Uploading Image...");

        ImageUploader uploader = new ImageUploader(this);
        String fileName = "outfit_" + System.currentTimeMillis() + ".jpg";

        uploader.uploadImage(localSnapshotUri, "outfits", fileName, new ImageUploader.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                cloudImageUrl = imageUrl; // Store the cloud URL
                saveOutfitToDatabase(outfitName, description);
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(AdminAddOutfitDetailsActivity.this,
                            "Failed to upload image: " + error, Toast.LENGTH_LONG).show();
                    saveButton.setEnabled(true);
                    saveButton.setText("Save Outfit");

                    // Fallback: try to save with local URI
                    saveOutfitToDatabase(outfitName, description);
                });
            }
        });
    }

    /** Save outfit data to Supabase database */
    private void saveOutfitToDatabase(String outfitName, String description) {
        runOnUiThread(() -> {
            saveButton.setText("Saving Outfit...");
        });

        String styles = TextUtils.join(", ", selectedStyles);
        String events = TextUtils.join(", ", selectedEvents);
        String seasons = TextUtils.join(", ", selectedSeasons);

        // Use cloud URL if available, otherwise fallback to local URI
        String finalImageUrl = cloudImageUrl;
        if (finalImageUrl == null && localSnapshotUri != null) {
            finalImageUrl = localSnapshotUri.toString();
        }

        if (finalImageUrl == null) {
            Toast.makeText(this, "No outfit image available!", Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            saveButton.setText("Save Outfit");
            return;
        }

        // Create JsonObject for Supabase
        JsonObject outfitJson = new JsonObject();
        outfitJson.addProperty("image_uri", finalImageUrl); // This is now a cloud URL or local URI
        outfitJson.addProperty("name", outfitName);
        outfitJson.addProperty("description", description);
        outfitJson.addProperty("gender", selectedGender);
        outfitJson.addProperty("events", events);
        outfitJson.addProperty("seasons", seasons);
        outfitJson.addProperty("styles", styles);
        outfitJson.addProperty("created_at", new java.util.Date().toString());

        // Save to Supabase
        Call<JsonObject> call = supabaseService.insertOutfit(outfitJson);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(AdminAddOutfitDetailsActivity.this,
                                "Outfit saved successfully!", Toast.LENGTH_SHORT).show();

                        // Return to OutfitSuggestionActivity
                        Intent intent = new Intent(AdminAddOutfitDetailsActivity.this, OutfitSuggestionActivity.class);
                        intent.putExtra("username", getIntent().getStringExtra("username"));
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(AdminAddOutfitDetailsActivity.this,
                                "Failed to save outfit: " + response.message(), Toast.LENGTH_SHORT).show();
                        saveButton.setEnabled(true);
                        saveButton.setText("Save Outfit");
                    }
                });
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(AdminAddOutfitDetailsActivity.this,
                            "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true);
                    saveButton.setText("Save Outfit");
                });
            }
        });
    }
}