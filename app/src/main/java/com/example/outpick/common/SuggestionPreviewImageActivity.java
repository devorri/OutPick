package com.example.outpick.common;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View; // Import View for View.GONE

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.outpick.R;

import java.io.File;

public class SuggestionPreviewImageActivity extends AppCompatActivity {

    private ImageView ivPreviewImage;
    // Keep reference to category view for initialization and hiding
    private TextView tvPreviewName, tvPreviewCategory, tvPreviewEvent, tvPreviewDate, tvPreviewAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestion_preview_image);

        // --- Initialize views ---
        ivPreviewImage = findViewById(R.id.ivPreviewImage);
        tvPreviewName = findViewById(R.id.tvPreviewName);

        // Initialize tvPreviewCategory (Safety step: must be initialized if present in XML)
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
            // Set the text, but then hide the entire TextView container
            tvPreviewCategory.setText(category != null && !category.isEmpty() ? category : "Unknown Category");
            tvPreviewCategory.setVisibility(View.GONE); // Crucial line to hide the view
        }

        tvPreviewEvent.setText(event != null && !event.isEmpty() ? event : "Unknown Event");
        tvPreviewDate.setText(date != null && !date.isEmpty() ? date : "Unknown Date");
        tvPreviewAction.setText(action != null && !action.isEmpty() ? action : "Unknown Action");

        // --- Load image with Glide ---
        if (imageUri != null && !imageUri.isEmpty()) {
            Uri uriToLoad;

            if (imageUri.startsWith("content://") ||
                    imageUri.startsWith("file://") ||
                    imageUri.startsWith("android.resource://")) {
                uriToLoad = Uri.parse(imageUri);
            } else {
                File file = new File(imageUri);
                uriToLoad = Uri.fromFile(file);
            }

            Glide.with(this)
                    .load(uriToLoad)
                    .placeholder(R.drawable.placeholder)   // Ensure drawable exists
                    .error(R.drawable.error_image)         // Fallback image
                    .into(ivPreviewImage);
        } else {
            ivPreviewImage.setImageResource(R.drawable.placeholder);
        }
    }
}
