package com.example.outpick.outfits;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outpick.database.models.Outfit;
import com.example.outpick.R;
import com.example.outpick.common.BaseDrawerActivity;
import com.example.outpick.common.adapters.FavoritesAdapter;
import com.example.outpick.database.repositories.OutfitRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends BaseDrawerActivity {

    private RecyclerView recyclerViewFavorites;
    private ImageView backArrow;
    private SupabaseService supabaseService;
    private OutfitRepository outfitRepository;
    private FavoritesAdapter adapter;

    // ⭐ CRITICAL: Current User ID (String for Supabase UUID)
    private String currentUserId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        // --- Supabase setup ---
        supabaseService = SupabaseClient.getService();
        outfitRepository = new OutfitRepository(supabaseService);

        // --- Get user ID from SharedPreferences ---
        currentUserId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("user_id", "");

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "User data error. Cannot load favorites.", Toast.LENGTH_LONG).show();
            // Handle error state immediately
        }

        // --- Drawer setup ---
        setupDrawer(R.id.drawer_layout, R.id.nav_view);

        // --- Set username in drawer header ---
        String username = getImmutableLoginId();
        NavigationView navView = findViewById(R.id.nav_view);
        View headerView = navView.getHeaderView(0);
        if (headerView != null && username != null) {
            TextView usernameText = headerView.findViewById(R.id.nav_header_username);
            if (usernameText != null) usernameText.setText(username);
        }

        // --- Back arrow ---
        backArrow = findViewById(R.id.backArrow);
        if (backArrow != null) backArrow.setOnClickListener(v -> onBackPressed());

        // --- RecyclerView setup ---
        recyclerViewFavorites = findViewById(R.id.recyclerViewFavorites);
        recyclerViewFavorites.setLayoutManager(new GridLayoutManager(this, 2));

        // --- Load favorite outfits ---
        loadFavorites();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Crucial for refreshing the list after a user removes a favorite
        if (!currentUserId.isEmpty()) {
            loadFavorites();
        }
    }

    /** Load favorite outfits from Supabase into RecyclerView (User-Scoped) */
    private void loadFavorites() {
        if (currentUserId.isEmpty()) {
            // If user is not logged in or ID is invalid, clear the view and stop
            if (adapter != null) adapter.updateData(new ArrayList<>());
            recyclerViewFavorites.setVisibility(View.GONE);
            return;
        }

        // Load favorites from Supabase
        loadFavoritesFromSupabase();
    }

    private void loadFavoritesFromSupabase() {
        // Show loading state
        recyclerViewFavorites.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                // Get user favorites from Supabase
                List<Outfit> favoriteOutfits = getUserFavoritesFromSupabase();

                runOnUiThread(() -> {
                    if (adapter == null) {
                        // CRITICAL: The adapter MUST know the user ID to handle the toggle click later
                        adapter = new FavoritesAdapter(FavoritesActivity.this, favoriteOutfits, supabaseService, currentUserId);
                        recyclerViewFavorites.setAdapter(adapter);
                    } else {
                        // Efficiently update data
                        adapter.updateData(favoriteOutfits);
                    }

                    // Toggle visibility
                    recyclerViewFavorites.setVisibility(favoriteOutfits.isEmpty() ? View.GONE : View.VISIBLE);

                    if (favoriteOutfits.isEmpty()) {
                        Toast.makeText(FavoritesActivity.this, "No favorite outfits found", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(FavoritesActivity.this, "Error loading favorites: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    recyclerViewFavorites.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private List<Outfit> getUserFavoritesFromSupabase() {
        List<Outfit> favoriteOutfits = new ArrayList<>();

        try {
            // Get user's favorite outfit IDs
            retrofit2.Call<List<com.google.gson.JsonObject>> favoritesCall =
                    supabaseService.getUserFavorites(currentUserId);
            retrofit2.Response<List<com.google.gson.JsonObject>> favoritesResponse = favoritesCall.execute();

            if (favoritesResponse.isSuccessful() && favoritesResponse.body() != null) {
                List<String> favoriteOutfitIds = new ArrayList<>();

                for (com.google.gson.JsonObject favorite : favoritesResponse.body()) {
                    if (favorite.has("outfit_id")) {
                        favoriteOutfitIds.add(favorite.get("outfit_id").getAsString());
                    }
                }

                // Get full outfit details for each favorite
                if (!favoriteOutfitIds.isEmpty()) {
                    List<Outfit> allOutfits = outfitRepository.getAllOutfits();
                    for (Outfit outfit : allOutfits) {
                        if (favoriteOutfitIds.contains(outfit.getId())) {
                            outfit.setFavorite(true); // Mark as favorite
                            favoriteOutfits.add(outfit);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return favoriteOutfits;
    }
}