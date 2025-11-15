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
import com.example.outpick.utils.FileUtils;

import java.io.InputStream;

public class UploadActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imagePreview;
    private Spinner spinnerCategory;
    private Bitmap selectedImageBitmap = null;
    private ClothingRepository clothingRepository;
    private String selectedImagePath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        // Initialize Supabase - Use the singleton pattern
        SupabaseService supabaseService = SupabaseClient.getService();
        clothingRepository = ClothingRepository.getInstance(supabaseService);

        imagePreview = findViewById(R.id.imagePreview);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        Button btnChooseImage = findViewById(R.id.btnChooseImage);
        Button btnSave = findViewById(R.id.btnSave);

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
            if (selectedImageBitmap == null || selectedImagePath == null) {
                Toast.makeText(this, "Choose an image first", Toast.LENGTH_SHORT).show();
                return;
            }

            String category = spinnerCategory.getSelectedItem().toString();
            saveToSupabase(category);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                // Copy image to internal storage and get path
                selectedImagePath = FileUtils.copyImageToInternalStorage(this, imageUri);

                if (selectedImagePath != null) {
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    selectedImageBitmap = BitmapFactory.decodeStream(inputStream);
                    imagePreview.setImageBitmap(selectedImageBitmap);
                } else {
                    Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveToSupabase(String category) {
        // Generate a name for the clothing item
        String itemName = category + " - " + System.currentTimeMillis();

        // Default values for other fields
        String season = "All-Season";
        String occasion = "Casual";

        // Show saving progress
        Toast.makeText(this, "Saving to cloud...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            boolean success = clothingRepository.addClothingItem(
                    itemName,
                    selectedImagePath, // This should be a URI that's accessible
                    category,
                    season,
                    occasion
            );

            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(UploadActivity.this, "Clothing item saved to cloud!", Toast.LENGTH_SHORT).show();
                    // Clear the form
                    selectedImageBitmap = null;
                    selectedImagePath = null;
                    imagePreview.setImageResource(android.R.color.transparent);
                    spinnerCategory.setSelection(0);
                } else {
                    Toast.makeText(UploadActivity.this, "Failed to save item to cloud", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}