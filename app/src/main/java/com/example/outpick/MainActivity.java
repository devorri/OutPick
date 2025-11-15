package com.example.outpick;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.outpick.closet.AddItemActivity;
import com.example.outpick.common.BaseDrawerActivity;
import com.example.outpick.common.PreviewImageActivity;
import com.example.outpick.common.adapters.ClosetAdapter;
import com.example.outpick.database.models.ClosetItem;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.outfits.CreateOutfitActivity;
import com.example.outpick.outfits.OutfitSuggestionActivity;
import com.example.outpick.outfits.TryOnActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseDrawerActivity {

    private String username;
    private String currentUserId;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int PERMISSION_REQUEST_CODE = 200;

    private Uri photoUri;
    private ClosetAdapter adapter;
    private SupabaseService supabaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Supabase
        supabaseService = SupabaseClient.getService();

        // Load user data
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = sharedPref.getString("username", "Guest");
        currentUserId = sharedPref.getString("user_id", "");

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            // You might want to redirect to login here
        }

        // Setup navigation drawer
        setupDrawer(R.id.drawer_layout, R.id.nav_view);

        // Profile icon opens drawer
        findViewById(R.id.iconProfile).setOnClickListener(v ->
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.END));

        // Filter icon → Try On screen
        findViewById(R.id.iconFilter).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TryOnActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        // Top tab switching buttons
        LinearLayout personalClosetBtn = findViewById(R.id.btn_personal_closet);
        LinearLayout outfitSuggestionBtn = findViewById(R.id.btn_outfit_suggestion);
        personalClosetBtn.setBackgroundResource(R.drawable.btn_border_left_white);
        outfitSuggestionBtn.setBackgroundResource(R.drawable.btn_border_right_gray);

        personalClosetBtn.setOnClickListener(v -> {
            // Already on this screen
        });

        outfitSuggestionBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OutfitSuggestionActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
            finish();
        });

        // Camera button → check permissions
        ImageButton cameraButton = findViewById(R.id.btn_camera);
        cameraButton.setOnClickListener(v -> checkCameraPermissionsAndOpen());

        // Plus button → Add/Create menu
        ImageButton outfitPlusButton = findViewById(R.id.outfitPlusButton);
        outfitPlusButton.setOnClickListener(v -> showAddMenu());

        // Load Closet items from Supabase
        setupClosetRecyclerView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check if the result is from EditProfileActivity
        if (requestCode == BaseDrawerActivity.REQUEST_CODE_EDIT_PROFILE && resultCode == RESULT_OK) {
            updateDrawerHeader();
            Toast.makeText(this, "Profile updated and drawer refreshed!", Toast.LENGTH_SHORT).show();
        }

        // Handle the original REQUEST_IMAGE_CAPTURE logic
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Intent intent = new Intent(MainActivity.this, PreviewImageActivity.class);
            intent.putExtra("imageUri", photoUri.toString());
            startActivity(intent);
        }
    }

    // ===== Check and Request Permissions =====
    private void checkCameraPermissionsAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES},
                        PERMISSION_REQUEST_CODE);
            } else {
                openCamera();
            }
        } else {
            // Android 12- permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                openCamera();
            }
        }
    }

    // ===== BottomSheet Add Menu =====
    private void showAddMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.popup_add_menu, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button addItemButton = view.findViewById(R.id.btn_add_item);
        Button createOutfitButton = view.findViewById(R.id.btn_create_outfit);

        addItemButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(MainActivity.this, AddItemActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        createOutfitButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(MainActivity.this, CreateOutfitActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        dialog.show();
    }

    // ===== Setup Closet RecyclerView with Supabase =====
    private void setupClosetRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerClosets);

        if (currentUserId.isEmpty()) {
            // Show empty state if user not logged in
            showEmptyClosetState(recyclerView);
            return;
        }

        // Load closets from Supabase
        loadClosetsFromSupabase(recyclerView);
    }

    private void loadClosetsFromSupabase(RecyclerView recyclerView) {
        Call<List<JsonObject>> call = supabaseService.getClosets();
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<List<JsonObject>> call, @NonNull Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ClosetItem> userClosets = parseClosetsFromJson(response.body());
                    setupClosetAdapter(recyclerView, userClosets);
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load closets", Toast.LENGTH_SHORT).show();
                    showEmptyClosetState(recyclerView);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<JsonObject>> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showEmptyClosetState(recyclerView);
            }
        });
    }

    private List<ClosetItem> parseClosetsFromJson(List<JsonObject> jsonObjects) {
        List<ClosetItem> closets = new ArrayList<>();
        Gson gson = new Gson();

        for (JsonObject jsonObject : jsonObjects) {
            try {
                // Only include closets for current user
                String userId = jsonObject.has("user_id") ? jsonObject.get("user_id").getAsString() : "";
                if (currentUserId.equals(userId)) {
                    ClosetItem closet = gson.fromJson(jsonObject, ClosetItem.class);
                    closets.add(closet);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return closets;
    }

    private void setupClosetAdapter(RecyclerView recyclerView, List<ClosetItem> userClosets) {
        // Build the final display list, starting with the two static cards
        List<ClosetItem> fullList = new ArrayList<>();
        fullList.add(new ClosetItem("Outfit Combinations", "",
                String.valueOf(Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.top_test3)),
                "outfit_card"));
        fullList.add(new ClosetItem("Create a closet", "",
                String.valueOf(Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.ic_create_closet)),
                "create_card"));

        // Add the user closets from Supabase
        fullList.addAll(userClosets);

        adapter = new ClosetAdapter(this, fullList);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    private void showEmptyClosetState(RecyclerView recyclerView) {
        // Show only the static cards when no user closets exist
        List<ClosetItem> fullList = new ArrayList<>();
        fullList.add(new ClosetItem("Outfit Combinations", "",
                String.valueOf(Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.top_test3)),
                "outfit_card"));
        fullList.add(new ClosetItem("Create a closet", "",
                String.valueOf(Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.ic_create_closet)),
                "create_card"));

        adapter = new ClosetAdapter(this, fullList);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    // ===== Open Camera =====
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        photoFile
                );
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    // ===== Permissions Result =====
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera and storage permissions are required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ===== Refresh when returning =====
    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.updateOutfitSnapshotCount();
        }
        setupClosetRecyclerView();
        updateDrawerHeader();
    }

    // ===== Updated Static method for YourClothesActivity =====
    public static ArrayList<String> getUserClosets(Context context) {
        ArrayList<String> closets = new ArrayList<>();

        // Get current user ID from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String currentUserId = prefs.getString("user_id", "");

        if (currentUserId.isEmpty()) {
            return closets; // Return empty list if user not logged in
        }

        // This would need to be implemented with a synchronous Supabase call
        // For now, returning empty list - you might need to refactor this to use callbacks
        Toast.makeText(context, "Closet loading updated - please refresh", Toast.LENGTH_SHORT).show();

        return closets;
    }
}