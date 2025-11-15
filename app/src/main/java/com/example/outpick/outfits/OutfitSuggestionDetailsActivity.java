package com.example.outpick.outfits;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.outpick.database.models.Outfit;
import com.example.outpick.R;
import com.example.outpick.database.repositories.OutfitRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutfitSuggestionDetailsActivity extends AppCompatActivity {

    private static final String TAG = "OutfitDetailsActivity";

    private Outfit outfit;
    private ImageView previewOutfitImageView;
    private ImageButton btnFavorite;
    private ImageButton btnBack;
    private TextView tvName, tvEvent, tvSeason, tvStyle, tvGender, tvDescription;
    private Button useButton, recreateButton;

    private SupabaseService supabaseService;
    private OutfitRepository outfitRepository;

    private String outfitId = ""; // Changed to String for Supabase UUID
    private String currentUserId = ""; // ⭐ CRITICAL: Current User ID (String for UUID)

    // Instance variables (fields) for safe lambda access
    private String imageUriStr;
    private String name;
    private String description;
    private String gender;
    private String event;
    private String season;
    private String style;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outfit_suggestion_details);

        // --- 1. Initialize Supabase ---
        supabaseService = SupabaseClient.getService();
        outfitRepository = new OutfitRepository(supabaseService);

        // --- 2. Initialize Views ---
        previewOutfitImageView = findViewById(R.id.previewOutfitImageView);
        btnFavorite = findViewById(R.id.btn_favorite);
        btnBack = findViewById(R.id.btn_back);
        tvName = findViewById(R.id.tv_outfitName);
        tvEvent = findViewById(R.id.tv_event);
        tvSeason = findViewById(R.id.tv_season);
        tvStyle = findViewById(R.id.tv_style);
        tvGender = findViewById(R.id.tv_gender);
        tvDescription = findViewById(R.id.tv_description);
        useButton = findViewById(R.id.useButton);
        recreateButton = findViewById(R.id.recreateButton);

        // --- 3. Retrieve Intent Data, including the User ID ---

        // ⭐ CRITICAL FIX: Retrieve currentUserId from the Intent
        currentUserId = getIntent().getStringExtra("user_id");

        // Retrieve Outfit details
        imageUriStr = getIntent().getStringExtra("imageUri");
        name = getIntent().getStringExtra("name");
        description = getIntent().getStringExtra("description");
        gender = getIntent().getStringExtra("gender");
        event = getIntent().getStringExtra("event");
        season = getIntent().getStringExtra("season");
        style = getIntent().getStringExtra("style");
        outfitId = getIntent().getStringExtra("id");

        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "User ID missing. Favorites disabled.", Toast.LENGTH_LONG).show();
            btnFavorite.setEnabled(false);
        }

        // --- Fallback values ---
        if (gender == null || gender.trim().isEmpty()) {
            gender = "Unisex";
        }
        if (name == null) name = "Unnamed Outfit";
        if (description == null) description = "";
        if (event == null) event = "";
        if (season == null) season = "";
        if (style == null) style = "";

        // --- 4. Initialize Outfit object ---
        outfit = new Outfit();
        outfit.setId(outfitId);
        outfit.setImageUri(imageUriStr);
        outfit.setName(name);
        outfit.setDescription(description);
        outfit.setGender(gender);
        outfit.setEvent(event);
        outfit.setSeason(season);
        outfit.setStyle(style);

        // Load favorite state from Supabase (user-scoped)
        loadOutfitFavoriteStatus();

        // --- Load image using Glide ---
        if (imageUriStr != null && !imageUriStr.isEmpty()) {
            Glide.with(this)
                    .load(Uri.parse(imageUriStr))
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(previewOutfitImageView);
        } else {
            previewOutfitImageView.setImageResource(R.drawable.placeholder_image);
        }

        // ------------------ FORMAT FOR DISPLAY ------------------
        String displayEvent = formatTagsForDisplay(event, "Daily");
        String displaySeason = formatTagsForDisplay(season, "N/A");
        String displayStyle = formatTagsForDisplay(style, "N/A");
        String displayGender = gender.isEmpty() ? "Unisex" : formatSingleWord(gender);
        String displayName = name;
        String displayDescription = description;

        tvName.setText("Outfit Name: " + displayName);
        tvEvent.setText("Event/Occasion: " + displayEvent);
        tvSeason.setText("Season: " + displaySeason);
        tvStyle.setText("Style: " + displayStyle);
        tvGender.setText("Gender: " + displayGender);
        tvDescription.setText("Description: " + displayDescription);

        // --- Favorite button ---
        updateFavoriteIcon();
        btnFavorite.setOnClickListener(v -> toggleFavorite());

        // --- Back button (signal change for caller) ---
        btnBack.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        // --- Initialize Use/Recreate buttons ---
        updateUseRecreateButtons();

        // --- Use button click ---
        useButton.setOnClickListener(v -> handleUseRecreateAction("Use", useButton, "Used"));

        // --- Recreate button click ---
        recreateButton.setOnClickListener(v -> handleUseRecreateAction("Recreate", recreateButton, "Recreated"));
    }

    // ⭐ CRITICAL: Override onBackPressed to ensure RESULT_OK is set
    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    // --- Helper to get favorite status from Supabase (User-Scoped) ---
    private void loadOutfitFavoriteStatus() {
        if (outfitId == null || outfitId.isEmpty() || currentUserId == null || currentUserId.isEmpty()) {
            outfit.setFavorite(false);
            return;
        }

        // Check if this outfit is in user's favorites
        Call<List<JsonObject>> call = supabaseService.checkFavorite(currentUserId, outfitId);
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean isFavorite = !response.body().isEmpty();
                    outfit.setFavorite(isFavorite);
                    updateFavoriteIcon();
                    Log.d(TAG, "Outfit ID: " + outfitId + ", User ID: " + currentUserId + ", Favorite: " + isFavorite);
                } else {
                    outfit.setFavorite(false);
                    updateFavoriteIcon();
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.e(TAG, "Failed to check favorite status: " + t.getMessage());
                outfit.setFavorite(false);
                updateFavoriteIcon();
            }
        });
    }

    // --- Favorite Toggle with Supabase (User-Scoped) ---
    private void toggleFavorite() {
        if (currentUserId == null || currentUserId.isEmpty() || outfitId == null || outfitId.isEmpty()) {
            Toast.makeText(this, "Cannot modify favorites without user/outfit ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean newFavoriteStatus = !outfit.isFavorite();
        outfit.setFavorite(newFavoriteStatus);

        if (newFavoriteStatus) {
            // Add to favorites
            addToFavorites();
        } else {
            // Remove from favorites
            removeFromFavorites();
        }

        updateFavoriteIcon();
        setResult(RESULT_OK);
    }

    private void addToFavorites() {
        JsonObject favorite = new JsonObject();
        favorite.addProperty("user_id", currentUserId);
        favorite.addProperty("outfit_id", outfitId);

        Call<JsonObject> call = supabaseService.addFavorite(favorite);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(OutfitSuggestionDetailsActivity.this,
                            "Outfit added to Favorites!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(OutfitSuggestionDetailsActivity.this,
                            "Failed to add to favorites", Toast.LENGTH_SHORT).show();
                    // Revert UI state on failure
                    outfit.setFavorite(false);
                    updateFavoriteIcon();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(OutfitSuggestionDetailsActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                // Revert UI state on failure
                outfit.setFavorite(false);
                updateFavoriteIcon();
            }
        });
    }

    private void removeFromFavorites() {
        Call<Void> call = supabaseService.removeFavorite(currentUserId, outfitId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(OutfitSuggestionDetailsActivity.this,
                            "Outfit removed from Favorites!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(OutfitSuggestionDetailsActivity.this,
                            "Failed to remove from favorites", Toast.LENGTH_SHORT).show();
                    // Revert UI state on failure
                    outfit.setFavorite(true);
                    updateFavoriteIcon();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(OutfitSuggestionDetailsActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                // Revert UI state on failure
                outfit.setFavorite(true);
                updateFavoriteIcon();
            }
        });
    }

    // ---------------- HELPER METHODS FOR FORMATTING ----------------
    private String formatTagsForDisplay(String rawTags, String defaultValue) {
        if (rawTags == null || rawTags.trim().isEmpty()) {
            return defaultValue;
        }

        String[] tags = rawTags.split(",");
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < tags.length; i++) {
            String tag = tags[i].trim();
            if (tag.isEmpty()) continue;

            formatted.append(formatSingleWord(tag));
            if (i < tags.length - 1) {
                formatted.append(", ");
            }
        }

        String result = formatted.toString().trim();
        if (result.endsWith(",")) {
            result = result.substring(0, result.length() - 1).trim();
        }

        return result.isEmpty() ? defaultValue : result;
    }

    private String formatSingleWord(String input) {
        if (input == null || input.isEmpty()) return "";

        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1));
            }
            if (i < words.length - 1) {
                result.append(" ");
            }
        }
        return result.toString();
    }

    // ---------------- UI/HISTORY LOGIC ----------------
    private void updateFavoriteIcon() {
        btnFavorite.setImageResource(outfit.isFavorite()
                ? R.drawable.ic_favorite
                : R.drawable.ic_favorite_border);
    }

    private void updateUseRecreateButtons() {
        // TODO: Implement Supabase-based history checking for Use/Recreate buttons
        // For now, set default states
        useButton.setText("Use");
        recreateButton.setText("Recreate");
    }

    private void handleUseRecreateAction(String action, Button button, String dbAction) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date());

        // TODO: Implement Supabase-based outfit history tracking
        // For now, just toggle the button text
        if (button.getText().toString().equals(action)) {
            button.setText(dbAction);
            Toast.makeText(this, "Outfit " + dbAction.toLowerCase(), Toast.LENGTH_SHORT).show();
        } else {
            button.setText(action);
            Toast.makeText(this, "Outfit " + action.toLowerCase() + " again", Toast.LENGTH_SHORT).show();
        }
    }
}