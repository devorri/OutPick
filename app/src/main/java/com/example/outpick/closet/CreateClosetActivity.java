package com.example.outpick.closet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.outpick.R;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.utils.ImageUploader;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateClosetActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "CreateClosetActivity";

    private ImageView imageCover;
    private EditText editClosetName;
    private Uri selectedImageUri = null;

    private SupabaseService supabaseService;
    private ImageUploader imageUploader;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_closet);

        // Initialize Supabase and ImageUploader
        supabaseService = SupabaseClient.getService();
        imageUploader = new ImageUploader(this);

        // Get current user ID
        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = sharedPref.getString("user_id", "");

        imageCover = findViewById(R.id.imageCover);
        editClosetName = findViewById(R.id.editClosetName);
        Button btnDone = findViewById(R.id.btnDone);
        ImageView btnBack = findViewById(R.id.btnBack);

        // Open gallery to pick image
        imageCover.setOnClickListener(v -> openImagePicker());

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Done button saves the closet to Supabase
        btnDone.setOnClickListener(v -> {
            String closetName = editClosetName.getText().toString().trim();

            if (closetName.isEmpty()) {
                editClosetName.setError("Please enter a closet name");
                return;
            }

            if (currentUserId.isEmpty()) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check for duplicate closet name in Supabase
            checkClosetNameExists(closetName);
        });
    }

    private void checkClosetNameExists(String closetName) {
        Log.d(TAG, "Checking if closet name exists: " + closetName);

        // First check if closet name already exists FOR THIS USER
        Call<List<JsonObject>> call = supabaseService.getClosets();
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                Log.d(TAG, "Closet name check response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Found " + response.body().size() + " existing closets");

                    for (JsonObject closet : response.body()) {
                        if (closet.has("name") && closet.get("name").getAsString().equalsIgnoreCase(closetName) &&
                                closet.has("user_id") && closet.get("user_id").getAsString().equals(currentUserId)) {
                            Log.w(TAG, "Closet name already exists for this user: " + closetName);
                            Toast.makeText(CreateClosetActivity.this, "You already have a closet with this name!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    // No duplicate found, proceed to save
                    Log.d(TAG, "No duplicate found, proceeding to save closet");
                    saveClosetToSupabase(closetName);
                } else {
                    Log.e(TAG, "Closet name check failed with code: " + response.code());
                    // If we can't check, still try to save (let Supabase handle unique constraint)
                    saveClosetToSupabase(closetName);
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.e(TAG, "Closet name check network error: " + t.getMessage());
                // If check fails, still try to save
                saveClosetToSupabase(closetName);
            }
        });
    }

    private void saveClosetToSupabase(String closetName) {
        Log.d(TAG, "Saving closet to Supabase: " + closetName);

        // Show loading
        Toast.makeText(this, "Creating closet...", Toast.LENGTH_SHORT).show();
        Button btnDone = findViewById(R.id.btnDone);
        btnDone.setEnabled(false);
        btnDone.setText("Creating...");

        if (selectedImageUri != null) {
            Log.d(TAG, "Selected image URI: " + selectedImageUri.toString());
            // Upload image to cloud storage first
            uploadImageAndSaveCloset(closetName, selectedImageUri);
        } else {
            Log.w(TAG, "No image selected, using default image");
            // Use default image
            String defaultImageUrl = "https://via.placeholder.com/300x300/CCCCCC/969696?text=Closet";
            saveClosetToDatabase(closetName, defaultImageUrl);
        }
    }

    /**
     * Upload image to Supabase Storage, then save closet with cloud URL
     */
    private void uploadImageAndSaveCloset(String closetName, Uri imageUri) {
        Log.d(TAG, "Starting image upload for closet: " + closetName);
        Log.d(TAG, "Image URI: " + imageUri.toString());

        String fileName = "closet_" + closetName.toLowerCase().replace(" ", "_") + "_" + System.currentTimeMillis() + ".jpg";
        String bucketName = "clothing";

        Log.d(TAG, "Generated filename: " + fileName);
        Log.d(TAG, "Target bucket: " + bucketName);

        imageUploader.uploadImage(imageUri, bucketName, fileName, new ImageUploader.UploadCallback() {
            @Override
            public void onSuccess(String cloudImageUrl) {
                Log.d(TAG, "✅ Image uploaded successfully to: " + cloudImageUrl);
                // Image uploaded successfully, now save closet with cloud URL
                saveClosetToDatabase(closetName, cloudImageUrl);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Image upload failed: " + error);

                runOnUiThread(() -> {
                    Toast.makeText(CreateClosetActivity.this,
                            "Failed to upload image: " + error + ". Using default image.", Toast.LENGTH_LONG).show();

                    // Fallback: save with default image URL
                    String fallbackImageUrl = "https://via.placeholder.com/300x300/CCCCCC/969696?text=Closet";
                    saveClosetToDatabase(closetName, fallbackImageUrl);

                    Button btnDone = findViewById(R.id.btnDone);
                    btnDone.setEnabled(true);
                    btnDone.setText("Done");
                });
            }
        });
    }

    /**
     * Save closet to database with CLOUD URL and USER ID
     */
    private void saveClosetToDatabase(String closetName, String cloudImageUrl) {
        Log.d(TAG, "Saving closet to database: " + closetName);
        Log.d(TAG, "Using image URL: " + cloudImageUrl);
        Log.d(TAG, "User ID: " + currentUserId);

        // Create closet object for Supabase
        JsonObject closet = new JsonObject();
        closet.addProperty("name", closetName);
        closet.addProperty("image_uri", cloudImageUrl);
        closet.addProperty("user_id", currentUserId); // ✅ CRITICAL: Add user ID
        closet.addProperty("created_at", new java.util.Date().toString());

        Log.d(TAG, "Closet data: " + closet.toString());

        Call<List<JsonObject>> call = supabaseService.insertCloset(closet);
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                Log.d(TAG, "Closet save response code: " + response.code());

                runOnUiThread(() -> {
                    Button btnDone = findViewById(R.id.btnDone);
                    btnDone.setEnabled(true);
                    btnDone.setText("Done");

                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ Closet saved successfully!");
                        Toast.makeText(CreateClosetActivity.this, "Closet saved to cloud!", Toast.LENGTH_SHORT).show();
                        finish(); // Go back to main screen
                    } else {
                        Log.e(TAG, "❌ Closet save failed with code: " + response.code());
                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                Log.e(TAG, "Error response body: " + errorBody);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body: " + e.getMessage());
                        }
                        Toast.makeText(CreateClosetActivity.this, "Failed to save closet", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                Log.e(TAG, "❌ Closet save network error: " + t.getMessage());
                runOnUiThread(() -> {
                    Button btnDone = findViewById(R.id.btnDone);
                    btnDone.setEnabled(true);
                    btnDone.setText("Done");
                    Toast.makeText(CreateClosetActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // Open gallery
    private void openImagePicker() {
        Log.d(TAG, "Opening image picker");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // Handle gallery result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            Log.d(TAG, "Image selected from gallery: " + selectedImageUri.toString());

            // Use Glide to display the image
            Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .into(imageCover);
        } else {
            Log.w(TAG, "Image selection cancelled or failed - request: " + requestCode + ", result: " + resultCode);
        }
    }
}