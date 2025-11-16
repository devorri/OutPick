package com.example.outpick.closet;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

    private ImageView imageCover;
    private EditText editClosetName;
    private Uri selectedImageUri = null;

    private SupabaseService supabaseService;
    private ImageUploader imageUploader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_closet);

        // Initialize Supabase and ImageUploader
        supabaseService = SupabaseClient.getService();
        imageUploader = new ImageUploader(this);

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

            // Check for duplicate closet name in Supabase
            checkClosetNameExists(closetName);
        });
    }

    private void checkClosetNameExists(String closetName) {
        // First check if closet name already exists
        Call<List<JsonObject>> call = supabaseService.getClosets();
        call.enqueue(new Callback<List<JsonObject>>() {
            @Override
            public void onResponse(Call<List<JsonObject>> call, Response<List<JsonObject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (JsonObject closet : response.body()) {
                        if (closet.has("name") && closet.get("name").getAsString().equalsIgnoreCase(closetName)) {
                            Toast.makeText(CreateClosetActivity.this, "A closet with this name already exists!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    // No duplicate found, proceed to save
                    saveClosetToSupabase(closetName);
                } else {
                    // If we can't check, still try to save (let Supabase handle unique constraint)
                    saveClosetToSupabase(closetName);
                }
            }

            @Override
            public void onFailure(Call<List<JsonObject>> call, Throwable t) {
                // If check fails, still try to save
                saveClosetToSupabase(closetName);
            }
        });
    }

    private void saveClosetToSupabase(String closetName) {
        // Show loading
        Toast.makeText(this, "Creating closet...", Toast.LENGTH_SHORT).show();
        Button btnDone = findViewById(R.id.btnDone);
        btnDone.setEnabled(false);
        btnDone.setText("Creating...");

        if (selectedImageUri != null) {
            // ✅ FIXED: Upload image to cloud storage first
            uploadImageAndSaveCloset(closetName, selectedImageUri);
        } else {
            // Use default image (you might want to upload a default image to cloud too)
            String defaultImageUrl = "https://your-project.supabase.co/storage/v1/object/public/closets/default_closet.jpg";
            saveClosetToDatabase(closetName, defaultImageUrl);
        }
    }

    /**
     * ✅ FIXED: Upload image to Supabase Storage, then save closet with cloud URL
     */
    private void uploadImageAndSaveCloset(String closetName, Uri imageUri) {
        String fileName = "closet_" + closetName.toLowerCase().replace(" ", "_") + "_" + System.currentTimeMillis() + ".jpg";

        imageUploader.uploadImage(imageUri, "closets", fileName, new ImageUploader.UploadCallback() {
            @Override
            public void onSuccess(String cloudImageUrl) {
                // Image uploaded successfully, now save closet with cloud URL
                saveClosetToDatabase(closetName, cloudImageUrl);
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(CreateClosetActivity.this,
                            "Failed to upload image: " + error + ". Using default image.", Toast.LENGTH_LONG).show();

                    // Fallback: save with default image URL
                    String defaultImageUrl = "https://your-project.supabase.co/storage/v1/object/public/closets/default_closet.jpg";
                    saveClosetToDatabase(closetName, defaultImageUrl);

                    Button btnDone = findViewById(R.id.btnDone);
                    btnDone.setEnabled(true);
                    btnDone.setText("Done");
                });
            }
        });
    }

    /**
     * ✅ FIXED: Save closet to database with CLOUD URL
     */
    private void saveClosetToDatabase(String closetName, String cloudImageUrl) {
        // Create closet object for Supabase
        JsonObject closet = new JsonObject();
        closet.addProperty("name", closetName);
        closet.addProperty("image_uri", cloudImageUrl); // ✅ Now storing CLOUD URL
        closet.addProperty("created_at", new java.util.Date().toString());

        // Save to Supabase
        Call<JsonObject> call = supabaseService.insertCloset(closet);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                runOnUiThread(() -> {
                    Button btnDone = findViewById(R.id.btnDone);
                    btnDone.setEnabled(true);
                    btnDone.setText("Done");

                    if (response.isSuccessful()) {
                        Toast.makeText(CreateClosetActivity.this, "Closet saved to cloud!", Toast.LENGTH_SHORT).show();
                        finish(); // Go back to main screen
                    } else {
                        Toast.makeText(CreateClosetActivity.this, "Failed to save closet", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
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
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // Handle gallery result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();

            // ✅ FIXED: Use Glide to display the image (handles both local and cloud URIs)
            Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .into(imageCover);
        }
    }
}