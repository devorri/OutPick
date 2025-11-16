package com.example.outpick.common;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.outpick.R;

import java.io.File;

public class SuggestionPreviewImageActivity extends AppCompatActivity {

    private ImageView ivPreviewImage;
    private TextView tvPreviewName, tvPreviewCategory, tvPreviewEvent, tvPreviewDate, tvPreviewAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestion_preview_image);

        // --- Initialize views ---
        ivPreviewImage = findViewById(R.id.ivPreviewImage);
        tvPreviewName = findViewById(R.id.tvPreviewName);
        tvPreviewCategory = findViewById(R.id.tvPreviewCategory);
        tvPreviewEvent = findViewById(R.id.tvPreviewEvent);
        tvPreviewDate = findViewById(R.id.tvPreviewDate);
        tvPreviewAction = findViewById(R.id.tvPreviewAction);

        // --- Ensure white background for snapshot image ---
        ivPreviewImage.setBackgroundColor(getResources().getColor(android.R.color.white));

        // --- Retrieve Intent data ---
        String imageUri = getIntent().getStringExtra("imageUri");
        String name = getIntent().getStringExtra("name");
        String category = getIntent().getStringExtra("category");
        String event = getIntent().getStringExtra("event");
        String date = getIntent().getStringExtra("date");
        String action = getIntent().getStringExtra("action");

        // --- Set text with fallback values ---
        tvPreviewName.setText(name != null && !name.isEmpty() ? name : "Unnamed Outfit");

        // --- HIDE CATEGORY LOGIC ---
        if (tvPreviewCategory != null) {
            tvPreviewCategory.setText(category != null && !category.isEmpty() ? category : "Unknown Category");
            tvPreviewCategory.setVisibility(View.GONE);
        }

        tvPreviewEvent.setText(event != null && !event.isEmpty() ? event : "Unknown Event");
        tvPreviewDate.setText(date != null && !date.isEmpty() ? date : "Unknown Date");
        tvPreviewAction.setText(action != null && !action.isEmpty() ? action : "Unknown Action");

        // --- Load image with Glide - UPDATED FOR CLOUD ---
        if (imageUri != null && !imageUri.isEmpty()) {
            loadImageWithGlide(imageUri);
        } else {
            ivPreviewImage.setImageResource(R.drawable.placeholder);
        }
    }

    private void loadImageWithGlide(String imageUri) {
        // Check if it's a cloud URL (Supabase Storage URL)
        if (isCloudUrl(imageUri)) {
            // It's a cloud URL - load directly
            Glide.with(this)
                    .load(imageUri) // Direct URL from Supabase Storage
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.error_image)
                    .into(ivPreviewImage);
        }
        // Check if it's a local file path (legacy support)
        else if (imageUri.startsWith("/") || imageUri.startsWith("file://")) {
            File file = new File(imageUri.replace("file://", ""));
            if (file.exists()) {
                Glide.with(this)
                        .load(file)
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.error_image)
                        .into(ivPreviewImage);
            } else {
                ivPreviewImage.setImageResource(R.drawable.error_image);
            }
        }
        // Check if it's a content URI (from gallery/camera)
        else if (imageUri.startsWith("content://")) {
            Glide.with(this)
                    .load(Uri.parse(imageUri))
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.error_image)
                    .into(ivPreviewImage);
        }
        // Check if it's an android resource
        else if (imageUri.startsWith("android.resource://")) {
            Glide.with(this)
                    .load(Uri.parse(imageUri))
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.error_image)
                    .into(ivPreviewImage);
        }
        // Unknown format - try to load as URL (might be cloud URL without https)
        else {
            // Try to load as direct URL
            Glide.with(this)
                    .load(imageUri)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.error_image)
                    .into(ivPreviewImage);
        }
    }

    private boolean isCloudUrl(String imageUri) {
        // Check if it's a Supabase Storage URL or any cloud URL
        return imageUri != null && (
                imageUri.startsWith("https://") ||
                        imageUri.startsWith("http://") ||
                        imageUri.contains("supabase.co/storage") ||
                        imageUri.contains("xaekxlyllgjxneyhurfp.supabase.co") // Your project URL
        );
    }

    // Helper method to extract just the filename for debugging
    private String getFilenameFromUrl(String url) {
        if (url == null) return "null";
        try {
            return url.substring(url.lastIndexOf('/') + 1);
        } catch (Exception e) {
            return url;
        }
    }
}