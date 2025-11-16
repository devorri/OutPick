package com.example.outpick.outfits;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outpick.MainActivity;
import com.example.outpick.database.models.Outfit;
import com.example.outpick.R;
import com.example.outpick.common.BaseDrawerActivity;
import com.example.outpick.common.PreviewImageActivity;
import com.example.outpick.common.adapters.OutfitSuggestionAdapter;
import com.example.outpick.database.repositories.OutfitRepository;
import com.example.outpick.database.repositories.UserOutfitRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class OutfitSuggestionActivity extends BaseDrawerActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_OUTFIT_DETAILS = 102;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final String TAG = "OutfitSuggestion";

    private Uri photoUri;
    private String username;
    private String userGender = "";
    private String currentUserId = "";

    private RecyclerView recyclerViewOutfits;
    private OutfitSuggestionAdapter adapter;
    private SupabaseService supabaseService;
    private OutfitRepository outfitRepository;
    private UserOutfitRepository userOutfitRepository;

    // Persistent filter selections
    private Set<String> lastSelectedCategories = new HashSet<>();
    private Set<String> lastSelectedGenders = new HashSet<>();
    private Set<String> lastSelectedEvents = new HashSet<>();
    private Set<String> lastSelectedSeasons = new HashSet<>();
    private Set<String> lastSelectedStyles = new HashSet<>();
    private String lastSearchKeyword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outfit_suggestion);

        // --- Initialize Supabase ---
        supabaseService = SupabaseClient.getService();
        outfitRepository = new OutfitRepository(supabaseService);
        userOutfitRepository = new UserOutfitRepository(supabaseService, outfitRepository);

        // --- Get username and User ID ---
        username = getImmutableLoginId();
        if (username == null || username.isEmpty()) {
            Log.e(TAG, "Username (Immutable ID) is missing. Finishing activity.");
            finish();
            return;
        }

        // Get user ID from SharedPreferences
        currentUserId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("user_id", "");
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "User ID not found. Favorites disabled.", Toast.LENGTH_LONG).show();
        }

        // --- Setup Drawer ---
        setupDrawer(R.id.drawer_layout, R.id.nav_view);

        findViewById(R.id.iconProfile).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        // --- Top Bar Filter Icon → TryOnActivity ---
        ImageView topFilterIcon = findViewById(R.id.iconFilterbutton);
        if (topFilterIcon != null) {
            topFilterIcon.setOnClickListener(v -> {
                Intent intent = new Intent(OutfitSuggestionActivity.this, TryOnActivity.class);
                intent.putExtra("username", username);
                startActivity(intent);
            });
        }

        // --- RecyclerView ---
        recyclerViewOutfits = findViewById(R.id.recyclerViewOutfits);
        recyclerViewOutfits.setLayoutManager(new GridLayoutManager(this, 2));

        // --- Load outfits from Supabase ---
        loadOutfitsFromSupabase();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Re-fetch the user's gender and ID
        username = getImmutableLoginId();
        currentUserId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("user_id", "");

        // Load user gender from Supabase
        loadUserGenderFromSupabase();

        // --- Apply filters initially / on resume ---
        applyLastFiltersInternally();

        // --- Bottom Sheet Filter ---
        FloatingActionButton btnFilter = findViewById(R.id.btn_filter_outfit);
        btnFilter.setOnClickListener(v -> openFilterBottomSheet());

        // --- Camera Button ---
        ImageView cameraButton = findViewById(R.id.btn_camera);
        cameraButton.setOnClickListener(v -> checkCameraPermissionsAndOpen());

        // --- Bottom Navigation ---
        findViewById(R.id.btn_personal_closet).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btn_outfit_suggestion).setOnClickListener(v -> { /* Already on this screen */ });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && photoUri != null) {
            // Camera flow
            Intent previewIntent = new Intent(this, PreviewImageActivity.class);
            previewIntent.putExtra("imageUri", photoUri.toString());
            previewIntent.putExtra("from_outfit_suggestion", true);
            startActivity(previewIntent);
        }

        // Refresh the outfit list if favorite status was changed in details
        if (requestCode == REQUEST_OUTFIT_DETAILS && resultCode == RESULT_OK) {
            applyLastFiltersInternally();
            Toast.makeText(this, "Outfit list refreshed.", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------- OPEN DETAILS ----------------
    private void openOutfitDetails(Outfit outfit) {
        Intent intent = new Intent(this, OutfitSuggestionDetailsActivity.class);
        intent.putExtra("id", outfit.getId());
        intent.putExtra("imageUri", outfit.getImageUri());
        intent.putExtra("name", outfit.getName());
        intent.putExtra("category", outfit.getCategory());
        intent.putExtra("description", outfit.getDescription());
        intent.putExtra("gender", outfit.getGender());
        intent.putExtra("event", outfit.getEvent());
        intent.putExtra("season", outfit.getSeason());
        intent.putExtra("style", outfit.getStyle());
        intent.putExtra("isFavorite", outfit.isFavorite());

        // Pass the User ID for the Details Activity
        intent.putExtra("user_id", currentUserId);

        startActivityForResult(intent, REQUEST_OUTFIT_DETAILS);
    }

    // ---------------- LOAD OUTFITS FROM SUPABASE ----------------
    private void loadOutfitsFromSupabase() {
        new Thread(() -> {
            try {
                // ✅ FIXED: Get ONLY user's assigned outfits
                List<Outfit> userOutfits;
                if (currentUserId.isEmpty()) {
                    userOutfits = new ArrayList<>(); // Empty if no user ID
                } else {
                    userOutfits = userOutfitRepository.getOutfitsForUser(currentUserId);
                }

                runOnUiThread(() -> {
                    // Store outfits for filtering
                    applyLastFiltersInternally();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading outfits: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // ---------------- LOAD USER GENDER FROM SUPABASE ----------------
    private void loadUserGenderFromSupabase() {
        if (currentUserId.isEmpty()) return;

        new Thread(() -> {
            try {
                retrofit2.Call<List<JsonObject>> call = supabaseService.getUserById(currentUserId);
                retrofit2.Response<List<JsonObject>> response = call.execute();

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    JsonObject user = response.body().get(0);
                    if (user.has("gender")) {
                        userGender = user.get("gender").getAsString();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading user gender: " + e.getMessage());
            }
        }).start();
    }

    // ---------------- APPLY FILTERS WITH USER GENDER ----------------
    private void applyLastFiltersInternally() {
        new Thread(() -> {
            try {
                // ✅ FIXED: Get ONLY user's assigned outfits
                List<Outfit> userOutfits;
                if (currentUserId.isEmpty()) {
                    userOutfits = new ArrayList<>(); // Empty if no user ID
                } else {
                    userOutfits = userOutfitRepository.getOutfitsForUser(currentUserId);
                }

                List<Outfit> filtered = new ArrayList<>();

                String normalizedUserGender = userGender != null ? userGender.trim().toLowerCase() : "";

                Set<String> categories = lastSelectedCategories.stream().map(String::toLowerCase).collect(Collectors.toSet());
                Set<String> genders = lastSelectedGenders.stream().map(String::toLowerCase).collect(Collectors.toSet());
                Set<String> events = lastSelectedEvents.stream().map(String::toLowerCase).collect(Collectors.toSet());
                Set<String> seasons = lastSelectedSeasons.stream().map(String::toLowerCase).collect(Collectors.toSet());
                Set<String> styles = lastSelectedStyles.stream().map(String::toLowerCase).collect(Collectors.toSet());

                boolean canCheckFavorites = !currentUserId.isEmpty();

                for (Outfit o : userOutfits) {
                    if (o.getImageUri() == null || o.getImageUri().trim().isEmpty()) continue;
                    if (o.getGender() == null) continue;

                    // 1. Critical Filtering Step: Only show outfits matching the user's current gender
                    if (!o.getGender().trim().equalsIgnoreCase(normalizedUserGender)) continue;

                    // 2. Load User-Scoped Favorite Status from Supabase
                    if (canCheckFavorites) {
                        boolean isFav = isOutfitFavoriteInSupabase(o.getId());
                        o.setFavorite(isFav);
                    } else {
                        o.setFavorite(false);
                    }

                    // 3. Apply general filters
                    if (filterMatches(o, categories, genders, events, seasons, styles, lastSearchKeyword)) {
                        filtered.add(o);
                    }
                }

                Collections.sort(filtered, Comparator.comparing(o -> o.getName().toLowerCase()));

                runOnUiThread(() -> updateRecyclerView(filtered));

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error applying filters: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private boolean isOutfitFavoriteInSupabase(String outfitId) {
        try {
            retrofit2.Call<List<JsonObject>> call = supabaseService.checkFavorite(currentUserId, outfitId);
            retrofit2.Response<List<JsonObject>> response = call.execute();
            return response.isSuccessful() && response.body() != null && !response.body().isEmpty();
        } catch (Exception e) {
            Log.e(TAG, "Error checking favorite status: " + e.getMessage());
            return false;
        }
    }

    // ---------------- RECYCLERVIEW UPDATE ----------------
    private void updateRecyclerView(List<Outfit> outfits) {
        if (adapter == null) {
            adapter = new OutfitSuggestionAdapter(
                    this,
                    outfits,
                    this::openOutfitDetails,
                    currentUserId,
                    supabaseService
            );
            recyclerViewOutfits.setAdapter(adapter);
        } else {
            adapter.updateList(outfits);
        }

        try {
            TextView tvNoOutfits = findViewById(R.id.tvNoOutfits);
            if (tvNoOutfits != null) {
                if (outfits.isEmpty()) {
                    tvNoOutfits.setVisibility(View.VISIBLE);
                    tvNoOutfits.setText("No outfits match your filter criteria.");
                    recyclerViewOutfits.setVisibility(View.GONE);
                } else {
                    tvNoOutfits.setVisibility(View.GONE);
                    recyclerViewOutfits.setVisibility(View.VISIBLE);
                }
            } else {
                recyclerViewOutfits.setVisibility(outfits.isEmpty() ? View.GONE : View.VISIBLE);
                if (outfits.isEmpty()) {
                    Toast.makeText(this, "No outfits found", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            recyclerViewOutfits.setVisibility(outfits.isEmpty() ? View.GONE : View.VISIBLE);
            if (outfits.isEmpty()) {
                Toast.makeText(this, "No outfits found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ---------------- CAMERA PERMISSIONS AND CAPTURE ----------------
    private void checkCameraPermissionsAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST_CODE);
            } else {
                openCamera();
            }
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
            }
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera and Storage permissions are required to take a photo.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ---------------- FILTERING LOGIC ----------------
    private boolean filterMatches(Outfit o, Set<String> categories, Set<String> genders,
                                  Set<String> events, Set<String> seasons, Set<String> styles,
                                  String keyword) {
        if (!keyword.isEmpty() && !o.getName().toLowerCase().contains(keyword.toLowerCase())) {
            return false;
        }

        if (!categories.isEmpty() && !isAnyTagMatching(normalizeTags(o.getCategory()), categories)) {
            return false;
        }
        if (!events.isEmpty() && !isAnyTagMatching(normalizeTags(o.getEvent()), events)) {
            return false;
        }
        if (!seasons.isEmpty() && !isAnyTagMatching(normalizeTags(o.getSeason()), seasons)) {
            return false;
        }
        if (!styles.isEmpty() && !isAnyTagMatching(normalizeTags(o.getStyle()), styles)) {
            return false;
        }

        return true;
    }

    private Set<String> normalizeTags(String tags) {
        if (tags == null || tags.trim().isEmpty()) return Collections.emptySet();
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private boolean isAnyTagMatching(Set<String> outfitTags, Set<String> filterTags) {
        if (filterTags.isEmpty()) return true;
        for (String tag : outfitTags) {
            if (filterTags.contains(tag)) return true;
        }
        return false;
    }

    // Placeholder for opening filter bottom sheet
    private void openFilterBottomSheet() {
        Toast.makeText(this, "Filter screen opened (Implementation pending)", Toast.LENGTH_SHORT).show();
    }
}