package com.example.outpick.common;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.outpick.R;
import com.example.outpick.database.repositories.ClothingRepository;
import com.example.outpick.database.supabase.SupabaseClient;
import com.example.outpick.database.supabase.SupabaseService;
import com.example.outpick.utils.ImageUploader;

import java.io.InputStream;

public class UploadActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imagePreview;
    private Spinner spinnerCategory;
    private Bitmap selectedImageBitmap = null;
    private ClothingRepository clothingRepository;
    private Uri selectedImageUri = null;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        // Initialize Supabase
        SupabaseService supabaseService = SupabaseClient.getService();
        clothingRepository = new ClothingRepository(supabaseService); // Use constructor instead of getInstance()

        imagePreview = findViewById(R.id.imagePreview);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        Button btnChooseImage = findViewById(R.id.btnChooseImage);
        btnSave = findViewById(R.id.btnSave);

        // Spinner Options
        String[] categories = {"Top", "Bottom", "Shoes", "Accessory"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);

        // Open gallery
        btnChooseImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        // Save to Supabase
        btnSave.setOnClickListener(v -> {
            if (selectedImageBitmap == null || selectedImageUri == null) {
                Toast.makeText(this, "Choose an image first", Toast.LENGTH_SHORT).show();
                return;
            }

            String category = spinnerCategory.getSelectedItem().toString();
            uploadImageAndSaveToSupabase(category);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                // Take persistable URI permission
                getContentResolver().takePersistableUriPermission(imageUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Load image for preview
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                selectedImageBitmap = BitmapFactory.decodeStream(inputStream);
                imagePreview.setImageBitmap(selectedImageBitmap);
                selectedImageUri = imageUri;

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadImageAndSaveToSupabase(String category) {
        // Show loading state
        btnSave.setEnabled(false);
        btnSave.setText("Uploading...");

        // Generate a unique filename
        String fileName = "clothing_" + category.toLowerCase() + "_" + System.currentTimeMillis() + ".jpg";

        ImageUploader uploader = new ImageUploader(this);
        uploader.uploadImage(selectedImageUri, "clothing", fileName, new ImageUploader.UploadCallback() {
            @Override
            public void onSuccess(String cloudImageUrl) {
                // Image uploaded successfully, now save clothing item with cloud URL
                saveClothingItemToDatabase(category, cloudImageUrl);
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(UploadActivity.this,
                            "Failed to upload image: " + error, Toast.LENGTH_LONG).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                });
            }
        });
    }

    private void saveClothingItemToDatabase(String category, String cloudImageUrl) {
        runOnUiThread(() -> {
            btnSave.setText("Saving...");
        });

        // Generate a name for the clothing item
        String itemName = category + " - " + System.currentTimeMillis();

        // Default values for other fields
        String season = "All-Season";
        String occasion = "Casual";

        new Thread(() -> {
            boolean success = clothingRepository.addClothingItem(
                    itemName,
                    cloudImageUrl, // Use CLOUD URL instead of local path
                    category,
                    season,
                    occasion
            );

            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(UploadActivity.this, "Clothing item saved to cloud!", Toast.LENGTH_SHORT).show();
                    // Clear the form
                    selectedImageBitmap = null;
                    selectedImageUri = null;
                    imagePreview.setImageResource(android.R.color.transparent);
                    spinnerCategory.setSelection(0);
                } else {
                    Toast.makeText(UploadActivity.this, "Failed to save item to cloud", Toast.LENGTH_SHORT).show();
                }
                btnSave.setEnabled(true);
                btnSave.setText("Save");
            });
        }).start();
    }
}