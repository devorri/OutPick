package com.example.outpick.common;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outpick.database.models.Outfit;
import com.example.outpick.R;
import com.example.outpick.common.adapters.SuggestionAdapter;
import com.example.outpick.database.repositories.OutfitRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ContentOutfitsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewOutfits;
    private SuggestionAdapter adapter;
    private SupabaseService supabaseService;
    private OutfitRepository outfitRepository;

    private ImageButton btnBackOutfits;
    private ImageButton btnOptions;
    private LinearLayout bottomBar;
    private boolean multiSelectActive = false;

    // ⭐ CRITICAL NEW FIELD: Store the current username
    private String currentUsername;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_outfits);

        // --- ⭐ CRITICAL: Get Username from Intent ---
        currentUsername = getIntent().getStringExtra("currentUsername");

        if (currentUsername == null || currentUsername.isEmpty()) {
            currentUsername = "guest";
            Toast.makeText(this, "Warning: Username not passed. Using 'guest'.", Toast.LENGTH_SHORT).show();
        }

        // Initialize Supabase
        supabaseService = SupabaseClient.getService();
        outfitRepository = new OutfitRepository(supabaseService);

        // Get user ID from SharedPreferences
        userId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("user_id", null);

        // --- Find views ---
        recyclerViewOutfits = findViewById(R.id.recyclerViewOutfits);
        recyclerViewOutfits.setLayoutManager(new GridLayoutManager(this, 2));

        btnBackOutfits = findViewById(R.id.btnBackOutfits);
        btnOptions = findViewById(R.id.btnOptions);
        bottomBar = findViewById(R.id.bottomBar);

        // --- Back button handler ---
        if (btnBackOutfits != null) {
            btnBackOutfits.setOnClickListener(v -> onBackPressed());
        }

        // --- Load outfits initially ---
        loadOutfitsFromSupabase();

        // --- Options button (3-dot opens bottom sheet) ---
        btnOptions.setOnClickListener(v -> showBottomSheet());

        // --- Bottom bar delete button ---
        findViewById(R.id.btnDelete).setOnClickListener(v -> {
            if (adapter != null) {
                deleteSelectedOutfits();
            }
        });
    }

    /** Shows the 3-dot bottom sheet dialog for options */
    private void showBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_outfits, null);
        dialog.setContentView(sheetView);

        Button btnSelectMultiple = sheetView.findViewById(R.id.btnSelectMultiple);
        btnSelectMultiple.setOnClickListener(v -> {
            dialog.dismiss();
            enterMultiSelectMode();
        });

        dialog.show();
    }

    /** Enter multi-select mode (show bottom bar, enable selection) */
    private void enterMultiSelectMode() {
        multiSelectActive = true;
        bottomBar.setVisibility(View.VISIBLE);
        if (adapter != null) {
            adapter.enableMultiSelectMode(true);
        }

        // Change 3-dot → X
        btnOptions.setImageResource(R.drawable.ic_close);
        btnOptions.setOnClickListener(v -> exitMultiSelectMode());
    }

    /** Exit multi-select mode and return to normal state */
    private void exitMultiSelectMode() {
        multiSelectActive = false;
        bottomBar.setVisibility(View.GONE);
        if (adapter != null) {
            adapter.enableMultiSelectMode(false);
        }

        // Change back X → 3-dot
        btnOptions.setImageResource(R.drawable.ic_more_vert);
        btnOptions.setOnClickListener(v -> showBottomSheet());
    }

    /** Load all outfits from Supabase */
    private void loadOutfitsFromSupabase() {
        Call<List<JsonObject>> call = supabaseService.getOutfits();
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Outfit> outfits = convertJsonToOutfits(response.body());

                    // ⭐ CRITICAL FIX: Use the new SuggestionAdapter constructor:
                    adapter = new SuggestionAdapter(ContentOutfitsActivity.this, outfits, false, currentUsername);
                    recyclerViewOutfits.setAdapter(adapter);

                    Toast.makeText(ContentOutfitsActivity.this, "Loaded " + outfits.size() + " outfits", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ContentOutfitsActivity.this, "Failed to load outfits", Toast.LENGTH_SHORT).show();
                    // Initialize with empty list
                    adapter = new SuggestionAdapter(ContentOutfitsActivity.this, new ArrayList<>(), false, currentUsername);
                    recyclerViewOutfits.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Toast.makeText(ContentOutfitsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                // Initialize with empty list
                adapter = new SuggestionAdapter(ContentOutfitsActivity.this, new ArrayList<>(), false, currentUsername);
                recyclerViewOutfits.setAdapter(adapter);
            }
        });
    }

    private List<Outfit> convertJsonToOutfits(List<JsonObject> jsonObjects) {
        List<Outfit> outfits = new ArrayList<>();
        for (JsonObject json : jsonObjects) {
            Outfit outfit = new Outfit();

            if (json.has("id")) outfit.setId(json.get("id").getAsString());
            if (json.has("name")) outfit.setName(json.get("name").getAsString());
            if (json.has("image_uri")) outfit.setImageUri(json.get("image_uri").getAsString());
            if (json.has("category")) outfit.setCategory(json.get("category").getAsString());
            if (json.has("season")) outfit.setSeason(json.get("season").getAsString());
            if (json.has("occasion")) outfit.setEvent(json.get("occasion").getAsString()); // FIXED: setEvent instead of setOccasion
            if (json.has("style")) outfit.setStyle(json.get("style").getAsString());
            if (json.has("gender")) outfit.setGender(json.get("gender").getAsString());

            // Set default values if needed
            if (outfit.getName() == null) outfit.setName("Unnamed Outfit");

            outfits.add(outfit);
        }
        return outfits;
    }

    /** Delete selected outfits from Supabase */
    private void deleteSelectedOutfits() {
        if (adapter == null) return;

        List<Outfit> selectedOutfits = adapter.getSelectedOutfits();
        if (selectedOutfits.isEmpty()) {
            Toast.makeText(this, "No outfits selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        Toast.makeText(this, "Deleting " + selectedOutfits.size() + " outfits...", Toast.LENGTH_SHORT).show();

        // Delete each selected outfit from Supabase
        int[] deleteCount = {0};
        for (Outfit outfit : selectedOutfits) {
            deleteOutfitFromSupabase(outfit, selectedOutfits.size(), deleteCount);
        }
    }

    private void deleteOutfitFromSupabase(Outfit outfit, int totalCount, int[] deleteCount) {
        Call<Void> call = supabaseService.deleteOutfit(outfit.getId());
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                deleteCount[0]++;
                if (response.isSuccessful()) {
                    // Remove from adapter
                    adapter.removeOutfit(outfit);
                } else {
                    Log.e("ContentOutfitsActivity", "Failed to delete outfit: " + outfit.getId());
                }

                // Check if all deletions are complete
                if (deleteCount[0] >= totalCount) {
                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        exitMultiSelectMode();
                        Toast.makeText(ContentOutfitsActivity.this, "Deleted " + deleteCount[0] + " outfits", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                deleteCount[0]++;
                Log.e("ContentOutfitsActivity", "Network error deleting outfit: " + t.getMessage());

                if (deleteCount[0] >= totalCount) {
                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        exitMultiSelectMode();
                        Toast.makeText(ContentOutfitsActivity.this, "Completed deletions", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh list when returning to the screen
        loadOutfitsFromSupabase();
        if (multiSelectActive) {
            exitMultiSelectMode();
        }
    }
}