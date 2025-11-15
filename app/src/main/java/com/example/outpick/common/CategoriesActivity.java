package com.example.outpick.common;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.outpick.MainActivity;
import com.example.outpick.R;
import com.google.android.material.navigation.NavigationView;

public class CategoriesActivity extends BaseDrawerActivity {

    private ImageView backArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        // Setup the navigation drawer
        setupDrawer(R.id.drawer_layout, R.id.nav_view);

        // Get the passed username
        String username = getIntent().getStringExtra("username");

        // Set the username in the drawer header
        if (username != null) {
            NavigationView navView = findViewById(R.id.nav_view);
            View headerView = navView.getHeaderView(0);
            TextView usernameText = headerView.findViewById(R.id.nav_header_username);
            if (usernameText != null) {
                usernameText.setText(username);
            }
        }

        // Open navigation drawer when profile icon is clicked
        findViewById(R.id.iconProfile).setOnClickListener(v ->
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.END));

        // Go to FilterActivity when filter icon is clicked
        findViewById(R.id.iconFilter).setOnClickListener(v -> {
            Intent intent = new Intent(CategoriesActivity.this, FilterActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        // Go to MainActivity when logo is clicked
        findViewById(R.id.imageLogo).setOnClickListener(v -> {
            Intent intent = new Intent(CategoriesActivity.this, MainActivity.class);
            intent.putExtra("username", username);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Prevent back navigation to this screen
        });

        // Handle back arrow (in FrameLayout with centered title)
        backArrow = findViewById(R.id.backArrow);
        if (backArrow != null) {
            backArrow.setOnClickListener(v -> onBackPressed());
        }
    }
}
