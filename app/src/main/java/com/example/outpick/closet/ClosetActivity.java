package com.example.outpick.closet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.example.outpick.MainActivity;
import com.example.outpick.R;
import com.example.outpick.common.BaseDrawerActivity;
import com.example.outpick.common.FilterActivity;
import com.example.outpick.common.adapters.ImageAdapter;
import com.example.outpick.database.models.ClothingItem;
import com.example.outpick.database.repositories.ClothingRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClosetActivity extends BaseDrawerActivity {

    private DrawerLayout drawerLayout;
    private ImageView iconProfile, iconFilter, imageLogo;
    private GridView gridView;

    private ArrayList<Bitmap> clothingImages = new ArrayList<>();
    private ArrayList<String> clothingIds = new ArrayList<>(); // Changed to String for Supabase UUID
    private ArrayList<String> clothingImageUrls = new ArrayList<>(); // ✅ ADDED: Store cloud URLs
    private SupabaseService supabaseService;
    private ClothingRepository clothingRepository;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_closet);

        // Initialize Supabase
        supabaseService = SupabaseClient.getService();
        clothingRepository = new ClothingRepository(supabaseService);

        // Get current user ID
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", "");

        // --- Top Bar Buttons ---
        drawerLayout = findViewById(R.id.drawer_layout);
        iconProfile = findViewById(R.id.iconProfile);
        iconFilter = findViewById(R.id.iconFilter);
        imageLogo = findViewById(R.id.imageLogo);

        iconProfile.setOnClickListener(v ->
                drawerLayout.openDrawer(findViewById(R.id.nav_view)));

        iconFilter.setOnClickListener(v ->
                startActivity(new Intent(this, FilterActivity.class)));

        imageLogo.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // --- GridView Logic ---
        gridView = findViewById(R.id.gridViewClothes);

        loadClothesFromSupabase();

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            // ✅ FIXED: Show image using URL instead of Bitmap
            String imageUrl = clothingImageUrls.get(position);
            showImageDialog(imageUrl);
        });

        gridView.setOnItemLongClickListener((parent, view, position, id) -> {
            String itemId = clothingIds.get(position);
            deleteClothingItem(itemId, position);
            return true;
        });
    }

    private void loadClothesFromSupabase() {
        clothingImages.clear();
        clothingIds.clear();
        clothingImageUrls.clear(); // ✅ Clear URLs list

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ FIXED: Use repository pattern for better data handling
        new Thread(() -> {
            try {
                List<ClothingItem> clothingItems = clothingRepository.getAllClothing();

                runOnUiThread(() -> {
                    // Process clothing items
                    for (ClothingItem item : clothingItems) {
                        // Only process items for current user
                        if (!currentUserId.equals(item.getId())) {
                            continue;
                        }

                        String itemId = item.getId();
                        String imageUrl = item.getImagePath();

                        if (itemId != null && !itemId.isEmpty() && imageUrl != null && !imageUrl.isEmpty()) {
                            clothingIds.add(itemId);
                            clothingImageUrls.add(imageUrl); // ✅ Store cloud URL

                            // ✅ For backward compatibility, keep bitmap list but use placeholder
                            // The actual image loading will be handled by ImageAdapter with Glide
                            clothingImages.add(null); // Placeholder, actual loading in adapter
                        }
                    }
                    updateGridView();
                    Toast.makeText(ClosetActivity.this, "Loaded " + clothingIds.size() + " items", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(ClosetActivity.this, "Error loading clothes: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void updateGridView() {
        // ✅ FIXED: Pass both images and URLs to adapter
        gridView.setAdapter(new ImageAdapter(this, clothingImages, clothingImageUrls));
    }

    private void deleteClothingItem(String itemId, int position) {
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Clothing Item")
                .setMessage("Are you sure you want to delete this clothing item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteFromSupabase(itemId, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteFromSupabase(String itemId, int position) {
        Call<Void> call = supabaseService.deleteClothing(itemId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        // Remove from local lists
                        if (position < clothingIds.size() && position < clothingImages.size() && position < clothingImageUrls.size()) {
                            clothingIds.remove(position);
                            clothingImages.remove(position);
                            clothingImageUrls.remove(position);
                            updateGridView();
                        }
                        Toast.makeText(ClosetActivity.this, "Clothing item deleted", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(ClosetActivity.this, "Failed to delete item", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                runOnUiThread(() ->
                        Toast.makeText(ClosetActivity.this, "Network error deleting item", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ✅ FIXED: Show image using Glide with cloud URL
    private void showImageDialog(String imageUrl) {
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(800, 800));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // ✅ Use Glide to load cloud image
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .into(imageView);

        new AlertDialog.Builder(this)
                .setView(imageView)
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to activity
        loadClothesFromSupabase();
    }
}