package com.example.outpick.outfits;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outpick.database.models.Outfit;
import com.example.outpick.R;
import com.example.outpick.common.BaseDrawerActivity;
import com.example.outpick.common.adapters.OutfitCombinationAdapter;
import com.example.outpick.database.repositories.OutfitRepository;
import com.example.outpick.database.repositories.UserOutfitRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;

import java.util.ArrayList;
import java.util.List;

public class ViewOutfitCombinationsActivity extends BaseDrawerActivity {

    private static final String TAG = "ViewOutfitCombinations";

    private RecyclerView recyclerView;
    private OutfitCombinationAdapter adapter;
    private List<Outfit> outfitList;
    private SupabaseService supabaseService;
    private OutfitRepository outfitRepository;
    private UserOutfitRepository userOutfitRepository;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_outfit_combinations);

        // Initialize Supabase
        supabaseService = SupabaseClient.getService();
        outfitRepository = new OutfitRepository(supabaseService);
        userOutfitRepository = new UserOutfitRepository(supabaseService, outfitRepository);

        setupDrawer(R.id.drawer_layout, R.id.nav_view);

        recyclerView = findViewById(R.id.recyclerOutfits);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        outfitList = new ArrayList<>();

        // Load user's outfits from Supabase
        loadOutfitsFromSupabase();

        adapter = new OutfitCombinationAdapter(this, outfitList);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        loadOutfitsFromSupabase();
    }

    private void loadOutfitsFromSupabase() {
        // Show loading state
        if (tvEmptyState != null) {
            tvEmptyState.setText("Loading your outfits...");
            tvEmptyState.setVisibility(View.VISIBLE);
        }
        recyclerView.setVisibility(View.GONE);

        // Get current user ID
        String currentUserId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("user_id", "");

        if (currentUserId.isEmpty()) {
            // User not logged in
            runOnUiThread(() -> {
                tvEmptyState.setText("Please log in to view your outfits");
                tvEmptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            });
            return;
        }

        new Thread(() -> {
            try {
                // ✅ FIXED: Get ONLY the current user's outfits
                List<Outfit> userOutfits = userOutfitRepository.getOutfitsForUser(currentUserId);

                // ✅ ADD LOGGING TO VERIFY USER-SPECIFIC DATA
                Log.d(TAG, "User ID: " + currentUserId);
                Log.d(TAG, "Found " + userOutfits.size() + " outfits for current user");
                for (Outfit outfit : userOutfits) {
                    Log.d(TAG, "User Outfit: " + outfit.getName() + " | Image URL: " + outfit.getImageUri());
                }

                runOnUiThread(() -> {
                    outfitList.clear();
                    outfitList.addAll(userOutfits);

                    if (adapter != null) {
                        adapter.updateList(userOutfits);
                    }

                    // Update empty state
                    if (tvEmptyState != null) {
                        if (userOutfits.isEmpty()) {
                            tvEmptyState.setText("No outfit combinations found for you yet!");
                            tvEmptyState.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            tvEmptyState.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading user outfits: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading your outfits: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (tvEmptyState != null) {
                        tvEmptyState.setText("Error loading your outfits");
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
    }
}