package com.example.outpick.common;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.outpick.MainActivity;
import com.example.outpick.outfits.OutfitSuggestionActivity;
import com.example.outpick.R;
import com.google.android.material.navigation.NavigationView;

public class FilterActivity extends BaseDrawerActivity {

    private String username;
    private LinearLayout btnPersonalCloset, btnOutfitSuggestion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        // Get username from intent
        username = getIntent().getStringExtra("username");
        if (username == null || username.trim().isEmpty()) {
            // Try to get from BaseDrawerActivity's immutableLoginId as fallback
            username = getImmutableLoginId();
            if (username == null || username.trim().isEmpty()) {
                finish();
                return;
            }
        }

        // Set up drawer - this now uses Supabase via BaseDrawerActivity
        setupDrawer(R.id.drawer_layout, R.id.nav_view);

        // The drawer username is automatically handled by BaseDrawerActivity
        // via updateDrawerHeader() so we don't need to set it manually

        // Top icons
        findViewById(R.id.iconProfile).setOnClickListener(v ->
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.END));

        findViewById(R.id.iconFilter).setOnClickListener(v -> {
            // Already in FilterActivity, so maybe do nothing or refresh?
            // Or consider removing this since we're already in FilterActivity
            refreshActivity();
        });

        // Camera button
        ImageButton cameraButton = findViewById(R.id.btn_camera);
        cameraButton.setOnClickListener(v -> {
            // TODO: Implement camera logic
            // This could open camera for outfit creation or clothing item addition
        });

        // Buttons
        btnPersonalCloset = findViewById(R.id.btn_personal_closet);
        btnOutfitSuggestion = findViewById(R.id.btn_outfit_suggestion);

        // Set default white backgrounds for both
        setButtonActive(btnPersonalCloset);
        setButtonActive(btnOutfitSuggestion);

        btnPersonalCloset.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
            finish(); // Optional: finish this activity to prevent going back
        });

        btnOutfitSuggestion.setOnClickListener(v -> {
            Intent intent = new Intent(this, OutfitSuggestionActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
            finish(); // Optional: finish this activity to prevent going back
        });
    }

    private void setButtonActive(LinearLayout button) {
        Drawable activeBg = ContextCompat.getDrawable(this, R.drawable.btn_border_left);
        button.setBackground(activeBg);
    }

    private void setButtonInactive(LinearLayout button) {
        Drawable inactiveBg = ContextCompat.getDrawable(this, R.color.light_gray);
        button.setBackground(inactiveBg);
    }

    private void refreshActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure drawer header is updated with latest user data from Supabase
        updateDrawerHeader();
    }
}