package com.example.outpick.closet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.outpick.MainActivity;
import com.example.outpick.R;
import com.example.outpick.common.BaseDrawerActivity;
import com.example.outpick.common.FilterActivity;
import com.example.outpick.common.adapters.ImageAdapter;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.gson.JsonObject;

import java.io.InputStream;
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
    private SupabaseService supabaseService;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_closet);

        // Initialize Supabase
        supabaseService = SupabaseClient.getService();

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
            Bitmap bitmap = clothingImages.get(position);
            showImageDialog(bitmap);
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

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<List<JsonObject>> call = supabaseService.getClothing();
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processClothingData(response.body());
                } else {
                    Toast.makeText(ClosetActivity.this, "Failed to load clothing items", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Toast.makeText(ClosetActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processClothingData(List<JsonObject> clothingItems) {
        new Thread(() -> {
            for (JsonObject item : clothingItems) {
                try {
                    // Only process items for current user
                    String userId = item.has("user_id") ? item.get("user_id").getAsString() : "";
                    if (!currentUserId.equals(userId)) {
                        continue;
                    }

                    String itemId = item.has("id") ? item.get("id").getAsString() : "";
                    String imagePath = item.has("image_path") ? item.get("image_path").getAsString() : "";

                    if (!itemId.isEmpty() && !imagePath.isEmpty()) {
                        Bitmap bitmap = loadBitmapFromUri(imagePath);
                        if (bitmap != null) {
                            runOnUiThread(() -> {
                                clothingIds.add(itemId);
                                clothingImages.add(bitmap);
                                updateGridView();
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private Bitmap loadBitmapFromUri(String imageUri) {
        try {
            if (imageUri.startsWith("content://") || imageUri.startsWith("file://")) {
                Uri uri = Uri.parse(imageUri);
                InputStream inputStream = getContentResolver().openInputStream(uri);
                return BitmapFactory.decodeStream(inputStream);
            } else if (imageUri.startsWith("http")) {
                // For web URLs, you might want to use Glide or Picasso
                // For now, return null and handle with placeholder
                return null;
            } else {
                // Local file path
                return BitmapFactory.decodeFile(imageUri);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateGridView() {
        gridView.setAdapter(new ImageAdapter(this, clothingImages));
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
                        if (position < clothingIds.size() && position < clothingImages.size()) {
                            clothingIds.remove(position);
                            clothingImages.remove(position);
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

    private void showImageDialog(Bitmap bitmap) {
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(800, 800));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

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