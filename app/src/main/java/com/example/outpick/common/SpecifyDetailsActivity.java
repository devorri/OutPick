package com.example.outpick.common;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.outpick.R;
import com.example.outpick.database.repositories.ClosetSnapshotRepository;
import com.example.outpick.database.repositories.OutfitRepository;
import com.example.outpick.database.repositories.UserOutfitRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.dialogs.StyleBottomSheetDialog;
import com.example.outpick.outfits.OutfitCombinationActivity;
import com.example.outpick.outfits.OutfitCreationActivity;
import com.example.outpick.utils.ImageUploader;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SpecifyDetailsActivity extends AppCompatActivity {

    private static final String TAG = "SpecifyDetailsActivity";

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

    // Supabase services
    private SupabaseService supabaseService;
    private OutfitRepository outfitRepository;
    private UserOutfitRepository userOutfitRepository;
    private ClosetSnapshotRepository closetSnapshotRepository;
    private ImageUploader imageUploader;
    private String currentUserId;

    // Closet selection
    private String selectedClosetId;
    private String selectedClosetName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_specify_details);

        // Initialize Supabase services
        supabaseService = SupabaseClient.getService();
        outfitRepository = new OutfitRepository(supabaseService);
        userOutfitRepository = new UserOutfitRepository(supabaseService, outfitRepository);
        closetSnapshotRepository = new ClosetSnapshotRepository(supabaseService);
        imageUploader = new ImageUploader(this);

        // Get current user ID
        currentUserId = getCurrentUserId();

        // Get selected closet from intent (if coming from OutfitCreationActivity)
        selectedClosetId = getIntent().getStringExtra("selected_closet_id");
        selectedClosetName = getIntent().getStringExtra("selected_closet_name");

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

        // Update save button text if closet is selected
        if (selectedClosetName != null) {
            saveButton.setText("Save to " + selectedClosetName);
        }

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

            // Prepare data
            String eventStr = selectedEvents.isEmpty() ? "" : TextUtils.join(", ", selectedEvents);
            String seasonStr = selectedSeasons.isEmpty() ? "" : TextUtils.join(", ", selectedSeasons);
            String styleStr = selectedStyles.isEmpty() ? "" : TextUtils.join(", ", selectedStyles);

            // Upload image and save outfit
            if (snapshot != null || snapshotUri != null) {
                uploadAndSaveOutfit(outfitName, eventStr, seasonStr, styleStr);
            } else {
                Toast.makeText(this, "No outfit image found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * FIXED: Use .jpg extension to match your Supabase bucket
     */
    private void uploadAndSaveOutfit(String outfitName, String event, String season, String style) {
        Toast.makeText(this, "Uploading outfit to cloud...", Toast.LENGTH_SHORT).show();

        // ✅ FIXED: Use .jpg extension to match your Supabase bucket
        String fileName = "outfit_" + System.currentTimeMillis() + ".jpg";

        if (snapshot != null) {
            // Upload byte array directly using the new method
            imageUploader.uploadImage(snapshot, "outfits", fileName, new ImageUploader.UploadCallback() {
                @Override
                public void onSuccess(String cloudImageUrl) {
                    saveOutfitWithCloset(cloudImageUrl, outfitName, event, season, style);
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(SpecifyDetailsActivity.this,
                                "Failed to upload outfit: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        } else if (snapshotUri != null) {
            // Upload from URI (existing method)
            uploadImageAndSave(snapshotUri, outfitName, event, season, style);
        }
    }

    private void uploadAndSaveOutfitFromUri(String outfitName, String event, String season, String style) {
        uploadImageAndSave(snapshotUri, outfitName, event, season, style);
    }

    /**
     * Upload image and save outfit with closet assignment
     */
    private void uploadImageAndSave(Uri imageUri, String name, String event, String season, String style) {
        Toast.makeText(this, "Uploading outfit to cloud...", Toast.LENGTH_SHORT).show();

        // ✅ FIXED: Use .jpg extension to match your Supabase bucket
        String fileName = "outfit_" + System.currentTimeMillis() + ".jpg";

        imageUploader.uploadImage(imageUri, "outfits", fileName, new ImageUploader.UploadCallback() {
            @Override
            public void onSuccess(String cloudImageUrl) {
                // Save outfit to database and assign to closet
                saveOutfitWithCloset(cloudImageUrl, name, event, season, style);
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SpecifyDetailsActivity.this,
                            "Failed to upload outfit: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Save outfit to database and assign to selected closet
     */
    private void saveOutfitWithCloset(String imageUrl, String name, String event, String season, String style) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Saving outfit to database...");
                Log.d(TAG, "Image URL: " + imageUrl);
                Log.d(TAG, "Name: " + name);

                // Save outfit to outfits table
                boolean outfitAdded = outfitRepository.addOutfit(
                        imageUrl,
                        name != null ? name : "New Outfit",
                        "General",
                        "",
                        "Unisex",
                        event != null ? event : "Casual",
                        season != null ? season : "All-Season",
                        style != null ? style : "Casual"
                );

                Log.d(TAG, "Outfit added to outfits table: " + outfitAdded);

                if (outfitAdded && currentUserId != null) {
                    // Get the newly created outfit ID
                    List<com.example.outpick.database.models.Outfit> allOutfits = outfitRepository.getAllOutfits();
                    Log.d(TAG, "Total outfits in database: " + allOutfits.size());

                    String newOutfitId = null;
                    for (com.example.outpick.database.models.Outfit outfit : allOutfits) {
                        Log.d(TAG, "Checking outfit: " + outfit.getName() + " - " + outfit.getImageUri());
                        if (outfit.getImageUri().equals(imageUrl)) {
                            newOutfitId = outfit.getId();
                            Log.d(TAG, "Found matching outfit with ID: " + newOutfitId);
                            break;
                        }
                    }

                    if (newOutfitId != null) {
                        // Assign outfit to user
                        boolean assigned = userOutfitRepository.assignOutfitToUser(
                                newOutfitId,
                                currentUserId,
                                "self"
                        );

                        Log.d(TAG, "Outfit assigned to user: " + assigned);

                        // ✅ ADD TO SELECTED CLOSET
                        if (selectedClosetId != null && assigned) {
                            boolean addedToCloset = closetSnapshotRepository.addOutfitToCloset(selectedClosetId, imageUrl);

                            runOnUiThread(() -> {
                                if (addedToCloset) {
                                    Toast.makeText(SpecifyDetailsActivity.this,
                                            "✅ Outfit saved to " + selectedClosetName + "!", Toast.LENGTH_SHORT).show();

                                    // Go to OutfitCombinationActivity
                                    navigateToOutfitCombination();
                                } else {
                                    Toast.makeText(SpecifyDetailsActivity.this,
                                            "Outfit saved but failed to add to closet", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(SpecifyDetailsActivity.this,
                                        "Outfit saved successfully!", Toast.LENGTH_SHORT).show();
                                navigateToOutfitCombination();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(SpecifyDetailsActivity.this,
                                    "Failed to save outfit - could not find outfit ID", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(SpecifyDetailsActivity.this,
                                "Failed to save outfit to database", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving outfit: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(SpecifyDetailsActivity.this,
                            "Error saving outfit: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Navigate to OutfitCombinationActivity
     */
    private void navigateToOutfitCombination() {
        Intent intent = new Intent(SpecifyDetailsActivity.this, OutfitCombinationActivity.class);
        String username = getIntent().getStringExtra("username");
        if (username != null) intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }

    /**
     * Get current user ID from SharedPreferences
     */
    private String getCurrentUserId() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        if (userId.isEmpty()) {
            Log.e(TAG, "No user ID found in SharedPreferences!");
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
        return userId;
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