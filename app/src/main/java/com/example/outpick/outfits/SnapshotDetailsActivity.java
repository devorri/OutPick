package com.example.outpick.outfits;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.outpick.R;
import com.example.outpick.database.models.Outfit;
import com.example.outpick.database.repositories.OutfitRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SnapshotDetailsActivity extends AppCompatActivity {

    private EditText etOutfitName;

    private ImageView ivSnapshot, eventArrow, seasonArrow, styleArrow;
    private ImageView btnClearEvent, btnClearSeason, btnClearStyle;
    private TextView tvEvent, tvSeason, tvStyle;
    private LinearLayout eventToggleLayout, seasonToggleLayout, styleToggleLayout;
    private FlexboxLayout eventLayout, seasonLayout, styleLayout;
    private MaterialButton btnSave, btnUse;

    private Set<String> selectedEvents = new HashSet<>();
    private Set<String> selectedSeasons = new HashSet<>();
    private Set<String> selectedStyles = new HashSet<>();

    private Set<String> originalEvents = new HashSet<>();
    private Set<String> originalSeasons = new HashSet<>();
    private Set<String> originalStyles = new HashSet<>();

    private String originalOutfitName = "";

    private String snapshotId = "";
    private SupabaseService supabaseService;
    private OutfitRepository outfitRepository;

    private static final int MAX_DISPLAY_ITEMS = 3;
    private static final int CHIP_BACKGROUND_UNSELECTED = R.drawable.chip_background;
    private static final int CHIP_BACKGROUND_SELECTED = R.drawable.selected_chip_border;
    private static final int PLACEHOLDER_IMAGE = R.drawable.placeholder_image;
    private static final int ERROR_IMAGE = R.drawable.error_image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snapshot_details);

        // Initialize Supabase
        supabaseService = SupabaseClient.getService();
        outfitRepository = new OutfitRepository(supabaseService);

        snapshotId = getIntent().getStringExtra("snapshotId");

        initViews();
        loadSnapshotData();
        setupToggleLayouts();
        setupClearButtons();
        setupSaveButton();
        setupUseButton();
        updateUseButtonText();

        // Back button logic
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
    }

    private void initViews() {
        ivSnapshot = findViewById(R.id.ivSnapshot);
        etOutfitName = findViewById(R.id.tvOutfitName);
        tvEvent = findViewById(R.id.selectedEventText);
        tvSeason = findViewById(R.id.selectedSeasonText);
        tvStyle = findViewById(R.id.selectedStyleText);

        eventToggleLayout = findViewById(R.id.eventToggleLayout);
        seasonToggleLayout = findViewById(R.id.seasonToggleLayout);
        styleToggleLayout = findViewById(R.id.styleToggleLayout);

        eventArrow = findViewById(R.id.eventArrow);
        seasonArrow = findViewById(R.id.seasonArrow);
        styleArrow = findViewById(R.id.styleArrow);

        btnClearEvent = findViewById(R.id.btnClearEvent);
        btnClearSeason = findViewById(R.id.btnClearSeason);
        btnClearStyle = findViewById(R.id.btnClearStyle);

        eventLayout = findViewById(R.id.eventSubcategoryLayout);
        seasonLayout = findViewById(R.id.seasonSubcategoryLayout);
        styleLayout = findViewById(R.id.styleSubcategoryLayout);

        btnSave = findViewById(R.id.btnSave);
        btnUse = findViewById(R.id.btnUse);

        etOutfitName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(android.text.Editable s) {
                checkIfSelectionsChanged();
            }
        });
    }

    private void loadSnapshotData() {
        byte[] snapshot = getIntent().getByteArrayExtra("snapshot");
        String imagePath = getIntent().getStringExtra("path");
        String imageUri = getIntent().getStringExtra("imageUri"); // ADDED: Cloud URL support
        String outfitName = getIntent().getStringExtra("outfitName");
        String event = getIntent().getStringExtra("event");
        String season = getIntent().getStringExtra("season");
        String style = getIntent().getStringExtra("style");

        // Load image - UPDATED FOR CLOUD SUPPORT
        if (snapshot != null && snapshot.length > 0) {
            Bitmap bmp = BitmapFactory.decodeByteArray(snapshot, 0, snapshot.length);
            ivSnapshot.setImageBitmap(bmp);
        } else if (imageUri != null && !imageUri.isEmpty() && isCloudUrl(imageUri)) {
            // Load from cloud URL
            Glide.with(this).load(imageUri)
                    .placeholder(PLACEHOLDER_IMAGE)
                    .error(ERROR_IMAGE)
                    .into(ivSnapshot);
        } else if (imagePath != null && !imagePath.isEmpty()) {
            // Load from local file
            File file = new File(imagePath);
            if (file.exists()) {
                Glide.with(this).load(file)
                        .placeholder(PLACEHOLDER_IMAGE)
                        .error(ERROR_IMAGE)
                        .into(ivSnapshot);
            } else {
                try {
                    Uri uri = Uri.parse(imagePath);
                    Glide.with(this).load(uri)
                            .placeholder(PLACEHOLDER_IMAGE)
                            .error(ERROR_IMAGE)
                            .into(ivSnapshot);
                } catch (Exception e) {
                    ivSnapshot.setImageResource(PLACEHOLDER_IMAGE);
                }
            }
        } else {
            ivSnapshot.setImageResource(PLACEHOLDER_IMAGE);
        }

        originalOutfitName = outfitName != null ? outfitName : "";
        etOutfitName.setText(originalOutfitName);

        if (event != null && !event.isEmpty())
            selectedEvents = new HashSet<>(Arrays.asList(event.split(", ")));
        if (season != null && !season.isEmpty())
            selectedSeasons = new HashSet<>(Arrays.asList(season.split(", ")));
        if (style != null && !style.isEmpty())
            selectedStyles = new HashSet<>(Arrays.asList(style.split(", ")));

        originalEvents = new HashSet<>(selectedEvents);
        originalSeasons = new HashSet<>(selectedSeasons);
        originalStyles = new HashSet<>(selectedStyles);

        tvEvent.setText(formatLimitedText(selectedEvents));
        tvSeason.setText(formatLimitedText(selectedSeasons));
        tvStyle.setText(formatLimitedText(selectedStyles));

        preselectChips(eventLayout, selectedEvents);
        preselectChips(seasonLayout, selectedSeasons);
        preselectChips(styleLayout, selectedStyles);

        checkIfSelectionsChanged();
    }

    private boolean isCloudUrl(String imageUri) {
        return imageUri != null && (
                imageUri.startsWith("https://") ||
                        imageUri.startsWith("http://") ||
                        imageUri.contains("supabase.co/storage") ||
                        imageUri.contains("xaekxlyllgjxneyhurfp.supabase.co")
        );
    }

    private String formatLimitedText(Set<String> items) {
        if (items.isEmpty()) return "";
        String[] array = items.toArray(new String[0]);
        if (array.length <= MAX_DISPLAY_ITEMS) {
            return String.join(", ", array);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < MAX_DISPLAY_ITEMS; i++) {
                sb.append(array[i]);
                if (i != MAX_DISPLAY_ITEMS - 1) sb.append(", ");
            }
            sb.append(" +").append(array.length - MAX_DISPLAY_ITEMS).append(" more");
            return sb.toString();
        }
    }

    private void setupToggleLayouts() {
        setupToggle(eventToggleLayout, eventLayout, eventArrow, selectedEvents, tvEvent, btnClearEvent);
        setupToggle(seasonToggleLayout, seasonLayout, seasonArrow, selectedSeasons, tvSeason, btnClearSeason);
        setupToggle(styleToggleLayout, styleLayout, styleArrow, selectedStyles, tvStyle, btnClearStyle);
    }

    private void setupToggle(LinearLayout toggleLayout, FlexboxLayout subLayout, ImageView arrow,
                             Set<String> selectedSet, TextView displayText, ImageView clearButton) {

        clearButton.setVisibility(selectedSet.isEmpty() ? View.GONE : View.VISIBLE);

        toggleLayout.setOnClickListener(v -> {
            boolean isExpanded = subLayout.getVisibility() == View.VISIBLE;
            subLayout.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            arrow.setRotation(isExpanded ? 0f : 180f);
        });

        for (int i = 0; i < subLayout.getChildCount(); i++) {
            View view = subLayout.getChildAt(i);
            if (view instanceof TextView) {
                TextView chip = (TextView) view;
                chip.setOnClickListener(v -> {
                    String value = chip.getText().toString();
                    if (selectedSet.contains(value)) {
                        selectedSet.remove(value);
                        chip.setBackgroundResource(CHIP_BACKGROUND_UNSELECTED);
                    } else {
                        selectedSet.add(value);
                        chip.setBackgroundResource(CHIP_BACKGROUND_SELECTED);
                    }
                    displayText.setText(formatLimitedText(selectedSet));
                    clearButton.setVisibility(selectedSet.isEmpty() ? View.GONE : View.VISIBLE);
                    checkIfSelectionsChanged();
                });
            }
        }
    }

    private void setupClearButtons() {
        btnClearEvent.setOnClickListener(v -> clearSelection(selectedEvents, eventLayout, tvEvent, btnClearEvent));
        btnClearSeason.setOnClickListener(v -> clearSelection(selectedSeasons, seasonLayout, tvSeason, btnClearSeason));
        btnClearStyle.setOnClickListener(v -> clearSelection(selectedStyles, styleLayout, tvStyle, btnClearStyle));
    }

    private void clearSelection(Set<String> selectedSet, FlexboxLayout layout, TextView displayText, ImageView clearButton) {
        selectedSet.clear();
        displayText.setText(formatLimitedText(selectedSet));
        clearButton.setVisibility(View.GONE);

        for (int i = 0; i < layout.getChildCount(); i++) {
            View view = layout.getChildAt(i);
            if (view instanceof TextView) {
                view.setBackgroundResource(CHIP_BACKGROUND_UNSELECTED);
            }
        }
        checkIfSelectionsChanged();
    }

    private void preselectChips(FlexboxLayout layout, Set<String> values) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View view = layout.getChildAt(i);
            if (view instanceof TextView) {
                TextView chip = (TextView) view;
                if (values.contains(chip.getText().toString())) {
                    chip.setBackgroundResource(CHIP_BACKGROUND_SELECTED);
                } else {
                    chip.setBackgroundResource(CHIP_BACKGROUND_UNSELECTED);
                }
            }
        }
    }

    private void checkIfSelectionsChanged() {
        String currentOutfitName = etOutfitName.getText().toString().trim();

        boolean nameChanged = !currentOutfitName.equals(originalOutfitName);

        boolean selectionsChanged = !selectedEvents.equals(originalEvents)
                || !selectedSeasons.equals(originalSeasons)
                || !selectedStyles.equals(originalStyles);

        btnSave.setVisibility(nameChanged || selectionsChanged ? View.VISIBLE : View.GONE);
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> {
            String currentOutfitName = etOutfitName.getText().toString().trim();

            if (currentOutfitName.isEmpty()) {
                Toast.makeText(this, "Outfit Name cannot be empty.", Toast.LENGTH_SHORT).show();
                etOutfitName.requestFocus();
                return;
            }
            if (selectedEvents.isEmpty() || selectedSeasons.isEmpty() || selectedStyles.isEmpty()) {
                Toast.makeText(this, "Please select at least one Event, Season, and Style.", Toast.LENGTH_SHORT).show();
                return;
            }

            String finalEvent = String.join(", ", selectedEvents);
            String finalSeason = String.join(", ", selectedSeasons);
            String finalStyle = String.join(", ", selectedStyles);

            // Update in Supabase
            updateOutfitInSupabase(currentOutfitName, finalEvent, finalSeason, finalStyle);
        });
    }

    private void updateOutfitInSupabase(String name, String event, String season, String style) {
        if (snapshotId == null || snapshotId.isEmpty()) {
            Toast.makeText(this, "Invalid outfit ID", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = outfitRepository.updateOutfit(snapshotId, name, null, null, null, event, season, style);

        if (success) {
            Toast.makeText(this, "Outfit updated in cloud!", Toast.LENGTH_SHORT).show();

            originalOutfitName = name;
            originalEvents = new HashSet<>(selectedEvents);
            originalSeasons = new HashSet<>(selectedSeasons);
            originalStyles = new HashSet<>(selectedStyles);

            checkIfSelectionsChanged();
        } else {
            Toast.makeText(this, "Failed to update outfit in cloud", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupUseButton() {
        btnUse.setOnClickListener(v -> handleUseSnapshot());
    }

    private void handleUseSnapshot() {
        if (snapshotId == null || snapshotId.isEmpty()) {
            Toast.makeText(this, "Outfit not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if outfit exists in history
        checkOutfitInHistory();
    }

    private void checkOutfitInHistory() {
        Call<List<JsonObject>> call = supabaseService.checkOutfitInHistory(snapshotId, "Used");
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                boolean existsInHistory = response.isSuccessful() && response.body() != null && !response.body().isEmpty();

                if (existsInHistory) {
                    // Remove from history
                    removeFromHistory();
                } else {
                    // Add to history
                    addToHistory();
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Toast.makeText(SnapshotDetailsActivity.this, "Network error checking history", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addToHistory() {
        String outfitName = etOutfitName.getText().toString().trim();
        String event = String.join(", ", selectedEvents);
        String season = String.join(", ", selectedSeasons);
        String style = String.join(", ", selectedStyles);
        String action = "Used";
        String date = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date());

        JsonObject history = new JsonObject();
        history.addProperty("outfit_ref_id", snapshotId);
        history.addProperty("outfit_name", outfitName);
        history.addProperty("style", style);
        history.addProperty("event", event);
        history.addProperty("season", season);
        history.addProperty("date_used", date);
        history.addProperty("action_taken", action);

        Call<JsonObject> call = supabaseService.insertOutfitHistory(history);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SnapshotDetailsActivity.this, "Outfit marked as 'Used' and added to history!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SnapshotDetailsActivity.this, "Failed to add to history", Toast.LENGTH_SHORT).show();
                }
                updateUseButtonText();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(SnapshotDetailsActivity.this, "Network error adding to history", Toast.LENGTH_SHORT).show();
                updateUseButtonText();
            }
        });
    }

    private void removeFromHistory() {
        Call<List<JsonObject>> call = supabaseService.checkOutfitInHistory(snapshotId, "Used");
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // ADDED NULL CHECK
                    JsonObject historyEntry = response.body().get(0);
                    if (historyEntry.has("id") && !historyEntry.get("id").isJsonNull()) {
                        String historyId = historyEntry.get("id").getAsString();
                        deleteFromHistory(historyId);
                    } else {
                        Toast.makeText(SnapshotDetailsActivity.this, "Invalid history entry", Toast.LENGTH_SHORT).show();
                        updateUseButtonText();
                    }
                } else {
                    Toast.makeText(SnapshotDetailsActivity.this, "Failed to find history entry", Toast.LENGTH_SHORT).show();
                    updateUseButtonText();
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Toast.makeText(SnapshotDetailsActivity.this, "Network error removing from history", Toast.LENGTH_SHORT).show();
                updateUseButtonText();
            }
        });
    }

    private void deleteFromHistory(String historyId) {
        Call<Void> call = supabaseService.deleteOutfitHistory(historyId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SnapshotDetailsActivity.this, "Outfit marked as 'Unused'", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SnapshotDetailsActivity.this, "Failed to remove from history", Toast.LENGTH_SHORT).show();
                }
                updateUseButtonText();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(SnapshotDetailsActivity.this, "Network error removing from history", Toast.LENGTH_SHORT).show();
                updateUseButtonText();
            }
        });
    }

    private void updateUseButtonText() {
        Call<List<JsonObject>> call = supabaseService.checkOutfitInHistory(snapshotId, "Used");
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                boolean existsInHistory = response.isSuccessful() && response.body() != null && !response.body().isEmpty();
                btnUse.setText(existsInHistory ? "Used" : "Use");
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                btnUse.setText("Use");
            }
        });
    }
}